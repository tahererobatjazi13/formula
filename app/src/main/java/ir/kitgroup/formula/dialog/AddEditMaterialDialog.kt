package ir.kitgroup.formula.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import ir.kitgroup.formula.R
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.databinding.DialogAddEditMaterialBinding
import java.text.DecimalFormat

class AddEditMaterialDialog(
    private val material: Material? = null,
    private val onSave: (Material) -> Unit
) : DialogFragment() {
    private val formatter = DecimalFormat("#,###,###,###")

    private lateinit var binding: DialogAddEditMaterialBinding

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAddEditMaterialBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        binding.tvTitleDialog.text = if (material == null) {
            getString(R.string.label_add_material)
        } else {
            getString(R.string.label_edit_material)
        }
        material?.let {
            binding.etMaterialName.setText(it.materialName)
            binding.etMaterialPrice.setText(formatter.format(it.price) + "")
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
                val resultMaterial = if (material != null)
                    Material(material.materialId, name, priceValue)
                else
                    Material(materialName = name, price = priceValue)

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

