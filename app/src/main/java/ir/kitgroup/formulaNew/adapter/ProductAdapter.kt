package ir.kitgroup.formulaNew.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.core.Util.calculatePricePerKg
import ir.kitgroup.formulaNew.core.Util.formatDateShamsi
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.core.Util.getTotalPriceForProduct
import ir.kitgroup.formulaNew.core.Util.getTotalQuantityForProduct
import ir.kitgroup.formulaNew.database.entity.Product
import ir.kitgroup.formulaNew.databinding.ItemProductBinding
import ir.kitgroup.formulaNew.viewmodel.ProductViewModel

class ProductAdapter(
    private val onUsage: (Product) -> Unit = {},
    private val onChangeLog: (Product) -> Unit = {},
    private val onDelete: (Product) -> Unit = {},
    private val onEdit: (Product) -> Unit = {},
    private val onClick: (Product) -> Unit = {},
    private val viewModel: ProductViewModel,

    ) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {
    private var totalPrice: Double = 0.0
    private var totalPriceKg: Double = 0.0
    private var pricePerKg: Double = 0.0

    private var totalQuantity: Double = 0.0
    private var formatTotalQuantity: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding =
            ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val productHeader = getItem(position)
        holder.bind(productHeader)
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(product: Product) {

            val label: String
            val date: Long

            if (product.updatedDate > product.createdDate) {
                label = binding.root.context.getString(R.string.label_update_date)
                date = product.updatedDate
            } else {
                label = binding.root.context.getString(R.string.label_create_date)
                date = product.createdDate
            }

            binding.tvTitleProductDate.text = label
            binding.tvProductDate.text = formatDateShamsi(date)

            binding.tvProductName.text = product.productName
            binding.ivDeleteProduct.setOnClickListener { onDelete(product) }
            binding.ivEditProduct.setOnClickListener { onEdit(product) }
            binding.cvMain.setOnClickListener { onClick(product) }
            binding.btnChangeLog.setOnClickListener { onChangeLog(product) }
            binding.btnUsageProduct.setOnClickListener { onUsage(product) }

            viewModel.getProductDetails(product.productId).observeForever { productDetails ->
                pricePerKg = calculatePricePerKg(
                    getTotalQuantityForProduct(productDetails),
                    getTotalPriceForProduct(productDetails)
                )
                totalPriceKg = pricePerKg
                totalPrice = getTotalPriceForProduct(productDetails)

                totalQuantity = productDetails.sumOf { it.quantity }
                formatTotalQuantity = formatQuantity(totalQuantity)
                binding.tvProductAmount.text = "$formatTotalQuantity گرم"

                binding.tvProductPrice.text = Util.priceFormatter.format(totalPrice) + " ریال "
                binding.tvProductPriceKg.text = Util.priceFormatter.format(pricePerKg) + " ریال "
            }
        }
    }
}

class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
    override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem.productId == newItem.productId
    }

    override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem == newItem
    }
}
