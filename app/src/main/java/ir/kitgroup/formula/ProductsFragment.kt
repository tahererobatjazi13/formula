package ir.kitgroup.formula

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
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
import ir.kitgroup.formula.Util.calculatePricePerKg
import ir.kitgroup.formula.Util.formatDateShamsi
import ir.kitgroup.formula.adapter.ProductAdapter
import ir.kitgroup.formula.adapter.getTotalPriceForProduct
import ir.kitgroup.formula.adapter.getTotalQuantityForProduct
import ir.kitgroup.formula.database.entity.Product
import ir.kitgroup.formula.databinding.FragmentProductsBinding
import ir.kitgroup.formula.dialog.AddEditProductDialog
import ir.kitgroup.formula.dialog.ConfirmDeleteDialog
import ir.kitgroup.formula.viewmodel.ProductViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null

    private lateinit var productAdapter: ProductAdapter

    private lateinit var allProducts: List<Product>
    private lateinit var filteredProductsList: List<Product>
    private var displayDateTime: String = ""
    private val formatter = DecimalFormat("#,###,###,###")

    private val binding get() = _binding!!
    private val productViewModel: ProductViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        rxBinding()
        initSpinner()
        allProducts = listOf()
        filteredProductsList = allProducts


        binding.svProduct.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                binding.spinnerFilter.setSelection(0)
                filterProducts(binding.svProduct.query?.toString() ?: "")
                filteredProductsList = allProducts.filter {
                    it.productName.contains(newText ?: "", ignoreCase = true)
                }
                productAdapter.submitList(filteredProductsList)
                return true
            }
        })

        productAdapter = ProductAdapter(

            onChangeLog = { product ->
                val action =
                    ProductsFragmentDirections.actionProductsFragmentToChangeLogFragment(
                        product.productId, 3
                    )
                findNavController().navigate(action)
            },
            onDelete = { product ->
                val dialog = ConfirmDeleteDialog {
                    productViewModel.delete(product)
                }
                dialog.show(childFragmentManager, "ConfirmDeleteDialog")
            },
            onEdit = { product ->
                val dialog = AddEditProductDialog(productViewModel, product)
                dialog.show(childFragmentManager, "EditProductDialog")

                childFragmentManager.registerFragmentLifecycleCallbacks(object :
                    FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentViewDestroyed(fm: FragmentManager, fragment: Fragment) {
                        if (fragment === dialog) {
                            // وقتی دیالوگ بسته شد
                            binding.spinnerFilter.setSelection(0)
                            filterProducts(binding.svProduct.query?.toString() ?: "")
                            fm.unregisterFragmentLifecycleCallbacks(this)
                        }
                    }
                }, false)
            },
            onClick = { product ->
                val action =
                    ProductsFragmentDirections.actionProductsFragmentToProductDetailsFragment(
                        product.productId,
                        product.productName,
                        product.updatedDate,
                        product.description,
                    )
                findNavController().navigate(action)
            }, productViewModel
        )

        binding.rvProducts.adapter = productAdapter
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        productViewModel.allProducts.observe(viewLifecycleOwner) { products ->
            productAdapter.submitList(products)
        }

        productViewModel.allProducts.observe(viewLifecycleOwner) { products ->
            allProducts = products
            productAdapter.submitList(allProducts)

            val isEmpty = allProducts.isEmpty()
            binding.tvNoItem.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.ivPdf.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.spinnerFilter.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                filterProducts(binding.svProduct.query?.toString() ?: "")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }



        binding.fabAddProduct.setOnClickListener {
            productViewModel.getAllRawMaterials().observe(viewLifecycleOwner) { rawMaterials ->
                if (rawMaterials.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        R.string.error_first_enter_material,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    AddEditProductDialog(productViewModel).show(
                        parentFragmentManager,
                        "AddProductDialog"
                    )
                }
            }
        }

    }

    private fun initSpinner() {
        val spinnerItems = resources.getStringArray(R.array.product_filter_options)
        val adapter = createSpinnerAdapter(spinnerItems)
        binding.spinnerFilter.adapter = adapter
        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val selectedItem = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // optional
            }
        }
    }

    private fun createSpinnerAdapter(data: Array<String>): ArrayAdapter<String> {
        return object :
            ArrayAdapter<String>(
                requireContext(),
                R.layout.item_spinner,
                R.id.tvName,
                data
            ) {
            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_spinner_dropdown, parent, false)
                val textView = view.findViewById<TextView>(R.id.tvName)
                textView.text = getItem(position)
                return view
            }
        }
    }

    private fun filterProducts(query: String) {
        val filteredByType = when (binding.spinnerFilter.selectedItemPosition) {
            1 -> allProducts.filter { it.isFinalProduct } // فقط محصولات نهایی
            2 -> allProducts.filter { !it.isFinalProduct } // فقط محصولات میانی
            else -> allProducts // همه
        }

        filteredProductsList = filteredByType.filter {
            it.productName.contains(query, ignoreCase = true)
        }
        val isEmpty = filteredProductsList.isEmpty()
        binding.tvNoItem.visibility = if (isEmpty) View.VISIBLE else View.GONE

        productAdapter.submitList(filteredProductsList)
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
            GlobalScope.launch(Dispatchers.Main) {
                generateListPDF(requireContext(), allProducts)
            }
        }
    }

    private suspend fun generateListPDF(
        context: Context,
        items: List<Product>,
    ) {
        try {
            val productsWithPrices = getProductsWithPrices(items)

            val fileName = context.getString(R.string.label_product_list)
            val pdfFile = File(context.getExternalFilesDir(null), "${fileName}.pdf")
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

            val headerText = context.getString(R.string.label_product_list)

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

            val table = PdfPTable(3)
            table.widthPercentage = 100f
            table.runDirection = PdfWriter.RUN_DIRECTION_RTL
            val columnWidths = floatArrayOf(2f, 2f, 2f)
            table.setWidths(columnWidths)

            table.addCell(
                createCell(
                    context.getString(R.string.label_product_name_item),
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

            for ((index, pair) in productsWithPrices.withIndex()) {
                val product = pair.first
                val price = pair.second
                val rowColor: BaseColor = if (index % 2 == 0) darkGrayBase else lightGrayBase

                val rowText = "${index + 1}. ${product.productName}"
                val cellName = createCell(rowText, farsiFont, rowColor)
                cellName.runDirection = PdfWriter.RUN_DIRECTION_RTL
                cellName.horizontalAlignment = Element.ALIGN_LEFT

                table.addCell(cellName)
                table.addCell(
                    createCell(
                        formatDateShamsi(product.updatedDate),
                        farsiFont, rowColor
                    )
                )
                table.addCell(createCell(formatter.format(price), farsiFont, rowColor))
            }

            document.add(table)
            document.close()
            openPDF(context, pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private suspend fun getProductsWithPrices(
        products: List<Product>
    ): List<Pair<Product, Double>> {
        return products.map { product ->
            val details = productViewModel.getProductDetailsSuspend(product.productId)
            val price = if (details.isNotEmpty()) {
                calculatePricePerKg(
                    getTotalQuantityForProduct(details),
                    getTotalPriceForProduct(details)
                )
            } else 0.0
            product to price
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