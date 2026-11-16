package com.app.inventarisbarang.ui.peminjaman

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.app.inventarisbarang.databinding.ActivityTambahPeminjamanBinding
import com.app.inventarisbarang.model.Barang
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TambahPeminjamanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahPeminjamanBinding
    private lateinit var sessionManager: SessionManager

    private var barangList: List<Barang> = emptyList()
    private var presetBarangId: Int = -1
    private var currentJumlah: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahPeminjamanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)
        presetBarangId = intent.getIntExtra("barang_id", -1)

        setupToolbar()
        setupStepper()
        setupDatePicker()
        loadBarangList()

        binding.btnAjukan.setOnClickListener {
            handleAjukan()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnToolbarInfo.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Panduan Peminjaman Aset")
                .setMessage("1. Pilih barang yang ingin dipinjam dari inventaris organisasi.\n\n2. Tentukan jumlah unit (maksimal sesuai sisa stok).\n\n3. Pengajuan akan ditinjau dan diverifikasi oleh Admin Logistik.\n\n4. Pastikan barang dirawat dengan baik dan dikembalikan tepat waktu.")
                .setPositiveButton("Mengerti", null)
                .show()
        }
    }

    private fun setupStepper() {
        binding.etJumlah.setText(currentJumlah.toString())

        binding.btnMinus.setOnClickListener {
            if (currentJumlah > 1) {
                currentJumlah--
                binding.etJumlah.setText(currentJumlah.toString())
            }
        }

        binding.btnPlus.setOnClickListener {
            val selectedPos = binding.spinnerBarang.selectedItemPosition
            val selectedBarang = barangList.getOrNull(selectedPos)
            val maxStok = selectedBarang?.stok ?: Int.MAX_VALUE

            if (currentJumlah < maxStok) {
                currentJumlah++
                binding.etJumlah.setText(currentJumlah.toString())
            } else {
                Toast.makeText(this, "Jumlah melebihi stok yang tersedia ($maxStok)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.etTanggalPinjam.setText(dateFormat.format(calendar.time))

        val datePickerListener = View.OnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    binding.etTanggalPinjam.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        binding.containerTanggalPinjam.setOnClickListener(datePickerListener)
        binding.etTanggalPinjam.setOnClickListener(datePickerListener)
    }

    private fun loadBarangList() {
        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@TambahPeminjamanActivity)
                val response = apiService.getBarang()
                if (response.isSuccessful) {
                    barangList = response.body()?.data ?: emptyList()
                    setupBarangSpinner()
                }
            } catch (_: Throwable) {}
        }
    }

    private fun setupBarangSpinner() {
        val items = if (barangList.isEmpty()) {
            listOf("-- Tidak ada barang tersedia --")
        } else {
            barangList.map { "${it.nama_barang} (Stok: ${it.stok})" }
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            items
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerBarang.adapter = adapter

        // Select preset if passed
        if (presetBarangId != -1) {
            val presetIndex = barangList.indexOfFirst { it.id == presetBarangId }
            if (presetIndex >= 0) {
                binding.spinnerBarang.setSelection(presetIndex)
            }
        }

        binding.spinnerBarang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val barang = barangList.getOrNull(position)
                if (barang != null) {
                    binding.tvStokInfo.text = "Stok tersedia: ${barang.stok} unit"
                    if (currentJumlah > barang.stok && barang.stok > 0) {
                        currentJumlah = barang.stok
                        binding.etJumlah.setText(currentJumlah.toString())
                    }
                } else {
                    binding.tvStokInfo.text = "Stok tersedia: -"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun handleAjukan() {
        if (barangList.isEmpty()) {
            Toast.makeText(this, "Tidak ada barang untuk dipinjam", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPos = binding.spinnerBarang.selectedItemPosition
        val selectedBarang = barangList.getOrNull(selectedPos)
        if (selectedBarang == null) {
            Toast.makeText(this, "Pilih barang terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val jumlahStr = binding.etJumlah.text.toString().trim()
        val tanggalPinjam = binding.etTanggalPinjam.text.toString().trim()
        val keterangan = binding.etKeterangan.text.toString().trim()

        if (jumlahStr.isEmpty()) {
            binding.etJumlah.error = "Jumlah tidak boleh kosong"
            binding.etJumlah.requestFocus()
            return
        }

        val jumlah = jumlahStr.toIntOrNull() ?: 0
        if (jumlah <= 0) {
            binding.etJumlah.error = "Jumlah pinjam harus lebih dari 0"
            binding.etJumlah.requestFocus()
            return
        }

        if (jumlah > selectedBarang.stok) {
            binding.etJumlah.error = "Jumlah melebihi stok yang tersedia (${selectedBarang.stok})"
            binding.etJumlah.requestFocus()
            return
        }

        if (tanggalPinjam.isEmpty()) {
            Toast.makeText(this, "Pilih tanggal pinjam", Toast.LENGTH_SHORT).show()
            return
        }

        val body = mutableMapOf<String, Any>(
            "user_id" to sessionManager.getUserId(),
            "barang_id" to selectedBarang.id,
            "jumlah" to jumlah,
            "tanggal_pinjam" to tanggalPinjam
        )
        if (keterangan.isNotEmpty()) {
            body["keterangan"] = keterangan
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@TambahPeminjamanActivity)
                val response = apiService.tambahPeminjaman(body)

                showLoading(false)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@TambahPeminjamanActivity,
                        "Pengajuan peminjaman berhasil dikirim",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@TambahPeminjamanActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                showLoading(false)
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@TambahPeminjamanActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAjukan.isEnabled = !isLoading
    }
}
