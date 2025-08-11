package ir.kitgroup.formula.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formula.R
import ir.kitgroup.formula.core.Util
import ir.kitgroup.formula.core.Util.formatDateShamsi
import ir.kitgroup.formula.database.entity.MaterialChangeLog
import ir.kitgroup.formula.databinding.ItemChangeLogBinding

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

            if (adapterPosition % 2 == 0) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.gray_light)
                )
            } else {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.gray_dark)
                )
            }
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
