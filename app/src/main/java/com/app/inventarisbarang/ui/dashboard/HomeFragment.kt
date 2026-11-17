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
import com.app.inventarisbarang.model.User
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.barang.TambahBarangActivity
import com.app.inventarisbarang.ui.peminjaman.DetailPeminjamanActivity
import com.app.inventarisbarang.ui.peminjaman.PeminjamanAdapter
import com.app.inventarisbarang.ui.peminjaman.TambahPeminjamanActivity
import com.app.inventarisbarang.utils.AppDialog
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var peminjamanAdapter: PeminjamanAdapter
    private var lastDashboardData: com.app.inventarisbarang.model.DashboardData? = null

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
            AppDialog.showNotificationSheet(
                context = requireContext(),
                onNotificationDismissed = {
                    binding.topBar.dotNotification.visibility = View.GONE
                },
                onViewPeminjaman = {
                    (activity as? DashboardActivity)?.switchTab(R.id.nav_peminjaman)
                }
            )
        }

        AppDialog.checkUnreadNotifications(requireContext()) { count ->
            if (isAdded) {
                binding.topBar.dotNotification.visibility = if (count > 0) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupUI() {
        val name = sessionManager.getName().ifEmpty { sessionManager.getUsername() }
        val role = sessionManager.getRole()

        binding.tvGreeting.text = "Halo, $name!"
        binding.tvRoleBadge.text = if (role.equals("admin", true)) "★ ADMIN UTAMA" else "ANGGOTA"
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
            if (!sessionManager.isAdmin()) {
                Toast.makeText(requireContext(), "Akses terbatas: Hanya Administrator yang dapat mengelola pengguna.", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(requireContext(), com.app.inventarisbarang.ui.pengguna.KelolaPenggunaActivity::class.java)
                startActivity(intent)
            }
        }

        binding.modulPindaiQR.setOnClickListener {
            val intent = Intent(requireContext(), com.app.inventarisbarang.ui.scan.ScanBarcodeActivity::class.java).apply {
                putExtra("SCAN_MODE", "BARANG")
            }
            startActivity(intent)
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
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val sheetBinding = com.app.inventarisbarang.databinding.BottomSheetKelolaPenggunaBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val dummyUsers = listOf(
            User(id = 1, name = "Administrator", username = "admin", role = "admin", divisi = "Divisi Sarana & Prasarana"),
            User(id = 2, name = "Anggota", username = "user", role = "user", divisi = "Divisi Operasional"),
            User(id = 3, name = "Ahmad Setiawan", username = "ahmad", role = "user", divisi = "Divisi Media & IT"),
            User(id = 4, name = "Rian Dirgantara", username = "rian", role = "user", divisi = "Humas & Dokumentasi"),
            User(id = 5, name = "Siti Indrawati", username = "siti", role = "user", divisi = "Fasilitator Workshop"),
            User(id = 6, name = "Bambang Pamungkas", username = "bambang", role = "user", divisi = "Divisi Acara")
        )

        sheetBinding.rvPengguna.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        sheetBinding.rvPengguna.adapter = UserManageAdapter(dummyUsers) { selectedUser ->
            Toast.makeText(requireContext(), "User terpilih: ${selectedUser.name} (${selectedUser.role})", Toast.LENGTH_SHORT).show()
        }

        sheetBinding.btnTambahPengguna.setOnClickListener {
            showTambahPenggunaDialog()
        }

        dialog.show()
    }

    private fun showTambahPenggunaDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tambah_pengguna, null)
        val etNama = dialogView.findViewById<android.widget.EditText>(R.id.etNamaUser)
        val etUsername = dialogView.findViewById<android.widget.EditText>(R.id.etUsernameUser)
        val etDivisi = dialogView.findViewById<android.widget.EditText>(R.id.etDivisiUser)
        val etPassword = dialogView.findViewById<android.widget.EditText>(R.id.etPasswordUser)
        val btnRoleUser = dialogView.findViewById<android.widget.LinearLayout>(R.id.btnRoleUser)
        val btnRoleAdmin = dialogView.findViewById<android.widget.LinearLayout>(R.id.btnRoleAdmin)
        val tvRoleUser = dialogView.findViewById<android.widget.TextView>(R.id.tvRoleUserText)
        val tvRoleAdmin = dialogView.findViewById<android.widget.TextView>(R.id.tvRoleAdminText)

        var selectedRole = "user"

        btnRoleUser.setOnClickListener {
            selectedRole = "user"
            btnRoleUser.setBackgroundResource(R.drawable.form_kondisi_selected_bg)
            tvRoleUser.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary))
            btnRoleAdmin.setBackgroundResource(R.drawable.form_kondisi_unselected_bg)
            tvRoleAdmin.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        btnRoleAdmin.setOnClickListener {
            selectedRole = "admin"
            btnRoleAdmin.setBackgroundResource(R.drawable.form_kondisi_selected_bg)
            tvRoleAdmin.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary))
            btnRoleUser.setBackgroundResource(R.drawable.form_kondisi_unselected_bg)
            tvRoleUser.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        val alertDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNama.text.toString().trim()
                val username = etUsername.text.toString().trim()
                if (nama.isNotEmpty() && username.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Pengguna $nama ($selectedRole) berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Nama dan Username wajib diisi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    private fun showKondisiFilterDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val sheetBinding = com.app.inventarisbarang.databinding.BottomSheetKondisiAsetBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        fun updateUI(stokBaik: Int, jenisBaik: Int, stokRingan: Int, jenisRingan: Int, stokBerat: Int, jenisBerat: Int, total: Int) {
            val pct = if (total > 0) (stokBaik * 100 / total) else 100
            sheetBinding.tvPersentaseKelaikan.text = "$pct% Siap"
            sheetBinding.pbKelaikan.progress = pct
            sheetBinding.tvRingkasanKelaikan.text = "Total $total unit fisik tercatat dalam sistem inventaris."

            sheetBinding.tvStokBaik.text = "$stokBaik Unit"
            sheetBinding.tvJenisBaik.text = "$jenisBaik klasifikasi barang"

            sheetBinding.tvStokRusakRingan.text = "$stokRingan Unit"
            sheetBinding.tvJenisRusakRingan.text = "$jenisRingan klasifikasi barang"

            sheetBinding.tvStokRusakBerat.text = "$stokBerat Unit"
            sheetBinding.tvJenisRusakBerat.text = "$jenisBerat klasifikasi barang"
        }

        // Tampilkan data cache awal jika tersedia
        lastDashboardData?.let { d ->
            val total = d.total_stok_barang
            val sBaik = if (d.stok_baik > 0 || total == 0) d.stok_baik else total
            updateUI(sBaik, d.jenis_baik, d.stok_rusak_ringan, d.jenis_rusak_ringan, d.stok_rusak_berat, d.jenis_rusak_berat, total)
        }

        // Ambil data terbaru secara realtime langsung dari katalog barang
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val res = apiService.getBarang()
                if (res.isSuccessful) {
                    val list = res.body()?.data ?: emptyList()
                    val sBaik = list.filter { it.kondisi.equals("Baik", true) }.sumOf { it.stok }
                    val jBaik = list.filter { it.kondisi.equals("Baik", true) }.size
                    val sRingan = list.filter { it.kondisi.contains("rusak ringan", true) || it.kondisi.contains("perbaikan", true) }.sumOf { it.stok }
                    val jRingan = list.filter { it.kondisi.contains("rusak ringan", true) || it.kondisi.contains("perbaikan", true) }.size
                    val sBerat = list.filter { it.kondisi.contains("rusak berat", true) || it.kondisi.equals("rusak", true) }.sumOf { it.stok }
                    val jBerat = list.filter { it.kondisi.contains("rusak berat", true) || it.kondisi.equals("rusak", true) }.size
                    val total = list.sumOf { it.stok }
                    updateUI(sBaik, jBaik, sRingan, jRingan, sBerat, jBerat, total)
                }
            } catch (_: Throwable) {}
        }

        sheetBinding.btnSheetClose.setOnClickListener {
            dialog.dismiss()
        }

        val navigateToBarang = {
            dialog.dismiss()
            (activity as? DashboardActivity)?.switchTab(R.id.nav_barang)
        }

        sheetBinding.btnLihatKatalog.setOnClickListener { navigateToBarang() }
        sheetBinding.cardKondisiBaik.setOnClickListener { navigateToBarang() }
        sheetBinding.cardKondisiPerbaikan.setOnClickListener { navigateToBarang() }
        sheetBinding.cardKondisiRusak.setOnClickListener { navigateToBarang() }

        dialog.show()
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
                        lastDashboardData = data
                        val totalKategori = data.total_kategori ?: 4
                        val totalJenis = data.total_jenis_barang
                        binding.tvTotalJenisBarang.text = "$totalKategori / $totalJenis"
                        binding.tvTotalStok.text = data.total_stok_barang.toString()
                        binding.tvPinjamMenunggu.text = data.peminjaman_menunggu.toString()
                        binding.tvPinjamAktif.text = data.peminjaman_aktif.toString()

                        val rawList = data.peminjaman_terbaru ?: emptyList()
                        val list = if (!sessionManager.isAdmin()) {
                            rawList.filter { it.user_id == sessionManager.getUserId() }
                        } else {
                            rawList
                        }
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
