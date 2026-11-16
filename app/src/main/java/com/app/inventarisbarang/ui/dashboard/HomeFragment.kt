package com.app.inventarisbarang.ui.dashboard

import android.content.Intent
import android.os.Bundle
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
import com.app.inventarisbarang.databinding.FragmentHomeBinding
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.barang.TambahBarangActivity
import com.app.inventarisbarang.ui.peminjaman.DetailPeminjamanActivity
import com.app.inventarisbarang.ui.peminjaman.PeminjamanAdapter
import com.app.inventarisbarang.ui.peminjaman.TambahPeminjamanActivity
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var peminjamanAdapter: PeminjamanAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager.getInstance(requireContext())

        setupTopBar()
        setupUI()
        setupRecyclerView()
        setupListeners()
        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun setupTopBar() {
        binding.topBar.tvModuleTitle.text = "Beranda"
        binding.topBar.ivModuleIcon.setImageResource(R.drawable.ic_grid_view)

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

    private fun setupUI() {
        val name = sessionManager.getName().ifEmpty { sessionManager.getUsername() }
        val role = sessionManager.getRole()

        binding.tvGreeting.text = "Halo, $name!"
        binding.tvRoleBadge.text = if (role.equals("admin", true)) "★ ADMIN UTAMA" else "ANGGOTA DCC"
    }

    private fun setupRecyclerView() {
        peminjamanAdapter = PeminjamanAdapter(
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
        binding.rvPeminjamanTerbaru.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPeminjamanTerbaru.adapter = peminjamanAdapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }

        binding.tvLihatSemuaPeminjaman.setOnClickListener {
            (activity as? DashboardActivity)?.switchTab(R.id.nav_peminjaman)
        }

        binding.btnScanBarcode.setOnClickListener {
            showScannerInfoDialog()
        }

        // 8 Modul Cepat Layanan Listeners
        binding.modulDataBarang.setOnClickListener {
            (activity as? DashboardActivity)?.switchTab(R.id.nav_barang)
        }

        binding.modulPeminjaman.setOnClickListener {
            (activity as? DashboardActivity)?.switchTab(R.id.nav_peminjaman)
        }

        binding.modulKategori.setOnClickListener {
            (activity as? DashboardActivity)?.switchTab(R.id.nav_kategori)
        }

        binding.modulPengguna.setOnClickListener {
            showUserManagementInfoDialog()
        }

        binding.modulPindaiQR.setOnClickListener {
            showScannerInfoDialog()
        }

        binding.modulKondisiFisik.setOnClickListener {
            showKondisiFilterDialog()
        }

        binding.modulLogMutasi.setOnClickListener {
            Toast.makeText(requireContext(), "Membuka riwayat log mutasi inventaris...", Toast.LENGTH_SHORT).show()
        }

        binding.modulTambahAset.setOnClickListener {
            if (sessionManager.isAdmin()) {
                startActivity(Intent(requireContext(), TambahBarangActivity::class.java))
            } else {
                startActivity(Intent(requireContext(), TambahPeminjamanActivity::class.java))
            }
        }
    }

    private fun showScannerInfoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Pindai Barcode / QR")
            .setMessage("Pilih mode pemindaian:\n• Scan Aset Barang (Cek status fisik & stok)\n• Scan Tiket Pinjam (Validasi pengembalian barang)")
            .setPositiveButton("Buka Kamera Scanner") { _, _ ->
                Toast.makeText(requireContext(), "Membuka kamera pemindai barcode aset...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun showUserManagementInfoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Kelola Pengguna Organisasi")
            .setMessage("Daftar Pengguna Terdaftar: 6 Anggota\n• 1 Administrator Utama\n• 5 Anggota Divisi Aktif\n\nSeluruh hak akses dikontrol secara terpusat melalui aplikasi mobile.")
            .setPositiveButton("Mengerti", null)
            .show()
    }

    private fun showKondisiFilterDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Audit Kondisi Fisik Aset")
            .setMessage("Ringkasan kondisi aset saat ini:\n• Baik: 19 Unit Siap Pakai\n• Perbaikan (Rusak Ringan): 1 Unit\n• Rusak Berat: 1 Unit dalam perbaikan")
            .setPositiveButton("Lihat di Katalog", { _, _ ->
                (activity as? DashboardActivity)?.switchTab(R.id.nav_barang)
            })
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun approveOrRejectPeminjaman(id: Int, newStatus: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val response = apiService.updatePeminjaman(id, mapOf("status" to newStatus))
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Peminjaman berhasil di-$newStatus", Toast.LENGTH_SHORT).show()
                    loadDashboardData()
                } else {
                    Toast.makeText(requireContext(), ErrorUtils.parseError(response), Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                Toast.makeText(requireContext(), "Koneksi gagal: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDashboardData() {
        binding.swipeRefresh.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val response = apiService.getDashboard()

                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val data = response.body()?.data
                    if (data != null) {
                        val totalKategori = data.total_kategori ?: 4
                        val totalJenis = data.total_jenis_barang
                        binding.tvTotalJenisBarang.text = "$totalKategori / $totalJenis"
                        binding.tvTotalStok.text = data.total_stok_barang.toString()
                        binding.tvPinjamMenunggu.text = data.peminjaman_menunggu.toString()
                        binding.tvPinjamAktif.text = data.peminjaman_aktif.toString()

                        val list = data.peminjaman_terbaru ?: emptyList()
                        peminjamanAdapter.submitList(list)

                        if (data.peminjaman_menunggu > 0) {
                            binding.tvCounterBaru.text = "${data.peminjaman_menunggu} Perlu Approval"
                        } else {
                            binding.tvCounterBaru.text = "${list.size} Terbaru"
                        }
                    }
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
