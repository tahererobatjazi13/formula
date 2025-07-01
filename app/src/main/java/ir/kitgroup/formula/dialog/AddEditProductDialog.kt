package ir.kitgroup.formula.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import ir.kitgroup.formula.R
import ir.kitgroup.formula.adapter.MaterialSelectionAdapter
import ir.kitgroup.formula.adapter.ProductSelectionAdapter
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.database.entity.Product
import ir.kitgroup.formula.databinding.DialogAddEditProductBinding
import ir.kitgroup.formula.viewmodel.ProductViewModel

class AddEditProductDialog(
    private val viewModel: ProductViewModel,
    private val product: Product? = null
) : DialogFragment() {
    private var totalPriceProduct: Double = 0.0
    private var _binding: DialogAddEditProductBinding? = null
    private val binding get() = _binding!!
    private lateinit var materialSelectionAdapter: MaterialSelectionAdapter
    private lateinit var productSelectionAdapter: ProductSelectionAdapter
    private lateinit var filteredMaterialsList: List<Material>
    private lateinit var allMaterialsList: List<Material>
    private lateinit var allProductsList: List<Product>
    private val selectedProducts = mutableListOf<Product>()
    private val selectedMaterials = mutableListOf<Material>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditProductBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // تغییر عنوان دیالوگ بر اساس افزودن یا ویرایش
        if (product == null) {
            binding.tvTitleDialog.text = getString(R.string.label_add_new_product)
            productSelectionAdapter = ProductSelectionAdapter({ product, totalPrice, isSelected ->

                totalPriceProduct = totalPrice

                if (isSelected) {
                    if (!selectedProducts.contains(product)) {
                        selectedProducts.add(product)
                    }
                } else {
                    selectedProducts.remove(product)
                }
            }, viewModel, 0)

        } else {
            binding.tvTitleDialog.text = getString(R.string.label_edit_product)
            productSelectionAdapter = ProductSelectionAdapter({ product, totalPrice, isSelected ->
                totalPriceProduct = totalPrice
                if (isSelected) {
                    if (!selectedProducts.contains(product)) {
                        selectedProducts.add(product)
                    }
                } else {
                    selectedProducts.remove(product)
                }
            }, viewModel, 1)
        }


        binding.rvProducts.adapter = productSelectionAdapter
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())

        viewModel.allProducts.observe(viewLifecycleOwner) { products ->
            allProductsList = products
            productSelectionAdapter.submitList(products)
        }

        materialSelectionAdapter = MaterialSelectionAdapter { material, isSelected ->
            if (isSelected) {
                if (!selectedMaterials.contains(material)) {
                    selectedMaterials.add(material)
                }
            } else {
                selectedMaterials.remove(material)
            }
        }

        for (material in selectedMaterials) {
            val viewHolder = binding.rvMaterials.findViewHolderForAdapterPosition(
                allMaterialsList.indexOf(material)
            )
                    as? MaterialSelectionAdapter.MaterialViewHolder
            viewHolder?.let {
                val updatedQuantity = it.binding.etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                val updatedPrice =
                    it.binding.tvPrice.text.toString().replace(",", "").toDoubleOrNull() ?: 0.0

                material.quantity = updatedQuantity
                material.price = updatedPrice
            }
        }

        binding.rvMaterials.adapter = materialSelectionAdapter
        binding.rvMaterials.layoutManager = LinearLayoutManager(requireContext())
        filteredMaterialsList = selectedMaterials

        binding.svProductMaterial.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText ?: ""

                // فیلتر مواد اولیه
                val filteredMaterials = allMaterialsList.filter {
                    it.materialName.contains(query, ignoreCase = true)
                }

                // فیلتر محصولات
                val filteredProducts = viewModel.allProducts.value?.filter {
                    it.productName.contains(query, ignoreCase = true)
                } ?: emptyList()


                // تنظیم داده‌ها در دو آداپتر
                materialSelectionAdapter.submitList(filteredMaterials)
                productSelectionAdapter.submitList(filteredProducts)

                // اگر متن خالی شد، لیست کامل را نمایش بده
                if (query.isEmpty()) {
                    materialSelectionAdapter.submitList(allMaterialsList)
                    productSelectionAdapter.submitList(viewModel.allProducts.value ?: emptyList())
                }

                return true
            }
        })

        viewModel.getAllRawMaterials().observe(viewLifecycleOwner) { rawMaterials ->
            allMaterialsList = rawMaterials // ذخیره لیست اصلی

            if (product != null) {
                binding.etProductName.setText(product.productName)
                binding.etProductDescription.setText(product.description)
                if (product.isFinalProduct) {
                    binding.rbFinalProduct.isChecked = true
                } else {
                    binding.rbIntermediateProduct.isChecked = true
                }
                viewModel.getProductDetails(product.productId)
                    .observe(viewLifecycleOwner) { details ->
                        val updatedMaterials = allMaterialsList.map { material ->
                            val detail =
                                details.find { it.materialId == material.materialId && it.type == 0 }
                            if (detail != null) {
                                material.copy(
                                    quantity = detail.quantity, // مقدار ذخیره شده
                                    price = detail.materialPrice // قیمت ذخیره شده
                                )
                            } else {
                                material.copy(
                                    quantity = 0.0,
                                    price = material.price
                                ) // مقدار صفر برای مواد جدید
                            }
                        }

                        val updatedProducts = allProductsList.map { product ->
                            val detail =
                                details.find { it.materialId == product.productId && it.type == 1 }
                            if (detail != null) {
                                product.copy(
                                    quantity = detail.quantity, // مقدار ذخیره شده
                                    price = detail.materialPrice // قیمت ذخیره شده
                                )
                            } else {
                                product.copy(
                                    quantity = 0.0,
                                    price = product.price
                                ) // مقدار صفر برای محصولات جدید
                            }
                        }

                        // مقداردهی اولیه مواد و محصولات انتخاب شده
                        selectedMaterials.clear()
                        selectedMaterials.addAll(updatedMaterials.filter { it.quantity > 0 })

                        selectedProducts.clear()
                        selectedProducts.addAll(updatedProducts.filter { it.quantity > 0 })

                        // ارسال داده به آداپترها
                        materialSelectionAdapter.submitList(updatedMaterials)
                        productSelectionAdapter.submitList(updatedProducts)
                    }
            } else {
                // اگر در حالت افزودن محصول هستیم، فقط لیست مواد اولیه را نمایش بده
                materialSelectionAdapter.submitList(allMaterialsList)
            }

        }

        binding.btnSave.setOnClickListener {
            val productName = binding.etProductName.text.toString()
            val productDescription = binding.etProductDescription.text.toString()

            if (productName.isEmpty()) {
                Toast.makeText(context, R.string.error_enter_product_name, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (selectedMaterials.isEmpty()) {
                Toast.makeText(context, R.string.error_no_material_selected, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (product != null) {

                val isFinal = binding.rbFinalProduct.isChecked

                // ویرایش محصول
                val updatedProduct = product.copy(
                    productName = productName,
                    description = productDescription,
                    isFinalProduct = isFinal
                )
                viewModel.updateProductWithDetails(
                    updatedProduct,
                    selectedMaterials,
                    selectedProducts
                )
            } else {
                val isFinal = binding.rbFinalProduct.isChecked

                val newProduct = Product(
                    productName = productName,
                    description = productDescription,
                    isFinalProduct = isFinal
                )
                viewModel.insertProductWithDetails(
                    newProduct,
                    totalPriceProduct,
                    selectedMaterials,
                    selectedProducts
                )
            }
            dialog?.dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.95).toInt()
        dialog?.window?.setLayout(width, height)
    }
}
