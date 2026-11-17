package com.app.inventarisbarang.ui.profil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.FragmentProfilBinding
import com.app.inventarisbarang.model.User
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.auth.LoginActivity
import com.app.inventarisbarang.utils.AppDialog
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager.getInstance(requireContext())

        setupTopBar()
        displayFallbackData()
        loadUserProfile()
        setupMenuActions()
    }

    private fun setupTopBar() {
        binding.topBar.tvModuleTitle.text = "Profil"
        binding.topBar.ivModuleIcon.setImageResource(R.drawable.ic_person)

        val avatar = sessionManager.getAvatar()
        if (avatar.isNotEmpty()) {
            binding.topBar.ivTopAvatar.load(avatar) {
                crossfade(true)
                placeholder(R.drawable.ic_person)
            }
        }

        binding.topBar.btnNotification.setOnClickListener {
            AppDialog.showNotificationSheet(requireContext())
        }
    }

    private fun displayFallbackData() {
        val name = sessionManager.getName().ifEmpty { sessionManager.getUsername() }
        val username = sessionManager.getUsername()
        val email = sessionManager.getEmail().ifEmpty { "admin@inventaris.id" }
        val role = sessionManager.getRole()
        val divisi = sessionManager.getDivisi()

        binding.tvNama.text = name
        binding.tvUsername.text = username
        binding.tvEmail.text = email
        binding.tvRoleBadge.text = if (sessionManager.isAdmin()) "★ ADMIN UTAMA" else "ANGGOTA"
        binding.tvRole.text = if (sessionManager.isAdmin()) "Super Administrator" else "Anggota Organisasi"
        binding.tvDivisiHeader.text = divisi
        binding.tvDivisiDetail.text = divisi

        val avatar = sessionManager.getAvatar()
        if (avatar.isNotEmpty()) {
            binding.ivAvatar.imageTintList = null
            binding.ivAvatar.setPadding(0, 0, 0, 0)
            binding.ivAvatar.load(avatar) {
                crossfade(true)
                placeholder(R.drawable.ic_person)
            }
        }
    }

    private fun loadUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                val response = apiService.getProfile(sessionManager.getUserId())
                if (response.isSuccessful) {
                    val user = response.body()?.data
                    if (user != null) {
                        updateUI(user)
                    }
                }
            } catch (_: Throwable) {
                // Fallback to sessionManager data
            }
        }
    }

    private fun updateUI(user: User) {
        binding.tvNama.text = user.name
        binding.tvUsername.text = user.username
        binding.tvEmail.text = user.email ?: "admin@inventaris.id"
        binding.tvRoleBadge.text = if (user.role.equals("admin", ignoreCase = true)) "★ ADMIN UTAMA" else "ANGGOTA"
        binding.tvRole.text = if (user.role.equals("admin", ignoreCase = true)) "Super Administrator" else "Anggota Organisasi"

        val divisi = user.divisi ?: "Divisi Operasional"
        binding.tvDivisiHeader.text = divisi
        binding.tvDivisiDetail.text = divisi

        if (user.totalApproval != null && user.totalApproval > 0) {
            binding.tvTotalApproval.text = user.totalApproval.toString()
        }

        if (!user.avatar.isNullOrEmpty()) {
            binding.ivAvatar.imageTintList = null
            binding.ivAvatar.setPadding(0, 0, 0, 0)
            binding.ivAvatar.load(user.avatar) {
                crossfade(true)
                placeholder(R.drawable.ic_person)
            }
            binding.topBar.ivTopAvatar.imageTintList = null
            binding.topBar.ivTopAvatar.setPadding(0, 0, 0, 0)
            binding.topBar.ivTopAvatar.load(user.avatar) {
                crossfade(true)
                placeholder(R.drawable.ic_person)
            }
        }
    }

    private fun setupMenuActions() {
        binding.btnMenuUbahPassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnMenuNotifikasi.setOnClickListener {
            Toast.makeText(requireContext(), "Notifikasi sistem aktif: Pengingat pengembalian otomatis", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = com.app.inventarisbarang.databinding.DialogChangePasswordBinding.inflate(layoutInflater)

        AlertDialog.Builder(requireContext())
            .setTitle("Ubah Kata Sandi")
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { _, _ ->
                val current = dialogBinding.etCurrentPassword.text.toString().trim()
                val newPass = dialogBinding.etNewPassword.text.toString().trim()
                if (current.isEmpty() || newPass.isEmpty()) {
                    Toast.makeText(requireContext(), "Form tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                doChangePassword(current, newPass)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun doChangePassword(current: String, newPass: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiConfig.getApiService(requireContext())
                val res = api.changePassword(mapOf("current_password" to current, "new_password" to newPass))
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Kata sandi berhasil diperbarui", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), ErrorUtils.parseError(res), Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                Toast.makeText(requireContext(), "Gagal: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLogoutConfirmDialog() {
        AppDialog.showConfirmation(
            context = requireContext(),
            title = "Keluar dari Akun?",
            message = "Sesi akun Anda akan diakhiri. Anda perlu login kembali untuk mengakses data inventaris.",
            positiveText = "Keluar",
            negativeText = "Batal",
            type = AppDialog.DialogType.DESTRUCTIVE
        ) {
            handleLogout()
        }
    }

    private fun handleLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(requireContext())
                apiService.logout()
            } catch (_: Throwable) {}

            sessionManager.clearSession()
            Toast.makeText(context, "Berhasil keluar", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
