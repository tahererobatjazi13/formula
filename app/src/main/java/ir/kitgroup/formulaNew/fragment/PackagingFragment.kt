package ir.kitgroup.formulaNew.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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
import ir.kitgroup.formulaNew.core.Util.getTotalPriceForPackaging
import ir.kitgroup.formulaNew.adapter.PackagingAdapter
import ir.kitgroup.formulaNew.database.entity.Packaging
import ir.kitgroup.formulaNew.databinding.FragmentPackagingBinding
import ir.kitgroup.formulaNew.dialog.AddEditPackagingDialog
import ir.kitgroup.formulaNew.dialog.ConfirmDeleteDialog
import ir.kitgroup.formulaNew.core.MaterialType
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.core.Util.formatQuantity
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

class PackagingFragment : Fragment() {

    private var _binding: FragmentPackagingBinding? = null
    private val packagingViewModel: PackagingViewModel by viewModels()
    private lateinit var packagingAdapter: PackagingAdapter
    private var displayDateTime: String = ""
    private lateinit var allPackaging: List<Packaging>
    private lateinit var filteredPackagingList: List<Packaging>
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeProducts()
        setupListeners()
    }

    private fun setupUI() {
        initDateTime()
        setupRecyclerView()
        setupToolbarAndNav()
        setupSearchView()
    }

    @SuppressLint("DefaultLocale")
    private fun initDateTime() {
        val jalaliDate = JalaliCalendar()
        val dateFormatted =
            String.format("%02d-%02d-%04d", jalaliDate.day, jalaliDate.month, jalaliDate.year)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = timeFormat.format(Date())
        displayDateTime = "$dateFormatted ، $time"
    }

    private fun setupRecyclerView() {
        allPackaging = listOf()
        filteredPackagingList = allPackaging

        packagingAdapter = PackagingAdapter(
            onDelete = { packaging ->
                val dialog = ConfirmDeleteDialog {
                    packagingViewModel.deletePackaging(packaging)
                }
                dialog.show(childFragmentManager, "ConfirmDeleteDialog")
            },
            onEdit = { showAddEditDialog(it) },
            onClick = { navigateToDetails(it) }, packagingViewModel
        )

        binding.rvPackaging.apply {
            adapter = packagingAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearchView() {

        binding.etSearch.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim().orEmpty()
            binding.ivClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE

            filteredPackagingList = if (query.isEmpty()) {
                allPackaging
            } else {
                allPackaging.filter {
                    it.packagingName.contains(query, ignoreCase = true)
                }
            }

            packagingAdapter.submitList(filteredPackagingList)
            binding.tvNoItem.visibility =
                if (filteredPackagingList.isEmpty()) View.VISIBLE else View.GONE
        }
        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.ivClearSearch.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.fabAddPackaging.setOnClickListener {
            packagingViewModel.getAllRawMaterialsByType(MaterialType.PACKAGING.value)
                .observe(viewLifecycleOwner) { rawMaterials ->
                    if (rawMaterials.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            R.string.error_first_enter_packaging,
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        AddEditPackagingDialog(packagingViewModel).show(
                            parentFragmentManager,
                            "AddProductDialog"
                        )
                    }
                }
        }
        binding.ivPdf.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                generateListPDF(requireContext(), packagingAdapter.currentList)
            }
        }
    }

    private fun observeProducts() {
        packagingViewModel.allPackagings.observe(viewLifecycleOwner) { packagings ->
            allPackaging = packagings
            updateUIWithProducts(packagings)
        }
    }

    private fun updateUIWithProducts(packagings: List<Packaging>) {
        packagingAdapter.submitList(packagings)

        val isEmpty = packagings.isEmpty()
        binding.tvNoItem.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.ivPdf.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun filterProducts(query: String) {
        packagingViewModel.allPackagings.value?.let { allProducts ->
            packagingAdapter.submitList(allProducts)
            binding.tvNoItem.visibility = if (allProducts.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun navigateToDetails(packaging: Packaging) {
        findNavController().navigate(
            PackagingFragmentDirections.actionPackagingFragmentToPackagingDetailsFragment(
                packaging.packagingId,
                packaging.packagingName,
                packaging.updatedDate,
                packaging.description,
                packaging.weight.toString()
            )
        )
    }

    private fun showAddEditDialog(packaging: Packaging) {
        AddEditPackagingDialog(packagingViewModel, packaging).also { dialog ->
            dialog.show(childFragmentManager, "EditProductDialog")
            childFragmentManager.registerFragmentLifecycleCallbacks(object :
                FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewDestroyed(fm: FragmentManager, fragment: Fragment) {
                    if (fragment === dialog) {
                        val query = binding.etSearch.text?.toString().orEmpty()
                        filterProducts(query)
                        fm.unregisterFragmentLifecycleCallbacks(this)
                    }
                }
            }, false)
        }
    }

    private suspend fun generateListPDF(
        context: Context,
        items: List<Packaging>,
    ) {
        try {
            val packagingsWithPrices = getPackagingsWithPrices(items)

            val fileName = context.getString(R.string.label_packaging_list)
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

            val headerText = context.getString(R.string.label_packaging_list)

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

            document.add(headerTable)
            document.add(dateTable)
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
                    context.getString(R.string.label_packaging_services),
                    farsiFont,
                    headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_last_update_date),
                    farsiFont,
                    headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_price_kg),
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
            for ((index, pair) in packagingsWithPrices.withIndex()) {
                val packaging = pair.first
                val price = pair.second
                val rowColor: BaseColor = if (index % 2 == 0) darkGrayBase else lightGrayBase

                val rowText = "${index + 1}. ${packaging.packagingName}"
                val cellName = createCell(rowText, farsiFont, rowColor)
                cellName.runDirection = PdfWriter.RUN_DIRECTION_RTL
                cellName.horizontalAlignment = Element.ALIGN_LEFT

                table.addCell(cellName)
                table.addCell(
                    createCell(
                        formatDateShamsi(packaging.updatedDate),
                        farsiFont, rowColor
                    )
                )
                table.addCell(createCell(Util.priceFormatter.format(price), farsiFont, rowColor))
                table.addCell(createCell(formatQuantity(packaging.weight), farsiFont, rowColor))
            }

            document.add(table)
            document.close()
            openPDF(context, pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getPackagingsWithPrices(
        packagings: List<Packaging>
    ): List<Pair<Packaging, Double>> {
        return packagings.map { packaging ->
            val details = packagingViewModel.getPackageDetailsSuspend(packaging.packagingId)
            val price = if (details.isNotEmpty()) {
                getTotalPriceForPackaging(
                    details
                )
            } else 0.0
            packaging to price
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

    private fun setupToolbarAndNav() {
        (requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)).apply {
            visibility = View.VISIBLE
        }
        (requireActivity().findViewById<Toolbar>(R.id.toolbar)).apply {
            visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}