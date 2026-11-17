package com.app.inventarisbarang.ui.kategori

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.DialogKategoriBinding
import com.app.inventarisbarang.databinding.FragmentKategoriBinding
import com.app.inventarisbarang.model.Kategori
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.dashboard.DashboardActivity
import com.app.inventarisbarang.utils.AppDialog
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class KategoriFragment : Fragment() {

    private var _binding: FragmentKategoriBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: KategoriAdapter
    private lateinit var sessionManager: SessionManager
    private var currentSearch: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKategoriBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager.getInstance(requireContext())

        setupTopBar()
        setupRecyclerView()
        setupSearch()
        setupFab()
        loadKategori()
    }

    override fun onResume() {
        super.onResume()
        loadKategori()
    }

    private fun setupTopBar() {
        binding.topBar.tvModuleTitle.text = "Kategori"
        binding.topBar.ivModuleIcon.setImageResource(R.drawable.ic_category)

        val avatar = sessionManager.getAvatar()
        if (avatar.isNotEmpty()) {
            binding.topBar.ivTopAvatar.imageTintList = null
            binding.topBar.ivTopAvatar.setPadding(0, 0, 0, 0)
            binding.topBar.ivTopAvatar.load(avatar) {
                crossfade(true)
                placeholder(R.drawable.ic_person)
            }
        }

        binding.topBar.btnTopProfile.setOnClickListener {
            (activity as? DashboardActivity)?.switchTab(R.id.nav_profil)
        }

        binding.topBar.btnNotification.setOnClickListener {
            AppDialog.showNotificationSheet(requireContext())
        }
    }

    private fun setupRecyclerView() {
        adapter = KategoriAdapter(
            onEditClick = { kategori -> showFormDialog(kategori) },
            onDeleteClick = { kategori -> showDeleteConfirmDialog(kategori) },
            isAdmin = sessionManager.isAdmin()
        )
        binding.rvKategori.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKategori.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadKategori()
        }
    }

    private fun setupSearch() {
        binding.etSearchKategori.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s?.toString()?.trim()?.ifEmpty { null }
                loadKategori()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
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

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = dialogBinding.etNamaKategori.text.toString().trim()
                val deskripsi = dialogBinding.etDeskripsiKategori.text.toString().trim()

                if (nama.isEmpty()) {
                    Toast.makeText(context, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val response = apiService.tambahKategori(body)
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    Toast.makeText(context, "Kategori berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    loadKategori()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateKategori(id: Int, body: Map<String, String>) {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val response = apiService.updateKategori(id, body)
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    Toast.makeText(context, "Kategori berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    loadKategori()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmDialog(kategori: Kategori) {
        AppDialog.showConfirmation(
            context = requireContext(),
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val response = apiService.deleteKategori(id)
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    Toast.makeText(context, "Kategori berhasil dihapus", Toast.LENGTH_SHORT).show()
                    loadKategori()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadKategori() {
        binding.swipeRefresh.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val response = apiService.getKategori(search = currentSearch)

                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

                    // Update Top Metrics
                    binding.tvCounterKategoriAktif.text = "${list.size} Kategori Aktif"
                    binding.tvTotalKategoriMetric.text = "${list.size} Terdaftar"

                    val metaTotalAset = response.body()?.meta?.get("total_aset_fisik")
                    val totalAset = metaTotalAset?.toString()?.toDoubleOrNull()?.toInt() ?: list.sumOf { it.barangs_count ?: 0 }
                    binding.tvTotalAsetFisikMetric.text = "$totalAset Unit Terdata"
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
