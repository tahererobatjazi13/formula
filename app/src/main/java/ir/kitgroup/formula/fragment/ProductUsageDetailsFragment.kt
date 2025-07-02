package ir.kitgroup.formula.fragment


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.formatDateShamsi
import ir.kitgroup.formula.Util.formatQuantity
import ir.kitgroup.formula.adapter.ProductUsageDetailAdapter
import ir.kitgroup.formula.database.entity.ProductDetail
import ir.kitgroup.formula.databinding.FragmentProductUsageDetailsBinding
import ir.kitgroup.formula.viewmodel.ProductViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ProductUsageDetailsFragment : Fragment() {

    private var _binding: FragmentProductUsageDetailsBinding? = null
    private val productViewModel: ProductViewModel by viewModels()
    private lateinit var productUsageDetailAdapter: ProductUsageDetailAdapter
    private var productDetail: List<ProductDetail>? = null
    private val args: ProductUsageDetailsFragmentArgs by navArgs()
    private val formatter = DecimalFormat("#,###,###,###")
    private val formatterQuantity = DecimalFormat("###,##0.###")
    private var productNamePdf: String = ""
    private var displayDateTime: String = ""
    private var productName: String = ""
    private var formattedQty: String = ""
    private var productDate: Long = 0
    private var totalPriceKilograms: Double = 0.0
    private var qty: Double = 0.0
    private var multipliedQuantity: Double = 0.0
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        hideUIElements()

        _binding = FragmentProductUsageDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        rxBinding()
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun init() {
        val id = args.id
        val productId = args.productId
        productName = args.productName
        productDate = args.productDate
        formattedQty = args.formattedQty

        val jalaliDate = JalaliCalendar()
        val dateFormatted =
            String.format("%02d-%02d-%04d", jalaliDate.day, jalaliDate.month, jalaliDate.year)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = timeFormat.format(Date())
        displayDateTime = "$dateFormatted ، $time"
        productNamePdf = "${productName}_$displayDateTime"

        binding.tvProductName.text = productName
        binding.tvProductDate.text = formatDateShamsi(productDate)

        totalPriceKilograms = args.totalPrice.toDoubleOrNull() ?: 0.0
        qty = args.formattedQty.toDoubleOrNull() ?: 0.0
        multipliedQuantity = if (args.type == 1) {
            qty * 1000
        } else {
            qty
        }
        binding.tvAmount.text = "${formatterQuantity.format(multipliedQuantity)} گرم"

        binding.tvTotalQuantity.text = formatterQuantity.format(multipliedQuantity) + ""
        binding.tvPriceKilograms.text = formatter.format(totalPriceKilograms)

        productViewModel.loadProductHistoryById(id)

        productUsageDetailAdapter = ProductUsageDetailAdapter { type, id, name, qty, price ->
            val action = ProductUsageDetailsFragmentDirections
                .actionProductUsageDetailsFragmentSelf(
                    2,
                    args.id,
                    id,
                    name,
                    args.productDate,
                    qty,
                    price
                )
            findNavController().navigate(action)
        }

        binding.rvMaterials.adapter = productUsageDetailAdapter
        binding.rvMaterials.layoutManager = LinearLayoutManager(requireContext())

        productViewModel.processedDetails.observe(viewLifecycleOwner) {
            productDetail = it
            productUsageDetailAdapter.submitList(it)
        }

        productViewModel.loadProcessedDetails(
            type = args.type,
            productId = productId,
            formattedQty = args.formattedQty.toDoubleOrNull() ?: 0.0
        )
    }

    private fun rxBinding() {
        binding.ivPdf.setOnClickListener {
            preparePdfData(productDetail!!) { rowList ->
                generatePdfWithData(requireContext(), rowList)
            }

        }
        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    data class PdfRowData(
        val name: String,
        val quantity: Double,
        val price: Double,
        val type: Int      // 0 = material, 1 = product
    )

    private fun preparePdfData(
        items: List<ProductDetail>,
        onReady: (List<PdfRowData>) -> Unit
    ) {
        val resultList = mutableListOf<PdfRowData>()
        var counter = 0

        for (material in items) {

            productViewModel.productHistory.observeForever { _ ->
                resultList.add(
                    PdfRowData(
                        name = material.materialName,
                        quantity = material.quantity,
                        price = material.price,
                        type = material.type
                    )
                )
                counter++
                if (counter == items.size) {
                    onReady(resultList)
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun generatePdfWithData(context: Context, items: List<PdfRowData>) {

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
            val farsiFont = Font(baseFont, 16f, Font.NORMAL)
            val farsiFontBold14 = Font(baseFont, 18f, Font.BOLD)
            val farsiFontBold18 = Font(baseFont, 24f, Font.BOLD, BaseColor.BLACK)

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

            val createNameTable = PdfPTable(1)
            createNameTable.widthPercentage = 100f
            createNameTable.spacingBefore = 10f

            val productCell = PdfPCell(Phrase("نام محصول : $productName", farsiFontBold14))
            productCell.horizontalAlignment = Element.ALIGN_LEFT
            productCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            productCell.border = Rectangle.NO_BORDER
            productCell.setPadding(10f)
            createNameTable.addCell(productCell)

            val createTimeDateTable = PdfPTable(1)
            createTimeDateTable.widthPercentage = 100f

            val dateCell =
                PdfPCell(Phrase("تاریخ و ساعت گزارش : $displayDateTime", farsiFontBold14))
            dateCell.horizontalAlignment = Element.ALIGN_LEFT
            dateCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            dateCell.border = Rectangle.NO_BORDER
            dateCell.setPadding(10f)
            createTimeDateTable.addCell(dateCell)

            val createDateTable = PdfPTable(1)
            createDateTable.widthPercentage = 100f
            createDateTable.horizontalAlignment = Element.ALIGN_LEFT

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

            val createQuantityTable = PdfPTable(1)
            createQuantityTable.widthPercentage = 100f
            createQuantityTable.horizontalAlignment = Element.ALIGN_RIGHT

            val roundedCell = multipliedQuantity
            val formattedCell = formatterQuantity.format(roundedCell)

            val createQuantityCell =
                PdfPCell(
                    Phrase(
                        "مقادیر مواد برای تولید $formattedCell گرم از محصول ",
                        farsiFontBold14
                    )
                )
            createQuantityCell.horizontalAlignment = Element.ALIGN_LEFT
            createQuantityCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            createQuantityCell.border = Rectangle.NO_BORDER
            createQuantityCell.setPadding(10f)
            createQuantityTable.addCell(createQuantityCell)

            document.add(headerTable)
            document.add(createNameTable)
            document.add(createTimeDateTable)
            document.add(createDateTable)
            document.add(createQuantityTable)
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
            val table = PdfPTable(3)
            table.widthPercentage = 100f
            table.runDirection = PdfWriter.RUN_DIRECTION_RTL
            val columnWidths = floatArrayOf(3f, 3f, 3f)
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
                    context.getString(R.string.label_price_kg), farsiFont, headerColorBase
                )
            )

            for (i in items.indices) {

                val item: PdfRowData = items[i]
                val rowColor = if ((item.type == 0)) materialColorBase else productColorBase

                val rowText = (i + 1).toString() + ". " + item.name
                val cellName = createCell(rowText, farsiFont, rowColor)
                cellName.runDirection = PdfWriter.RUN_DIRECTION_RTL
                cellName.horizontalAlignment = Element.ALIGN_LEFT

                val formattedCell = formatQuantity(item.quantity)

                val cellQuantity = createCell(
                    formattedCell, farsiFont, rowColor
                )

                table.addCell(cellName)
                table.addCell(cellQuantity)

                val cellPriceKg = createCell(
                    formatter.format(item.price), farsiFont, rowColor
                )
                table.addCell(cellPriceKg)
            }


            // افزودن ردیف جمع کل به انتهای جدول
            val totalText = "جمع کل (" + items.size + " ردیف)"
            table.addCell(createCell(totalText, farsiFontBold14, footerColorBase))

            val rounded = multipliedQuantity
            val formatted = formatterQuantity.format(rounded)

            table.addCell(
                createCell(formatted, farsiFontBold14, footerColorBase)
            )

            table.addCell(
                createCell(
                    formatter.format(
                        totalPriceKilograms
                    ), farsiFontBold14, footerColorBase
                )
            )

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

    private fun hideUIElements() {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility =
            View.GONE
        requireActivity().findViewById<Toolbar>(R.id.toolbar)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}