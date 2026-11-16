package com.app.inventarisbarang.ui.kategori

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ItemKategoriBinding
import com.app.inventarisbarang.model.Kategori

class KategoriAdapter(
    private var list: List<Kategori> = emptyList(),
    private val onEditClick: (Kategori) -> Unit,
    private val onDeleteClick: (Kategori) -> Unit,
    private val isAdmin: Boolean = false
) : RecyclerView.Adapter<KategoriAdapter.ViewHolder>() {

    fun submitList(newList: List<Kategori>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKategoriBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(private val binding: ItemKategoriBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(kategori: Kategori) {
            val context = binding.root.context
            binding.tvNamaKategori.text = kategori.nama_kategori
            binding.tvKodeKategori.text = "• ID: ${kategori.kode_kategori ?: "KTG-00${kategori.id}"}"
            binding.tvDeskripsiKategori.text =
                if (!kategori.deskripsi.isNullOrBlank()) kategori.deskripsi else "Peralatan dan perlengkapan inventaris organisasi"
            binding.tvBarangCount.text = "${kategori.barangs_count ?: 0} Barang"
            binding.tvUpdatedInfo.text = "Terdaftar aktif"

            // Distinct category accent color
            val colorList = listOf(
                Pair("#EEF2FF", "#4F46E5"), // Indigo
                Pair("#FEF3C7", "#D97706"), // Amber
                Pair("#ECFDF5", "#059669"), // Emerald
                Pair("#E0F2FE", "#0284C7"), // Sky
                Pair("#F3E8FF", "#7C3AED")  // Purple
            )
            val selectedAccent = colorList[Math.abs(kategori.id) % colorList.size]
            binding.ivCategoryIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor(selectedAccent.first))
            binding.ivCategoryIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(selectedAccent.second))

            if (isAdmin) {
                binding.btnEditKategori.visibility = View.VISIBLE
                binding.btnHapusKategori.visibility = View.VISIBLE
                binding.btnEditKategori.setOnClickListener { onEditClick(kategori) }
                binding.btnHapusKategori.setOnClickListener { onDeleteClick(kategori) }
            } else {
                binding.btnEditKategori.visibility = View.GONE
                binding.btnHapusKategori.visibility = View.GONE
            }
        }
    }
}
