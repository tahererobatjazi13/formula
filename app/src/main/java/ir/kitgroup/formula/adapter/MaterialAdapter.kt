package ir.kitgroup.formula.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.formatDateShamsi
import ir.kitgroup.formula.database.entity.Material
import ir.kitgroup.formula.databinding.ItemMaterialBinding
import ir.kitgroup.formula.model.MaterialNature
import ir.kitgroup.formula.model.MaterialType
import java.text.DecimalFormat

class MaterialAdapter(
    private val onChangeLog: (Material) -> Unit = {},
    private val onDelete: (Material) -> Unit = {},
    private val onEdit: (Material) -> Unit = {}
) : ListAdapter<Material, MaterialAdapter.MaterialViewHolder>(MaterialDiffCallback()) {
    private val formatter = DecimalFormat("#,###,###,###")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val binding =
            ItemMaterialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MaterialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        val material = getItem(position)
        holder.bind(material)
    }

    inner class MaterialViewHolder(private val binding: ItemMaterialBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(material: Material) {

            val label: String
            val date: Long

            if (material.updatedDate > material.createdDate) {
                label = binding.root.context.getString(R.string.label_update_date)
                date = material.updatedDate
            } else {
                label = binding.root.context.getString(R.string.label_create_date)
                date = material.createdDate
            }
            if (material.type == MaterialType.MATERIAL.value) {
                binding.tvTitleMaterialName.text =
                    binding.root.context.getString(R.string.label_material_name_item)

                binding.tvNature.visibility = View.GONE
            } else {
                binding.tvNature.visibility = View.VISIBLE

                binding.tvTitleMaterialName.text =
                    binding.root.context.getString(R.string.label_packaging_name_item)

                if (material.nature == MaterialNature.PHYSICAL.value) {
                    binding.tvNature.text = binding.root.context.getString(R.string.label_physical)
                    binding.tvNature.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.orange_FF9800
                        )
                    )
                } else {
                    binding.tvNature.text = binding.root.context.getString(R.string.label_virtual)
                    binding.tvNature.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.green_21BF73
                        )
                    )
                }
            }

            binding.tvTitleMaterialDate.text = label
            binding.tvMaterialDate.text = formatDateShamsi(date)
            binding.tvMaterialName.text = material.materialName
            binding.tvMaterialPrice.text = formatter.format(material.price) + " ریال "
            binding.ivDeleteMaterial.setOnClickListener { onDelete(material) }
            binding.ivEditMaterial.setOnClickListener { onEdit(material) }
            binding.ivChangeLog.setOnClickListener { onChangeLog(material) }
        }
    }
}

class MaterialDiffCallback : DiffUtil.ItemCallback<Material>() {
    override fun areItemsTheSame(oldItem: Material, newItem: Material): Boolean {
        return oldItem.materialId == newItem.materialId
    }

    override fun areContentsTheSame(oldItem: Material, newItem: Material): Boolean {
        return oldItem == newItem
    }
}
