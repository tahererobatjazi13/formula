package ir.kitgroup.formula.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.formatDateShamsi
import ir.kitgroup.formula.Util.getTotalPriceForPackaging
import ir.kitgroup.formula.database.entity.Packaging
import ir.kitgroup.formula.databinding.ItemPackagingBinding
import ir.kitgroup.formula.viewmodel.PackagingViewModel
import java.text.DecimalFormat

class PackagingAdapter(
    private val onDelete: (Packaging) -> Unit = {},
    private val onEdit: (Packaging) -> Unit = {},
    private val onClick: (Packaging) -> Unit = {},
    private val packagingViewModel: PackagingViewModel,

    ) : ListAdapter<Packaging, PackagingAdapter.PackagingViewHolder>
    (PackagingDiffCallback()) {
    private val formatter = DecimalFormat("#,###,###,###")
    private var pricePerKg: Double = 0.0
    private val formatterQuantity = DecimalFormat("###,##0.###")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackagingViewHolder {
        val binding =
            ItemPackagingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PackagingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackagingViewHolder, position: Int) {
        val productHeader = getItem(position)
        holder.bind(productHeader)
    }

    inner class PackagingViewHolder(private val binding: ItemPackagingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(packaging: Packaging) {

            val label: String
            val date: Long

            if (packaging.updatedDate > packaging.createdDate) {
                label = binding.root.context.getString(R.string.label_update_date)
                date = packaging.updatedDate
            } else {
                label = binding.root.context.getString(R.string.label_create_date)
                date = packaging.createdDate
            }
            packagingViewModel.getPackageDetails(packaging.packagingId)
                .observeForever { productDetails ->
                    pricePerKg = getTotalPriceForPackaging(
                        productDetails
                    )
                    binding.tvPackagingPrice.text = formatter.format(pricePerKg) + " ریال "
                }
            binding.tvTitlePackagingDate.text = label
            binding.tvPackagingDate.text = formatDateShamsi(date)
            binding.tvPackagingWeight.text = "${formatterQuantity.format(packaging.weight)} گرم"

            binding.tvPackagingName.text = packaging.packagingName
            binding.ivDeletePackaging.setOnClickListener { onDelete(packaging) }
            binding.ivEditPackaging.setOnClickListener { onEdit(packaging) }
            binding.cvMain.setOnClickListener { onClick(packaging) }
        }
    }
}

class PackagingDiffCallback : DiffUtil.ItemCallback<Packaging>() {
    override fun areItemsTheSame(oldItem: Packaging, newItem: Packaging): Boolean {
        return oldItem.packagingId == newItem.packagingId
    }

    override fun areContentsTheSame(oldItem: Packaging, newItem: Packaging): Boolean {
        return oldItem == newItem
    }
}
