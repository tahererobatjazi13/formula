package ir.kitgroup.formula.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import ir.kitgroup.formula.R
import ir.kitgroup.formula.databinding.DialogConfirmDeleteBinding


class ConfirmDeleteDialog(
    private val onDeleteConfirmed: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DialogConfirmDeleteBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        binding.btnConfirm.setOnClickListener {
            onDeleteConfirmed()
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        return binding.root
    }
}
