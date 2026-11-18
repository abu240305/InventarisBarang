package com.app.inventarisbarang.ui.dashboard

import android.content.Intent
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
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.app.inventarisbarang.ui.kategori.KategoriFragment
import com.app.inventarisbarang.ui.peminjaman.PeminjamanFragment
import com.app.inventarisbarang.ui.profil.ProfilFragment
import com.app.inventarisbarang.utils.AppDialog
import com.app.inventarisbarang.utils.AppNotificationManager
import com.app.inventarisbarang.utils.SessionManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var sessionManager: SessionManager

    private var currentScanTarget = "BARANG"

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            AppNotificationManager.syncAndNotify(this, forceNotifyFirst = true)
        }
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Pemindaian dibatalkan", Toast.LENGTH_SHORT).show()
        } else {
            val scannedCode = result.contents
            handleScannedBarcode(scannedCode)
        }
    }

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

        setupNotifications()
    }

    override fun onResume() {
        super.onResume()
        AppNotificationManager.syncAndNotify(this, forceNotifyFirst = false)
    }

    private fun setupNotifications() {
        AppNotificationManager.initChannel(this)
        AppNotificationManager.scheduleBackgroundSync(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                AppNotificationManager.syncAndNotify(this, forceNotifyFirst = true)
            }
        } else {
            AppNotificationManager.syncAndNotify(this, forceNotifyFirst = true)
        }
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
            // Langsung buka layar kamera scanner 1 kali klik (tanpa pop-up perantara)
            val intent = Intent(this, com.app.inventarisbarang.ui.scan.ScanBarcodeActivity::class.java).apply {
                putExtra("SCAN_MODE", "BARANG")
            }
            startActivity(intent)
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
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetBinding = com.app.inventarisbarang.databinding.BottomSheetQrScannerBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.cardScanBarang.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, com.app.inventarisbarang.ui.scan.ScanBarcodeActivity::class.java).apply {
                putExtra("SCAN_MODE", "BARANG")
            }
            startActivity(intent)
        }

        sheetBinding.cardScanTiket.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, com.app.inventarisbarang.ui.scan.ScanBarcodeActivity::class.java).apply {
                putExtra("SCAN_MODE", "TIKET")
            }
            startActivity(intent)
        }

        sheetBinding.btnStartCameraScan.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, com.app.inventarisbarang.ui.scan.ScanBarcodeActivity::class.java).apply {
                putExtra("SCAN_MODE", "BARANG")
            }
            startActivity(intent)
        }

        dialog.show()
    }

    private fun startBarcodeScanner(promptText: String) {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            setPrompt(promptText)
            setCameraId(0) // Kamera belakang
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
        }
        barcodeLauncher.launch(options)
    }

    private fun handleScannedBarcode(code: String) {
        val targetDesc = if (currentScanTarget == "BARANG") "Aset Barang" else "Tiket Peminjaman"

        AppDialog.showConfirmation(
            context = this,
            title = "Kode Terdeteksi ($targetDesc)",
            message = "Kode berhasil terbaca oleh scanner. Apakah Anda ingin langsung membuka dan memproses data ini?",
            highlight = code,
            positiveText = "Buka Sekarang",
            negativeText = "Tutup",
            type = AppDialog.DialogType.SUCCESS
        ) {
            if (currentScanTarget == "BARANG") {
                switchTab(R.id.nav_barang)
            } else {
                switchTab(R.id.nav_peminjaman)
            }
        }
    }
}
