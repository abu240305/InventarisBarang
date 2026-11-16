package com.app.inventarisbarang.ui.barang

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ItemBarangBinding
import com.app.inventarisbarang.model.Barang

class BarangAdapter(
    private var list: List<Barang> = emptyList(),
    private val onItemClick: (Barang) -> Unit,
    private val onEditClick: ((Barang) -> Unit)? = null,
    private val onDeleteClick: ((Barang) -> Unit)? = null,
    private val isAdmin: Boolean = false
) : RecyclerView.Adapter<BarangAdapter.ViewHolder>() {

    fun submitList(newList: List<Barang>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBarangBinding.inflate(
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

    inner class ViewHolder(private val binding: ItemBarangBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(barang: Barang) {
            val context = binding.root.context
            binding.tvKodeBarang.text = "SKU: ${barang.kode_barang}"
            binding.tvNamaBarang.text = barang.nama_barang
            binding.tvKategori.text = barang.kategori?.nama_kategori ?: "Tanpa Kategori"
            binding.tvLokasiRak.text = barang.lokasi_rak ?: "Rak A-01"
            binding.tvStok.text = "${barang.stok} unit"
            binding.tvKondisiBadge.text = "• ${barang.kondisi}"

            // Availability status
            val ketersediaan = barang.status_ketersediaan ?: if (barang.stok > 0) "Tersedia" else "Kosong"
            binding.tvKetersediaan.text = if (ketersediaan.equals("Tersedia", ignoreCase = true)) "✓ Tersedia" else "✕ Kosong"
            val ketersediaanColor = if (ketersediaan.equals("Tersedia", ignoreCase = true)) "#059669" else "#DC2626"
            binding.tvKetersediaan.setTextColor(android.graphics.Color.parseColor(ketersediaanColor))

            // Load image with Coil
            if (!barang.gambar.isNullOrEmpty()) {
                binding.ivGambarBarang.load(barang.gambar) {
                    crossfade(true)
                    placeholder(R.drawable.ic_inventory)
                    error(R.drawable.ic_inventory)
                }
            } else {
                binding.ivGambarBarang.setImageResource(R.drawable.ic_inventory)
            }

            val (bgColorRes, textColorRes) = when (barang.kondisi.lowercase()) {
                "baik" -> Pair(R.color.kondisi_baik_bg, R.color.kondisi_baik_text)
                "rusak ringan" -> Pair(R.color.kondisi_rusak_ringan_bg, R.color.kondisi_rusak_ringan_text)
                "rusak berat" -> Pair(R.color.kondisi_rusak_berat_bg, R.color.kondisi_rusak_berat_text)
                else -> Pair(R.color.primary_light, R.color.primary)
            }

            binding.tvKondisiBadge.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, bgColorRes))
            binding.tvKondisiBadge.setTextColor(ContextCompat.getColor(context, textColorRes))

            // Actions for Admin
            if (isAdmin) {
                binding.btnEdit.visibility = android.view.View.VISIBLE
                binding.btnDelete.visibility = android.view.View.VISIBLE
                binding.btnEdit.setOnClickListener { onEditClick?.invoke(barang) }
                binding.btnDelete.setOnClickListener { onDeleteClick?.invoke(barang) }
            } else {
                binding.btnEdit.visibility = android.view.View.GONE
                binding.btnDelete.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(barang)
            }
        }
    }
}
