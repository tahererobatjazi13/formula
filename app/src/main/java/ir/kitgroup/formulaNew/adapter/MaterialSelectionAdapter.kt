package ir.kitgroup.formulaNew.adapter

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.core.Util.calculatePrice
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.database.entity.Material
import ir.kitgroup.formulaNew.databinding.ItemSelectionBinding

class MaterialSelectionAdapter(
    private val onMaterialSelected: (Material, Boolean) -> Unit
) : ListAdapter<Material, MaterialSelectionAdapter.MaterialViewHolder>(MaterialSelectionDiffCallback()) {

    private var formatQuantity: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val binding =
            ItemSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MaterialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MaterialViewHolder(val binding: ItemSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var textWatcher: TextWatcher? = null

        fun bind(material: Material) {
            binding.tvName.text = material.materialName

            val color =
                MaterialColors.getColor(itemView.context, R.attr.colorBackGreen, Color.BLACK)
            binding.root.setBackgroundColor(color)

            textWatcher?.let { binding.etQuantity.removeTextChangedListener(it) }

            val quantity = material.quantity

            if (binding.etQuantity.tag != quantity) {
                val textToShow = if (quantity == 0.0) "" else formatQuantity(quantity)
                binding.etQuantity.setText(textToShow)
                binding.etQuantity.tag = quantity
            }

            binding.etQuantity.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val newQuantity = binding.etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                    material.quantity = newQuantity
                    binding.etQuantity.tag = newQuantity
                }
            }

            binding.tvPrice.text = Util.priceFormatter.format(material.price)
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
            val totalPrice = calculatePrice(quantity, price)
            binding.tvTotalPrice.text = Util.priceFormatter.format(totalPrice)
        }
    }
}

class MaterialSelectionDiffCallback : DiffUtil.ItemCallback<Material>() {
    override fun areItemsTheSame(oldItem: Material, newItem: Material) =
        oldItem.materialId == newItem.materialId

    override fun areContentsTheSame(oldItem: Material, newItem: Material) = oldItem == newItem
}
