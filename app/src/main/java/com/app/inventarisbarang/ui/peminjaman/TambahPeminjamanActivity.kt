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
import com.app.inventarisbarang.R
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
            val selectedBarang = barangList.find { it.id == selectedBarangId }
            val maxStok = selectedBarang?.stok ?: Int.MAX_VALUE

            if (currentJumlah < maxStok) {
                currentJumlah++
                binding.etJumlah.setText(currentJumlah.toString())
            } else {
                Toast.makeText(this, "Jumlah melebihi stok yang tersedia ($maxStok)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val calendarPinjam = Calendar.getInstance()
    private val calendarKembali = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun setupDatePicker() {
        binding.etTanggalPinjam.setText(dateFormat.format(calendarPinjam.time))
        binding.etTanggalKembali.setText(dateFormat.format(calendarKembali.time))
        updateDurasiBadgeAndChips()

        val datePickerPinjamListener = View.OnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendarPinjam.set(year, month, dayOfMonth)
                    binding.etTanggalPinjam.setText(dateFormat.format(calendarPinjam.time))

                    // If kembali is before pinjam, adjust kembali to pinjam + 3
                    if (calendarKembali.before(calendarPinjam)) {
                        calendarKembali.time = calendarPinjam.time
                        calendarKembali.add(Calendar.DAY_OF_YEAR, 3)
                        binding.etTanggalKembali.setText(dateFormat.format(calendarKembali.time))
                    }
                    updateDurasiBadgeAndChips()
                },
                calendarPinjam.get(Calendar.YEAR),
                calendarPinjam.get(Calendar.MONTH),
                calendarPinjam.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        binding.containerTanggalPinjam.setOnClickListener(datePickerPinjamListener)
        binding.etTanggalPinjam.setOnClickListener(datePickerPinjamListener)

        val datePickerKembaliListener = View.OnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendarKembali.set(year, month, dayOfMonth)
                    binding.etTanggalKembali.setText(dateFormat.format(calendarKembali.time))
                    updateDurasiBadgeAndChips()
                },
                calendarKembali.get(Calendar.YEAR),
                calendarKembali.get(Calendar.MONTH),
                calendarKembali.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.minDate = calendarPinjam.timeInMillis
            datePickerDialog.show()
        }

        binding.containerTanggalKembali.setOnClickListener(datePickerKembaliListener)
        binding.etTanggalKembali.setOnClickListener(datePickerKembaliListener)

        // Shortcut chips
        binding.chipDurasi3Hari.setOnClickListener {
            setDurasiDays(3)
        }
        binding.chipDurasi5Hari.setOnClickListener {
            setDurasiDays(5)
        }
        binding.chipDurasi7Hari.setOnClickListener {
            setDurasiDays(7)
        }
    }

    private fun setDurasiDays(days: Int) {
        calendarKembali.time = calendarPinjam.time
        calendarKembali.add(Calendar.DAY_OF_YEAR, days)
        binding.etTanggalKembali.setText(dateFormat.format(calendarKembali.time))
        updateDurasiBadgeAndChips()
    }

    private fun updateDurasiBadgeAndChips() {
        val diffMillis = calendarKembali.timeInMillis - calendarPinjam.timeInMillis
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)

        binding.tvDurasiBadge.text = "Durasi: $diffDays Hari"

        fun styleChip(chip: android.widget.TextView, isActive: Boolean) {
            if (isActive) {
                chip.setBackgroundResource(R.drawable.form_kondisi_selected_bg)
                chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary))
            } else {
                chip.setBackgroundResource(R.drawable.form_kondisi_unselected_bg)
                chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary))
            }
        }

        styleChip(binding.chipDurasi3Hari, diffDays == 3)
        styleChip(binding.chipDurasi5Hari, diffDays == 5)
        styleChip(binding.chipDurasi7Hari, diffDays == 7)
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

    private var selectedBarangId: Int? = null

    private fun setupBarangSpinner() {
        if (presetBarangId != -1) {
            val preset = barangList.find { it.id == presetBarangId }
            if (preset != null) {
                selectedBarangId = preset.id
                binding.tvSelectedBarang.text = "${preset.nama_barang} (Stok: ${preset.stok})"
                binding.tvStokInfo.text = "Stok tersedia: ${preset.stok} unit"
            }
        } else if (barangList.isNotEmpty() && selectedBarangId == null) {
            val first = barangList.first()
            selectedBarangId = first.id
            binding.tvSelectedBarang.text = "${first.nama_barang} (Stok: ${first.stok})"
            binding.tvStokInfo.text = "Stok tersedia: ${first.stok} unit"
        }

        binding.btnSelectBarang.setOnClickListener {
            if (barangList.isEmpty()) {
                Toast.makeText(this, "Tidak ada data barang tersedia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dropdownItems = barangList.map { brg ->
                com.app.inventarisbarang.utils.ModernDropdownItem(
                    id = brg.id,
                    title = brg.nama_barang,
                    subtitle = "SKU: ${brg.kode_barang}",
                    badgeText = "Stok: ${brg.stok}",
                    isBadgePositive = brg.stok > 0,
                    isSelected = brg.id == selectedBarangId,
                    iconRes = R.drawable.ic_inventory_2
                )
            }

            val dropdownMenu = com.app.inventarisbarang.utils.ModernDropdownMenu(
                context = this,
                anchorView = binding.btnSelectBarang,
                chevronView = binding.ivChevronBarang,
                items = dropdownItems
            ) { selectedItem ->
                selectedBarangId = selectedItem.id
                val chosen = barangList.find { it.id == selectedItem.id }
                if (chosen != null) {
                    binding.tvSelectedBarang.text = "${chosen.nama_barang} (Stok: ${chosen.stok})"
                    binding.tvStokInfo.text = "Stok tersedia: ${chosen.stok} unit"
                    if (currentJumlah > chosen.stok && chosen.stok > 0) {
                        currentJumlah = chosen.stok
                        binding.etJumlah.setText(currentJumlah.toString())
                    }
                }
            }

            dropdownMenu.show()
        }
    }

    private fun handleAjukan() {
        if (barangList.isEmpty()) {
            Toast.makeText(this, "Tidak ada barang untuk dipinjam", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedBarangId == null) {
            Toast.makeText(this, "Pilih barang terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedBarang = barangList.find { it.id == selectedBarangId }
        if (selectedBarang == null) {
            Toast.makeText(this, "Pilih barang terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val jumlahStr = binding.etJumlah.text.toString().trim()
        val tanggalPinjam = binding.etTanggalPinjam.text.toString().trim()
        val tanggalKembali = binding.etTanggalKembali.text.toString().trim()
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
            Toast.makeText(this, "Pilih tanggal mulai pinjam", Toast.LENGTH_SHORT).show()
            return
        }

        if (tanggalKembali.isEmpty()) {
            Toast.makeText(this, "Pilih batas tanggal pengembalian aset", Toast.LENGTH_SHORT).show()
            return
        }

        val body = mutableMapOf<String, Any>(
            "user_id" to sessionManager.getUserId(),
            "barang_id" to selectedBarang.id,
            "jumlah" to jumlah,
            "tanggal_pinjam" to tanggalPinjam,
            "tanggal_kembali" to tanggalKembali
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
