package ir.kitgroup.formula.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.calculatePackagingPrice
import ir.kitgroup.formula.Util.formatQuantity
import ir.kitgroup.formula.database.entity.PackagingDetail
import ir.kitgroup.formula.databinding.ItemSelectionBinding
import ir.kitgroup.formula.viewmodel.PackagingViewModel
import java.text.DecimalFormat

class PackageDetailAdapter(
    private val packagingViewModel: PackagingViewModel,
) : ListAdapter<PackagingDetail, PackageDetailAdapter.PackageDetailViewHolder>(
    PackagingDetailDiffCallback()
) {
    private val formatter = DecimalFormat("#,###,###,###")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageDetailViewHolder {
        val binding =
            ItemSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PackageDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackageDetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PackageDetailViewHolder(private val binding: ItemSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(packagingDetail: PackagingDetail) {

            binding.root.setBackgroundColor(
                ContextCompat.getColor(itemView.context, R.color.color_light_green)
            )
            packagingViewModel.getPackageDetails(packagingDetail.packagingId)
                .observeForever { productDetails ->

                    binding.tvPrice.text = formatter.format(packagingDetail.materialPrice)
                    binding.tvTotalPrice.text = formatter.format(
                        calculatePackagingPrice(
                            packagingDetail.quantity,
                            packagingDetail.materialPrice
                        )
                    )
                }

            binding.tvName.text = packagingDetail.materialName
            binding.etQuantity.isEnabled = false
            binding.tvPrice.isEnabled = false

            val formattedQuantity = formatQuantity(packagingDetail.quantity)
            binding.etQuantity.setText(formattedQuantity)
        }
    }
}

class PackagingDetailDiffCallback : DiffUtil.ItemCallback<PackagingDetail>() {
    override fun areItemsTheSame(oldItem: PackagingDetail, newItem: PackagingDetail) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: PackagingDetail, newItem: PackagingDetail) =
        oldItem == newItem
}
