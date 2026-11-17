package com.app.inventarisbarang.ui.barang

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityEditBarangBinding
import com.app.inventarisbarang.databinding.BottomSheetScanSkuBinding
import com.app.inventarisbarang.model.Barang
import com.app.inventarisbarang.model.Kategori
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.utils.ErrorUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class EditBarangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBarangBinding
    private var barangId: Int = -1
    private var currentBarang: Barang? = null
    private var kategoriList: List<Kategori> = emptyList()
    private var selectedKondisi: String = "Baik"
    private var currentStok: Int = 1
    private var selectedKategoriId: Int? = null
    private var selectedImageUri: Uri? = null

    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPreviewFoto.load(uri) {
                crossfade(true)
            }
            binding.ivPreviewFoto.visibility = View.VISIBLE
            binding.tvChangeImageBadge.visibility = View.VISIBLE
            binding.layoutImagePlaceholder.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBarangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        barangId = intent.getIntExtra("barang_id", -1)
        if (barangId == -1) {
            Toast.makeText(this, "ID Barang tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupStepperStok()
        setupScanAutoFill()
        setupKondisiButtons()
        setupImagePicker()
        loadData()

        binding.btnSimpan.setOnClickListener {
            handleSimpan()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupImagePicker() {
        binding.cardPickImage.setOnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.btnToggleUrlMode.setOnClickListener {
            val isVisible = binding.layoutUrlInput.visibility == View.VISIBLE
            binding.layoutUrlInput.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.btnToggleUrlMode.text = if (isVisible) "Input via URL?" else "Sembunyikan URL"
        }
    }

    private fun setupStepperStok() {
        binding.etStok.setText(currentStok.toString())

        binding.btnMinusStok.setOnClickListener {
            val now = binding.etStok.text.toString().toIntOrNull() ?: currentStok
            if (now > 0) {
                currentStok = now - 1
                binding.etStok.setText(currentStok.toString())
            }
        }

        binding.btnPlusStok.setOnClickListener {
            val now = binding.etStok.text.toString().toIntOrNull() ?: currentStok
            currentStok = now + 1
            binding.etStok.setText(currentStok.toString())
        }
    }

    private fun setupScanAutoFill() {
        binding.btnScanKodeBarang.setOnClickListener {
            showModernScanSkuBottomSheet()
        }
    }

    private fun showModernScanSkuBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetBinding = BottomSheetScanSkuBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        var isSheetTorchOn = false

        sheetBinding.btnSheetClose.setOnClickListener {
            dialog.dismiss()
        }

        sheetBinding.btnSheetTorch.setOnClickListener {
            isSheetTorchOn = !isSheetTorchOn
            sheetBinding.sheetBarcodeView.setTorch(isSheetTorchOn)
            if (isSheetTorchOn) {
                sheetBinding.btnSheetTorch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
            } else {
                sheetBinding.btnSheetTorch.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.transparent)
            }
        }

        sheetBinding.btnGenerateAutoSku.setOnClickListener {
            val randomSku = "BRG-${(100..999).random()}"
            binding.etKodeBarang.setText(randomSku)
            Toast.makeText(this, "SKU: $randomSku dibuat!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        sheetBinding.sheetBarcodeView.decodeSingle(object : com.journeyapps.barcodescanner.BarcodeCallback {
            override fun barcodeResult(result: com.journeyapps.barcodescanner.BarcodeResult?) {
                result?.text?.let { code ->
                    runOnUiThread {
                        binding.etKodeBarang.setText(code)
                        Toast.makeText(this@EditBarangActivity, "Kode SKU: $code berhasil dipindai!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
        })

        dialog.setOnShowListener {
            sheetBinding.sheetBarcodeView.resume()
        }

        dialog.setOnDismissListener {
            sheetBinding.sheetBarcodeView.pause()
        }

        dialog.show()
    }

    private fun setupKondisiButtons() {
        updateKondisiUI("Baik")

        binding.btnKondisiBaik.setOnClickListener {
            updateKondisiUI("Baik")
        }
        binding.btnKondisiPerbaikan.setOnClickListener {
            updateKondisiUI("Perbaikan")
        }
        binding.btnKondisiRusak.setOnClickListener {
            updateKondisiUI("Rusak")
        }
    }

    private fun updateKondisiUI(kondisi: String) {
        selectedKondisi = kondisi
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.form_kondisi_selected_bg)
        val unselectedBg = ContextCompat.getDrawable(this, R.drawable.form_kondisi_unselected_bg)

        val activeTextColor = ContextCompat.getColor(this, R.color.text_primary)
        val inactiveTextColor = ContextCompat.getColor(this, R.color.text_secondary)

        when (kondisi) {
            "Baik" -> {
                binding.btnKondisiBaik.background = selectedBg
                binding.tvKondisiBaik.setTextColor(activeTextColor)

                binding.btnKondisiPerbaikan.background = unselectedBg
                binding.tvKondisiPerbaikan.setTextColor(inactiveTextColor)

                binding.btnKondisiRusak.background = unselectedBg
                binding.tvKondisiRusak.setTextColor(inactiveTextColor)
            }
            "Perbaikan", "Rusak Ringan" -> {
                binding.btnKondisiBaik.background = unselectedBg
                binding.tvKondisiBaik.setTextColor(inactiveTextColor)

                binding.btnKondisiPerbaikan.background = selectedBg
                binding.tvKondisiPerbaikan.setTextColor(activeTextColor)

                binding.btnKondisiRusak.background = unselectedBg
                binding.tvKondisiRusak.setTextColor(inactiveTextColor)
            }
            "Rusak", "Rusak Berat" -> {
                binding.btnKondisiBaik.background = unselectedBg
                binding.tvKondisiBaik.setTextColor(inactiveTextColor)

                binding.btnKondisiPerbaikan.background = unselectedBg
                binding.tvKondisiPerbaikan.setTextColor(inactiveTextColor)

                binding.btnKondisiRusak.background = selectedBg
                binding.tvKondisiRusak.setTextColor(activeTextColor)
            }
        }
    }

    private fun setupKategoriPicker() {
        binding.btnSelectKategori.setOnClickListener {
            if (kategoriList.isEmpty()) {
                Toast.makeText(this, "Memuat data kategori...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dropdownItems = kategoriList.map { kat ->
                com.app.inventarisbarang.utils.ModernDropdownItem(
                    id = kat.id,
                    title = kat.nama_kategori,
                    subtitle = kat.deskripsi ?: "Klasifikasi aset inventaris",
                    isSelected = kat.id == selectedKategoriId,
                    iconRes = R.drawable.ic_category
                )
            }

            val dropdownMenu = com.app.inventarisbarang.utils.ModernDropdownMenu(
                context = this,
                anchorView = binding.btnSelectKategori,
                chevronView = binding.ivChevronKategori,
                items = dropdownItems
            ) { selectedItem ->
                selectedKategoriId = selectedItem.id
                binding.tvSelectedKategori.text = selectedItem.title
                binding.tvSelectedKategori.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }

            dropdownMenu.show()
        }
    }

    private fun loadData() {
        showLoading(true)
        setupKategoriPicker()

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@EditBarangActivity)
                val kategoriResponse = apiService.getKategori()
                if (kategoriResponse.isSuccessful) {
                    kategoriList = kategoriResponse.body()?.data ?: emptyList()
                }

                val barangResponse = apiService.getDetailBarang(barangId)
                showLoading(false)

                if (barangResponse.isSuccessful) {
                    val barang = barangResponse.body()?.data
                    if (barang != null) {
                        currentBarang = barang
                        populateForm(barang)
                    } else {
                        Toast.makeText(this@EditBarangActivity, "Barang tidak ditemukan", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    val errorMsg = ErrorUtils.parseError(barangResponse)
                    Toast.makeText(this@EditBarangActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (t: Throwable) {
                showLoading(false)
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@EditBarangActivity, errorMsg, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun populateForm(barang: Barang) {
        binding.etKodeBarang.setText(barang.kode_barang)
        binding.etNamaBarang.setText(barang.nama_barang)
        currentStok = barang.stok
        binding.etStok.setText(currentStok.toString())
        binding.etLokasiRak.setText(barang.lokasi_rak ?: barang.lokasi ?: "")
        binding.etDeskripsi.setText(barang.deskripsi ?: "")

        val matchedKategori = kategoriList.find { it.id == barang.kategori_id }
        if (matchedKategori != null) {
            selectedKategoriId = matchedKategori.id
            binding.tvSelectedKategori.text = matchedKategori.nama_kategori
            binding.tvSelectedKategori.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }

        // Image prefill
        if (!barang.gambar.isNullOrBlank()) {
            binding.ivPreviewFoto.load(barang.gambar) {
                crossfade(true)
            }
            binding.ivPreviewFoto.visibility = View.VISIBLE
            binding.tvChangeImageBadge.visibility = View.VISIBLE
            binding.layoutImagePlaceholder.visibility = View.GONE
            binding.etGambarUrl.setText(barang.gambar)
        }

        when {
            barang.kondisi.equals("Rusak Ringan", ignoreCase = true) || barang.kondisi.equals("Perbaikan", ignoreCase = true) -> {
                updateKondisiUI("Perbaikan")
            }
            barang.kondisi.equals("Rusak Berat", ignoreCase = true) || barang.kondisi.equals("Rusak", ignoreCase = true) -> {
                updateKondisiUI("Rusak")
            }
            else -> {
                updateKondisiUI("Baik")
            }
        }
    }

    private fun handleSimpan() {
        val kodeBarang = binding.etKodeBarang.text.toString().trim()
        val namaBarang = binding.etNamaBarang.text.toString().trim()
        val stokStr = binding.etStok.text.toString().trim()
        val lokasiRak = binding.etLokasiRak.text.toString().trim()
        val gambarUrl = binding.etGambarUrl.text.toString().trim()
        val deskripsi = binding.etDeskripsi.text.toString().trim()

        if (kodeBarang.isEmpty()) {
            binding.etKodeBarang.error = "Kode barang tidak boleh kosong"
            binding.etKodeBarang.requestFocus()
            return
        }

        if (namaBarang.isEmpty()) {
            binding.etNamaBarang.error = "Nama barang tidak boleh kosong"
            binding.etNamaBarang.requestFocus()
            return
        }

        if (selectedKategoriId == null) {
            Toast.makeText(this, "Pilih kategori terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val kategoriId = selectedKategoriId ?: return

        if (stokStr.isEmpty()) {
            binding.etStok.error = "Stok tidak boleh kosong"
            binding.etStok.requestFocus()
            return
        }

        val stok = stokStr.toIntOrNull() ?: currentStok

        val kondisiValue = when (selectedKondisi) {
            "Perbaikan", "Rusak Ringan" -> "Rusak Ringan"
            "Rusak", "Rusak Berat" -> "Rusak Berat"
            else -> "Baik"
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@EditBarangActivity)
                val response = if (selectedImageUri != null) {
                    val file = uriToFile(selectedImageUri!!)
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val gambarPart = MultipartBody.Part.createFormData("gambar", file.name, requestFile)

                    apiService.updateBarangMultipart(
                        id = barangId,
                        kodeBarang = kodeBarang.toRequestBody("text/plain".toMediaTypeOrNull()),
                        namaBarang = namaBarang.toRequestBody("text/plain".toMediaTypeOrNull()),
                        kategoriId = kategoriId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                        stok = stok.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                        kondisi = kondisiValue.toRequestBody("text/plain".toMediaTypeOrNull()),
                        deskripsi = if (deskripsi.isNotEmpty()) deskripsi.toRequestBody("text/plain".toMediaTypeOrNull()) else null,
                        lokasiRak = if (lokasiRak.isNotEmpty()) lokasiRak.toRequestBody("text/plain".toMediaTypeOrNull()) else null,
                        lokasi = if (lokasiRak.isNotEmpty()) lokasiRak.toRequestBody("text/plain".toMediaTypeOrNull()) else null,
                        gambar = gambarPart
                    )
                } else {
                    val body = mutableMapOf<String, Any>(
                        "kode_barang" to kodeBarang,
                        "nama_barang" to namaBarang,
                        "kategori_id" to kategoriId,
                        "stok" to stok,
                        "kondisi" to kondisiValue
                    )
                    if (lokasiRak.isNotEmpty()) {
                        body["lokasi_rak"] = lokasiRak
                        body["lokasi"] = lokasiRak
                    }
                    if (gambarUrl.isNotEmpty()) {
                        body["gambar"] = gambarUrl
                    }
                    if (deskripsi.isNotEmpty()) {
                        body["deskripsi"] = deskripsi
                    }
                    apiService.updateBarang(barangId, body)
                }

                showLoading(false)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@EditBarangActivity,
                        "Perubahan data barang berhasil disimpan",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@EditBarangActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                showLoading(false)
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@EditBarangActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uriToFile(uri: Uri): File {
        val contentResolver = applicationContext.contentResolver
        val tempFile = File.createTempFile("upload_edit_", ".jpg", cacheDir)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSimpan.isEnabled = !isLoading
    }
}
