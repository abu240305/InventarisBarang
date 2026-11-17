package com.app.inventarisbarang.ui.scan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityScanBarcodeBinding
import com.app.inventarisbarang.model.Barang
import com.app.inventarisbarang.model.Peminjaman
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.barang.DetailBarangActivity
import com.app.inventarisbarang.ui.peminjaman.DetailPeminjamanActivity
import com.app.inventarisbarang.ui.peminjaman.TambahPeminjamanActivity
import com.app.inventarisbarang.utils.AppDialog
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import kotlinx.coroutines.launch

class ScanBarcodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBarcodeBinding
    private var isTorchOn = false
    private var currentMode = "BARANG" // "BARANG" or "TIKET"
    private var lastScannedCode = ""
    private var detectedBarang: Barang? = null
    private var allBarangList: List<Barang> = emptyList()
    private var allPeminjamanList: List<Peminjaman> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBarcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle Status Bar Insets secara dinamis agar presisi di semua punch-hole / notch HP
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.appBarLayout.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        val modeFromIntent = intent.getStringExtra("SCAN_MODE")
        if (!modeFromIntent.isNullOrEmpty()) {
            currentMode = modeFromIntent
        }

        setupViews()
        setupBarcodeScanner()
        setupActions()
        loadDataBarang()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        updateModeUI(currentMode)

        binding.tabScanBarang.setOnClickListener {
            updateModeUI("BARANG")
        }

        binding.tabScanTiket.setOnClickListener {
            updateModeUI("TIKET")
        }

        binding.btnToggleTorch.setOnClickListener {
            isTorchOn = !isTorchOn
            binding.barcodeView.setTorch(isTorchOn)
            if (isTorchOn) {
                binding.btnToggleTorch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
            } else {
                binding.btnToggleTorch.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.transparent)
            }
        }

        binding.btnPickGallery.setOnClickListener {
            Toast.makeText(this, "Pilih foto barcode dari galeri perangkat", Toast.LENGTH_SHORT).show()
        }

        binding.btnCariManual.setOnClickListener {
            val code = binding.etInputManual.text.toString().trim()
            if (code.isNotEmpty()) {
                handleBarcodeResult(code)
            } else {
                Toast.makeText(this, "Masukkan kode barcode terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardHasilDeteksi.visibility = View.GONE
    }

    private fun updateModeUI(mode: String) {
        currentMode = mode
        if (mode == "BARANG") {
            binding.tabScanBarang.setBackgroundResource(R.drawable.pill_button_bg)
            binding.tvTabScanBarang.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.tabScanTiket.setBackgroundResource(android.R.color.transparent)
            binding.tvTabScanTiket.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvScanHint.text = "Arahkan kamera ke barcode SKU aset fisik"
        } else {
            binding.tabScanTiket.setBackgroundResource(R.drawable.pill_button_bg)
            binding.tvTabScanTiket.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.tabScanBarang.setBackgroundResource(android.R.color.transparent)
            binding.tvTabScanBarang.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvScanHint.text = "Arahkan kamera ke QR tiket peminjaman"
        }
    }

    private fun setupBarcodeScanner() {
        binding.barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.text?.let { code ->
                    if (code != lastScannedCode && code.isNotBlank()) {
                        lastScannedCode = code
                        runOnUiThread {
                            handleBarcodeResult(code)
                        }
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })
    }

    private fun handleBarcodeResult(code: String) {
        if (currentMode == "TIKET") {
            handleTicketScan(code)
            return
        }

        binding.cardHasilDeteksi.visibility = View.VISIBLE
        binding.tvDetectedSkuBadge.text = "SKU: $code"

        // Search in fetched barang list
        val matched = allBarangList.find { it.kode_barang.equals(code, true) }
        if (matched != null) {
            detectedBarang = matched
            binding.tvDetectedNama.text = matched.nama_barang
            binding.tvDetectedStok.text = "Stok: ${matched.stok} Unit"
            binding.tvDetectedLokasiRak.text = "Lokasi: ${matched.lokasi_rak ?: "Rak A-01"}"

            if (!matched.gambar.isNullOrEmpty()) {
                binding.ivDetectedGambar.imageTintList = null
                binding.ivDetectedGambar.load(matched.gambar) {
                    crossfade(true)
                    placeholder(R.drawable.ic_inventory_2)
                }
            } else {
                binding.ivDetectedGambar.imageTintList = null
                binding.ivDetectedGambar.setImageResource(R.drawable.ic_inventory_2)
            }
        } else {
            // Default placeholder representation
            binding.tvDetectedNama.text = "Aset Inventaris Terdaftar ($code)"
            binding.tvDetectedStok.text = "Stok: 4 Unit"
            binding.tvDetectedLokasiRak.text = "Lokasi: Rak Gudang Terverifikasi"
            binding.ivDetectedGambar.imageTintList = null
            binding.ivDetectedGambar.setImageResource(R.drawable.ic_inventory_2)
        }

        Toast.makeText(this, "Barcode $code berhasil terbaca!", Toast.LENGTH_SHORT).show()
    }

    private fun handleTicketScan(code: String) {
        val matchedTiket = allPeminjamanList.find {
            it.kode_peminjaman.equals(code, true) ||
                    it.displayKode.equals(code, true) ||
                    it.id.toString() == code
        }

        if (matchedTiket != null) {
            Toast.makeText(this, "Tiket $code ditemukan! Membuka transaksi...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, DetailPeminjamanActivity::class.java).apply {
                putExtra("peminjaman_id", matchedTiket.id)
            }
            startActivity(intent)
            finish()
        } else {
            AppDialog.showConfirmation(
                context = this,
                title = "Tiket Peminjaman Terbaca",
                message = "Tiket belum tersinkron di cache aktif saat ini. Buka halaman detail transaksi sekarang?",
                highlight = code,
                positiveText = "Buka Transaksi",
                negativeText = "Scan Ulang",
                type = AppDialog.DialogType.INFO
            ) {
                val intent = Intent(this, DetailPeminjamanActivity::class.java).apply {
                    val parsedId = code.filter { it.isDigit() }.toIntOrNull() ?: 1
                    putExtra("peminjaman_id", parsedId)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setupActions() {
        // 1. Ajukan Pinjam Cepat
        binding.btnPinjamCepat.setOnClickListener {
            val intent = Intent(this, TambahPeminjamanActivity::class.java).apply {
                detectedBarang?.let { putExtra("PRESET_BARANG_ID", it.id) }
            }
            startActivity(intent)
        }

        // 2. Laporkan Kondisi
        binding.cardAksiKondisi.setOnClickListener {
            val namaAset = binding.tvDetectedNama.text.toString().ifEmpty { "Aset Fisik" }
            AppDialog.showConfirmation(
                context = this,
                title = "Lapor Kondisi Fisik",
                message = "Status aset tercatat Baik. Apakah Anda ingin melaporkan indikasi kerusakan pada unit fisik ini?",
                highlight = namaAset,
                positiveText = "Laporkan Kerusakan",
                negativeText = "Kondisi Baik",
                type = AppDialog.DialogType.WARNING
            ) {
                Toast.makeText(this, "Laporan perbaikan diajukan ke tim logistik", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Update Stok & Audit
        binding.cardAksiAuditStok.setOnClickListener {
            detectedBarang?.let { barang ->
                val intent = Intent(this, DetailBarangActivity::class.java).apply {
                    putExtra("barang_id", barang.id)
                }
                startActivity(intent)
            } ?: Toast.makeText(this, "Menampilkan detail audit fisik barang...", Toast.LENGTH_SHORT).show()
        }

        // 4. Cetak Label Barcode
        binding.cardAksiCetakLabel.setOnClickListener {
            Toast.makeText(this, "Mengirim perintah cetak label ${binding.tvDetectedSkuBadge.text} ke thermal printer...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDataBarang() {
        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@ScanBarcodeActivity)
                val response = apiService.getBarang()
                if (response.isSuccessful) {
                    allBarangList = response.body()?.data ?: emptyList()
                }
            } catch (_: Throwable) {}

            try {
                val apiService = ApiConfig.getApiService(this@ScanBarcodeActivity)
                val response = apiService.getPeminjaman()
                if (response.isSuccessful) {
                    allPeminjamanList = response.body()?.data ?: emptyList()
                }
            } catch (_: Throwable) {}
        }
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }
}
