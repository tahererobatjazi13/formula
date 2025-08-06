package ir.kitgroup.formula.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.calculatePackagingPrice
import ir.kitgroup.formula.Util.formatQuantity
import ir.kitgroup.formula.database.entity.Packaging
import ir.kitgroup.formula.databinding.ItemPackagingUsageDetailBinding
import ir.kitgroup.formula.viewmodel.PackagingViewModel
import java.text.DecimalFormat

class PackagingUsageDetailAdapter(
    private val packagingViewModel: PackagingViewModel,
    var onQuantityChanged: (() -> Unit)? = null,
    private val onPackagingSelected: (Packaging) -> Unit
) : ListAdapter<Packaging, PackagingUsageDetailAdapter.PackagingUsageDetailViewHolder>(
    PackagingUsageDetailDiffCallback()
) {
    private val formatter = DecimalFormat("#,###,###,###")
    private var textWatcher: TextWatcher? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PackagingUsageDetailViewHolder {
        val binding = ItemPackagingUsageDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PackagingUsageDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackagingUsageDetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PackagingUsageDetailViewHolder(private val binding: ItemPackagingUsageDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(packaging: Packaging) {
            binding.tvName.text = packaging.packagingName
            binding.tvWeight.text = formatQuantity(packaging.weight)

            binding.root.setBackgroundColor(
                ContextCompat.getColor(itemView.context, R.color.color_light_green)
            )

            textWatcher?.let { binding.etQuantity.removeTextChangedListener(it) }

            val savedUsedWeight =
                packagingViewModel.getUsedWeightForPackaging(packaging.packagingId)

            val displayQuantity = if (savedUsedWeight > 0) savedUsedWeight else packaging.quantity
            val formatted = formatQuantity(displayQuantity)
            if (binding.etQuantity.text.toString() != formatted) {
                binding.etQuantity.setText(formatted)
            }
            binding.etQuantity.tag = displayQuantity

            binding.tvPrice.text = formatter.format(packaging.price)
            updateTotalPrice(packaging.price, displayQuantity)

            textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val count = s.toString().toDoubleOrNull() ?: 0.0
                    val unitWeight = packaging.weight

                    if (packaging.quantity != count) {
                        if (packagingViewModel.canUseWeight(
                                count,
                                unitWeight,
                                packaging.packagingId
                            )
                        ) {
                            packaging.quantity = count
                            onPackagingSelected(packaging)
                            updateTotalPrice(packaging.price, count)
                            binding.etQuantity.tag = count

                            packagingViewModel.updateUsedWeightForPackaging(
                                packaging.packagingId,
                                count,
                                unitWeight
                            )
                        } else {
                            val previousValid = binding.etQuantity.tag as? Double ?: 0.0
                            val formattedPrev = formatQuantity(previousValid)

                            binding.etQuantity.removeTextChangedListener(this)
                            binding.etQuantity.setText(formattedPrev)
                            binding.etQuantity.setSelection(formattedPrev.length)
                            binding.etQuantity.addTextChangedListener(this)

                            Toast.makeText(
                                binding.root.context,
                                R.string.error_entered_quantity_greater_total_weight,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        onPackagingSelected(packaging)
                        updateTotalPrice(packaging.price, count)
                        packagingViewModel.updateUsedWeightForPackaging(
                            packaging.packagingId,
                            count,
                            unitWeight
                        )
                        onQuantityChanged?.invoke()

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

class PackagingUsageDetailDiffCallback : DiffUtil.ItemCallback<Packaging>() {
    override fun areItemsTheSame(oldItem: Packaging, newItem: Packaging) =
        oldItem.packagingId == newItem.packagingId

    override fun areContentsTheSame(oldItem: Packaging, newItem: Packaging) =
        oldItem.packagingId == newItem.packagingId &&
                oldItem.packagingName == newItem.packagingName &&
                oldItem.weight == newItem.weight &&
                oldItem.price == newItem.price &&
                oldItem.quantity == newItem.quantity
}
