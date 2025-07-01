package ir.kitgroup.formula.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.calculatePrice
import ir.kitgroup.formula.Util.calculatePricePerKg
import ir.kitgroup.formula.Util.formatQuantity
import ir.kitgroup.formula.database.entity.ProductDetail
import ir.kitgroup.formula.databinding.ItemSelectionBinding
import ir.kitgroup.formula.viewmodel.ProductViewModel
import java.text.DecimalFormat

class ProductDetailAdapter(
    private val onClick: (Int) -> Unit = {}, private val productViewModel: ProductViewModel,
) : ListAdapter<ProductDetail, ProductDetailAdapter.ProductDetailViewHolder>(
    ProductDetailDiffCallback()
) {
    private val formatter = DecimalFormat("#,###,###,###")
    private var totalPrice: Double = 0.0
    private var totalPriceKg: Double = 0.0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductDetailViewHolder {
        val binding =
            ItemSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductDetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductDetailViewHolder(private val binding: ItemSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(material: ProductDetail) {

            if (material.type == 1) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.color_light_pink)
                )
                productViewModel.getProductDetails(material.materialId)
                    .observeForever { productDetails ->
                        totalPrice = getTotalPriceForProduct(productDetails)
                        totalPriceKg = calculatePricePerKg(
                            getTotalQuantityForProduct(productDetails),
                            getTotalPriceForProduct(productDetails)
                        )
                        binding.tvPrice.text = formatter.format(totalPriceKg)
                        binding.tvTotalPrice.text = formatter.format(
                            calculatePrice(material.quantity, totalPriceKg)
                        )
                    }
            } else {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.color_light_green)
                )
                binding.tvPrice.text = formatter.format(material.materialPrice)
                binding.tvTotalPrice.text = formatter.format(
                    calculatePrice(material.quantity, material.materialPrice)
                )
            }

            binding.tvName.text = material.materialName
            binding.etQuantity.isEnabled = false
            binding.tvPrice.isEnabled = false

            val formattedQuantity = formatQuantity(material.quantity)
            binding.etQuantity.setText(formattedQuantity)

            if (material.type == 1)
                binding.root.setOnClickListener { onClick(material.materialId) }

        }
    }
}


class ProductDetailDiffCallback : DiffUtil.ItemCallback<ProductDetail>() {
    override fun areItemsTheSame(oldItem: ProductDetail, newItem: ProductDetail) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ProductDetail, newItem: ProductDetail) =
        oldItem == newItem
}
