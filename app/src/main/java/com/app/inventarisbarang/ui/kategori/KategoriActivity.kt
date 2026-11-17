package com.app.inventarisbarang.ui.kategori

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.inventarisbarang.databinding.ActivityKategoriBinding
import com.app.inventarisbarang.databinding.DialogKategoriBinding
import com.app.inventarisbarang.model.Kategori
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.utils.AppDialog
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class KategoriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKategoriBinding
    private lateinit var adapter: KategoriAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKategoriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        loadKategori()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = KategoriAdapter(
            onEditClick = { kategori -> showFormDialog(kategori) },
            onDeleteClick = { kategori -> showDeleteConfirmDialog(kategori) },
            isAdmin = sessionManager.isAdmin()
        )
        binding.rvKategori.layoutManager = LinearLayoutManager(this)
        binding.rvKategori.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadKategori()
        }
    }

    private fun setupFab() {
        if (sessionManager.isAdmin()) {
            binding.fabTambahKategori.visibility = View.VISIBLE
            binding.fabTambahKategori.setOnClickListener {
                showFormDialog(null)
            }
        } else {
            binding.fabTambahKategori.visibility = View.GONE
        }
    }

    private fun showFormDialog(kategori: Kategori?) {
        val isEdit = kategori != null
        val title = if (isEdit) "Ubah Kategori" else "Tambah Kategori"

        val dialogBinding = DialogKategoriBinding.inflate(layoutInflater)

        if (isEdit) {
            dialogBinding.etNamaKategori.setText(kategori?.nama_kategori)
            dialogBinding.etDeskripsiKategori.setText(kategori?.deskripsi ?: "")
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = dialogBinding.etNamaKategori.text.toString().trim()
                val deskripsi = dialogBinding.etDeskripsiKategori.text.toString().trim()

                if (nama.isEmpty()) {
                    Toast.makeText(this, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val body = mutableMapOf("nama_kategori" to nama)
                if (deskripsi.isNotEmpty()) {
                    body["deskripsi"] = deskripsi
                }

                if (isEdit && kategori != null) {
                    updateKategori(kategori.id, body)
                } else {
                    tambahKategori(body)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun tambahKategori(body: Map<String, String>) {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@KategoriActivity)
                val response = apiService.tambahKategori(body)
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    Toast.makeText(this@KategoriActivity, "Kategori berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    loadKategori()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@KategoriActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@KategoriActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateKategori(id: Int, body: Map<String, String>) {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@KategoriActivity)
                val response = apiService.updateKategori(id, body)
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    Toast.makeText(this@KategoriActivity, "Kategori berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    loadKategori()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@KategoriActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@KategoriActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeleteConfirmDialog(kategori: Kategori) {
        AppDialog.showConfirmation(
            context = this,
            title = "Hapus Kategori?",
            message = "Apakah Anda yakin ingin menghapus kategori ini? Data yang terhapus tidak dapat dipulihkan.",
            highlight = kategori.nama_kategori,
            positiveText = "Hapus Kategori",
            negativeText = "Batal",
            type = AppDialog.DialogType.DESTRUCTIVE
        ) {
            deleteKategori(kategori.id)
        }
    }

    private fun deleteKategori(id: Int) {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@KategoriActivity)
                val response = apiService.deleteKategori(id)
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    Toast.makeText(this@KategoriActivity, "Kategori berhasil dihapus", Toast.LENGTH_SHORT).show()
                    loadKategori()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@KategoriActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@KategoriActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadKategori() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@KategoriActivity)
                val response = apiService.getKategori()
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@KategoriActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@KategoriActivity, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
