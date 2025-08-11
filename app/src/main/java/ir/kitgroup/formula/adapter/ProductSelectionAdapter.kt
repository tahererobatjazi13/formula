package ir.kitgroup.formula.adapter


import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formula.R
import ir.kitgroup.formula.core.Util
import ir.kitgroup.formula.core.Util.calculatePrice
import ir.kitgroup.formula.core.Util.calculatePricePerKg
import ir.kitgroup.formula.core.Util.formatQuantity
import ir.kitgroup.formula.core.Util.getTotalPriceForProduct
import ir.kitgroup.formula.core.Util.getTotalQuantityForProduct
import ir.kitgroup.formula.database.entity.Product
import ir.kitgroup.formula.databinding.ItemSelectionBinding
import ir.kitgroup.formula.viewmodel.ProductViewModel

class ProductSelectionAdapter(
    private val onProductSelected: (Product, Double, Boolean) -> Unit,
    private val viewModel: ProductViewModel,
    private val type: Int,
) :
    ListAdapter<Product, ProductSelectionAdapter.MaterialViewHolder>(ProductSelectionDiffCallback()) {
    private var totalPrice: Double = 0.0
    private var totalPriceKg: Double = 0.0
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
        fun bind(product: Product) {

            binding.root.setBackgroundColor(
                ContextCompat.getColor(itemView.context, R.color.color_light_pink)
            )
            binding.tvName.text = product.productName

            val quantity = if (type == 0) 0.0 else product.quantity

            formatQuantity = formatQuantity(quantity)
            binding.etQuantity.setText(formatQuantity)

            var currentTextWatcher: TextWatcher? = null

            viewModel.getProductDetails(product.productId).observeForever { productDetails ->
                val pricePerKg = calculatePricePerKg(
                    getTotalQuantityForProduct(productDetails),
                    getTotalPriceForProduct(productDetails)
                )
                totalPriceKg = pricePerKg
                totalPrice = getTotalPriceForProduct(productDetails)

                binding.tvPrice.text = Util.priceFormatter.format(pricePerKg)

                // حالا با توجه به type، قیمت را محاسبه کن
                if (type == 1) {
                    updateTotalPrice(pricePerKg, quantity)
                } else {
                    updateTotalPrice(product.price, quantity)
                }
            }

            binding.etQuantity.apply {
                currentTextWatcher?.let { removeTextChangedListener(it) }

                currentTextWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val newQuantity = s.toString().toDoubleOrNull() ?: 0.0

                        val price =
                            binding.tvPrice.text.toString().replace(",", "").toDoubleOrNull() ?: 0.0

                        product.quantity = newQuantity
                        binding.tvTotalPrice.text = Util.priceFormatter.format(
                            calculatePrice(newQuantity, price)
                        )
                        onProductSelected(product, totalPriceKg, newQuantity > 0)
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }
                }

                addTextChangedListener(currentTextWatcher)
            }

        }

        private fun updateTotalPrice(price: Double, quantity: Double) {
            val totalPrice = price / 1000 * quantity
            binding.tvTotalPrice.text = Util.priceFormatter.format(totalPrice)
        }
    }
}

class ProductSelectionDiffCallback : DiffUtil.ItemCallback<Product>() {
    override fun areItemsTheSame(oldItem: Product, newItem: Product) =
        oldItem.productId == newItem.productId

    override fun areContentsTheSame(oldItem: Product, newItem: Product) = oldItem == newItem
}
