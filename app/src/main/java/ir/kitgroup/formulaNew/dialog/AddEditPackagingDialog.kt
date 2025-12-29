package ir.kitgroup.formulaNew.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.adapter.MaterialSelectionAdapter
import ir.kitgroup.formulaNew.adapter.PackagingSelectionAdapter
import ir.kitgroup.formulaNew.database.entity.Material
import ir.kitgroup.formulaNew.database.entity.Packaging
import ir.kitgroup.formulaNew.databinding.DialogAddEditPackagingBinding
import ir.kitgroup.formulaNew.core.MaterialType
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.viewmodel.PackagingViewModel

class AddEditPackagingDialog(
    private val packagingViewModel: PackagingViewModel,
    private val packaging: Packaging? = null
) : DialogFragment() {
    private var _binding: DialogAddEditPackagingBinding? = null
    private val binding get() = _binding!!
    private lateinit var packagingSelectionAdapter: PackagingSelectionAdapter
    private lateinit var filteredMaterialsList: List<Material>
    private lateinit var allMaterialsList: List<Material>
    private val selectedMaterials = mutableListOf<Material>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditPackagingBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchEditText()
        if (packaging == null) {
            binding.tvTitleDialog.text = getString(R.string.label_add_new_packaging)
        } else {
            binding.tvTitleDialog.text = getString(R.string.label_edit_new_packaging)
        }

        packagingViewModel.getAllRawMaterialsByType(MaterialType.PACKAGING.value)
            .observe(viewLifecycleOwner) { materials ->
                allMaterialsList = materials
                packagingSelectionAdapter.submitList(materials)
            }

        packagingSelectionAdapter = PackagingSelectionAdapter { material, isSelected ->
            if (isSelected) {
                if (!selectedMaterials.contains(material)) {
                    selectedMaterials.add(material)
                }
            } else {
                selectedMaterials.remove(material)
            }
        }
        binding.etPackagingWeight.addTextChangedListener(object : TextWatcher {
            private var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            @SuppressLint("DefaultLocale")
            override fun afterTextChanged(s: Editable?) {
                if (isEditing || s.isNullOrEmpty()) return

                isEditing = true

                val cleanString = s.toString().replace(",", "")

                try {
                    val parsed = cleanString.toDoubleOrNull()
                    if (parsed != null) {
                        val formatted = String.format("%,.0f", parsed)
                        binding.etPackagingWeight.setText(formatted)
                        binding.etPackagingWeight.setSelection(formatted.length)
                    }
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }

                isEditing = false
            }
        })

        for (material in selectedMaterials) {
            val viewHolder = binding.rvPackaging.findViewHolderForAdapterPosition(
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

        binding.rvPackaging.adapter = packagingSelectionAdapter
        binding.rvPackaging.layoutManager = LinearLayoutManager(requireContext())
        filteredMaterialsList = selectedMaterials


        packagingViewModel.getAllRawMaterialsByType(MaterialType.PACKAGING.value)
            .observe(viewLifecycleOwner) { rawMaterials ->
                allMaterialsList = rawMaterials

                if (packaging != null) {
                    binding.etPackagingName.setText(packaging.packagingName)
                /*    binding.etPackagingWeight.setText(
                        formatQuantity(packaging.weight)*/
                                binding.etPackagingWeight.setText(Util.priceFormatter.format(packaging.weight) + "")

                    binding.etPackagingDescription.setText(packaging.description)

                    packagingViewModel.getPackageDetails(packaging.packagingId)
                        .observe(viewLifecycleOwner) { details ->
                            val updatedMaterials = allMaterialsList.map { material ->
                                val detail =
                                    details.find { it.materialId == material.materialId }
                                if (detail != null) {
                                    material.copy(
                                        quantity = detail.quantity,
                                        price = detail.materialPrice
                                    )
                                } else {
                                    material.copy(
                                        quantity = 0.0,
                                        price = material.price
                                    )
                                }
                            }
                            selectedMaterials.clear()
                            selectedMaterials.addAll(updatedMaterials.filter { it.quantity > 0 })

                            packagingSelectionAdapter.submitList(updatedMaterials)
                        }
                } else {
                    packagingSelectionAdapter.submitList(allMaterialsList)
                }
            }

        binding.btnSave.setOnClickListener {
            val productName = binding.etPackagingName.text.toString()
            val productDescription = binding.etPackagingDescription.text.toString()
            val packagingWeightText = binding.etPackagingWeight.text.toString().replace(",", "")

            val packagingWeight = packagingWeightText.toDoubleOrNull() ?: 0.0

            if (productName.isEmpty()) {
                Toast.makeText(context, R.string.error_enter_packaging_name, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (packagingWeightText.isEmpty()) {
                Toast.makeText(context, R.string.error_enter_packaging_weight, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (selectedMaterials.isEmpty()) {
                Toast.makeText(context, R.string.error_no_packaging_selected, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (packaging != null) {
                val totalPrice = selectedMaterials.sumOf { it.quantity * it.price }
                // ویرایش بسته بندی
                val updatedPackaging = packaging.copy(
                    packagingName = productName,
                    description = productDescription,
                    weight = packagingWeight,
                    price = totalPrice
                )
                packagingViewModel.updatePackagingWithDetails(
                    updatedPackaging,
                    selectedMaterials
                )
            } else {
                val totalPrice = selectedMaterials.sumOf { it.quantity * it.price }

                val newPackaging = Packaging(
                    packagingName = productName,
                    description = productDescription,
                    price = totalPrice,
                    weight = packagingWeight
                )
                packagingViewModel.insertPackagingWithDetails(
                    newPackaging,
                    selectedMaterials,
                )
            }
            dialog?.dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setupSearchEditText() {
        binding.etSearch.addTextChangedListener { editable ->
            val query = editable?.toString().orEmpty()
            binding.ivClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE

            val filteredMaterials = if (query.isEmpty()) {
                allMaterialsList
            } else {
                allMaterialsList.filter {
                    it.materialName.contains(query, ignoreCase = true)
                }
            }

            packagingSelectionAdapter.submitList(filteredMaterials)
            binding.tvNoItem.visibility =
                if (filteredMaterials.isEmpty()) View.VISIBLE else View.GONE
        }
        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.ivClearSearch.visibility = View.GONE
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
