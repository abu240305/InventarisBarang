package com.app.inventarisbarang.ui.peminjaman

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityDetailPeminjamanBinding
import com.app.inventarisbarang.databinding.BottomSheetPreviewQrTiketBinding
import com.app.inventarisbarang.model.Peminjaman
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.utils.AppDialog
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetailPeminjamanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailPeminjamanBinding
    private lateinit var sessionManager: SessionManager
    private var peminjamanId: Int = -1
    private var currentPeminjaman: Peminjaman? = null
    private var currentQrBitmap: Bitmap? = null

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

        setupStatusBar()
        setupToolbar()
        setupListeners()
        loadDetail()

        binding.swipeRefresh.setOnRefreshListener {
            loadDetail()
        }
    }

    private fun setupStatusBar() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.appBarLayout.setPadding(0, statusBarHeight, 0, 0)
            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnToolbarShare.setOnClickListener {
            shareQrTicket()
        }
    }

    private fun setupListeners() {
        // Admin actions
        binding.btnSetujui.setOnClickListener {
            showConfirmDialog(
                title = "Setujui Peminjaman",
                message = "Apakah Anda yakin ingin menyetujui peminjaman ini dan menyerahkan aset ke peminjam?",
                positiveText = "Ya, Setujui"
            ) {
                updateStatus("Disetujui")
            }
        }

        binding.btnTolak.setOnClickListener {
            showConfirmDialog(
                title = "Tolak Peminjaman",
                message = "Apakah Anda yakin ingin menolak permohonan peminjaman ini?",
                positiveText = "Tolak Pengajuan"
            ) {
                updateStatus("Ditolak")
            }
        }

        binding.btnKembalikan.setOnClickListener {
            showConfirmDialog(
                title = "Konfirmasi Pengembalian",
                message = "Aset inventaris telah diterima kembali dalam kondisi baik di gudang logistik?",
                positiveText = "Konfirmasi Pengembalian"
            ) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Calendar.getInstance().time)
                updateStatus("Dikembalikan", today)
            }
        }

        binding.btnHapusPeminjaman.setOnClickListener {
            showDeleteConfirmDialog()
        }

        // User cancel action
        binding.btnBatalkanUser.setOnClickListener {
            showDeleteConfirmDialog()
        }

        // QR Actions
        binding.btnSalinTiket.setOnClickListener {
            copyTicketCode()
        }

        binding.btnBagikanTiket.setOnClickListener {
            shareQrTicket()
        }

        binding.ivQrCodeTiket.setOnClickListener {
            showQrDialog()
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
        // 1. Status & Kode Tiket
        val displayKode = peminjaman.displayKode
        binding.tvKodeTiket.text = "TIKET: $displayKode"
        binding.tvQrCodeValue.text = displayKode
        binding.tvStatusBadge.text = "• ${peminjaman.status}"
        binding.tvStatusTempo.text = peminjaman.status_tempo ?: if (peminjaman.status.equals("Disetujui", true)) "Sedang Berjalan" else "Perlu Review"

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

        // 2. Barang Card
        val barang = peminjaman.barang
        binding.tvNamaBarang.text = barang?.nama_barang ?: "Barang #${peminjaman.barang_id}"
        binding.tvKategoriBarang.text = barang?.kategori?.nama_kategori ?: "Katalog Umum"
        binding.tvLokasiRak.text = barang?.lokasi_rak ?: "Rak Gudang A-01"
        binding.tvJumlah.text = "Pinjam: ${peminjaman.jumlah} Unit Fisik"

        val fotoBarang = barang?.gambar
        if (!fotoBarang.isNullOrEmpty()) {
            binding.ivGambarBarang.imageTintList = null
            binding.ivGambarBarang.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.ivGambarBarang.load(fotoBarang) {
                crossfade(true)
                placeholder(R.drawable.ic_inventory_2)
                error(R.drawable.ic_inventory_2)
            }
        } else {
            binding.ivGambarBarang.imageTintList = null
            binding.ivGambarBarang.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.ivGambarBarang.setImageResource(R.drawable.ic_inventory_2)
        }

        // 3. Peminjam & Jadwal Card
        val borrowerName = peminjaman.user?.name ?: "User #${peminjaman.user_id}"
        binding.tvPeminjamName.text = borrowerName
        binding.tvPeminjamDetail.text = "@${peminjaman.user?.username ?: "-"} • ${peminjaman.user?.divisi ?: "Divisi Operasional"}"

        val initials = borrowerName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifEmpty { "US" }
        binding.tvAvatarInitial.text = initials

        binding.tvTanggalPinjam.text = peminjaman.tanggal_pinjam
        binding.tvTanggalKembali.text = peminjaman.tanggal_kembali ?: "Belum dikembalikan"
        binding.tvKeterangan.text = if (!peminjaman.keterangan.isNullOrBlank()) peminjaman.keterangan else "Tidak ada catatan keperluan khusus."

        // 4. Generate QR Code Tiket Asli
        generateQrTicket(displayKode, peminjaman.qr_code_url)

        // 5. Role based button visibility & Sticky Bottom Bar
        var hasAction = false
        if (sessionManager.isAdmin()) {
            binding.btnBatalkanUser.visibility = View.GONE
            binding.layoutAdminActions.visibility = View.VISIBLE

            when (peminjaman.status.lowercase()) {
                "menunggu" -> {
                    binding.layoutReviewActions.visibility = View.VISIBLE
                    binding.btnKembalikan.visibility = View.GONE
                    binding.btnHapusPeminjaman.visibility = View.GONE
                    hasAction = true
                }
                "disetujui" -> {
                    binding.layoutReviewActions.visibility = View.GONE
                    binding.btnKembalikan.visibility = View.VISIBLE
                    binding.btnHapusPeminjaman.visibility = View.GONE
                    hasAction = true
                }
                else -> { // Ditolak / Dikembalikan
                    binding.layoutReviewActions.visibility = View.GONE
                    binding.btnKembalikan.visibility = View.GONE
                    binding.btnHapusPeminjaman.visibility = View.VISIBLE
                    hasAction = true
                }
            }
        } else {
            binding.layoutAdminActions.visibility = View.GONE
            if (peminjaman.status.equals("menunggu", ignoreCase = true)) {
                binding.btnBatalkanUser.visibility = View.VISIBLE
                hasAction = true
            } else {
                binding.btnBatalkanUser.visibility = View.GONE
                hasAction = false
            }
        }

        binding.cardBottomAction.visibility = if (hasAction) View.VISIBLE else View.GONE
    }

    private fun generateQrTicket(code: String, fallbackUrl: String? = null) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val hints = mapOf(
                    EncodeHintType.MARGIN to 1,
                    EncodeHintType.CHARACTER_SET to "UTF-8"
                )
                val bitMatrix = MultiFormatWriter().encode(code, BarcodeFormat.QR_CODE, 600, 600, hints)
                val bitmap = BarcodeEncoder().createBitmap(bitMatrix)
                withContext(Dispatchers.Main) {
                    currentQrBitmap = bitmap
                    binding.ivQrCodeTiket.setImageBitmap(bitmap)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    if (!fallbackUrl.isNullOrBlank()) {
                        binding.ivQrCodeTiket.load(fallbackUrl)
                    }
                }
            }
        }
    }

    private fun copyTicketCode() {
        val code = currentPeminjaman?.displayKode ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Kode Tiket Peminjaman", code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Kode tiket $code disalin ke papan klip", Toast.LENGTH_SHORT).show()
    }

    private fun shareQrTicket() {
        val peminjaman = currentPeminjaman ?: return
        val bitmap = currentQrBitmap

        if (bitmap == null) {
            Toast.makeText(this, "QR Code belum siap untuk dibagikan", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cachePath = File(cacheDir, "shared_qr")
                cachePath.mkdirs()
                val file = File(cachePath, "TIKET_${peminjaman.displayKode}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val contentUri: Uri = FileProvider.getUriForFile(
                    this@DetailPeminjamanActivity,
                    "${packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Tiket Peminjaman Inventaris Barang\n" +
                                "Nomor Tiket: ${peminjaman.displayKode}\n" +
                                "Barang: ${peminjaman.barang?.nama_barang ?: "-"}\n" +
                                "Peminjam: ${peminjaman.user?.name ?: "-"}\n" +
                                "Jumlah: ${peminjaman.jumlah} Unit\n" +
                                "Status: ${peminjaman.status}\n" +
                                "Batas Kembali: ${peminjaman.tanggal_kembali ?: "-"}"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    startActivity(Intent.createChooser(shareIntent, "Bagikan Tiket Peminjaman"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetailPeminjamanActivity, "Gagal membagikan tiket: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showQrDialog() {
        val peminjaman = currentPeminjaman ?: return
        val bitmap = currentQrBitmap ?: return

        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetPreviewQrTiketBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvSheetKodeTiket.text = "TIKET: ${peminjaman.displayKode}"
        sheetBinding.ivSheetQrCode.setImageBitmap(bitmap)
        sheetBinding.tvSheetNamaBarang.text = peminjaman.barang?.nama_barang ?: "-"
        sheetBinding.tvSheetPeminjam.text = peminjaman.user?.name ?: "-"

        sheetBinding.btnSheetSalin.setOnClickListener {
            copyTicketCode()
            dialog.dismiss()
        }

        sheetBinding.btnSheetBagikan.setOnClickListener {
            shareQrTicket()
            dialog.dismiss()
        }

        sheetBinding.btnSheetClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showConfirmDialog(title: String, message: String, positiveText: String, onConfirm: () -> Unit) {
        val type = if (title.contains("Tolak", ignoreCase = true)) {
            AppDialog.DialogType.DESTRUCTIVE
        } else {
            AppDialog.DialogType.SUCCESS
        }
        AppDialog.showConfirmation(
            context = this,
            title = title,
            message = message,
            highlight = currentPeminjaman?.displayKode,
            positiveText = positiveText,
            negativeText = "Batal",
            type = type,
            onConfirm = onConfirm
        )
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
        val title = if (sessionManager.isAdmin()) "Hapus Transaksi Peminjaman?" else "Batalkan Pengajuan Pinjam?"
        val msg = if (sessionManager.isAdmin()) {
            "Apakah Anda yakin ingin menghapus data transaksi peminjaman ini secara permanen?"
        } else {
            "Apakah Anda yakin ingin membatalkan pengajuan peminjaman aset ini?"
        }

        AppDialog.showConfirmation(
            context = this,
            title = title,
            message = msg,
            highlight = currentPeminjaman?.displayKode,
            positiveText = if (sessionManager.isAdmin()) "Hapus Transaksi" else "Ya, Batalkan",
            negativeText = "Kembali",
            type = AppDialog.DialogType.DESTRUCTIVE
        ) {
            deletePeminjaman()
        }
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
