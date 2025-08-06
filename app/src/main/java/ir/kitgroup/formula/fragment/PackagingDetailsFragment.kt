package ir.kitgroup.formula.fragment


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
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
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
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.formatDateShamsi
import ir.kitgroup.formula.Util.formatQuantity
import ir.kitgroup.formula.adapter.PackageDetailAdapter
import ir.kitgroup.formula.database.entity.PackagingDetail
import ir.kitgroup.formula.databinding.FragmentPackagingDetailsBinding
import ir.kitgroup.formula.viewmodel.PackagingViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class PackagingDetailsFragment : Fragment() {

    private var _binding: FragmentPackagingDetailsBinding? = null
    private val packagingViewModel: PackagingViewModel by viewModels()
    private lateinit var packageDetailAdapter: PackageDetailAdapter
    private var packagingDetail: List<PackagingDetail>? = null
    private val args: PackagingDetailsFragmentArgs by navArgs()
    private val formatter = DecimalFormat("#,###,###,###")
    private val formatterQuantity = DecimalFormat("###,##0.###")
    private var productNamePdf: String = ""
    private var displayDateTime: String = ""
    private var productDescription: String = ""
    private var packageWeight: Double = 0.0
    private var productDate: Long = 0
    private var productId: Int = 0
    private var productName: String = ""
    private var totalPrice: Double = 0.0
    private var price: Double = 0.0
    private var formatTotalQuantity: String = ""
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)).apply {
            visibility = View.GONE
        }
        (requireActivity().findViewById<Toolbar>(R.id.toolbar)).apply {
            visibility = View.GONE
        }
        _binding = FragmentPackagingDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        rxBinding()
        initAdapter()
        setupObservers()
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun init() {
        productId = args.packageId
        productName = args.packageName
        productDate = args.packageDate
        productDescription = args.packageDescription

        val jalaliDate = JalaliCalendar()
        val dateFormatted =
            String.format("%02d-%02d-%04d", jalaliDate.day, jalaliDate.month, jalaliDate.year)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = timeFormat.format(Date())
        displayDateTime = "$dateFormatted ، $time"
        productNamePdf = "${productName}_$displayDateTime"

        binding.tvProductName.text = productName
        binding.tvProductDate.text = formatDateShamsi(productDate)

        packageWeight = args.packageWeight.toDoubleOrNull() ?: 0.0
        binding.tvPackagingWeight.text = "${formatterQuantity.format(packageWeight)} گرم"

        if (productDescription.isEmpty()) {
            binding.tvProductDescription.visibility = View.GONE
            binding.tvTitleProductDescription.visibility = View.GONE
        } else {
            binding.tvProductDescription.text = productDescription
            binding.tvTitleProductDescription.visibility = View.VISIBLE
            binding.tvProductDescription.visibility = View.VISIBLE
        }
    }

    private fun initAdapter() {
        packageDetailAdapter = PackageDetailAdapter(packagingViewModel)
        binding.rvMaterials.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMaterials.adapter = packageDetailAdapter
    }

    private fun rxBinding() {
        binding.ivPdf.setOnClickListener {
            generateListPDF(requireContext(), packagingDetail!!)
        }
        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupObservers() {
        packagingViewModel.getPackageDetails(args.packageId)
            .observe(viewLifecycleOwner) { details ->
                packagingDetail = details
                packageDetailAdapter.submitList(details)

                val totalQuantity = details.sumOf { it.quantity }
                totalPrice = details.sumOf { it.quantity * it.materialPrice }
                price = details.sumOf { it.materialPrice }
                formatTotalQuantity = formatQuantity(totalQuantity)
                binding.tvTotalQuantity.text = formatTotalQuantity
                binding.tvTotalPrice.text = formatter.format(totalPrice)
                binding.tvPrice.text = formatter.format(price)
            }
    }

    private fun generateListPDF(context: Context, items: List<PackagingDetail>) {

        val productDetailsMap = mutableMapOf<Int, List<PackagingDetail>>()

        val latch = CountDownLatch(items.size)

        items.forEach { item ->
            packagingViewModel.getPackageDetails(args.packageId)
                .observeForever(object : Observer<List<PackagingDetail>> {
                    override fun onChanged(value: List<PackagingDetail>) {
                        productDetailsMap[item.materialId] = value
                        latch.countDown()
                        packagingViewModel.getPackageDetails(args.packageId).removeObserver(this)
                    }
                })
        }

        Thread {
            latch.await(3, TimeUnit.SECONDS)

            Handler(Looper.getMainLooper()).post {
                generatePdfWithData(context, items)
            }
        }.start()
    }

    private fun generatePdfWithData(
        context: Context,
        items: List<PackagingDetail>,
    ) {
        try {
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

            // اضافه کردن عنوان گزارش و نام
            val headerText =
                context.getString(R.string.label_report_packaging_detail)

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

            val productCell = PdfPCell(Phrase("نام بسته بندی : $productName", farsiFontBold14))
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

            val createWightTable = PdfPTable(1)
            createWightTable.widthPercentage = 100f
            createWightTable.horizontalAlignment = Element.ALIGN_RIGHT
            val formattedQuantity ="${formatterQuantity.format(packageWeight)} گرم"

            val createWight =
                PdfPCell(Phrase("وزن بسته بندی: $formattedQuantity", farsiFontBold14))
            createWight.horizontalAlignment = Element.ALIGN_LEFT
            createWight.runDirection = PdfWriter.RUN_DIRECTION_RTL
            createWight.border = Rectangle.NO_BORDER
            createWight.setPadding(10f)
            createWightTable.addCell(createWight)

            document.add(headerTable)
            infoTable.addCell(dateCell)
            infoTable.addCell(productCell)
            document.add(infoTable)
            document.add(createDateTable)
            document.add(createWightTable)
            document.add(Paragraph("\n"))

            val materialColor = ContextCompat.getColor(context, R.color.color_light_green)
            val headerColor = ContextCompat.getColor(context, R.color.colorAccent)
            val footerColor = ContextCompat.getColor(context, R.color.white)

            // تبدیل رنگ‌ها به BaseColor (RGB)
            val materialColorBase = BaseColor(
                Color.red(materialColor),
                Color.green(materialColor),
                Color.blue(materialColor)
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
                    context.getString(R.string.label_quantity_number_unit),
                    farsiFont,
                    headerColorBase
                )
            )

            table.addCell(
                createCell(
                    context.getString(R.string.label_price_packaging_unit),
                    farsiFont,
                    headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_price_total_unit), farsiFont, headerColorBase
                )
            )

            for (i in items.indices) {

                val item: PackagingDetail = items[i]

                val rowText = (i + 1).toString() + ". " + item.materialName
                val cellName = createCell(rowText, farsiFont, materialColorBase)
                cellName.runDirection = PdfWriter.RUN_DIRECTION_RTL
                cellName.horizontalAlignment = Element.ALIGN_LEFT

                val formattedQuantity = formatQuantity(item.quantity)

                val cellQuantity = createCell(
                    formattedQuantity, farsiFont, materialColorBase
                )

                table.addCell(cellName)
                table.addCell(cellQuantity)

                val cellPriceKg = createCell(
                    formatter.format(item.materialPrice), farsiFont, materialColorBase
                )
                val cellPrice = createCell(
                    formatter.format(
                        item.quantity * item.materialPrice
                    ), farsiFont, materialColorBase
                )
                table.addCell(cellPriceKg)
                table.addCell(cellPrice)
            }

            // افزودن ردیف جمع کل به انتهای جدول
            val totalText = "جمع کل (" + items.size + " ردیف)"
            table.addCell(createCell(totalText, farsiFontBold14, footerColorBase))

            table.addCell(
                createCell(
                    formatTotalQuantity, farsiFontBold14, footerColorBase
                )
            )
            table.addCell(
                createCell(
                    formatter.format(
                        price
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}