package ir.kitgroup.formulaNew.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.core.Util.formatDateShamsi
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.database.entity.ProductHistory
import ir.kitgroup.formulaNew.databinding.ItemUsageBinding

class ProductUsageAdapter(
    private val onClick: (ProductHistory, String, String) -> Unit,
    private val onDelete: (ProductHistory) -> Unit = {},
) :
    ListAdapter<ProductHistory, ProductUsageAdapter.ProductUsageViewHolder>(ProductUsageDiffCallback()) {
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
            val context = binding.root.context
            val attrRes = when {
                item.id == lastInsertedId -> R.attr.colorBackGreen
                adapterPosition % 2 == 0 -> R.attr.colorBackGrayLight
                else -> R.attr.colorBackGrayDark
            }

            binding.root.setBackgroundColor(TypedValue().apply {
                context.theme.resolveAttribute(attrRes, this, true)
            }.data)


            val formattedQuantity = formatQuantity(item.quantity)
            binding.tvQuantity.text = formatQuantity(item.quantity*1000)
            binding.tvTotalPrice.text = Util.priceFormatter.format(item.totalPrice)
            binding.tvUnitPrice.text = Util.priceFormatter.format(item.unitPrice)
            binding.tvDate.text = formatDateShamsi(item.date)

            binding.llMain.setOnClickListener {
                onClick(
                    item, formattedQuantity,
                    item.totalPrice.toString()
                )
            }
            binding.ivDelete.setOnClickListener { onDelete(item) }
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
