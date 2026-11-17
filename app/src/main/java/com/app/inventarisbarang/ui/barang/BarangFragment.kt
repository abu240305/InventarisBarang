package com.app.inventarisbarang.ui.barang

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.FragmentBarangBinding
import com.app.inventarisbarang.model.Barang
import com.app.inventarisbarang.model.Kategori
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.dashboard.DashboardActivity
import com.app.inventarisbarang.utils.AppDialog
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class BarangFragment : Fragment() {

    private var _binding: FragmentBarangBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BarangAdapter
    private lateinit var sessionManager: SessionManager

    private var kategoriList: List<Kategori> = emptyList()
    private var selectedKategoriId: Int? = null
    private var currentSearch: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBarangBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager.getInstance(requireContext())

        setupTopBar()
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

    private fun setupTopBar() {
        binding.topBar.tvModuleTitle.text = "Barang"
        binding.topBar.ivModuleIcon.setImageResource(R.drawable.ic_inventory)

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
            AppDialog.showNotificationSheet(requireContext()) {
                (activity as? DashboardActivity)?.switchTab(R.id.nav_peminjaman)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = BarangAdapter(
            onItemClick = { barang ->
                val intent = Intent(requireContext(), DetailBarangActivity::class.java).apply {
                    putExtra("barang_id", barang.id)
                }
                startActivity(intent)
            },
            onEditClick = { barang ->
                val intent = Intent(requireContext(), EditBarangActivity::class.java).apply {
                    putExtra("barang_id", barang.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { barang ->
                deleteBarang(barang)
            },
            isAdmin = sessionManager.isAdmin()
        )
        binding.rvBarang.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBarang.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadBarang()
        }
    }

    private fun deleteBarang(barang: Barang) {
        AppDialog.showConfirmation(
            context = requireContext(),
            title = "Hapus Aset Barang?",
            message = "Apakah Anda yakin ingin menghapus data aset ini dari katalog inventaris? Tindakan ini tidak dapat dibatalkan.",
            highlight = barang.nama_barang,
            positiveText = "Hapus Aset",
            negativeText = "Batal",
            type = AppDialog.DialogType.DESTRUCTIVE
        ) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val api = ApiConfig.getApiService(requireContext())
                    val res = api.deleteBarang(barang.id)
                    if (res.isSuccessful) {
                        Toast.makeText(requireContext(), "Barang berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadBarang()
                    } else {
                        Toast.makeText(requireContext(), ErrorUtils.parseError(res), Toast.LENGTH_SHORT).show()
                    }
                } catch (t: Throwable) {
                    Toast.makeText(requireContext(), "Gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupFab() {
        if (sessionManager.isAdmin()) {
            binding.fabTambahBarang.visibility = View.VISIBLE
            binding.fabTambahBarang.setOnClickListener {
                startActivity(Intent(requireContext(), TambahBarangActivity::class.java))
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val response = apiService.getKategori()
                if (response.isSuccessful) {
                    kategoriList = response.body()?.data ?: emptyList()
                    setupCategoryChips()
                }
            } catch (_: Throwable) {}
        }
    }

    private fun setupCategoryChips() {
        binding.layoutCategoryChips.removeAllViews()

        // 1. All chip
        addChip(null, "Semua Kategori", selectedKategoriId == null)

        // 2. Individual categories chips
        kategoriList.forEach { kategori ->
            val countStr = if (kategori.barangs_count != null && kategori.barangs_count > 0) " ${kategori.barangs_count}" else ""
            addChip(kategori.id, "${kategori.nama_kategori}$countStr", selectedKategoriId == kategori.id)
        }
    }

    private fun addChip(kategoriId: Int?, label: String, isSelected: Boolean) {
        val chip = TextView(requireContext()).apply {
            text = label
            textSize = 12f
            setPadding(dpToPx(14f), dpToPx(7f), dpToPx(14f), dpToPx(7f))
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            val layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(8f)
            }
            this.layoutParams = layoutParams

            if (isSelected) {
                setBackgroundResource(R.drawable.filter_chip_active)
                setTextColor(Color.WHITE)
            } else {
                setBackgroundResource(R.drawable.filter_chip_inactive)
                setTextColor(Color.parseColor("#64748B"))
            }

            setOnClickListener {
                selectedKategoriId = kategoriId
                setupCategoryChips()
                loadBarang()
            }
        }
        binding.layoutCategoryChips.addView(chip)
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun loadBarang() {
        binding.swipeRefresh.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val response = apiService.getBarang(
                    search = currentSearch,
                    kategoriId = selectedKategoriId
                )

                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

                    // Update Bento strip counters
                    val totalUnits = list.sumOf { it.stok }
                    val siapPakaiUnits = list.filter { it.kondisi.equals("Baik", true) }.sumOf { it.stok }
                    val perbaikanUnits = list.filter { !it.kondisi.equals("Baik", true) }.sumOf { it.stok }

                    binding.tvTotalUnitCount.text = "$totalUnits Unit"
                    binding.tvSiapPakaiCount.text = "$siapPakaiUnits Unit"
                    binding.tvPerbaikanCount.text = "$perbaikanUnits Unit"
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(context, "Koneksi gagal: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
