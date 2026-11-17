package com.app.inventarisbarang.utils

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.BottomSheetNotifikasiBinding
import com.app.inventarisbarang.databinding.DialogConfirmationBinding
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.notifikasi.NotifikasiAdapter
import com.app.inventarisbarang.ui.peminjaman.DetailPeminjamanActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppDialog {

    enum class DialogType {
        DESTRUCTIVE,
        SUCCESS,
        WARNING,
        INFO
    }

    /**
     * Menampilkan dialog konfirmasi modern & profesional dengan sudut melengkung 24dp,
     * header ikon tematik, dan tombol aksi Material 3.
     */
    fun showConfirmation(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "Ya, Konfirmasi",
        negativeText: String = "Batal",
        highlight: String? = null,
        type: DialogType = DialogType.DESTRUCTIVE,
        onConfirm: () -> Unit
    ): AlertDialog {
        val binding = DialogConfirmationBinding.inflate(LayoutInflater.from(context))

        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set konten teks
        binding.tvDialogTitle.text = title
        binding.tvDialogMessage.text = message
        binding.btnDialogPositive.text = positiveText
        binding.btnDialogNegative.text = negativeText

        if (!highlight.isNullOrBlank()) {
            binding.tvDialogHighlight.visibility = View.VISIBLE
            binding.tvDialogHighlight.text = highlight
        } else {
            binding.tvDialogHighlight.visibility = View.GONE
        }

        // Terapkan warna & ikon berdasarkan DialogType
        when (type) {
            DialogType.DESTRUCTIVE -> {
                binding.layoutDialogIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                binding.ivDialogIcon.setImageResource(R.drawable.ic_delete)
                binding.ivDialogIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#DC2626"))
                binding.btnDialogPositive.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DC2626"))
            }
            DialogType.SUCCESS -> {
                binding.layoutDialogIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                binding.ivDialogIcon.setImageResource(R.drawable.ic_check_circle)
                binding.ivDialogIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#059669"))
                binding.btnDialogPositive.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#059669"))
            }
            DialogType.WARNING -> {
                binding.layoutDialogIconBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                binding.ivDialogIcon.setImageResource(R.drawable.ic_info)
                binding.ivDialogIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
                binding.btnDialogPositive.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
            }
            DialogType.INFO -> {
                val primaryColor = ContextCompat.getColor(context, R.color.primary)
                val primaryLight = ContextCompat.getColor(context, R.color.primary_light)
                binding.layoutDialogIconBg.backgroundTintList = ColorStateList.valueOf(primaryLight)
                binding.ivDialogIcon.setImageResource(R.drawable.ic_info)
                binding.ivDialogIcon.imageTintList = ColorStateList.valueOf(primaryColor)
                binding.btnDialogPositive.backgroundTintList = ColorStateList.valueOf(primaryColor)
            }
        }

        binding.btnDialogNegative.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnDialogPositive.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
        return dialog
    }

    /**
     * Menampilkan BottomSheet Notifikasi Real-Time yang tersambung langsung dengan endpoint /api/notifikasi
     */
    fun showNotificationSheet(
        context: Context,
        onNotificationDismissed: (() -> Unit)? = null,
        onViewPeminjaman: (() -> Unit)? = null
    ): BottomSheetDialog {
        val sheetDialog = BottomSheetDialog(context)
        val binding = BottomSheetNotifikasiBinding.inflate(LayoutInflater.from(context))
        sheetDialog.setContentView(binding.root)

        binding.btnNotifClose.setOnClickListener {
            sheetDialog.dismiss()
        }

        binding.btnNotifTutup.setOnClickListener {
            sheetDialog.dismiss()
            onNotificationDismissed?.invoke()
        }

        binding.rvNotifikasi.layoutManager = LinearLayoutManager(context)

        // Tampilkan loading awal
        binding.progressBarNotif.visibility = View.VISIBLE
        binding.layoutEmptyNotif.visibility = View.GONE
        binding.rvNotifikasi.visibility = View.GONE

        // Ambil data notifikasi real-time dari backend
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                val apiService = ApiConfig.getApiService(context)
                val response = apiService.getNotifikasi()

                binding.progressBarNotif.visibility = View.GONE

                if (response.isSuccessful) {
                    val notifList = response.body()?.data ?: emptyList()
                    if (notifList.isNotEmpty()) {
                        binding.tvSheetNotifCount.visibility = View.VISIBLE
                        binding.tvSheetNotifCount.text = "${notifList.size} Baru"
                        binding.layoutEmptyNotif.visibility = View.GONE
                        binding.rvNotifikasi.visibility = View.VISIBLE

                        val adapter = NotifikasiAdapter(notifList) { notif ->
                            sheetDialog.dismiss()
                            if (notif.peminjaman_id != null) {
                                val intent = Intent(context, DetailPeminjamanActivity::class.java).apply {
                                    putExtra("peminjaman_id", notif.peminjaman_id)
                                }
                                context.startActivity(intent)
                            } else {
                                onViewPeminjaman?.invoke()
                            }
                        }
                        binding.rvNotifikasi.adapter = adapter
                    } else {
                        binding.tvSheetNotifCount.visibility = View.GONE
                        binding.layoutEmptyNotif.visibility = View.VISIBLE
                        binding.rvNotifikasi.visibility = View.GONE
                    }
                } else {
                    binding.tvSheetNotifCount.visibility = View.GONE
                    binding.layoutEmptyNotif.visibility = View.VISIBLE
                    binding.rvNotifikasi.visibility = View.GONE
                }
            } catch (_: Exception) {
                binding.progressBarNotif.visibility = View.GONE
                binding.layoutEmptyNotif.visibility = View.VISIBLE
                binding.rvNotifikasi.visibility = View.GONE
            }
        }

        sheetDialog.show()
        return sheetDialog
    }

    /**
     * Memeriksa jumlah notifikasi yang belum dibaca dari server
     * untuk menyalakan/mematikan titik merah (dotNotification) di TopBar.
     */
    fun checkUnreadNotifications(context: Context, onResult: (Int) -> Unit) {
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                val apiService = ApiConfig.getApiService(context)
                val response = apiService.getNotifikasi()
                if (response.isSuccessful) {
                    val count = response.body()?.data?.size ?: 0
                    onResult(count)
                } else {
                    onResult(0)
                }
            } catch (_: Exception) {
                onResult(0)
            }
        }
    }
}
