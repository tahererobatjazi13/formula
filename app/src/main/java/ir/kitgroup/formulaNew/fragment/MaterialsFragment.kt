package ir.kitgroup.formulaNew.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
import ir.kitgroup.formulaNew.adapter.MaterialAdapter
import ir.kitgroup.formulaNew.core.MaterialType
import ir.kitgroup.formulaNew.core.MaterialType.*
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.core.Util.formatDateShamsi
import ir.kitgroup.formulaNew.database.entity.Material
import ir.kitgroup.formulaNew.databinding.FragmentMaterialsBinding
import ir.kitgroup.formulaNew.dialog.AddEditMaterialDialog
import ir.kitgroup.formulaNew.dialog.ConfirmDeleteDialog
import ir.kitgroup.formulaNew.viewmodel.MaterialViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MaterialsFragment : Fragment() {

    private var _binding: FragmentMaterialsBinding? = null
    private val materialViewModel: MaterialViewModel by viewModels()
    private lateinit var materialAdapter: MaterialAdapter
    private lateinit var allMaterials: List<Material>
    private lateinit var filteredMaterialsList: List<Material>
    private var displayDateTime: String = ""
    private val binding get() = _binding!!
    private var currentSelectedType: String = MATERIAL.value

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMaterialsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        initTabButtons()
        rxBinding()
        initAdapter()
        setupObservers()
    }

    @SuppressLint("DefaultLocale", "InflateParams")
    private fun init() {
        val jalaliDate = JalaliCalendar()
        val dateFormatted =
            String.format("%02d-%02d-%04d", jalaliDate.day, jalaliDate.month, jalaliDate.year)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = timeFormat.format(Date())
        displayDateTime = "$dateFormatted ، $time"
    }

    private fun initTabButtons() {
        val btnPackaging = binding.btnPackaging
        val btnMaterial = binding.btnMaterial

        selectTab(btnPackaging, btnMaterial, false)

        btnPackaging.setOnClickListener {
            selectTab(btnPackaging, btnMaterial, true)
        }

        btnMaterial.setOnClickListener {
            selectTab(btnPackaging, btnMaterial, false)
        }
    }

    private fun selectTab(
        btnPackaging: TextView,
        btnMaterial: TextView,
        isPackagingSelected: Boolean
    ) {
        val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_selected_tab)
        val unselectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_unselected_tab)
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val blackColor = ContextCompat.getColor(requireContext(), R.color.black)

        if (isPackagingSelected) {
            btnPackaging.background = selectedBg
            btnPackaging.setTextColor(whiteColor)

            btnMaterial.background = unselectedBg
            btnMaterial.setTextColor(blackColor)
        } else {
            btnMaterial.background = selectedBg
            btnMaterial.setTextColor(whiteColor)

            btnPackaging.background = unselectedBg
            btnPackaging.setTextColor(blackColor)
        }
    }

    private fun rxBinding() {
        binding.ivPdf.setOnClickListener {
            generateListPDF(requireContext(), allMaterials)
        }


        binding.fabAddMaterial.setOnClickListener {
            when (currentSelectedType) {
                PACKAGING.value, MATERIAL.value -> {
                    val dialog =
                        AddEditMaterialDialog(defaultType = currentSelectedType) { rawMaterial ->
                            materialViewModel.insert(rawMaterial)

                            if (rawMaterial.type == PACKAGING.value) {
                                selectTab(binding.btnPackaging, binding.btnMaterial, true)
                            } else {
                                selectTab(binding.btnPackaging, binding.btnMaterial, false)
                            }
                        }
                    dialog.show(childFragmentManager, "AddRawMaterialDialog")
                }

            }
        }


        binding.etSearch.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim().orEmpty()
            binding.ivClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE

            filteredMaterialsList = if (query.isEmpty()) {
                allMaterials.filter { it.type == currentSelectedType }
            } else {
                allMaterials.filter {
                    it.type == currentSelectedType && it.materialName.contains(
                        query,
                        ignoreCase = true
                    )
                }
            }

            materialAdapter.submitList(filteredMaterialsList)
            binding.tvNoItem.visibility =
                if (filteredMaterialsList.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.ivClearSearch.visibility = View.GONE

            updateListForSelectedTab(currentSelectedType)
        }

        binding.btnPackaging.setOnClickListener {
            selectTab(binding.btnPackaging, binding.btnMaterial, true)
            currentSelectedType = PACKAGING.value
            updateListForSelectedTab(currentSelectedType)
        }

        binding.btnMaterial.setOnClickListener {
            selectTab(binding.btnPackaging, binding.btnMaterial, false)
            currentSelectedType = MATERIAL.value
            updateListForSelectedTab(currentSelectedType)
        }

    }

    private fun initAdapter() {

        allMaterials = listOf()
        filteredMaterialsList = allMaterials

        materialAdapter = MaterialAdapter(

            onChangeLog = { rawMaterial ->
                val action =
                    MaterialsFragmentDirections.actionMaterialsFragmentToChangeLogFragment(
                        rawMaterial.materialId, 1
                    )
                findNavController().navigate(action)
            },

            onDelete = { material ->

                if (material.type == PACKAGING.value) {
                    materialViewModel.canDeletePackaging(material.materialId) { canDelete ->
                        if (canDelete) {
                            val dialog = ConfirmDeleteDialog {
                                materialViewModel.delete(material)
                            }
                            dialog.show(childFragmentManager, "ConfirmDeleteDialog")
                        } else {
                            Toast.makeText(
                                requireContext(), R.string.error_packaging_used_cannot_removed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {

                    materialViewModel.canDeleteMaterial(material.materialId) { canDelete ->
                        if (canDelete) {
                            val dialog = ConfirmDeleteDialog {
                                materialViewModel.delete(material)
                            }
                            dialog.show(childFragmentManager, "ConfirmDeleteDialog")

                        } else {
                            Toast.makeText(
                                requireContext(), R.string.error_material_used_cannot_removed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            },
            onEdit = { rawMaterial ->
                val dialog = AddEditMaterialDialog(rawMaterial) { editedMaterial ->
                    materialViewModel.updateMaterialAndProductDetails(editedMaterial)
                }
                dialog.show(childFragmentManager, "EditRawMaterialDialog")
            }
        )

        binding.rvMaterials.adapter = materialAdapter
        binding.rvMaterials.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        materialViewModel.allMaterials.observe(viewLifecycleOwner) { materials ->
            allMaterials = materials
            updateListForSelectedTab(currentSelectedType)
        }
    }

    // متد به‌روز شده برای آپدیت لیست
    private fun updateListForSelectedTab(selectedType: String) {
        filteredMaterialsList = allMaterials.filter { it.type == selectedType }

        val query = binding.etSearch.text?.toString()?.trim().orEmpty()
        if (query.isNotEmpty()) {
            filteredMaterialsList = filteredMaterialsList.filter {
                it.materialName.contains(query, ignoreCase = true)
            }
        }

        materialAdapter.submitList(filteredMaterialsList)
        binding.tvNoItem.visibility =
            if (filteredMaterialsList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun generateListPDF(
        context: Context,
        items: List<Material>,
    ) {
        try {
            val fileName = context.getString(R.string.label_material_list)
            // محل ذخیره PDF
            val pdfFile =
                File(context.getExternalFilesDir(null), "${fileName}.pdf")
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

            val headerText =
                context.getString(R.string.label_material_list)

            val headerTable = PdfPTable(1)
            headerTable.widthPercentage = 100f

            // ایجاد سلول برای عنوان گزارش
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


            // تبدیل رنگ‌ها به BaseColor (RGB)
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

            // اضافه کردن جدول داده‌ها
            val table = PdfPTable(5)
            table.widthPercentage = 100f
            table.runDirection = PdfWriter.RUN_DIRECTION_RTL
            val columnWidths = floatArrayOf(4f, 2f, 2f, 2f, 2f)
            table.setWidths(columnWidths)

            // اضافه کردن هدر جدول
            table.addCell(
                createCell(
                    context.getString(R.string.label_material_name),
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
                    context.getString(R.string.label_current_price),
                    farsiFont,
                    headerColorBase
                )
            )

            table.addCell(
                createCell(
                    context.getString(R.string.label_day_price_item), farsiFont, headerColorBase
                )
            )
            table.addCell(
                createCell(
                    context.getString(R.string.label_desc), farsiFont, headerColorBase
                )
            )

            for (i in items.indices) {

                val item: Material = items[i]
                val rowColor: BaseColor = if ((i % 2 == 0)) darkGrayBase else lightGrayBase

                val rowText = (i + 1).toString() + ". " + item.materialName
                val cellName = createCell(rowText, farsiFont, rowColor)
                cellName.runDirection = PdfWriter.RUN_DIRECTION_RTL
                cellName.horizontalAlignment = Element.ALIGN_LEFT

                val price = item.price

                table.addCell(cellName)
                table.addCell(
                    createCell(
                        formatDateShamsi(item.updatedDate),
                        farsiFont,
                        rowColor
                    )
                )
                table.addCell(
                    createCell(
                        Util.priceFormatter.format(price),
                        farsiFont,
                        rowColor
                    )
                )
                table.addCell(createCell("", farsiFont, rowColor))
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

    private fun updateTabSelection() {
        if (currentSelectedType == PACKAGING.value) {
            selectTab(binding.btnPackaging, binding.btnMaterial, true)
        } else {
            selectTab(binding.btnPackaging, binding.btnMaterial, false)
        }

        updateListForSelectedTab(currentSelectedType)
    }

    override fun onResume() {
        super.onResume()
        updateTabSelection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}