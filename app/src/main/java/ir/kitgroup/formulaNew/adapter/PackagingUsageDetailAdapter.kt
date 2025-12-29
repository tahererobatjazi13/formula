package ir.kitgroup.formulaNew.adapter

import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.core.Util.calculatePackagingPrice
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.database.entity.Packaging
import ir.kitgroup.formulaNew.databinding.ItemPackagingUsageDetailBinding
import ir.kitgroup.formulaNew.viewmodel.PackagingViewModel
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat

class PackagingUsageDetailAdapter(
    private val packagingViewModel: PackagingViewModel, private val packagePrice: Double,
    private val totalQty: Double,
    var onQuantityChanged: (() -> Unit)? = null,
    private val onPackagingSelected: (Packaging) -> Unit
) : ListAdapter<Packaging, PackagingUsageDetailAdapter.PackagingUsageDetailViewHolder>(
    PackagingUsageDetailDiffCallback()
) {
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

            val packagingCost = packaging.price
            val totalWeight = packaging.weight
            val productCostForUsed = (totalWeight * packagePrice) / totalQty
            val totalCost = productCostForUsed + packagingCost

            val context = itemView.context
            val attrRes =
                if (adapterPosition % 2 == 0) R.attr.colorBackGrayLight else R.attr.colorBackGrayDark

            binding.root.setBackgroundColor(TypedValue().apply {
                context.theme.resolveAttribute(attrRes, this, true)
            }.data)

            textWatcher?.let { binding.etQuantity.removeTextChangedListener(it) }

            val savedUsedWeight =
                packagingViewModel.getUsedWeightForPackaging(packaging.packagingId)

            val displayQuantity = if (savedUsedWeight > 0) savedUsedWeight else packaging.quantity
            val formatted = formatQuantity(displayQuantity)

            val textToShow = if (displayQuantity == 0.0) "" else formatted

            if (binding.etQuantity.text.toString() != textToShow) {
                binding.etQuantity.setText(textToShow)
            }

            binding.etQuantity.tag = displayQuantity
            binding.tvPrice.text = Util.priceFormatter.format(totalCost)
            updateTotalPrice(totalCost, displayQuantity)

            binding.tvName.setOnClickListener {
                val context = binding.root.context
                packagingViewModel.getPackageDetails(packaging.packagingId)
                    .observeForever { productDetails ->

                        // استفاده از SpannableStringBuilder
                        val message = SpannableStringBuilder()

                        fun appendLineColored(title: String, value: String) {
                            val startTitle = message.length
                            message.append(title)
                            message.setSpan(
                                ForegroundColorSpan(
                                    ContextCompat.getColor(
                                        context,
                                        android.R.color.black
                                    )
                                ),
                                startTitle,
                                message.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            val startValue = message.length
                            message.append(value + "\n")
                            message.setSpan(
                                ForegroundColorSpan(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.color_primary
                                    )
                                ),
                                startValue,
                                message.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }

                        appendLineColored("📦  نام بسته‌بندی:  ", packaging.packagingName)
                        appendLineColored(
                            "⚖️  وزن هر بسته:  ",
                            "${formatQuantity(totalWeight)} گرم"
                        )
                        appendLineColored(
                            "💰  هزینه محصول:  ",
                            "${Util.priceFormatter.format(productCostForUsed)} ریال"
                        )
                        appendLineColored(
                            "🏷️  هزینه بسته‌بندی:  ",
                            "${Util.priceFormatter.format(packaging.price)} ریال"
                        )
                        appendLineColored(
                            "💵  هزینه محصول + بسته‌بندی:  ",
                            "${Util.priceFormatter.format(totalCost)} ریال"
                        )

                        val dialogView = LayoutInflater.from(context)
                            .inflate(R.layout.dialog_package_info_item, null)

                        val tvDialogMessage =
                            dialogView.findViewById<TextView>(R.id.tvDialogMessage)
                        val btnDialogOk = dialogView.findViewById<TextView>(R.id.btnDialogOk)

                        tvDialogMessage.text = message

                        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
                            .setView(dialogView)
                            .create()

                        btnDialogOk.setOnClickListener { dialog.dismiss() }
                        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                        dialog.show()
                    }
            }

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
                            updateTotalPrice(totalCost, count)
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
                        updateTotalPrice(totalCost, count)
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
            binding.tvTotalPrice.text = Util.priceFormatter.format(totalPrice)
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
