package com.app.inventarisbarang.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ItemModernDropdownBinding
import com.app.inventarisbarang.databinding.LayoutModernDropdownPopupBinding

data class ModernDropdownItem(
    val id: Int,
    val title: String,
    val subtitle: String? = null,
    val badgeText: String? = null,
    val isBadgePositive: Boolean = true,
    val isSelected: Boolean = false,
    val iconRes: Int = R.drawable.ic_category
)

class ModernDropdownMenu(
    private val context: Context,
    private val anchorView: View,
    private val chevronView: View? = null,
    private val items: List<ModernDropdownItem>,
    private val onItemSelected: (ModernDropdownItem) -> Unit
) {
    private var popupWindow: PopupWindow? = null

    fun show() {
        val inflater = LayoutInflater.from(context)
        val binding = LayoutModernDropdownPopupBinding.inflate(inflater)

        val adapter = ModernDropdownAdapter(items) { selectedItem ->
            popupWindow?.dismiss()
            onItemSelected(selectedItem)
        }

        binding.rvDropdownItems.layoutManager = LinearLayoutManager(context)
        binding.rvDropdownItems.adapter = adapter

        val width = if (anchorView.width > 0) anchorView.width else ViewGroup.LayoutParams.MATCH_PARENT
        val density = context.resources.displayMetrics.density

        val popup = PopupWindow(binding.root, width, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.isOutsideTouchable = true
        popup.elevation = 16 * density
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        chevronView?.animate()?.rotation(180f)?.setDuration(200)?.start()

        popup.setOnDismissListener {
            chevronView?.animate()?.rotation(0f)?.setDuration(200)?.start()
        }

        this.popupWindow = popup
        popup.showAsDropDown(anchorView, 0, (6 * density).toInt())
    }

    fun dismiss() {
        popupWindow?.dismiss()
    }
}

class ModernDropdownAdapter(
    private val items: List<ModernDropdownItem>,
    private val onItemClick: (ModernDropdownItem) -> Unit
) : RecyclerView.Adapter<ModernDropdownAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemModernDropdownBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemModernDropdownBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        with(holder.binding) {
            ivItemIcon.setImageResource(item.iconRes)
            tvItemTitle.text = item.title

            if (!item.subtitle.isNullOrBlank()) {
                tvItemSubtitle.visibility = View.VISIBLE
                tvItemSubtitle.text = item.subtitle
            } else {
                tvItemSubtitle.visibility = View.GONE
            }

            if (!item.badgeText.isNullOrBlank()) {
                tvItemBadge.visibility = View.VISIBLE
                tvItemBadge.text = item.badgeText
                if (item.isBadgePositive) {
                    tvItemBadge.background = ContextCompat.getDrawable(context, R.drawable.pill_badge_green)
                    tvItemBadge.setTextColor(ContextCompat.getColor(context, R.color.secondary))
                } else {
                    tvItemBadge.background = ContextCompat.getDrawable(context, R.drawable.pill_badge_red)
                    tvItemBadge.setTextColor(ContextCompat.getColor(context, R.color.error))
                }
            } else {
                tvItemBadge.visibility = View.GONE
            }

            if (item.isSelected) {
                ivItemCheck.visibility = View.VISIBLE
                tvItemTitle.setTextColor(ContextCompat.getColor(context, R.color.primary))
            } else {
                ivItemCheck.visibility = View.GONE
                tvItemTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }

            itemDivider.visibility = if (position == items.size - 1) View.GONE else View.VISIBLE

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
