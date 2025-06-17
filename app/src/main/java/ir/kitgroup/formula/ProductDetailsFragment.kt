package ir.kitgroup.formula


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import ir.huri.jcal.JalaliCalendar
import ir.kitgroup.formula.Util.calculatePrice
import ir.kitgroup.formula.Util.calculatePricePerKg
import ir.kitgroup.formula.Util.formatDateShamsi
import ir.kitgroup.formula.Util.formatQuantity
import ir.kitgroup.formula.adapter.ProductDetailAdapter
import ir.kitgroup.formula.adapter.getTotalPriceForProduct
import ir.kitgroup.formula.adapter.getTotalQuantityForProduct
import ir.kitgroup.formula.database.entity.ProductDetail
import ir.kitgroup.formula.databinding.FragmentProductDetailsBinding
import ir.kitgroup.formula.viewmodel.ProductViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class ProductDetailsFragment : Fragment() {

    private var _binding: FragmentProductDetailsBinding? = null
    private val args: ProductDetailsFragmentArgs by navArgs()

    private lateinit var productDetailAdapter: ProductDetailAdapter
    private val formatter = DecimalFormat("#,###,###,###")
    private var productNamePdf: String = ""
    private var displayDateTime: String = ""
    private var productName: String = ""
    private var productDate: Long = 0
    private var formattedTotal: String = ""
    private var priceKilograms: Double = 0.0
    private var totalPrice: Double = 0.0

    private val binding get() = _binding!!
    private val productViewModel: ProductViewModel by viewModels()
    private var productDetail: List<ProductDetail>? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        rxBinding()
    }

    @SuppressLint("DefaultLocale")
    private fun init() {
        val productId = args.productId
        productName = args.productName
        productDate = args.productDate
        val productDescription = args.productDescription

        val jalaliDate = JalaliCalendar()
        val dateFormatted =
            String.format("%02d-%02d-%04d", jalaliDate.day, jalaliDate.month, jalaliDate.year)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = timeFormat.format(Date())
        displayDateTime = "$dateFormatted ، $time"
        productNamePdf = "${productName}_$displayDateTime"

        (requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)).apply {
            visibility = View.GONE
        }

        binding.tvProductName.text = productName
        binding.tvProductDate.text = formatDateShamsi(productDate)

        if (productDescription.isEmpty()) {
            binding.tvProductDescription.visibility = View.GONE
            binding.tvTitleProductDescription.visibility = View.GONE
        } else {
            binding.tvProductDescription.text = productDescription
            binding.tvTitleProductDescription.visibility = View.VISIBLE
            binding.tvProductDescription.visibility = View.VISIBLE
        }

        productDetailAdapter = ProductDetailAdapter(
            onClick = { product ->
                val action =
                    ProductDetailsFragmentDirections.actionProductDetailsFragmentSelf(
                        product,
                        productName,
                        productDate,
                        productDescription,
                    )
                findNavController().navigate(action)
            }, viewModel = productViewModel
        )

        binding.rvMaterials.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMaterials.adapter = productDetailAdapter

        productViewModel.getProductDetails(productId).observe(viewLifecycleOwner) { details ->
            productDetail = details
            productDetailAdapter.submitList(details)

            val (materials, products) = details.partition { it.type == 0 }

            // محاسبه قیمت و مقدار مواد اولیه
            val totalPriceMaterial =
                materials.sumOf { calculatePrice(it.quantity, it.materialPrice) }
            val totalQuantityMaterial = materials.sumOf { it.quantity }

            var totalPriceProduct = 0.0
            var counter = 0
            val expectedCount = products.size

            // اگر محصولی نداشتیم، مستقیماً جمع نهایی را نمایش بده
            if (expectedCount == 0) {
                updateTotalUI(
                    totalPriceMaterial,
                    totalQuantityMaterial,
                    0.0,
                    0.0
                )
                return@observe
            }

            // برای هر محصول، قیمت جزئی را به دست می‌آوریم
            for (product in products) {
                productViewModel.getProductDetails(product.materialId)
                    .observeForever { productDetails ->

                        val unitPrice = calculatePricePerKg(
                            getTotalQuantityForProduct(productDetails),
                            getTotalPriceForProduct(productDetails)
                        )

                        totalPriceProduct += calculatePrice(product.quantity, unitPrice)
                        counter++

                        // زمانی که همه محصولات پردازش شدند، جمع کل را نمایش بده
                        if (counter == expectedCount) {
                            val totalQuantityProduct = products.sumOf { it.quantity }

                            updateTotalUI(
                                totalPriceMaterial,
                                totalQuantityMaterial,
                                totalPriceProduct,
                                totalQuantityProduct
                            )
                        }
                    }
            }
        }
    }

    private fun rxBinding() {
        binding.ivPdf.setOnClickListener {
            generateListPDF(requireContext(), productDetail!!)
        }
    }

    private fun generateListPDF(context: Context, items: List<ProductDetail>) {
        val productItems = items.filter { it.type == 1 }

        // گرفتن همه‌ی اطلاعات مورد نیاز محصولات
        val productDetailsMap = mutableMapOf<Int, List<ProductDetail>>()

        val latch = CountDownLatch(productItems.size)

        productItems.forEach { item ->
            productViewModel.getProductDetails(item.materialId)
                .observeForever(object : Observer<List<ProductDetail>> {
                    override fun onChanged(value: List<ProductDetail>) {
                        productDetailsMap[item.materialId] = value
                        latch.countDown()
                        productViewModel.getProductDetails(item.materialId).removeObserver(this)
                    }
                })
        }

        // منتظر می‌مونیم تا همه‌ی دیتا بیاد (با Timeout مناسب مثلاً 3 ثانیه)
        Thread {
            latch.await(3, TimeUnit.SECONDS)

            // حالا ساخت PDF بعد از دریافت دیتا
            Handler(Looper.getMainLooper()).post {
                generatePdfWithData(context, items, productDetailsMap)
            }
        }.start()
    }

    private fun updateTotalUI(
        totalPriceMaterial: Double,
        totalQuantityMaterial: Double,
        totalPriceProduct: Double,
        totalQuantityProduct: Double
    ) {
        totalPrice = totalPriceMaterial + totalPriceProduct
        val totalQuantity = totalQuantityMaterial + totalQuantityProduct

        formattedTotal = if (totalQuantity % 1 == 0.0) {
            totalQuantity.toInt().toString()
        } else {
            val decimalPart = totalQuantity.toString().split(".").getOrNull(1) ?: ""
            val decimalPlaces = decimalPart.length.coerceAtMost(4)
            String.format("%.${decimalPlaces}f", totalQuantity)
        }
        binding.tvTotalQuantity.text = formattedTotal

        binding.tvTotalPrice.text = formatter.format(totalPrice)

        priceKilograms = calculatePricePerKg(totalQuantity, totalPrice)
        binding.tvPriceKilograms.text = formatter.format(priceKilograms)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)).apply {
            visibility = View.VISIBLE
        }
    }

    private fun generatePdfWithData(
        context: Context,
        items: List<ProductDetail>,
        productDetailsMap: Map<Int, List<ProductDetail>>
    ) {
        try {

            // محل ذخیره PDF
            val pdfFile =
                File(context.getExternalFilesDir(null), "${productNamePdf}.pdf")
            val fos = FileOutputStream(pdfFile)
            val document = Document(PageSize.A4, 15f, 15f, 15f, 15f)
            PdfWriter.getInstance(document, fos)
            document.open()

            val baseFont = BaseFont.createFont(
                "assets/fonts/yekan.ttf",
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED
            )
            val farsiFont = Font(baseFont, 12f, Font.NORMAL)
            val farsiFontBold14 = Font(baseFont, 14f, Font.BOLD)
            val farsiFontBold18 = Font(baseFont, 20f, Font.BOLD, BaseColor.BLACK)

            // اضافه کردن عنوان گزارش و نام محصول
            val headerText =
                context.getString(R.string.label_report_product)

            val headerTable = PdfPTable(1)
            headerTable.widthPercentage = 100f

            // ایجاد سلول برای عنوان گزارش
            val headerCell = PdfPCell(Phrase(headerText, farsiFontBold18))
            headerCell.horizontalAlignment = Element.ALIGN_CENTER
            headerCell.runDirection = PdfWriter.RUN_DIRECTION_LTR
            headerCell.border = Rectangle.NO_BORDER
            headerCell.setPadding(10f)
            headerTable.addCell(headerCell)

            val dateCell =
                PdfPCell(Phrase("تاریخ و ساعت گزارش : $displayDateTime", farsiFontBold14))
            dateCell.horizontalAlignment = Element.ALIGN_RIGHT
            dateCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            dateCell.border = Rectangle.NO_BORDER
            dateCell.setPadding(10f)

            val productCell = PdfPCell(Phrase("نام محصول : $productName", farsiFontBold14))
            productCell.horizontalAlignment = Element.ALIGN_LEFT
            productCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            productCell.border = Rectangle.NO_BORDER
            productCell.setPadding(10f)

            val infoTable = PdfPTable(2)
            infoTable.widthPercentage = 100f
            infoTable.spacingBefore = 10f

            val createDateTable = PdfPTable(1)
            createDateTable.widthPercentage = 100f
            createDateTable.horizontalAlignment = Element.ALIGN_RIGHT

            val input = formatDateShamsi(productDate)
            val parts = input.split(" - ")
            val dateParts = parts[0].split("/")
            val reversedDate = "${dateParts[2]}/${dateParts[1]}/${dateParts[0]}"
            val finalResult = "$reversedDate - ${parts[1]}"

            val createDateCell =
                PdfPCell(Phrase("آخرین تاریخ آپدیت: $finalResult", farsiFontBold14))
            createDateCell.horizontalAlignment = Element.ALIGN_LEFT
            createDateCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            createDateCell.border = Rectangle.NO_BORDER
            createDateCell.setPadding(10f)
            createDateTable.addCell(createDateCell)

            document.add(headerTable)
            infoTable.addCell(dateCell)
            infoTable.addCell(productCell)
            document.add(infoTable)
            document.add(createDateTable)
            document.add(Paragraph("\n"))

            val materialColor = ContextCompat.getColor(context, R.color.color_light_green)
            val productColor = ContextCompat.getColor(context, R.color.color_light_pink)
            val headerColor = ContextCompat.getColor(context, R.color.colorAccent)
            val footerColor = ContextCompat.getColor(context, R.color.white)

            // تبدیل رنگ‌ها به BaseColor (RGB)
            val materialColorBase = BaseColor(
                Color.red(materialColor),
                Color.green(materialColor),
                Color.blue(materialColor)
            )
            val productColorBase = BaseColor(
                Color.red(productColor),
                Color.green(productColor),
                Color.blue(productColor)
            )
            val headerColorBase =
                BaseColor(Color.red(headerColor), Color.green(headerColor), Color.blue(headerColor))
            val footerColorBase =
                BaseColor(Color.red(footerColor), Color.green(footerColor), Color.blue(footerColor))

            // اضافه کردن جدول داده‌ها
            val table = PdfPTable(4)
            table.widthPercentage = 100f
            table.runDirection = PdfWriter.RUN_DIRECTION_RTL
            val columnWidths = floatArrayOf(3f, 3f, 1.5f, 3f)
            table.setWidths(columnWidths)

            // اضافه کردن هدر جدول
            table.addCell(
                createCell(
                    context.getString(R.string.label_name),
                    farsiFont,
                    headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_quantity_unit),
                    farsiFont,
                    headerColorBase
                )
            )

            table.addCell(
                createCell(
                    context.getString(R.string.label_price_unit), farsiFont, headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_price_total_unit), farsiFont, headerColorBase
                )
            )

            for (i in items.indices) {

                val item: ProductDetail = items[i]
                val rowColor = if ((item.type == 0)) materialColorBase else productColorBase

                val rowText = (i + 1).toString() + ". " + item.materialName
                val cellName = createCell(rowText, farsiFont, rowColor)
                cellName.runDirection = PdfWriter.RUN_DIRECTION_RTL
                cellName.horizontalAlignment = Element.ALIGN_LEFT

                val formattedQuantity = formatQuantity(item.quantity)

                val cellQuantity = createCell(
                    formattedQuantity, farsiFont, rowColor
                )

                table.addCell(cellName)
                table.addCell(cellQuantity)

                if (item.type == 1) {

                    val details = productDetailsMap[item.materialId] ?: emptyList()
                    val unitPriceProduct = calculatePricePerKg(
                        getTotalQuantityForProduct(details),
                        getTotalPriceForProduct(details)
                    )

                    table.addCell(
                        createCell(
                            formatter.format(unitPriceProduct),
                            farsiFont,
                            rowColor
                        )
                    )
                    table.addCell(
                        createCell(
                            formatter.format(calculatePrice(item.quantity, unitPriceProduct)),
                            farsiFont,
                            rowColor
                        )
                    )
                } else {

                    val cellPriceKg = createCell(
                        formatter.format(item.materialPrice), farsiFont, rowColor
                    )
                    val cellPrice = createCell(
                        formatter.format(
                            calculatePrice(item.quantity, item.materialPrice)
                        ), farsiFont, rowColor
                    )
                    table.addCell(cellPriceKg)
                    table.addCell(cellPrice)
                }
            }

            // افزودن ردیف جمع کل به انتهای جدول
            val totalText = "جمع کل (" + items.size + " ردیف)"
            table.addCell(createCell(totalText, farsiFontBold14, footerColorBase))

            table.addCell(
                createCell(
                    formattedTotal, farsiFontBold14, footerColorBase
                )
            )
            table.addCell(
                createCell(
                    formatter.format(
                        priceKilograms
                    ), farsiFontBold14, footerColorBase
                )
            )
            table.addCell(
                createCell(
                    formatter.format(
                        totalPrice
                    ), farsiFontBold14, footerColorBase
                )
            )
            table.addCell(createCell("", farsiFontBold14, footerColorBase))
            table.addCell(createCell("", farsiFontBold14, footerColorBase))
            table.addCell(createCell("", farsiFontBold14, footerColorBase))
            document.add(table)

            document.close()
            openPDF(context, pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createCell(text: String, font: Font, backgroundColor: BaseColor): PdfPCell {
        val cell = PdfPCell(Phrase(text, font))
        cell.horizontalAlignment = Element.ALIGN_CENTER
        cell.runDirection = PdfWriter.RUN_DIRECTION_RTL
        cell.backgroundColor = backgroundColor
        cell.setPadding(14f)
        return cell
    }


    private fun openPDF(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "ir.kitgroup.formula.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }
}