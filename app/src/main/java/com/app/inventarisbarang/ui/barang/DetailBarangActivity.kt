package com.app.inventarisbarang.ui.barang

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityDetailBarangBinding
import com.app.inventarisbarang.model.Barang
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.peminjaman.TambahPeminjamanActivity
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class DetailBarangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBarangBinding
    private lateinit var sessionManager: SessionManager
    private var barangId: Int = -1
    private var currentBarang: Barang? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBarangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)
        barangId = intent.getIntExtra("barang_id", -1)

        if (barangId == -1) {
            Toast.makeText(this, "ID Barang tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRoleViews()
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
    }

    private fun setupRoleViews() {
        if (sessionManager.isAdmin()) {
            binding.layoutAdminActions.visibility = View.VISIBLE
            binding.btnAjukanPinjam.visibility = View.GONE

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
            binding.layoutAdminActions.visibility = View.GONE
            binding.btnAjukanPinjam.visibility = View.VISIBLE

            binding.btnAjukanPinjam.setOnClickListener {
                val intent = Intent(this, TambahPeminjamanActivity::class.java).apply {
                    putExtra("barang_id", barangId)
                }
                startActivity(intent)
            }
        }
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
        binding.tvNamaBarang.text = barang.nama_barang
        binding.tvKategori.text = "Kategori: ${barang.kategori?.nama_kategori ?: "-"}"
        binding.tvStok.text = "${barang.stok} Unit"
        binding.tvKondisiBadge.text = barang.kondisi
        binding.tvDeskripsi.text = if (!barang.deskripsi.isNullOrBlank()) barang.deskripsi else "Tidak ada deskripsi tambahan."

        val (bgColorRes, textColorRes) = when (barang.kondisi.lowercase()) {
            "baik" -> Pair(R.color.kondisi_baik_bg, R.color.kondisi_baik_text)
            "rusak ringan" -> Pair(R.color.kondisi_rusak_ringan_bg, R.color.kondisi_rusak_ringan_text)
            "rusak berat" -> Pair(R.color.kondisi_rusak_berat_bg, R.color.kondisi_rusak_berat_text)
            else -> Pair(R.color.primary_light, R.color.primary)
        }

        binding.tvKondisiBadge.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, bgColorRes))
        binding.tvKondisiBadge.setTextColor(ContextCompat.getColor(this, textColorRes))

        // If user and stock is 0, disable or adjust borrow button
        if (!sessionManager.isAdmin()) {
            if (barang.stok <= 0) {
                binding.btnAjukanPinjam.isEnabled = false
                binding.btnAjukanPinjam.text = "Stok Habis"
            } else {
                binding.btnAjukanPinjam.isEnabled = true
                binding.btnAjukanPinjam.text = "Ajukan Peminjaman"
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Barang")
            .setMessage("Apakah Anda yakin ingin menghapus barang \"${currentBarang?.nama_barang ?: ""}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteBarang()
            }
            .setNegativeButton("Batal", null)
            .show()
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
