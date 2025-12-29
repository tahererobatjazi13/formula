package ir.kitgroup.formulaNew.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.core.Util.calculatePackagingPrice
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.database.entity.PackagingDetail
import ir.kitgroup.formulaNew.databinding.ItemSelectionBinding
import ir.kitgroup.formulaNew.viewmodel.PackagingViewModel

class PackageDetailAdapter(
    private val packagingViewModel: PackagingViewModel,
) : ListAdapter<PackagingDetail, PackageDetailAdapter.PackageDetailViewHolder>(
    PackagingDetailDiffCallback()
) {
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

            val context = itemView.context
            val attrRes = if (adapterPosition % 2 == 0) R.attr.colorBackGrayLight else R.attr.colorBackGrayDark

            binding.root.setBackgroundColor(TypedValue().apply {
                context.theme.resolveAttribute(attrRes, this, true)
            }.data)

            packagingViewModel.getPackageDetails(packagingDetail.packagingId)
                .observeForever { productDetails ->

                    binding.tvPrice.text = Util.priceFormatter.format(packagingDetail.materialPrice)
                    binding.tvTotalPrice.text = Util.priceFormatter.format(
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
