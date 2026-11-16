package com.app.inventarisbarang.ui.dashboard

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityDashboardBinding
import com.app.inventarisbarang.ui.barang.BarangFragment
import com.app.inventarisbarang.ui.kategori.KategoriFragment
import com.app.inventarisbarang.ui.peminjaman.PeminjamanFragment
import com.app.inventarisbarang.ui.profil.ProfilFragment
import com.app.inventarisbarang.utils.SessionManager

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var sessionManager: SessionManager

    val homeFragment = HomeFragment()
    val barangFragment = BarangFragment()
    val peminjamanFragment = PeminjamanFragment()
    val kategoriFragment = KategoriFragment()
    val profilFragment = ProfilFragment()

    private var activeFragment: Fragment = homeFragment
    private var currentTabId: Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)

        setupWindowInsets()
        setupBottomNavigation()

        // Set initial fragment
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, homeFragment, "HOME")
            commit()
        }
        updateNavUI(R.id.nav_home)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )

            // Padding top to prevent status bar / camera notch collision
            binding.fragmentContainer.setPadding(0, systemBars.top, 0, 0)

            // Margin bottom for floating scaffold to prevent gesture / navigation bar collision
            (binding.bottomNavScaffold.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                lp.bottomMargin = systemBars.bottom
                binding.bottomNavScaffold.layoutParams = lp
            }

            // Margin bottom for content container
            (binding.fragmentContainer.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                lp.bottomMargin = systemBars.bottom + dpToPx(76f).toInt()
                binding.fragmentContainer.layoutParams = lp
            }

            insets
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics,
        )
    }

    private fun setupBottomNavigation() {
        binding.navBeranda.setOnClickListener {
            switchTab(R.id.nav_home)
        }

        binding.navBarang.setOnClickListener {
            switchTab(R.id.nav_barang)
        }

        binding.navPinjam.setOnClickListener {
            switchTab(R.id.nav_peminjaman)
        }

        binding.navProfil.setOnClickListener {
            switchTab(R.id.nav_profil)
        }

        binding.fabScanQR.setOnClickListener {
            showQuickScanDialog()
        }
    }

    private fun updateNavUI(selectedId: Int) {
        val colorActive = ContextCompat.getColor(this, R.color.primary)
        val colorInactive = ContextCompat.getColor(this, R.color.nav_inactive)

        // Reset all
        binding.ivNavBeranda.imageTintList = ColorStateList.valueOf(colorInactive)
        binding.tvNavBeranda.setTextColor(colorInactive)
        binding.tvNavBeranda.setTypeface(null, android.graphics.Typeface.NORMAL)

        binding.ivNavBarang.imageTintList = ColorStateList.valueOf(colorInactive)
        binding.tvNavBarang.setTextColor(colorInactive)
        binding.tvNavBarang.setTypeface(null, android.graphics.Typeface.NORMAL)

        binding.ivNavPinjam.imageTintList = ColorStateList.valueOf(colorInactive)
        binding.tvNavPinjam.setTextColor(colorInactive)
        binding.tvNavPinjam.setTypeface(null, android.graphics.Typeface.NORMAL)

        binding.ivNavProfil.imageTintList = ColorStateList.valueOf(colorInactive)
        binding.tvNavProfil.setTextColor(colorInactive)
        binding.tvNavProfil.setTypeface(null, android.graphics.Typeface.NORMAL)

        // Set active
        when (selectedId) {
            R.id.nav_home -> {
                binding.ivNavBeranda.imageTintList = ColorStateList.valueOf(colorActive)
                binding.tvNavBeranda.setTextColor(colorActive)
                binding.tvNavBeranda.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            R.id.nav_barang -> {
                binding.ivNavBarang.imageTintList = ColorStateList.valueOf(colorActive)
                binding.tvNavBarang.setTextColor(colorActive)
                binding.tvNavBarang.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            R.id.nav_peminjaman -> {
                binding.ivNavPinjam.imageTintList = ColorStateList.valueOf(colorActive)
                binding.tvNavPinjam.setTextColor(colorActive)
                binding.tvNavPinjam.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            R.id.nav_profil -> {
                binding.ivNavProfil.imageTintList = ColorStateList.valueOf(colorActive)
                binding.tvNavProfil.setTextColor(colorActive)
                binding.tvNavProfil.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
    }

    fun switchTab(itemId: Int) {
        currentTabId = itemId
        updateNavUI(itemId)

        when (itemId) {
            R.id.nav_home -> showFragment(homeFragment, "HOME")
            R.id.nav_barang -> showFragment(barangFragment, "BARANG")
            R.id.nav_peminjaman -> showFragment(peminjamanFragment, "PEMINJAMAN")
            R.id.nav_kategori -> showFragment(kategoriFragment, "KATEGORI")
            R.id.nav_profil -> showFragment(profilFragment, "PROFIL")
        }
    }

    private fun showFragment(targetFragment: Fragment, tag: String) {
        if (targetFragment == activeFragment) return

        val transaction = supportFragmentManager.beginTransaction()

        if (!targetFragment.isAdded) {
            transaction.hide(activeFragment).add(R.id.fragmentContainer, targetFragment, tag)
        } else {
            transaction.hide(activeFragment).show(targetFragment)
        }

        activeFragment = targetFragment
        transaction.commit()
    }

    private fun showQuickScanDialog() {
        AlertDialog.Builder(this)
            .setTitle("Pindai Barcode / QR")
            .setMessage("Pilih mode pemindaian:\n• Scan Aset Barang (Cek status & stok)\n• Scan Tiket Pinjam (Validasi pengembalian)")
            .setPositiveButton("Buka Kamera Scanner") { _, _ ->
                Toast.makeText(this, "Membuka modul QR Scanner...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Tutup", null)
            .show()
    }
}
