package com.app.inventarisbarang.ui.barang

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.print.PrintHelper
import coil.load
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityDetailBarangBinding
import com.app.inventarisbarang.databinding.BottomSheetPreviewQrBarangBinding
import com.app.inventarisbarang.model.Barang
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.peminjaman.TambahPeminjamanActivity
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

class DetailBarangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBarangBinding
    private lateinit var sessionManager: SessionManager
    private var barangId: Int = -1
    private var currentBarang: Barang? = null
    private var currentQrBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBarangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle Status Bar Insets agar AppBar tidak tertabrak jam & notch HP
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { _, insets ->
            val statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
            binding.appBarLayout.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        sessionManager = SessionManager.getInstance(this)
        barangId = intent.getIntExtra("barang_id", -1)

        if (barangId == -1) {
            Toast.makeText(this, "ID Barang tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRoleViews()
        setupQrActions()
        loadDetailBarang()

        binding.swipeRefresh.setOnRefreshListener {
            loadDetailBarang()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDetailBarang()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnToolbarShare.setOnClickListener {
            shareQrCode()
        }
        binding.btnToolbarEdit.setOnClickListener {
            val intent = Intent(this, EditBarangActivity::class.java).apply {
                putExtra("barang_id", barangId)
            }
            startActivity(intent)
        }
    }

    private fun setupRoleViews() {
        val isAdmin = sessionManager.isAdmin()

        if (isAdmin) {
            binding.btnToolbarEdit.visibility = View.VISIBLE
            binding.layoutAdminActions.visibility = View.VISIBLE
            binding.btnAjukanPinjam.visibility = View.GONE

            binding.btnAdminPinjam.setOnClickListener {
                bukaFormPinjam()
            }

            binding.btnEditBarang.setOnClickListener {
                val intent = Intent(this, EditBarangActivity::class.java).apply {
                    putExtra("barang_id", barangId)
                }
                startActivity(intent)
            }

            binding.btnHapusBarang.setOnClickListener {
                showDeleteConfirmDialog()
            }
        } else {
            binding.btnToolbarEdit.visibility = View.GONE
            binding.layoutAdminActions.visibility = View.GONE
            binding.btnAjukanPinjam.visibility = View.VISIBLE

            binding.btnAjukanPinjam.setOnClickListener {
                bukaFormPinjam()
            }
        }
    }

    private fun bukaFormPinjam() {
        val intent = Intent(this, TambahPeminjamanActivity::class.java).apply {
            putExtra("barang_id", barangId)
        }
        startActivity(intent)
    }

    private fun setupQrActions() {
        binding.btnSalinSku.setOnClickListener {
            val sku = currentBarang?.kode_barang
            if (!sku.isNullOrBlank()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("SKU Barang", sku)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "SKU $sku disalin ke papan klip", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBagikanQr.setOnClickListener {
            shareQrCode()
        }

        binding.btnCetakQr.setOnClickListener {
            printQrLabel()
        }

        binding.ivQrCode.setOnClickListener {
            showQrDialog()
        }
    }

    private fun showQrDialog() {
        val barang = currentBarang ?: return
        val bitmap = currentQrBitmap ?: return

        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetPreviewQrBarangBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvSheetKodeBarang.text = "KODE ASET: ${barang.kode_barang}"
        sheetBinding.ivSheetQrCode.setImageBitmap(bitmap)
        sheetBinding.tvSheetNamaBarang.text = barang.nama_barang
        sheetBinding.tvSheetKategori.text = barang.kategori?.nama_kategori ?: "Katalog Umum"
        sheetBinding.tvSheetLokasi.text = barang.displayLokasi

        sheetBinding.btnSheetCetak.setOnClickListener {
            printQrLabel()
            dialog.dismiss()
        }

        sheetBinding.btnSheetSalin.setOnClickListener {
            val sku = barang.kode_barang
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("SKU Barang", sku)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "SKU $sku disalin ke papan klip", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        sheetBinding.btnSheetBagikan.setOnClickListener {
            shareQrCode()
            dialog.dismiss()
        }

        sheetBinding.btnSheetClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadDetailBarang() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@DetailBarangActivity)
                val response = apiService.getDetailBarang(barangId)
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val barang = response.body()?.data
                    if (barang != null) {
                        currentBarang = barang
                        displayBarang(barang)
                    } else {
                        Toast.makeText(this@DetailBarangActivity, "Data barang kosong", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@DetailBarangActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@DetailBarangActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayBarang(barang: Barang) {
        binding.tvKodeBarang.text = barang.kode_barang
        binding.tvQrCodeValue.text = barang.kode_barang
        binding.tvNamaBarang.text = barang.nama_barang
        binding.tvKategori.text = barang.kategori?.nama_kategori ?: "Klasifikasi Terdata"
        binding.tvStok.text = "${barang.stok} Unit"
        binding.tvLokasi.text = "📍 ${barang.displayLokasi}"
        binding.tvDeskripsi.text = if (!barang.deskripsi.isNullOrBlank()) barang.deskripsi else "Tidak ada catatan spesifikasi tambahan untuk aset ini."

        // Photo loading
        if (!barang.gambar.isNullOrBlank()) {
            binding.ivFotoBarang.visibility = View.VISIBLE
            binding.layoutPlaceholderFoto.visibility = View.GONE
            binding.ivFotoBarang.load(barang.gambar) {
                crossfade(true)
                placeholder(R.drawable.form_input_bg)
                error(R.drawable.form_input_bg)
            }
        } else {
            binding.ivFotoBarang.visibility = View.GONE
            binding.layoutPlaceholderFoto.visibility = View.VISIBLE
        }

        // Kondisi styling
        val kondisiLower = barang.kondisi.lowercase()
        when {
            kondisiLower.contains("rusak ringan") || kondisiLower.contains("perbaikan") -> {
                binding.tvKondisiBadge.text = "● Rusak Ringan"
                binding.tvKondisiBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.kondisi_rusak_ringan_bg)
                binding.tvKondisiBadge.setTextColor(ContextCompat.getColor(this, R.color.kondisi_rusak_ringan_text))
            }
            kondisiLower.contains("rusak") -> {
                binding.tvKondisiBadge.text = "● Rusak Berat"
                binding.tvKondisiBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.kondisi_rusak_berat_bg)
                binding.tvKondisiBadge.setTextColor(ContextCompat.getColor(this, R.color.kondisi_rusak_berat_text))
            }
            else -> {
                binding.tvKondisiBadge.text = "● Baik"
                binding.tvKondisiBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.kondisi_baik_bg)
                binding.tvKondisiBadge.setTextColor(ContextCompat.getColor(this, R.color.kondisi_baik_text))
            }
        }

        // Status ketersediaan
        if (barang.stok > 0) {
            binding.tvStatusKetersediaan.text = "Siap Dipinjam"
            binding.tvStatusKetersediaan.setTextColor(ContextCompat.getColor(this, R.color.secondary))
            binding.btnAjukanPinjam.isEnabled = true
            binding.btnAjukanPinjam.text = "Ajukan Peminjaman Aset →"
            binding.btnAdminPinjam.isEnabled = true
        } else {
            binding.tvStatusKetersediaan.text = "Stok Habis"
            binding.tvStatusKetersediaan.setTextColor(ContextCompat.getColor(this, R.color.error))
            binding.btnAjukanPinjam.isEnabled = false
            binding.btnAjukanPinjam.text = "Stok Habis / Tidak Tersedia"
            binding.btnAdminPinjam.isEnabled = false
        }

        // Generate QR Code
        generateQrCode(barang.kode_barang, barang.qr_code_url)
    }

    private fun generateQrCode(code: String, fallbackUrl: String? = null) {
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
                    binding.ivQrCode.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!fallbackUrl.isNullOrBlank()) {
                        binding.ivQrCode.load(fallbackUrl)
                    }
                }
            }
        }
    }

    private fun shareQrCode() {
        val barang = currentBarang ?: return
        val bitmap = currentQrBitmap

        if (bitmap == null) {
            Toast.makeText(this, "QR Code belum siap untuk dibagikan", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cachePath = File(cacheDir, "shared_qr")
                cachePath.mkdirs()
                val file = File(cachePath, "QR_${barang.kode_barang}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val contentUri: Uri = FileProvider.getUriForFile(
                    this@DetailBarangActivity,
                    "${packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Label QR Code Inventaris Barang\n" +
                                "Barang: ${barang.nama_barang}\n" +
                                "SKU: ${barang.kode_barang}\n" +
                                "Lokasi: ${barang.displayLokasi}\n" +
                                "Stok: ${barang.stok} Unit"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    startActivity(Intent.createChooser(shareIntent, "Bagikan QR Code Aset"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetailBarangActivity, "Gagal membagikan QR Code: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun printQrLabel() {
        val barang = currentBarang ?: return
        val bitmap = currentQrBitmap

        if (bitmap == null) {
            Toast.makeText(this, "QR Code belum siap dicetak", Toast.LENGTH_SHORT).show()
            return
        }

        val printHelper = PrintHelper(this).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
        }
        printHelper.printBitmap("Label-Inventaris-${barang.kode_barang}", bitmap)
    }

    private fun showDeleteConfirmDialog() {
        val namaBarang = currentBarang?.nama_barang ?: ""
        AppDialog.showConfirmation(
            context = this,
            title = "Hapus Aset Inventaris?",
            message = "Apakah Anda yakin ingin menghapus barang ini? Data aset yang dihapus tidak dapat dipulihkan.",
            highlight = namaBarang,
            positiveText = "Hapus Aset",
            negativeText = "Batal",
            type = AppDialog.DialogType.DESTRUCTIVE
        ) {
            deleteBarang()
        }
    }

    private fun deleteBarang() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@DetailBarangActivity)
                val response = apiService.deleteBarang(barangId)
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@DetailBarangActivity,
                        "Barang berhasil dihapus",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@DetailBarangActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@DetailBarangActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
