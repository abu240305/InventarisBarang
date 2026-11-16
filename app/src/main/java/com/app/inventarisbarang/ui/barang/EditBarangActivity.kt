package com.app.inventarisbarang.ui.barang

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityEditBarangBinding
import com.app.inventarisbarang.model.Barang
import com.app.inventarisbarang.model.Kategori
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.utils.ErrorUtils
import kotlinx.coroutines.launch

class EditBarangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBarangBinding
    private var barangId: Int = -1
    private var currentBarang: Barang? = null
    private var kategoriList: List<Kategori> = emptyList()
    private var selectedKondisi: String = "Baik"

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
        setupKondisiButtons()
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

    private fun loadData() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@EditBarangActivity)
                val kategoriResponse = apiService.getKategori()
                if (kategoriResponse.isSuccessful) {
                    kategoriList = kategoriResponse.body()?.data ?: emptyList()
                    val items = kategoriList.map { it.nama_kategori }
                    val adapter = ArrayAdapter(
                        this@EditBarangActivity,
                        android.R.layout.simple_spinner_item,
                        items
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    binding.spinnerKategori.adapter = adapter
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
        binding.etStok.setText(barang.stok.toString())
        binding.etDeskripsi.setText(barang.deskripsi ?: "")

        val kategoriIndex = kategoriList.indexOfFirst { it.id == barang.kategori_id }
        if (kategoriIndex >= 0) {
            binding.spinnerKategori.setSelection(kategoriIndex)
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
            Toast.makeText(this, "Kategori tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedKategoriPos = binding.spinnerKategori.selectedItemPosition
        val selectedKategori = kategoriList.getOrNull(selectedKategoriPos)
        if (selectedKategori == null) {
            Toast.makeText(this, "Pilih kategori yang valid", Toast.LENGTH_SHORT).show()
            return
        }

        if (stokStr.isEmpty()) {
            binding.etStok.error = "Stok tidak boleh kosong"
            binding.etStok.requestFocus()
            return
        }

        val stok = stokStr.toIntOrNull() ?: 0

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
        if (deskripsi.isNotEmpty()) {
            body["deskripsi"] = deskripsi
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@EditBarangActivity)
                val response = apiService.updateBarang(barangId, body)

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

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSimpan.isEnabled = !isLoading
    }
}
