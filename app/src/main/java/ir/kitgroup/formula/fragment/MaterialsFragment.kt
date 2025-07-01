package ir.kitgroup.formula.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.formatDateShamsi
import ir.kitgroup.formula.adapter.MaterialAdapter
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.databinding.FragmentMaterialsBinding
import ir.kitgroup.formula.dialog.AddEditMaterialDialog
import ir.kitgroup.formula.dialog.ConfirmDeleteDialog
import ir.kitgroup.formula.viewmodel.MaterialViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MaterialsFragment : Fragment() {

    private var _binding: FragmentMaterialsBinding? = null
    private val materialViewModel: MaterialViewModel by viewModels()
    private lateinit var materialAdapter: MaterialAdapter
    private lateinit var allMaterials: List<Material>
    private lateinit var filteredMaterialsList: List<Material>
    private val formatter = DecimalFormat("#,###,###,###")
    private var displayDateTime: String = ""
    private val binding get() = _binding!!

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
        rxBinding()
        initAdapter()
        setupObservers()
    }

    @SuppressLint("DefaultLocale")
    private fun init() {
        val jalaliDate = JalaliCalendar()
        val dateFormatted =
            String.format("%02d-%02d-%04d", jalaliDate.day, jalaliDate.month, jalaliDate.year)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = timeFormat.format(Date())
        displayDateTime = "$dateFormatted ، $time"
    }


    private fun rxBinding() {
        binding.ivPdf.setOnClickListener {
            generateListPDF(requireContext(), allMaterials)
        }

        binding.fabAddMaterial.setOnClickListener {
            val dialog = AddEditMaterialDialog { rawMaterial ->
                materialViewModel.insert(rawMaterial)
            }
            dialog.show(childFragmentManager, "AddRawMaterialDialog")
        }
    }

    private fun initAdapter() {
        allMaterials = listOf()
        filteredMaterialsList = allMaterials

        binding.svMaterial.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filteredMaterialsList = allMaterials.filter {
                    it.materialName.contains(newText ?: "", ignoreCase = true)
                }
                materialAdapter.submitList(filteredMaterialsList)
                return true
            }
        })

        materialAdapter = MaterialAdapter(

            onChangeLog = { rawMaterial ->
                val action =
                    MaterialsFragmentDirections.actionMaterialsFragmentToChangeLogFragment(
                        rawMaterial.materialId, 1
                    )
                findNavController().navigate(action)
            },
            onDelete = { rawMaterial ->
                val dialog = ConfirmDeleteDialog {
                    materialViewModel.delete(rawMaterial)
                }
                dialog.show(childFragmentManager, "ConfirmDeleteDialog")
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
            materialAdapter.submitList(materials)

            val isEmpty = materials.isEmpty()
            binding.tvNoItem.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.ivPdf.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
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
                        formatter.format(price),
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