package ir.kitgroup.formulaNew.adapter

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.database.entity.ProductDetail
import ir.kitgroup.formulaNew.databinding.ItemUsageDetailBinding
import java.text.DecimalFormat


class ProductUsageDetailAdapter(
    private val onClick: (Int, Int, String, String, String) -> Unit
) : ListAdapter<ProductDetail, ProductUsageDetailAdapter.ViewHolder>(
    ProductUsageDetailDiffCallback()
) {
    private val formatter = DecimalFormat("#,###")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemUsageDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemUsageDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(detail: ProductDetail) = with(binding) {
            if (detail.type == 1)
                tvName.text = detail.materialName+"*"
            else tvName.text = detail.materialName

            tvQuantity.text = formatQuantity(detail.quantity)
            tvTotalPrice.text = formatter.format(detail.price)

            root.setBackgroundColor(TypedValue().apply {
                root.context.theme.resolveAttribute(
                    if (detail.type == 1) R.attr.colorBackPink else R.attr.colorBackGreen,
                    this,
                    true
                )
            }.data)

            root.setOnClickListener(null)
            if (detail.type == 1) {
                root.setOnClickListener {
                    onClick(
                        2,
                        detail.materialId,
                        detail.materialName,
                        detail.quantity.toString(),
                        detail.price.toString()
                    )
                }
            }
        }
    }
}

class ProductUsageDetailDiffCallback : DiffUtil.ItemCallback<ProductDetail>() {
    override fun areItemsTheSame(oldItem: ProductDetail, newItem: ProductDetail) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ProductDetail, newItem: ProductDetail) =
        oldItem == newItem
}
