package com.app.inventarisbarang.ui.barang

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityTambahBarangBinding
import com.app.inventarisbarang.model.Kategori
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.utils.ErrorUtils
import kotlinx.coroutines.launch

class TambahBarangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahBarangBinding
    private var kategoriList: List<Kategori> = emptyList()
    private var selectedKondisi: String = "Baik"
    private var currentStok: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahBarangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupStepperStok()
        setupScanAutoFill()
        setupKondisiButtons()
        loadKategori()

        binding.btnSimpan.setOnClickListener {
            handleSimpan()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupStepperStok() {
        binding.etStok.setText(currentStok.toString())

        binding.btnMinusStok.setOnClickListener {
            if (currentStok > 1) {
                currentStok--
                binding.etStok.setText(currentStok.toString())
            }
        }

        binding.btnPlusStok.setOnClickListener {
            currentStok++
            binding.etStok.setText(currentStok.toString())
        }
    }

    private fun setupScanAutoFill() {
        binding.btnScanKodeBarang.setOnClickListener {
            val randomSku = "BRG-${(100..999).random()}"
            binding.etKodeBarang.setText(randomSku)
            Toast.makeText(this, "Kode SKU $randomSku berhasil di-generate dari pemindai", Toast.LENGTH_SHORT).show()
        }
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
            "Perbaikan" -> {
                binding.btnKondisiBaik.background = unselectedBg
                binding.tvKondisiBaik.setTextColor(inactiveTextColor)

                binding.btnKondisiPerbaikan.background = selectedBg
                binding.tvKondisiPerbaikan.setTextColor(activeTextColor)

                binding.btnKondisiRusak.background = unselectedBg
                binding.tvKondisiRusak.setTextColor(inactiveTextColor)
            }
            "Rusak" -> {
                binding.btnKondisiBaik.background = unselectedBg
                binding.tvKondisiBaik.setTextColor(inactiveTextColor)

                binding.btnKondisiPerbaikan.background = unselectedBg
                binding.tvKondisiPerbaikan.setTextColor(inactiveTextColor)

                binding.btnKondisiRusak.background = selectedBg
                binding.tvKondisiRusak.setTextColor(activeTextColor)
            }
        }
    }

    private fun loadKategori() {
        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@TambahBarangActivity)
                val response = apiService.getKategori()
                if (response.isSuccessful) {
                    kategoriList = response.body()?.data ?: emptyList()
                    val items = if (kategoriList.isEmpty()) {
                        listOf("-- Belum ada kategori --")
                    } else {
                        kategoriList.map { it.nama_kategori }
                    }

                    val adapter = ArrayAdapter(
                        this@TambahBarangActivity,
                        android.R.layout.simple_spinner_item,
                        items
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    binding.spinnerKategori.adapter = adapter
                }
            } catch (_: Throwable) {}
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

        if (kategoriList.isEmpty()) {
            Toast.makeText(this, "Silakan buat kategori terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedKategoriPos = binding.spinnerKategori.selectedItemPosition
        val selectedKategori = kategoriList.getOrNull(selectedKategoriPos)
        if (selectedKategori == null) {
            Toast.makeText(this, "Pilih kategori yang valid", Toast.LENGTH_SHORT).show()
            return
        }

        val stok = stokStr.toIntOrNull() ?: currentStok

        // Map condition value for compatibility
        val kondisiValue = when (selectedKondisi) {
            "Perbaikan" -> "Rusak Ringan"
            "Rusak" -> "Rusak Berat"
            else -> "Baik"
        }

        val body = mutableMapOf<String, Any>(
            "kode_barang" to kodeBarang,
            "nama_barang" to namaBarang,
            "kategori_id" to selectedKategori.id,
            "stok" to stok,
            "kondisi" to kondisiValue
        )
        if (lokasiRak.isNotEmpty()) {
            body["lokasi_rak"] = lokasiRak
        }
        if (gambarUrl.isNotEmpty()) {
            body["gambar"] = gambarUrl
        }
        if (deskripsi.isNotEmpty()) {
            body["deskripsi"] = deskripsi
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@TambahBarangActivity)
                val response = apiService.tambahBarang(body)

                showLoading(false)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@TambahBarangActivity,
                        "Barang $namaBarang berhasil disimpan",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@TambahBarangActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                showLoading(false)
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@TambahBarangActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSimpan.isEnabled = !isLoading
    }
}
