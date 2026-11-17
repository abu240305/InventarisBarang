package com.app.inventarisbarang.ui.notifikasi

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ItemNotifikasiBinding
import com.app.inventarisbarang.model.Notifikasi

class NotifikasiAdapter(
    private val list: List<Notifikasi>,
    private val onItemClick: (Notifikasi) -> Unit
) : RecyclerView.Adapter<NotifikasiAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemNotifikasiBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotifikasiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val context = holder.itemView.context

        with(holder.binding) {
            tvNotifJudul.text = item.judul
            tvNotifPesan.text = item.pesan
            tvNotifWaktu.text = item.waktu

            if (!item.kode_transaksi.isNullOrBlank()) {
                tvNotifKode.visibility = View.VISIBLE
                tvNotifKode.text = item.kode_transaksi
            } else {
                tvNotifKode.visibility = View.GONE
            }

            when (item.tipe) {
                "peminjaman_baru" -> {
                    layoutNotifIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                    ivNotifIcon.setImageResource(R.drawable.ic_assignment)
                    ivNotifIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
                    tvNotifActionHint.text = "Tinjau Pengajuan →"
                }
                "peminjaman_disetujui" -> {
                    layoutNotifIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                    ivNotifIcon.setImageResource(R.drawable.ic_check_circle)
                    ivNotifIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#059669"))
                    tvNotifActionHint.text = "Lihat Tiket →"
                }
                "peringatan_tempo" -> {
                    layoutNotifIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DBEAFE"))
                    ivNotifIcon.setImageResource(R.drawable.ic_calendar)
                    ivNotifIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
                    tvNotifActionHint.text = "Lihat Detail →"
                }
                "terlambat", "peminjaman_ditolak" -> {
                    layoutNotifIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                    ivNotifIcon.setImageResource(R.drawable.ic_delete)
                    ivNotifIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#DC2626"))
                    tvNotifActionHint.text = "Lihat Status →"
                }
                else -> {
                    layoutNotifIconBg.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_light))
                    ivNotifIcon.setImageResource(R.drawable.ic_info)
                    ivNotifIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
                    tvNotifActionHint.text = "Buka →"
                }
            }

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
