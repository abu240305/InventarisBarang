package com.app.inventarisbarang.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.inventarisbarang.databinding.ItemPickerOptionBinding

data class PickerOptionItem(
    val id: Int,
    val title: String,
    val subtitle: String? = null,
    val isSelected: Boolean = false
)

class BottomSheetPickerAdapter(
    private val items: List<PickerOptionItem>,
    private val onItemClick: (PickerOptionItem) -> Unit
) : RecyclerView.Adapter<BottomSheetPickerAdapter.PickerViewHolder>() {

    inner class PickerViewHolder(private val binding: ItemPickerOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PickerOptionItem) {
            binding.tvItemTitle.text = item.title

            if (!item.subtitle.isNullOrEmpty()) {
                binding.tvItemSubtitle.text = item.subtitle
                binding.tvItemSubtitle.visibility = View.VISIBLE
            } else {
                binding.tvItemSubtitle.visibility = View.GONE
            }

            if (item.isSelected) {
                binding.ivCheckIndicator.visibility = View.VISIBLE
                binding.root.strokeColor = binding.root.context.getColor(com.app.inventarisbarang.R.color.primary)
                binding.root.strokeWidth = 3
            } else {
                binding.ivCheckIndicator.visibility = View.GONE
                binding.root.strokeColor = binding.root.context.getColor(com.app.inventarisbarang.R.color.border)
                binding.root.strokeWidth = 1
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickerViewHolder {
        val binding = ItemPickerOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
