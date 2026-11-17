package com.app.inventarisbarang.ui.peminjaman

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ItemPeminjamanBinding
import com.app.inventarisbarang.model.Peminjaman

class PeminjamanAdapter(
    private var list: List<Peminjaman> = emptyList(),
    private val onItemClick: (Peminjaman) -> Unit,
    private val onApproveClick: ((Peminjaman) -> Unit)? = null,
    private val onRejectClick: ((Peminjaman) -> Unit)? = null,
    private val isAdmin: Boolean = false
) : RecyclerView.Adapter<PeminjamanAdapter.ViewHolder>() {

    fun submitList(newList: List<Peminjaman>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPeminjamanBinding.inflate(
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

    inner class ViewHolder(private val binding: ItemPeminjamanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(peminjaman: Peminjaman) {
            val context = binding.root.context

            // 1. Code badge & Tempo badge
            binding.tvKodePeminjaman.text = peminjaman.kode_peminjaman ?: "TRX-${peminjaman.id}"
            val sisaHariText = peminjaman.status_tempo ?: if (peminjaman.sisa_hari != null) "• Sisa ${peminjaman.sisa_hari} hari" else "• Aktif"
            binding.tvStatusTempo.text = sisaHariText

            // 2. Status Badge with dynamic colors
            binding.tvStatusBadge.text = peminjaman.status
            val (bgColorRes, textColorRes) = when (peminjaman.status.lowercase()) {
                "menunggu" -> Pair(R.color.status_menunggu_bg, R.color.status_menunggu_text)
                "disetujui" -> Pair(R.color.status_disetujui_bg, R.color.status_disetujui_text)
                "ditolak" -> Pair(R.color.status_ditolak_bg, R.color.status_ditolak_text)
                "dikembalikan" -> Pair(R.color.status_dikembalikan_bg, R.color.status_dikembalikan_text)
                else -> Pair(R.color.primary_light, R.color.primary)
            }
            binding.tvStatusBadge.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, bgColorRes))
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, textColorRes))

            // 3. Item Name & Category
            binding.tvNamaBarang.text = peminjaman.barang?.nama_barang ?: "Barang #${peminjaman.barang_id}"
            binding.tvKategori.text = peminjaman.barang?.kategori?.nama_kategori ?: "Katalog Umum"

            // 4. Thumbnail loading with Coil
            val gambar = peminjaman.barang?.gambar
            if (!gambar.isNullOrEmpty()) {
                binding.ivThumbnail.imageTintList = null
                binding.ivThumbnail.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                binding.ivThumbnail.load(gambar) {
                    crossfade(true)
                    placeholder(R.drawable.ic_inventory_2)
                    error(R.drawable.ic_inventory_2)
                }
            } else {
                binding.ivThumbnail.imageTintList = null
                binding.ivThumbnail.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                binding.ivThumbnail.setImageResource(R.drawable.ic_inventory_2)
            }

            // 5. Borrower Info (Initial Avatar + Name + Divisi)
            val borrowerName = peminjaman.user?.name ?: "User #${peminjaman.user_id}"
            binding.tvPeminjam.text = borrowerName
            binding.tvDivisi.text = peminjaman.user?.divisi ?: "Anggota"

            val initials = borrowerName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")
            binding.tvAvatarInitial.text = if (initials.isNotEmpty()) initials else "U"

            // 6. Quantity & Dates
            binding.tvJumlah.text = "${peminjaman.jumlah} Unit"
            binding.tvTanggalPinjam.text = "Pinjam: ${peminjaman.tanggal_pinjam}"
            binding.tvTanggalKembali.text = "Tempo: ${peminjaman.tanggal_kembali ?: "-"}"

            // 7. Action buttons (Approve / Reject) for Admin when status is Menunggu
            if (isAdmin && peminjaman.status.equals("Menunggu", ignoreCase = true)) {
                binding.layoutActionButtons.visibility = View.VISIBLE
                binding.btnSetujui.setOnClickListener { onApproveClick?.invoke(peminjaman) }
                binding.btnTolak.setOnClickListener { onRejectClick?.invoke(peminjaman) }
            } else {
                binding.layoutActionButtons.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(peminjaman)
            }
        }
    }
}
