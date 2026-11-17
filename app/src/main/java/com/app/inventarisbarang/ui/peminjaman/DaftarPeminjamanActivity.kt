package com.app.inventarisbarang.ui.peminjaman

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.inventarisbarang.databinding.ActivityDaftarPeminjamanBinding
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class DaftarPeminjamanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDaftarPeminjamanBinding
    private lateinit var adapter: PeminjamanAdapter
    private lateinit var sessionManager: SessionManager

    private var selectedStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarPeminjamanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        setupFilterSpinner()
        setupFab()
        loadPeminjaman()
    }

    override fun onResume() {
        super.onResume()
        loadPeminjaman()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = PeminjamanAdapter(
            onItemClick = { peminjaman ->
                val intent = Intent(this, DetailPeminjamanActivity::class.java).apply {
                    putExtra("peminjaman_id", peminjaman.id)
                }
                startActivity(intent)
            },
            isAdmin = sessionManager.isAdmin()
        )
        binding.rvPeminjaman.layoutManager = LinearLayoutManager(this)
        binding.rvPeminjaman.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadPeminjaman()
        }
    }

    private fun setupFilterSpinner() {
        val statusList = listOf("Semua", "Menunggu", "Disetujui", "Ditolak", "Dikembalikan")
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            statusList
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerStatusFilter.adapter = spinnerAdapter

        binding.spinnerStatusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStatus = if (position == 0) null else statusList[position]
                loadPeminjaman()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupFab() {
        binding.fabTambahPeminjaman.setOnClickListener {
            startActivity(Intent(this, TambahPeminjamanActivity::class.java))
        }
    }

    private fun loadPeminjaman() {
        binding.swipeRefresh.isRefreshing = true

        val userId = if (sessionManager.isAdmin()) null else sessionManager.getUserId()

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@DaftarPeminjamanActivity)
                val response = apiService.getPeminjaman(
                    userId = userId,
                    status = selectedStatus
                )

                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val rawList = response.body()?.data ?: emptyList()
                    val list = if (!sessionManager.isAdmin()) {
                        rawList.filter { it.user_id == sessionManager.getUserId() }
                    } else {
                        rawList
                    }
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@DaftarPeminjamanActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@DaftarPeminjamanActivity, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
