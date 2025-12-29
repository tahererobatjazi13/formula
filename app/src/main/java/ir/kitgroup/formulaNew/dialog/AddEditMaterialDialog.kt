package ir.kitgroup.formulaNew.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.database.entity.Material
import ir.kitgroup.formulaNew.databinding.DialogAddEditMaterialBinding
import ir.kitgroup.formulaNew.core.MaterialNature
import ir.kitgroup.formulaNew.core.MaterialType
import ir.kitgroup.formulaNew.core.Util

class AddEditMaterialDialog(
    private val material: Material? = null,
    private val defaultType: String = MaterialType.MATERIAL.value,
    private val onSave: (Material) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogAddEditMaterialBinding

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAddEditMaterialBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        if (material != null) {
            when (material.type) {
                MaterialType.MATERIAL.value -> {

                    binding.tvTitleMaterialName.text =
                        getString(R.string.label_material_name_star)
                    binding.tvTitleMaterialPrice.text =
                        getString(R.string.label_price_star)
                    binding.tvTitleDialog.text =
                        getString(R.string.label_edit_material)
                    binding.rgNature.visibility = View.GONE
                }

                MaterialType.PACKAGING.value -> {
                    binding.tvTitleMaterialName.text =
                        getString(R.string.label_packaging_services_star)
                    binding.tvTitleMaterialPrice.text =
                        getString(R.string.label_price_item_star)
                    binding.tvTitleDialog.text =
                        getString(R.string.label_edit_packaging)
                    binding.rgNature.visibility = View.VISIBLE
                }
            }
            when (material.nature) {
                MaterialNature.PHYSICAL.value -> {
                    binding.rbPhysical.isChecked =true
                }

                MaterialNature.VIRTUAL.value -> {
                    binding.rbVirtual.isChecked =true
                }
            }
        } else {
            when (defaultType) {
                MaterialType.MATERIAL.value -> {
                    binding.tvTitleMaterialName.text =
                        getString(R.string.label_material_name_star)
                    binding.tvTitleMaterialPrice.text =
                        getString(R.string.label_price_star)
                    binding.tvTitleDialog.text = getString(R.string.label_add_material)
                    binding.rgNature.visibility = View.GONE
                }

                MaterialType.PACKAGING.value -> {
                    binding.tvTitleMaterialName.text =
                        getString(R.string.label_packaging_services_star)
                    binding.tvTitleMaterialPrice.text =
                        getString(R.string.label_price_item_star)
                    binding.tvTitleDialog.text = getString(R.string.label_add_packaging)
                    binding.rgNature.visibility = View.VISIBLE
                }
            }
        }

        material?.let {
            binding.etMaterialName.setText(it.materialName)
            binding.etMaterialPrice.setText(Util.priceFormatter.format(it.price) + "")
        }

        binding.etMaterialPrice.addTextChangedListener(object : TextWatcher {
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
                        binding.etMaterialPrice.setText(formatted)
                        binding.etMaterialPrice.setSelection(formatted.length)
                    }
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }

                isEditing = false
            }
        })
        binding.btnSave.setOnClickListener {
            val name = binding.etMaterialName.text.toString()
            val price = binding.etMaterialPrice.text.toString().replace(",", "")
            val priceValue = price.toDoubleOrNull() ?: -1.0

            if (name.isNotBlank() && price.isNotBlank()) {
                val nature =
                    if (binding.rbPhysical.isChecked) MaterialNature.PHYSICAL.value else MaterialNature.VIRTUAL.value

                val resultMaterial =
                    material?.copy(
                        materialName = name,
                        price = priceValue,
                        type = material.type,
                        nature = nature
                    )
                        ?: Material(
                            materialName = name,
                            price = priceValue,
                            type = defaultType,
                            nature = nature
                        )

                onSave(resultMaterial)
                dismiss()
            } else {
                Toast.makeText(context, R.string.error_request_fields, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt() // 85% عرض صفحه
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}

