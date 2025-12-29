package ir.kitgroup.formulaNew.fragment


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.core.Util.formatDateShamsi
import ir.kitgroup.formulaNew.adapter.PackagingUsageDetailAdapter
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.database.entity.Packaging
import ir.kitgroup.formulaNew.databinding.FragmentPackagingUsageDetailsBinding
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

    // Data vars
    private var productId: Int = 0
    private var productUsageId: Long = 0
    private var productName: String = ""
    private var productDate: Long = 0
    private var qty: Double = 0.0
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
            }

        packagingUsageDetailAdapter.onQuantityChanged = { updateTotalPriceUI() }

        binding.rvPackagingList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = packagingUsageDetailAdapter
        }
    }

    private fun rxBinding() = binding.apply {

        binding.ivPdf.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                generateListPDF(requireContext(), selectedPackagings)
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
            val remaining = qty - usedWeight
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
        items: List<Packaging>,
    ) {
        try {
            // val productsWithPrices = getProductsWithPrices(items)

            val fileName = context.getString(R.string.label_product_list)
            val pdfFile = File(context.getExternalFilesDir(null), "${fileName}.pdf")
            val fos = withContext(Dispatchers.IO) {
                FileOutputStream(pdfFile)
            }
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

            val headerText = context.getString(R.string.label_packaging_production_detail)

            val headerTable = PdfPTable(1)
            headerTable.widthPercentage = 100f
            val headerCell = PdfPCell(Phrase(headerText, farsiFontBold18))
            headerCell.horizontalAlignment = Element.ALIGN_CENTER
            headerCell.runDirection = PdfWriter.RUN_DIRECTION_LTR
            headerCell.border = Rectangle.NO_BORDER
            headerCell.setPadding(10f)
            headerTable.addCell(headerCell)

            val dateTable = PdfPTable(2)
            dateTable.widthPercentage = 100f
            dateTable.setWidths(floatArrayOf(1f, 3f))

            val emptyCell = PdfPCell(Phrase("", farsiFont))
            emptyCell.border = Rectangle.NO_BORDER
            val dateCell =
                PdfPCell(Phrase("تاریخ و ساعت گزارش : $displayDateTime", farsiFontBold14))
            dateCell.horizontalAlignment = Element.ALIGN_RIGHT
            dateCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            dateCell.border = Rectangle.NO_BORDER
            dateCell.setPadding(15f)
            dateTable.addCell(emptyCell)
            dateTable.addCell(dateCell)

            val createQuantityTable = PdfPTable(1)
            createQuantityTable.widthPercentage = 100f
            createQuantityTable.horizontalAlignment = Element.ALIGN_RIGHT

            val createQuantityCell =
                PdfPCell(
                    Phrase(
                        "مقادیر مواد برای تولید ${formatQuantity(qty)} گرم از محصول ",
                        farsiFontBold14
                    )
                )
            createQuantityCell.horizontalAlignment = Element.ALIGN_LEFT
            createQuantityCell.runDirection = PdfWriter.RUN_DIRECTION_RTL
            createQuantityCell.border = Rectangle.NO_BORDER
            createQuantityCell.setPadding(10f)
            createQuantityTable.addCell(createQuantityCell)

            document.add(headerTable)
            document.add(dateTable)
            document.add(createQuantityTable)

            document.add(Paragraph("\n"))

            val darkGrayColor = ContextCompat.getColor(context, R.color.gray_dark)
            val lightGrayColor = ContextCompat.getColor(context, R.color.gray_light)
            val headerColor = ContextCompat.getColor(context, R.color.colorAccent)

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
                BaseColor(Color.red(headerColor), Color.green(headerColor), Color.blue(headerColor))

            val table = PdfPTable(4)
            table.widthPercentage = 100f
            table.runDirection = PdfWriter.RUN_DIRECTION_RTL
            val columnWidths = floatArrayOf(2f, 2f, 2f, 2f)
            table.setWidths(columnWidths)

            table.addCell(
                createCell(
                    context.getString(R.string.label_name),
                    farsiFont,
                    headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_quantity_weight_unit),
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

            }

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