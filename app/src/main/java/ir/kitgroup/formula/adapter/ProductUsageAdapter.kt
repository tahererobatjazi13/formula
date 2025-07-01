package ir.kitgroup.formula.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.formatDateShamsi
import ir.kitgroup.formula.Util.formatQuantity
import ir.kitgroup.formula.database.entity.ProductHistory
import ir.kitgroup.formula.databinding.ItemUsageBinding
import java.text.DecimalFormat

class ProductUsageAdapter(
    private val onClick: (ProductHistory, String, String) -> Unit
) :
    ListAdapter<ProductHistory, ProductUsageAdapter.ProductUsageViewHolder>(ProductUsageDiffCallback()) {
    private val formatter = DecimalFormat("#,###,###,###")
    private var lastInsertedId: Long? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductUsageViewHolder {
        val binding =
            ItemUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductUsageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductUsageViewHolder, position: Int) {
        val material = getItem(position)
        holder.bind(material)
    }

    inner class ProductUsageViewHolder(private val binding: ItemUsageBinding) :

        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductHistory) {

            // پس‌زمینه براساس ID جدید یا سطر زوج/فرد
            val bgColorRes = when {
                item.id == lastInsertedId -> R.color.color_light_green
                position % 2 == 0 -> R.color.gray_light
                else -> R.color.gray_dark
            }

            binding.root.setBackgroundColor(ContextCompat.getColor(binding.root.context, bgColorRes))

            val formattedQuantity = formatQuantity(item.quantity)
            binding.tvQuantity.text = formattedQuantity
            binding.tvTotalPrice.text = formatter.format(item.totalPrice)
            binding.tvUnitPrice.text = formatter.format(item.unitPrice)
            binding.tvDate.text = formatDateShamsi(item.date)

            binding.llMain.setOnClickListener { onClick(item, formattedQuantity,
                item.totalPrice.toString()
            ) }
        }
    }

    fun setLastInsertedId(id: Long) {
        lastInsertedId = id
        // فقط آیتم جدید و قبلی را به‌روزرسانی کن
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) notifyItemChanged(index)

        val previousIndex = currentList.indexOfFirst { it.id == lastInsertedId && it.id != id }
        if (previousIndex != -1) notifyItemChanged(previousIndex)
    }
}

class ProductUsageDiffCallback : DiffUtil.ItemCallback<ProductHistory>() {
    override fun areItemsTheSame(oldItem: ProductHistory, newItem: ProductHistory): Boolean {
        return oldItem.productId == newItem.productId
    }

    override fun areContentsTheSame(
        oldItem: ProductHistory,
        newItem: ProductHistory
    ): Boolean {
        return oldItem == newItem
    }
}
