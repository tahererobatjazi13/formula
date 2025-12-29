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
import ir.kitgroup.formulaNew.database.entity.MaterialChangeLog
import ir.kitgroup.formulaNew.databinding.ItemChangeLogBinding

class ChangeLogAdapter :
    ListAdapter<MaterialChangeLog, ChangeLogAdapter.MaterialViewHolder>(ChangeLogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val binding =
            ItemChangeLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MaterialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        val material = getItem(position)
        holder.bind(material)
    }

    inner class MaterialViewHolder(private val binding: ItemChangeLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(material: MaterialChangeLog) {

            val context = itemView.context
            val attrRes = if (adapterPosition % 2 == 0) R.attr.colorBackGrayLight else R.attr.colorBackGrayDark

            binding.root.setBackgroundColor(TypedValue().apply {
                context.theme.resolveAttribute(attrRes, this, true)
            }.data)

            binding.tvChangeDate.text = formatDateShamsi(material.changeDate)
            binding.tvName.text = material.materialName
            binding.tvOldPrice.text = Util.priceFormatter.format(material.oldValue)
            binding.tvNewPrice.text = Util.priceFormatter.format(material.newValue)
        }
    }
}

class ChangeLogDiffCallback : DiffUtil.ItemCallback<MaterialChangeLog>() {
    override fun areItemsTheSame(oldItem: MaterialChangeLog, newItem: MaterialChangeLog): Boolean {
        return oldItem.materialId == newItem.materialId
    }

    override fun areContentsTheSame(
        oldItem: MaterialChangeLog,
        newItem: MaterialChangeLog
    ): Boolean {
        return oldItem == newItem
    }
}
