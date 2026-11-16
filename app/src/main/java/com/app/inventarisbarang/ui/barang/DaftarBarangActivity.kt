package com.app.inventarisbarang.ui.barang

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.inventarisbarang.databinding.ActivityDaftarBarangBinding
import com.app.inventarisbarang.model.Kategori
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class DaftarBarangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDaftarBarangBinding
    private lateinit var adapter: BarangAdapter
    private lateinit var sessionManager: SessionManager

    private var kategoriList: List<Kategori> = emptyList()
    private var selectedKategoriId: Int? = null
    private var currentSearch: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarBarangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupSearchAndFilter()
        loadKategori()
        loadBarang()
    }

    override fun onResume() {
        super.onResume()
        loadBarang()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = BarangAdapter(
            onItemClick = { barang ->
                val intent = Intent(this, DetailBarangActivity::class.java).apply {
                    putExtra("barang_id", barang.id)
                }
                startActivity(intent)
            },
            isAdmin = sessionManager.isAdmin()
        )
        binding.rvBarang.layoutManager = LinearLayoutManager(this)
        binding.rvBarang.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadBarang()
        }
    }

    private fun setupFab() {
        if (sessionManager.isAdmin()) {
            binding.fabTambahBarang.visibility = View.VISIBLE
            binding.fabTambahBarang.setOnClickListener {
                startActivity(Intent(this, TambahBarangActivity::class.java))
            }
        } else {
            binding.fabTambahBarang.visibility = View.GONE
        }
    }

    private fun setupSearchAndFilter() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentSearch = binding.etSearch.text.toString().trim().ifEmpty { null }
                loadBarang()
                true
            } else {
                false
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty() && currentSearch != null) {
                    currentSearch = null
                    loadBarang()
                }
            }
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            currentSearch = null
            loadBarang()
        }
    }

    private fun loadKategori() {
        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@DaftarBarangActivity)
                val response = apiService.getKategori()
                if (response.isSuccessful) {
                    kategoriList = response.body()?.data ?: emptyList()
                    setupKategoriSpinner()
                }
            } catch (_: Throwable) {}
        }
    }

    private fun setupKategoriSpinner() {
        val spinnerItems = mutableListOf("Semua Kategori")
        spinnerItems.addAll(kategoriList.map { it.nama_kategori })

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            spinnerItems
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerKategoriFilter.adapter = spinnerAdapter

        binding.spinnerKategoriFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedKategoriId = if (position == 0) {
                    null
                } else {
                    kategoriList.getOrNull(position - 1)?.id
                }
                loadBarang()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadBarang() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@DaftarBarangActivity)
                val response = apiService.getBarang(
                    search = currentSearch,
                    kategoriId = selectedKategoriId
                )

                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@DaftarBarangActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@DaftarBarangActivity, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
