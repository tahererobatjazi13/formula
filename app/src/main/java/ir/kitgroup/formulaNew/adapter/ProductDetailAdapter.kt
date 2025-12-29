package ir.kitgroup.formulaNew.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.core.Util.calculatePrice
import ir.kitgroup.formulaNew.core.Util.calculatePricePerKg
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.core.Util.getTotalPriceForProduct
import ir.kitgroup.formulaNew.core.Util.getTotalQuantityForProduct
import ir.kitgroup.formulaNew.database.entity.ProductDetail
import ir.kitgroup.formulaNew.databinding.ItemProductDetailBinding
import ir.kitgroup.formulaNew.viewmodel.ProductViewModel
import java.text.DecimalFormat
import java.util.Locale

class ProductDetailAdapter(
    private val onClick: (Int) -> Unit = {}, private val productViewModel: ProductViewModel,
) : ListAdapter<ProductDetail, ProductDetailAdapter.ProductDetailViewHolder>(
    ProductDetailDiffCallback()
) {
    private var totalPrice: Double = 0.0
    private var totalPriceKg: Double = 0.0
    private var totalQuantityAll: Double = 0.0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductDetailViewHolder {
        val binding =
            ItemProductDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductDetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductDetailViewHolder(private val binding: ItemProductDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(material: ProductDetail) {

            if (material.type == 1) {
                binding.tvName.text = material.materialName + "*"

                val color =
                    MaterialColors.getColor(itemView.context, R.attr.colorBackPink, Color.BLACK)
                binding.root.setBackgroundColor(color)

                productViewModel.getProductDetails(material.materialId)
                    .observeForever { productDetails ->
                        totalPrice = getTotalPriceForProduct(productDetails)
                        totalPriceKg = calculatePricePerKg(
                            getTotalQuantityForProduct(productDetails),
                            getTotalPriceForProduct(productDetails)
                        )
                        binding.tvPrice.text = Util.priceFormatter.format(totalPriceKg)
                        binding.tvTotalPrice.text = Util.priceFormatter.format(
                            calculatePrice(material.quantity, totalPriceKg)
                        )
                    }
            } else {
                val color =
                    MaterialColors.getColor(itemView.context, R.attr.colorBackGreen, Color.BLACK)
                binding.root.setBackgroundColor(color)
                binding.tvName.text = material.materialName

                binding.tvPrice.text = Util.priceFormatter.format(material.materialPrice)
                binding.tvTotalPrice.text = Util.priceFormatter.format(
                    calculatePrice(material.quantity, material.materialPrice)
                )
            }

            // محاسبه درصد
            if (totalQuantityAll > 0) {
                val percent = (material.quantity / totalQuantityAll) * 100

                val df = DecimalFormat("0.##")
                binding.tvPercent.text = df.format(percent)
            } else {
                binding.tvPercent.text = "-"
            }
            binding.etQuantity.isEnabled = false
            binding.tvPrice.isEnabled = false

            val formattedQuantity = formatQuantity(material.quantity)
            binding.etQuantity.setText(formattedQuantity)

            if (material.type == 1)
                binding.root.setOnClickListener { onClick(material.materialId) }
        }
    }

    fun setTotalQuantity(total: Double) {
        totalQuantityAll = total
        notifyDataSetChanged()
    }
}


class ProductDetailDiffCallback : DiffUtil.ItemCallback<ProductDetail>() {
    override fun areItemsTheSame(oldItem: ProductDetail, newItem: ProductDetail) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ProductDetail, newItem: ProductDetail) =
        oldItem == newItem
}
