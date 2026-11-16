package com.app.inventarisbarang.ui.peminjaman

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
import com.app.inventarisbarang.databinding.FragmentPeminjamanBinding
import com.app.inventarisbarang.model.Peminjaman
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.dashboard.DashboardActivity
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class PeminjamanFragment : Fragment() {

    private var _binding: FragmentPeminjamanBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PeminjamanAdapter
    private lateinit var sessionManager: SessionManager
    private var selectedStatus: String? = null
    private var currentSearch: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeminjamanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager.getInstance(requireContext())

        setupTopBar()
        setupRecyclerView()
        setupStatusChips()
        setupSearch()
        setupFab()
        loadPeminjaman()
    }

    override fun onResume() {
        super.onResume()
        loadPeminjaman()
    }

    private fun setupTopBar() {
        binding.topBar.tvModuleTitle.text = "Pinjam"
        binding.topBar.ivModuleIcon.setImageResource(R.drawable.ic_assignment)

        val avatar = sessionManager.getAvatar()
        if (avatar.isNotEmpty()) {
            binding.topBar.ivTopAvatar.load(avatar) {
                crossfade(true)
                placeholder(R.drawable.ic_person)
            }
        }

        binding.topBar.btnTopProfile.setOnClickListener {
            (activity as? DashboardActivity)?.switchTab(R.id.nav_profil)
        }

        binding.topBar.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Semua notifikasi inventaris telah diperbarui", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = PeminjamanAdapter(
            onItemClick = { peminjaman ->
                val intent = Intent(requireContext(), DetailPeminjamanActivity::class.java).apply {
                    putExtra("peminjaman_id", peminjaman.id)
                }
                startActivity(intent)
            },
            onApproveClick = { peminjaman ->
                approveOrRejectPeminjaman(peminjaman.id, "Disetujui")
            },
            onRejectClick = { peminjaman ->
                approveOrRejectPeminjaman(peminjaman.id, "Ditolak")
            },
            isAdmin = sessionManager.isAdmin()
        )
        binding.rvPeminjaman.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPeminjaman.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadPeminjaman()
        }
    }

    private fun approveOrRejectPeminjaman(id: Int, newStatus: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val response = apiService.updatePeminjaman(id, mapOf("status" to newStatus))
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Peminjaman berhasil di-$newStatus", Toast.LENGTH_SHORT).show()
                    loadPeminjaman()
                } else {
                    Toast.makeText(requireContext(), ErrorUtils.parseError(response), Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                Toast.makeText(requireContext(), "Gagal: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupStatusChips() {
        binding.chipSemua.setOnClickListener { selectStatus(null, binding.chipSemua) }
        binding.chipMenunggu.setOnClickListener { selectStatus("Menunggu", binding.chipMenunggu) }
        binding.chipDisetujui.setOnClickListener { selectStatus("Disetujui", binding.chipDisetujui) }
        binding.chipDikembalikan.setOnClickListener { selectStatus("Dikembalikan", binding.chipDikembalikan) }
    }

    private fun selectStatus(status: String?, selectedView: TextView) {
        selectedStatus = status

        val allChips = listOf(binding.chipSemua, binding.chipMenunggu, binding.chipDisetujui, binding.chipDikembalikan)
        allChips.forEach { chip ->
            if (chip == selectedView) {
                chip.setBackgroundResource(R.drawable.filter_chip_active)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.filter_chip_inactive)
                chip.setTextColor(Color.parseColor("#64748B"))
            }
        }
        loadPeminjaman()
    }

    private fun setupSearch() {
        binding.etSearchPeminjaman.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentSearch = binding.etSearchPeminjaman.text.toString().trim().ifEmpty { null }
                loadPeminjaman()
                true
            } else {
                false
            }
        }

        binding.etSearchPeminjaman.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnClearSearchPeminjaman.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty() && currentSearch != null) {
                    currentSearch = null
                    loadPeminjaman()
                }
            }
        })

        binding.btnClearSearchPeminjaman.setOnClickListener {
            binding.etSearchPeminjaman.text?.clear()
            currentSearch = null
            loadPeminjaman()
        }
    }

    private fun setupFab() {
        binding.fabTambahPeminjaman.setOnClickListener {
            startActivity(Intent(requireContext(), TambahPeminjamanActivity::class.java))
        }
    }

    private fun loadPeminjaman() {
        binding.swipeRefresh.isRefreshing = true

        val userId = if (sessionManager.isAdmin()) null else sessionManager.getUserId()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val response = apiService.getPeminjaman(
                    search = currentSearch,
                    userId = userId,
                    status = selectedStatus
                )

                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

                    // Update counter aktif
                    val aktifCount = list.count { it.status.equals("Disetujui", ignoreCase = true) }
                    binding.tvCounterAktif.text = "• $aktifCount Aktif"
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
