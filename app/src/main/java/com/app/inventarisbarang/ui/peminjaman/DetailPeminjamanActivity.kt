package com.app.inventarisbarang.ui.peminjaman

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityDetailPeminjamanBinding
import com.app.inventarisbarang.model.Peminjaman
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetailPeminjamanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailPeminjamanBinding
    private lateinit var sessionManager: SessionManager
    private var peminjamanId: Int = -1
    private var currentPeminjaman: Peminjaman? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailPeminjamanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)
        peminjamanId = intent.getIntExtra("peminjaman_id", -1)

        if (peminjamanId == -1) {
            Toast.makeText(this, "ID Peminjaman tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupListeners()
        loadDetail()

        binding.swipeRefresh.setOnRefreshListener {
            loadDetail()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        // Admin buttons
        binding.btnSetujui.setOnClickListener {
            updateStatus("Disetujui")
        }

        binding.btnTolak.setOnClickListener {
            updateStatus("Ditolak")
        }

        binding.btnKembalikan.setOnClickListener {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Calendar.getInstance().time)
            updateStatus("Dikembalikan", today)
        }

        binding.btnHapusPeminjaman.setOnClickListener {
            showDeleteConfirmDialog()
        }

        // User cancel button
        binding.btnBatalkanUser.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    private fun loadDetail() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@DetailPeminjamanActivity)
                val response = apiService.getDetailPeminjaman(peminjamanId)
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val peminjaman = response.body()?.data
                    if (peminjaman != null) {
                        currentPeminjaman = peminjaman
                        displayPeminjaman(peminjaman)
                    } else {
                        Toast.makeText(this@DetailPeminjamanActivity, "Data peminjaman kosong", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@DetailPeminjamanActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@DetailPeminjamanActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayPeminjaman(peminjaman: Peminjaman) {
        binding.tvNamaBarang.text = peminjaman.barang?.nama_barang ?: "Barang #${peminjaman.barang_id}"
        binding.tvPeminjamName.text = peminjaman.user?.name ?: "User #${peminjaman.user_id}"
        binding.tvPeminjamDetail.text = "@${peminjaman.user?.username ?: "-"} • ${peminjaman.user?.email ?: "Tidak ada email"}"
        binding.tvJumlah.text = "${peminjaman.jumlah} Unit"
        binding.tvTanggalPinjam.text = peminjaman.tanggal_pinjam
        binding.tvTanggalKembali.text = peminjaman.tanggal_kembali ?: "Belum dikembalikan"
        binding.tvKeterangan.text = if (!peminjaman.keterangan.isNullOrBlank()) peminjaman.keterangan else "Tidak ada keterangan."
        binding.tvStatusBadge.text = peminjaman.status

        val (bgColorRes, textColorRes) = when (peminjaman.status.lowercase()) {
            "menunggu" -> Pair(R.color.status_menunggu_bg, R.color.status_menunggu_text)
            "disetujui" -> Pair(R.color.status_disetujui_bg, R.color.status_disetujui_text)
            "ditolak" -> Pair(R.color.status_ditolak_bg, R.color.status_ditolak_text)
            "dikembalikan" -> Pair(R.color.status_dikembalikan_bg, R.color.status_dikembalikan_text)
            else -> Pair(R.color.primary_light, R.color.primary)
        }

        binding.tvStatusBadge.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, bgColorRes))
        binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, textColorRes))

        // Role based button visibility
        if (sessionManager.isAdmin()) {
            binding.layoutAdminActions.visibility = View.VISIBLE
            binding.btnBatalkanUser.visibility = View.GONE

            // Contextual visibility based on current status
            when (peminjaman.status.lowercase()) {
                "menunggu" -> {
                    binding.btnSetujui.visibility = View.VISIBLE
                    binding.btnTolak.visibility = View.VISIBLE
                    binding.btnKembalikan.visibility = View.GONE
                }
                "disetujui" -> {
                    binding.btnSetujui.visibility = View.GONE
                    binding.btnTolak.visibility = View.GONE
                    binding.btnKembalikan.visibility = View.VISIBLE
                }
                else -> {
                    binding.btnSetujui.visibility = View.GONE
                    binding.btnTolak.visibility = View.GONE
                    binding.btnKembalikan.visibility = View.GONE
                }
            }
        } else {
            binding.layoutAdminActions.visibility = View.GONE
            // If user and status is "menunggu", can cancel
            if (peminjaman.status.equals("menunggu", ignoreCase = true)) {
                binding.btnBatalkanUser.visibility = View.VISIBLE
            } else {
                binding.btnBatalkanUser.visibility = View.GONE
            }
        }
    }

    private fun updateStatus(newStatus: String, tanggalKembali: String? = null) {
        binding.swipeRefresh.isRefreshing = true

        val body = mutableMapOf<String, Any>(
            "status" to newStatus
        )
        if (tanggalKembali != null) {
            body["tanggal_kembali"] = tanggalKembali
        }

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@DetailPeminjamanActivity)
                val response = apiService.updatePeminjaman(peminjamanId, body)
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@DetailPeminjamanActivity,
                        "Status peminjaman berhasil diubah menjadi $newStatus",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadDetail()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@DetailPeminjamanActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@DetailPeminjamanActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        val title = if (sessionManager.isAdmin()) "Hapus Peminjaman" else "Batalkan Pengajuan"
        val msg = if (sessionManager.isAdmin()) {
            "Apakah Anda yakin ingin menghapus data transaksi peminjaman ini?"
        } else {
            "Apakah Anda yakin ingin membatalkan pengajuan peminjaman ini?"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Ya, Hapus") { _, _ ->
                deletePeminjaman()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePeminjaman() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@DetailPeminjamanActivity)
                val response = apiService.deletePeminjaman(peminjamanId)
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@DetailPeminjamanActivity,
                        "Transaksi peminjaman berhasil dihapus",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@DetailPeminjamanActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@DetailPeminjamanActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
