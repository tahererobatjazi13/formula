package ir.kitgroup.formulaNew.fragment


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Chunk
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import ir.huri.jcal.JalaliCalendar
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.adapter.PackagingSummaryAdapter
import ir.kitgroup.formulaNew.core.Util.formatDateShamsi
import ir.kitgroup.formulaNew.adapter.PackagingUsageDetailAdapter
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.database.entity.Packaging
import ir.kitgroup.formulaNew.databinding.FragmentPackagingUsageDetailsBinding
import ir.kitgroup.formulaNew.model.RawPackagingSummary
import ir.kitgroup.formulaNew.viewmodel.PackagingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PackagingUsageDetailsFragment : Fragment() {

    private var _binding: FragmentPackagingUsageDetailsBinding? = null
    private val binding get() = _binding!!

    private val packagingViewModel: PackagingViewModel by viewModels()
    private lateinit var packagingUsageDetailAdapter: PackagingUsageDetailAdapter
    private val args: PackagingUsageDetailsFragmentArgs by navArgs()
    private val selectedPackagings = mutableListOf<Packaging>()

    private val rawPackagingSummaryMap =
        mutableMapOf<String, Double>() // materialName -> totalQuantity
    private lateinit var packagingSummaryAdapter: PackagingSummaryAdapter
    private var rawPackagingSummaryListForPdf: List<RawPackagingSummary> = emptyList()

    // Data vars
    private var productId: Int = 0
    private var productUsageId: Long = 0
    private var productName: String = ""
    private var productDate: Long = 0
    private var qty: Double = 0.0
    private var remaining: Double = 0.0
    private var packagePrice: Double = 0.0
    private var productNamePdf: String = ""
    private var displayDateTime: String = ""
    private var isEditMode: Boolean = true


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        hideUIElements()
        _binding = FragmentPackagingUsageDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initArgs()
        initUI()
        initAdapter()
        rxBinding()
        setupObservers()
        loadExistingData()
    }

    private fun initArgs() {
        productId = args.packageId
        productUsageId = args.id
        productName = args.packageName
        productDate = args.packageDate
        qty = args.formattedQty.toDoubleOrNull() ?: 0.0
        packagePrice = args.packagePrice.toDoubleOrNull() ?: 0.0

        val jalaliDate = JalaliCalendar()
        val dateFormatted = "%02d-%02d-%04d".format(
            jalaliDate.day, jalaliDate.month, jalaliDate.year
        )
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        displayDateTime = "$dateFormatted ، $time"
        productNamePdf = "${productName}_$displayDateTime"
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun initUI() = binding.apply {
        tvProductName.text = productName
        tvProductDate.text = formatDateShamsi(productDate)
        tvProductAmount.text = "${formatQuantity(qty)} گرم"
        tvProductPrice.text = "${Util.priceFormatter.format(packagePrice)} ریال"
        tvRemaining.text = "${formatQuantity(qty)} گرم"
        packagingViewModel.setTotalProductWeight(qty)
    }

    private fun initAdapter() {

        packagingUsageDetailAdapter =
            PackagingUsageDetailAdapter(packagingViewModel, packagePrice, qty) { packaging ->
                if (!selectedPackagings.contains(packaging)) {
                    selectedPackagings.add(packaging)
                } else {
                    selectedPackagings.find { it.packagingId == packaging.packagingId }?.quantity =
                        packaging.quantity
                }

                updateTotalPriceUI()
                lifecycleScope.launch {
                    updateRawMaterialSummary()
                }
            }

        packagingUsageDetailAdapter.onQuantityChanged = {
            updateTotalPriceUI()
            lifecycleScope.launch {
                updateRawMaterialSummary()
            }
        }

        binding.rvPackagingList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = packagingUsageDetailAdapter
        }


        packagingSummaryAdapter = PackagingSummaryAdapter()
        binding.rvPackagingSummary.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = packagingSummaryAdapter
        }
    }

    private fun rxBinding() = binding.apply {

        binding.ivPdf.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                generateListPDF(
                    requireContext(), rawPackagingSummaryListForPdf
                )
            }
        }
        ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnEditSave.setOnClickListener {
            if (selectedPackagings.isEmpty()) {
                Toast.makeText(context, R.string.error_no_packaging_selected, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    packagingViewModel.saveOrUpdateUsages(
                        productId,
                        productUsageId.toInt(),
                        selectedPackagings,
                        isEditMode
                    )

                    Toast.makeText(
                        context,
                        if (isEditMode)
                            R.string.msg_packaging_consumption_edit
                        else
                            R.string.msg_packaging_consumption_saved,
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(context, "خطا در ذخیره‌سازی!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        packagingViewModel.totalUsedWeight.observe(viewLifecycleOwner) { usedWeight ->
            remaining = qty - usedWeight
            binding.tvRemaining.text = "${formatQuantity(remaining)} گرم"
        }

        packagingViewModel.allPackagings.observe(viewLifecycleOwner) { packagings ->
            updateUIWithProducts(packagings)

            packagingViewModel._packagingUsages.value.let { usages ->
                selectedPackagings.clear()
                usages.forEach { usage ->
                    if (usage.usedWeight > 0) {
                        val packaging = packagings.find { it.packagingId == usage.packagingId }
                        packaging?.let {
                            it.quantity = usage.usedWeight
                            selectedPackagings.add(it)
                        }
                    }
                }

                updateTotalPriceUI()
                // به‌روزرسانی خلاصه پس از بارگذاری اولیه
                lifecycleScope.launch {
                    updateRawMaterialSummary()
                }
            }
        }

        //  وضعیت ویرایش
        packagingViewModel.isEditMode.observe(viewLifecycleOwner) { isEdit ->
            isEditMode = isEdit
            binding.btnEditSave.text =
                getString(if (isEdit) R.string.label_edit else R.string.label_save)
            if (isEdit) {
                binding.ivPdf.visibility = View.VISIBLE
            } else {
                binding.ivPdf.visibility = View.GONE
            }
        }
    }

    private suspend fun updateRawMaterialSummary() {
        rawPackagingSummaryMap.clear()

        for (packaging in selectedPackagings) {
            if (packaging.quantity <= 0) continue

            val details = packagingViewModel.getPackageDetailsSuspend(packaging.packagingId)
            for (detail in details) {
                val totalNeeded = detail.quantity * packaging.quantity
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    rawPackagingSummaryMap[detail.materialName] =
                        rawPackagingSummaryMap.getOrDefault(detail.materialName, 0.0) + totalNeeded
                }
            }
        }

        val summaryList = rawPackagingSummaryMap.map { (name, qty) ->
            RawPackagingSummary(name, qty)
        }.sortedBy { it.name }

        withContext(Dispatchers.Main) {
            packagingSummaryAdapter.updateList(summaryList)
            rawPackagingSummaryListForPdf = summaryList

            binding.clPackagingSummary.visibility =
                if (summaryList.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun loadExistingData() {
        packagingViewModel.loadUsagesForProduct(productUsageId)
        updateTotalPriceUI()
    }

    private fun updateUIWithProducts(packagings: List<Packaging>) {
        packagingUsageDetailAdapter.submitList(packagings)
    }

    private fun updateTotalPriceUI() {

        val totalPrice = selectedPackagings.sumOf { item ->
            (((item.weight * packagePrice) / qty) + item.price) * item.quantity
        }
        binding.tvTotalPrice.text = Util.priceFormatter.format(totalPrice)

        // به‌روزرسانی قیمت کل محصول + بسته‌بندی
        updateTotalProductWithPackagingPrice()
    }


    @SuppressLint("SetTextI18n")
    private fun updateTotalProductWithPackagingPrice() {
        val packagingTotalPrice = selectedPackagings.sumOf { it.quantity * it.price }
        val total = packagingTotalPrice + packagePrice
        binding.tvTotalPriceProductPackaging.text = "${Util.priceFormatter.format(total)} ریال"
    }

    private fun hideUIElements() {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility =
            View.GONE
        requireActivity().findViewById<Toolbar>(R.id.toolbar)?.visibility = View.GONE
    }

    private suspend fun generateListPDF(
        context: Context,
        rawPackagingSummaryList: List<RawPackagingSummary>

    ) {
        try {

            val fileName = context.getString(R.string.label_packaging)
            val pdfFile = File(context.getExternalFilesDir(null), "${fileName}.pdf")
            val fos = withContext(Dispatchers.IO) {
                FileOutputStream(pdfFile)
            }
            val document = Document(PageSize.A4, 10f, 10f, 10f, 10f)
            PdfWriter.getInstance(document, fos)
            document.open()

            val baseFont = BaseFont.createFont(
                "assets/fonts/yekan.ttf",
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED
            )
            val farsiFont = Font(baseFont, 9f, Font.NORMAL)
            val farsiFontBold14 = Font(baseFont, 11f, Font.BOLD)
            val farsiFontBold18 = Font(baseFont, 14f, Font.BOLD, BaseColor.BLACK)

            val headerText = context.getString(R.string.label_company_name)

            val headerTable = PdfPTable(1)
            headerTable.widthPercentage = 100f
            val headerCell = PdfPCell(Phrase(headerText, farsiFontBold18))
            headerCell.horizontalAlignment = Element.ALIGN_CENTER
            headerCell.runDirection = PdfWriter.RUN_DIRECTION_LTR
            headerCell.border = Rectangle.NO_BORDER
            headerCell.setPadding(8f)
            headerTable.addCell(headerCell)

            val dateTable = PdfPTable(2)
            dateTable.widthPercentage = 100f
            dateTable.setWidths(floatArrayOf(1f, 3f))
            document.add(headerTable)


            val logo = Image.getInstance(context.assets.open("cocopartylatin.png").readBytes())
            logo.alignment = Image.ALIGN_CENTER
            logo.scaleToFit(100f, 100f)
            logo.spacingAfter = 8f

            document.add(logo)
            document.add(Paragraph("\n"))

            val infoTable = PdfPTable(2)
            infoTable.widthPercentage = 100f
            infoTable.setWidths(floatArrayOf(2f, 2f))

            val formCell = PdfPCell(
                Phrase(
                    context.getString(R.string.label_packaging_request_form),
                    farsiFontBold14
                )
            )
            formCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            formCell.horizontalAlignment = Element.ALIGN_LEFT
            formCell.border = Rectangle.NO_BORDER
            formCell.setPadding(5f)

            val dateCell = PdfPCell(
                Phrase("تاریخ و زمان : $displayDateTime", farsiFontBold14)
            )
            dateCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            dateCell.horizontalAlignment = Element.ALIGN_RIGHT
            dateCell.border = Rectangle.NO_BORDER
            dateCell.setPadding(5f)

            infoTable.addCell(dateCell)
            infoTable.addCell(formCell)

            document.add(infoTable)

            val lineSeparator = com.itextpdf.text.pdf.draw.LineSeparator()
            lineSeparator.lineWidth = 1f
            lineSeparator.offset = 0f // یا مقدار منفی کوچک مثل -1f

            val separatorParagraph = Paragraph().apply {
                add(Chunk(lineSeparator))
                spacingBefore = 2f  // فاصله قبل از خط
                spacingAfter =2f   // فاصله بعد از خط
            }

            document.add(separatorParagraph)


            val kilo = qty / 1000.0
            val kiloFormatted = String.format(Locale.US, "%.3f", kilo)

            /* val createQuantityTable = PdfPTable(1)
             createQuantityTable.widthPercentage = 100f

              val phrase = Phrase().apply {
                 // خط اول
                 add(Chunk("مقادیر مواد برای تولید ", farsiFont))
                 // گرم + کیلو داخل گیومه
                 add(
                     Chunk(
                         "«${formatQuantity(qty)} گرم معادل($kiloFormatted کیلوگرم)» ",
                         farsiFontBold14
                     )
                 )
                 add(Chunk("از محصول ", farsiFont))
                 add(
                     Chunk(
                         "«$productName»",
                         farsiFontBold14
                     )
                 )
                 add(Chunk.NEWLINE)
                 add(Chunk.NEWLINE)

                 // خط دوم: باقی مانده
                 add(
                     Chunk(
                         "مقدار باقی مانده مواد محصول: ",
                         farsiFont
                     )
                 )
                 add(
                     Chunk(
                         "«${formatQuantity(remaining)} گرم»",
                         farsiFontBold14
                     )
                 )
             }

             val createQuantityCell = PdfPCell(phrase).apply {
                 runDirection = PdfWriter.RUN_DIRECTION_RTL
                 horizontalAlignment = Element.ALIGN_LEFT
                 border = Rectangle.BOX
                 setPadding(10f)
             }

             createQuantityTable.addCell(createQuantityCell)
             document.add(createQuantityTable)*/

            val darkGrayColor = ContextCompat.getColor(context, R.color.gray_dark)
            val lightGrayColor = ContextCompat.getColor(context, R.color.gray_light)

            val darkGrayBase = BaseColor(
                Color.red(darkGrayColor),
                Color.green(darkGrayColor),
                Color.blue(darkGrayColor)
            )
            val lightGrayBase = BaseColor(
                Color.red(lightGrayColor),
                Color.green(lightGrayColor),
                Color.blue(lightGrayColor)
            )
            val headerColorBase =
                BaseColor(
                    Color.red(lightGrayColor),
                    Color.green(lightGrayColor),
                    Color.blue(lightGrayColor)
                )

            val materialTitle = PdfPTable(1)
            materialTitle.widthPercentage = 100f

            val materialCell = PdfPCell(Phrase(" مواداولیه ", farsiFontBold14))
            materialCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            materialCell.horizontalAlignment = Element.ALIGN_LEFT
            materialCell.border = Rectangle.NO_BORDER
            materialCell.setPadding(10f)

            materialTitle.addCell(materialCell)
            document.add(materialTitle)

            val materialTable = PdfPTable(5)
            materialTable.widthPercentage = 100f
            materialTable.runDirection = PdfWriter.RUN_DIRECTION_RTL
            materialTable.setWidths(floatArrayOf(2f, 1f, 1f, 1f, 2f))

            materialTable.addCell(
                createHeaderCell(
                    context.getString(R.string.label_name),
                    farsiFont,
                    headerColorBase
                )
            )
            materialTable.addCell(
                createHeaderCell(
                    context.getString(R.string.label_delivery_quantity),
                    farsiFont,
                    headerColorBase
                )
            )
            materialTable.addCell(
                createCell(
                    context.getString(R.string.label_dosage_quantity),
                    farsiFont,
                    headerColorBase
                )
            )
            materialTable.addCell(
                createCell(
                    context.getString(R.string.label_remainder),
                    farsiFont,
                    headerColorBase
                )
            )
            materialTable.addCell(
                createCell(
                    context.getString(R.string.label_description), farsiFont, headerColorBase
                )
            )

            val nameCell = createCell(productName, farsiFont, lightGrayBase)
            nameCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            nameCell.horizontalAlignment = Element.ALIGN_LEFT

            materialTable.addCell(nameCell)
            val qtyText = "${formatQuantity(qty)} گرم\n $kiloFormatted کیلوگرم"

            materialTable.addCell(
                createCell(qtyText, farsiFont, lightGrayBase)
            )
            materialTable.addCell(
                createCell(
                    "",
                    farsiFont,
                    lightGrayBase
                )
            )
            materialTable.addCell(
                createCell(
                    "${formatQuantity(remaining)} گرم",
                    farsiFont,
                    lightGrayBase
                )
            )
            materialTable.addCell(
                createCell(
                    "",
                    farsiFont,
                    lightGrayBase
                )
            )

            document.add(materialTable)

            val summaryTitle = PdfPTable(1)
            summaryTitle.widthPercentage = 100f

            val titleCell = PdfPCell(Phrase(" ملزومات بسته‌بندی ", farsiFontBold14))
            titleCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            titleCell.horizontalAlignment = Element.ALIGN_LEFT
            titleCell.border = Rectangle.NO_BORDER
            titleCell.setPadding(10f)

            summaryTitle.addCell(titleCell)
            document.add(summaryTitle)


            val summaryTable = PdfPTable(5)
            summaryTable.widthPercentage = 100f
            summaryTable.runDirection = PdfWriter.RUN_DIRECTION_RTL
            summaryTable.setWidths(floatArrayOf(2f, 1f, 1f, 1f, 2f))

            summaryTable.addCell(
                createHeaderCell(
                    context.getString(R.string.label_name),
                    farsiFont,
                    headerColorBase
                )
            )
            summaryTable.addCell(
                createHeaderCell(
                    context.getString(R.string.label_delivery_quantity),
                    farsiFont,
                    headerColorBase
                )
            )
            summaryTable.addCell(
                createCell(
                    context.getString(R.string.label_dosage_quantity),
                    farsiFont,
                    headerColorBase
                )
            )
            summaryTable.addCell(
                createCell(
                    context.getString(R.string.label_remainder),
                    farsiFont,
                    headerColorBase
                )
            )
            summaryTable.addCell(
                createCell(
                    context.getString(R.string.label_description), farsiFont, headerColorBase
                )
            )
            rawPackagingSummaryList.forEachIndexed { index, item ->
                val rowColor = if (index % 2 == 0) darkGrayBase else lightGrayBase

                val rowText = "${index + 1}. ${item.name}"
                val nameCell = createCell(rowText, farsiFont, rowColor)
                nameCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
                nameCell.horizontalAlignment = Element.ALIGN_LEFT

                summaryTable.addCell(nameCell)

                summaryTable.addCell(
                    createCell(
                        formatQuantity(item.quantity),
                        farsiFont,
                        rowColor
                    )
                )
                summaryTable.addCell(
                    createCell(
                        "",
                        farsiFont,
                        rowColor
                    )
                )
                summaryTable.addCell(
                    createCell(
                        "",
                        farsiFont,
                        rowColor
                    )
                )
                summaryTable.addCell(
                    createCell(
                        "",
                        farsiFont,
                        rowColor
                    )
                )
            }
            document.add(summaryTable)


            val packingTitle = PdfPTable(1)
            packingTitle.widthPercentage = 100f

            val packingCell = PdfPCell(Phrase(" محصول نهایی ", farsiFontBold14))
            packingCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            packingCell.horizontalAlignment = Element.ALIGN_LEFT
            packingCell.border = Rectangle.NO_BORDER
            packingCell.setPadding(10f)

            packingTitle.addCell(packingCell)
            document.add(packingTitle)

            val table = PdfPTable(6)
            table.widthPercentage = 100f
            table.runDirection = PdfWriter.RUN_DIRECTION_RTL
            val columnWidths = floatArrayOf(2f, 1f, 1f, 1f, 1f, 2f)
            table.setWidths(columnWidths)

            table.addCell(
                createHeaderCell(
                    context.getString(R.string.label_name),
                    farsiFont,
                    headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_quantity_weight_unit_item),
                    farsiFont,
                    headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_delivery_quantity),
                    farsiFont,
                    headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_production_quantity),
                    farsiFont,
                    headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_remainder),
                    farsiFont,
                    headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_description), farsiFont, headerColorBase
                )
            )
            for ((index, packaging) in selectedPackagings.withIndex()) {
                val rowColor: BaseColor = if (index % 2 == 0) darkGrayBase else lightGrayBase

                val rowText = "${index + 1}. ${packaging.packagingName}"
                val cellName = createCell(rowText, farsiFont, rowColor)
                cellName.runDirection = PdfWriter.RUN_DIRECTION_RTL
                cellName.horizontalAlignment = Element.ALIGN_LEFT

                table.addCell(cellName)

                table.addCell(
                    createCell(
                        formatQuantity(packaging.weight),
                        farsiFont, rowColor
                    )
                )
                table.addCell(
                    createCell(
                        formatQuantity(packaging.quantity),
                        farsiFont,
                        rowColor
                    )
                )
                table.addCell(createCell("", farsiFont, rowColor))
                table.addCell(createCell("", farsiFont, rowColor))
                table.addCell(createCell("", farsiFont, rowColor))
            }

            document.add(table)
            document.add(Paragraph("\n"))

            val descriptionTable = PdfPTable(1)
            descriptionTable.widthPercentage = 100f

            val descriptionCell = PdfPCell(
                Phrase(
                    "توضیحات:",
                    farsiFont
                )
            )
            descriptionCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            descriptionCell.horizontalAlignment = Element.ALIGN_LEFT
            descriptionCell.minimumHeight = 60f
            descriptionCell.border = Rectangle.BOX
            descriptionCell.setPadding(8f)

            descriptionTable.addCell(descriptionCell)
            document.add(descriptionTable)
            document.add(Paragraph("\n"))

            val signTable = PdfPTable(2)
            signTable.widthPercentage = 100f
            signTable.setWidths(floatArrayOf(1f, 1f))

            val receiverCell = PdfPCell(
                Phrase("نام و امضای تحویل‌گیرنده\n\n\n", farsiFont)
            )
            receiverCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            receiverCell.horizontalAlignment = Element.ALIGN_CENTER
            receiverCell.minimumHeight = 50f
            receiverCell.border = Rectangle.BOX

            val producerCell = PdfPCell(
                Phrase("نام و امضای مسئول تولید\n\n\n", farsiFont)
            )
            producerCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            producerCell.horizontalAlignment = Element.ALIGN_CENTER
            producerCell.minimumHeight = 50f
            producerCell.border = Rectangle.BOX

            signTable.addCell(receiverCell)
            signTable.addCell(producerCell)

            document.add(signTable)

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
        cell.setPadding(8f)
        return cell
    }

    private fun createHeaderCell(text: String, font: Font, backgroundColor: BaseColor): PdfPCell {
        val cell = PdfPCell(Phrase(text, font))
        cell.horizontalAlignment = Element.ALIGN_LEFT
        cell.runDirection = PdfWriter.RUN_DIRECTION_RTL
        cell.backgroundColor = backgroundColor
        cell.setPadding(8f)
        return cell
    }

    private fun openPDF(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )
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