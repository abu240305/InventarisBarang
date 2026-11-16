package com.app.inventarisbarang.ui.profil

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityProfilBinding
import com.app.inventarisbarang.model.User
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.auth.LoginActivity
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class ProfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)

        setupToolbar()
        displayFallbackData()
        loadUserProfile()

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun displayFallbackData() {
        val name = sessionManager.getName().ifEmpty { sessionManager.getUsername() }
        val username = sessionManager.getUsername()
        val email = sessionManager.getEmail().ifEmpty { "Tidak ada email" }
        val role = sessionManager.getRole()

        binding.tvNama.text = name
        binding.tvUsername.text = username
        binding.tvEmail.text = email
        binding.tvRoleBadge.text = role.replaceFirstChar { it.uppercase() }

        val badgeBg = if (sessionManager.isAdmin()) R.color.role_admin_bg else R.color.role_user_bg
        binding.tvRoleBadge.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, badgeBg))
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@ProfilActivity)
                val response = apiService.getProfile(sessionManager.getUserId())
                if (response.isSuccessful) {
                    val user = response.body()?.data
                    if (user != null) {
                        updateUI(user)
                    }
                }
            } catch (_: Throwable) {
                // Fallback to sessionManager data already displayed
            }
        }
    }

    private fun updateUI(user: User) {
        binding.tvNama.text = user.name
        binding.tvUsername.text = user.username
        binding.tvEmail.text = user.email ?: "Tidak ada email"
        binding.tvRoleBadge.text = user.role.replaceFirstChar { it.uppercase() }

        val badgeBg = if (user.role.equals("admin", ignoreCase = true)) {
            R.color.role_admin_bg
        } else {
            R.color.role_user_bg
        }
        binding.tvRoleBadge.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, badgeBg))
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            .setPositiveButton("Keluar") { _, _ ->
                handleLogout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun handleLogout() {
        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@ProfilActivity)
                apiService.logout()
            } catch (_: Throwable) {
                // Even if backend fails, clear session locally
            }

            sessionManager.clearSession()
            Toast.makeText(this@ProfilActivity, "Berhasil keluar", Toast.LENGTH_SHORT).show()

            val intent = Intent(this@ProfilActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
