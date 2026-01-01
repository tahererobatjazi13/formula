package ir.kitgroup.formulaNew.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.databinding.ItemPackagingSummaryBinding
import ir.kitgroup.formulaNew.model.RawPackagingSummary

class PackagingSummaryAdapter : RecyclerView.Adapter<PackagingSummaryAdapter.VH>() {
    private val items = mutableListOf<RawPackagingSummary>()

    fun updateList(newList: List<RawPackagingSummary>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    class VH(val binding: ItemPackagingSummaryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ItemPackagingSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvName.text = item.name
            tvQuantity.text = "${formatQuantity(item.quantity)} عدد"
        }
    }

    override fun getItemCount() = items.size
}