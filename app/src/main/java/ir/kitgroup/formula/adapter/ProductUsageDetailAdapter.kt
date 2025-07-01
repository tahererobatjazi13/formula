package ir.kitgroup.formula.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.formatQuantity
import ir.kitgroup.formula.database.entity.ProductDetail
import ir.kitgroup.formula.databinding.ItemUsageDetailBinding
import java.text.DecimalFormat


class ProductUsageDetailAdapter(
    private val onClick: (Int,Int, String, String, String) -> Unit
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

        fun bind(detail: ProductDetail) = with(binding) {

            tvName.text = detail.materialName
            tvQuantity.text = formatQuantity(detail.quantity)
            tvTotalPrice.text = formatter.format(detail.price)

            val bgColorRes = if (detail.type == 1)
                R.color.color_light_pink
            else
                R.color.color_light_green

            root.setBackgroundColor(ContextCompat.getColor(root.context, bgColorRes))

            root.setOnClickListener(null)
            if (detail.type == 1) {
                root.setOnClickListener {
                    onClick(2,
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
