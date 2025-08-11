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
import ir.kitgroup.formula.core.Util.formatQuantity
import ir.kitgroup.formula.adapter.MaterialSelectionAdapter
import ir.kitgroup.formula.adapter.PackagingSelectionAdapter
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.database.entity.Packaging
import ir.kitgroup.formula.databinding.DialogAddEditPackagingBinding
import ir.kitgroup.formula.core.MaterialType
import ir.kitgroup.formula.viewmodel.PackagingViewModel

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

        binding.svPackaging.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText ?: ""

                val filteredMaterials = allMaterialsList.filter {
                    it.materialName.contains(query, ignoreCase = true)
                }

                packagingSelectionAdapter.submitList(filteredMaterials)

                if (query.isEmpty()) {
                    packagingSelectionAdapter.submitList(allMaterialsList)
                }
                return true
            }
        })

        packagingViewModel.getAllRawMaterialsByType(MaterialType.PACKAGING.value)
            .observe(viewLifecycleOwner) { rawMaterials ->
                allMaterialsList = rawMaterials

                if (packaging != null) {
                    binding.etPackagingName.setText(packaging.packagingName)
                    binding.etPackagingWeight.setText(
                        formatQuantity(packaging.weight)
                    )
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
            val packagingWeightText = binding.etPackagingWeight.text.toString()
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
                // ویرایش بسته بندی
                val updatedPackaging = packaging.copy(
                    packagingName = productName,
                    description = productDescription,
                    weight = packagingWeight
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
