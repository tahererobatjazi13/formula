package ir.kitgroup.formula.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.calculatePackagingPrice
import ir.kitgroup.formula.Util.formatQuantity
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.databinding.ItemSelectionBinding
import ir.kitgroup.formula.model.MaterialNature
import java.text.DecimalFormat

class PackagingSelectionAdapter(
    private val onMaterialSelected: (Material, Boolean) -> Unit
) : ListAdapter<Material, PackagingSelectionAdapter.PackagingViewHolder>(
    PackagingSelectionDiffCallback()
) {

    private val formatter = DecimalFormat("#,###,###,###")
    private var formatQuantity: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackagingViewHolder {
        val binding =
            ItemSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PackagingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackagingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PackagingViewHolder(val binding: ItemSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var textWatcher: TextWatcher? = null

        fun bind(material: Material) {
            binding.tvName.text = material.materialName
            if (material.nature == MaterialNature.PHYSICAL.value)
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.color_light_orange
                    )
                )
            else binding.root.setBackgroundColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.color_light_green
                )
            )

            textWatcher?.let { binding.etQuantity.removeTextChangedListener(it) }

            val quantity = material.quantity

            // فقط در صورتی مقدار را تنظیم کن که قبلاً تنظیم نشده باشد
            if (binding.etQuantity.tag != quantity) {
                formatQuantity = formatQuantity(quantity)
                binding.etQuantity.setText(formatQuantity)
                binding.etQuantity.tag =
                    quantity
            }

            binding.etQuantity.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val newQuantity = binding.etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                    material.quantity = newQuantity
                    binding.etQuantity.tag = newQuantity
                }
            }

            binding.tvPrice.text = formatter.format(material.price)
            updateTotalPrice(material.price, quantity)

            textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val newQuantity = s.toString().toDoubleOrNull() ?: 0.0
                    val price = material.price

                    if (material.quantity != newQuantity) {
                        material.quantity = newQuantity
                        updateTotalPrice(price, newQuantity)
                        onMaterialSelected(material, newQuantity > 0)
                        binding.etQuantity.tag = newQuantity
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }

            binding.etQuantity.addTextChangedListener(textWatcher)
        }

        private fun updateTotalPrice(price: Double, quantity: Double) {
            val totalPrice = calculatePackagingPrice(quantity, price)
            binding.tvTotalPrice.text = formatter.format(totalPrice)
        }
    }

}

class PackagingSelectionDiffCallback : DiffUtil.ItemCallback<Material>() {
    override fun areItemsTheSame(oldItem: Material, newItem: Material) =
        oldItem.materialId == newItem.materialId

    override fun areContentsTheSame(oldItem: Material, newItem: Material) = oldItem == newItem
}
