package com.app.inventarisbarang.ui.pengguna

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityKelolaPenggunaBinding
import com.app.inventarisbarang.model.User
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class KelolaPenggunaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKelolaPenggunaBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: PenggunaAdapter

    private var allUsers: List<User> = emptyList()
    private var currentSearch: String? = null
    private var selectedRoleFilter: String? = null // null: Semua, "admin": Admin, "user": Anggota

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKelolaPenggunaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)

        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Akses terbatas: Hanya Administrator yang dapat mengelola pengguna.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupSearchAndFilter()
        setupFab()

        binding.swipeRefresh.setOnRefreshListener {
            loadPengguna()
        }

        loadPengguna()
    }

    private fun setupToolbar() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { _, insets ->
            val statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
            binding.appBarLayout.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = PenggunaAdapter(
            onItemClick = { user ->
                showUserOptionsDialog(user)
            },
            onActionClick = { user ->
                showUserOptionsDialog(user)
            }
        )
        binding.rvPengguna.layoutManager = LinearLayoutManager(this)
        binding.rvPengguna.adapter = adapter
    }

    private fun setupFab() {
        binding.fabTambahPengguna.setOnClickListener {
            showTambahPenggunaDialog()
        }
    }

    private fun setupSearchAndFilter() {
        binding.etSearchPengguna.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentSearch = binding.etSearchPengguna.text.toString().trim().ifEmpty { null }
                filterAndDisplayUsers()
                true
            } else {
                false
            }
        }

        binding.etSearchPengguna.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                currentSearch = s?.toString()?.trim()?.ifEmpty { null }
                filterAndDisplayUsers()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchPengguna.text?.clear()
            currentSearch = null
            filterAndDisplayUsers()
        }

        binding.chipSemua.setOnClickListener {
            selectRoleFilter(null)
        }

        binding.chipAdmin.setOnClickListener {
            selectRoleFilter("admin")
        }

        binding.chipAnggota.setOnClickListener {
            selectRoleFilter("user")
        }
    }

    private fun selectRoleFilter(role: String?) {
        selectedRoleFilter = role

        val activeBg = ContextCompat.getColorStateList(this, R.color.primary)
        val inactiveBg = ContextCompat.getColorStateList(this, R.color.white)
        val activeTextColor = ContextCompat.getColor(this, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(this, R.color.text_secondary)

        when (role) {
            null -> {
                binding.chipSemua.backgroundTintList = activeBg
                binding.chipSemua.setTextColor(activeTextColor)

                binding.chipAdmin.backgroundTintList = inactiveBg
                binding.chipAdmin.setTextColor(inactiveTextColor)

                binding.chipAnggota.backgroundTintList = inactiveBg
                binding.chipAnggota.setTextColor(inactiveTextColor)
            }
            "admin" -> {
                binding.chipSemua.backgroundTintList = inactiveBg
                binding.chipSemua.setTextColor(inactiveTextColor)

                binding.chipAdmin.backgroundTintList = activeBg
                binding.chipAdmin.setTextColor(activeTextColor)

                binding.chipAnggota.backgroundTintList = inactiveBg
                binding.chipAnggota.setTextColor(inactiveTextColor)
            }
            "user" -> {
                binding.chipSemua.backgroundTintList = inactiveBg
                binding.chipSemua.setTextColor(inactiveTextColor)

                binding.chipAdmin.backgroundTintList = inactiveBg
                binding.chipAdmin.setTextColor(inactiveTextColor)

                binding.chipAnggota.backgroundTintList = activeBg
                binding.chipAnggota.setTextColor(activeTextColor)
            }
        }

        filterAndDisplayUsers()
    }

    private fun filterAndDisplayUsers() {
        var filtered = allUsers

        selectedRoleFilter?.let { role ->
            filtered = filtered.filter { it.role.equals(role, true) }
        }

        currentSearch?.let { q ->
            val query = q.lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                        it.username.lowercase().contains(query) ||
                        (it.divisi?.lowercase()?.contains(query) == true) ||
                        (it.email?.lowercase()?.contains(query) == true)
            }
        }

        adapter.submitList(filtered)
        binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadPengguna() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val api = ApiConfig.getApiService(this@KelolaPenggunaActivity)
                val response = api.getUsers()
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    allUsers = response.body()?.data ?: emptyList()

                    // Update Statistik Bento
                    val totalAkun = allUsers.size
                    val totalAdmin = allUsers.count { it.role.equals("admin", true) }
                    val totalAnggota = allUsers.count { it.role.equals("user", true) }

                    binding.tvStatTotal.text = totalAkun.toString()
                    binding.tvStatAdmin.text = totalAdmin.toString()
                    binding.tvStatAnggota.text = totalAnggota.toString()

                    binding.chipSemua.text = "Semua ($totalAkun)"
                    binding.chipAdmin.text = "Admin ($totalAdmin)"
                    binding.chipAnggota.text = "Anggota ($totalAnggota)"

                    filterAndDisplayUsers()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@KelolaPenggunaActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@KelolaPenggunaActivity, "Gagal memuat pengguna: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUserOptionsDialog(user: User) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetBinding = com.app.inventarisbarang.databinding.BottomSheetUserActionsBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvSheetUserName.text = user.name.ifEmpty { user.username }
        val divisiText = user.divisi?.ifEmpty { "Divisi Operasional" } ?: "Divisi Operasional"
        sheetBinding.tvSheetUserSubtitle.text = "@${user.username} • $divisiText"

        val initials = if (user.name.isNotBlank()) {
            user.name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
        } else {
            user.username.take(2).uppercase()
        }
        sheetBinding.tvSheetAvatarInitial.text = initials.ifEmpty { "U" }

        val isAdmin = user.role.equals("admin", true)
        if (isAdmin) {
            sheetBinding.tvSheetRoleTag.text = "ADMIN"
            sheetBinding.tvSheetRoleTag.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
        } else {
            sheetBinding.tvSheetRoleTag.text = "ANGGOTA"
            sheetBinding.tvSheetRoleTag.backgroundTintList = ContextCompat.getColorStateList(this, R.color.secondary)
        }

        val isActive = user.statusAktif ?: true
        sheetBinding.dotSheetOnlineStatus.backgroundTintList = ContextCompat.getColorStateList(
            this,
            if (isActive) R.color.secondary else R.color.text_muted
        )

        sheetBinding.btnSheetClose.setOnClickListener {
            dialog.dismiss()
        }

        sheetBinding.btnSheetEditUser.setOnClickListener {
            dialog.dismiss()
            showEditPenggunaDialog(user)
        }

        sheetBinding.btnSheetResetPassword.setOnClickListener {
            dialog.dismiss()
            showResetPasswordDialog(user)
        }

        sheetBinding.btnSheetDeleteUser.setOnClickListener {
            dialog.dismiss()
            confirmDeleteUser(user)
        }

        dialog.show()
    }

    private fun showTambahPenggunaDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetBinding = com.app.inventarisbarang.databinding.DialogTambahPenggunaBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvDialogTitle.text = "Tambah Pengguna Baru"
        sheetBinding.tvDialogSubtitle.text = "Lengkapi data akun dan hak akses personel"
        sheetBinding.btnSimpanUser.text = "Simpan Akun"

        var selectedRole = "user"

        fun updateRoleUI() {
            if (selectedRole == "admin") {
                sheetBinding.btnRoleAdmin.setBackgroundResource(R.drawable.form_kondisi_selected_bg)
                sheetBinding.tvRoleAdminText.text = "✓ Administrator"
                sheetBinding.tvRoleAdminText.setTextColor(ContextCompat.getColor(this, R.color.primary))

                sheetBinding.btnRoleUser.setBackgroundResource(R.drawable.form_kondisi_unselected_bg)
                sheetBinding.tvRoleUserText.text = "Anggota"
                sheetBinding.tvRoleUserText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            } else {
                sheetBinding.btnRoleUser.setBackgroundResource(R.drawable.form_kondisi_selected_bg)
                sheetBinding.tvRoleUserText.text = "✓ Anggota"
                sheetBinding.tvRoleUserText.setTextColor(ContextCompat.getColor(this, R.color.primary))

                sheetBinding.btnRoleAdmin.setBackgroundResource(R.drawable.form_kondisi_unselected_bg)
                sheetBinding.tvRoleAdminText.text = "Administrator"
                sheetBinding.tvRoleAdminText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
        updateRoleUI()

        sheetBinding.btnRoleUser.setOnClickListener {
            selectedRole = "user"
            updateRoleUI()
        }

        sheetBinding.btnRoleAdmin.setOnClickListener {
            selectedRole = "admin"
            updateRoleUI()
        }

        var isPasswordVisible = false
        sheetBinding.btnToggleFormPassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                sheetBinding.etPasswordUser.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
                sheetBinding.btnToggleFormPassword.setImageResource(R.drawable.ic_visibility)
            } else {
                sheetBinding.etPasswordUser.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                sheetBinding.btnToggleFormPassword.setImageResource(R.drawable.ic_visibility_off)
            }
            sheetBinding.etPasswordUser.setSelection(sheetBinding.etPasswordUser.text?.length ?: 0)
        }

        sheetBinding.btnDialogClose.setOnClickListener { dialog.dismiss() }
        sheetBinding.btnBatalUser.setOnClickListener { dialog.dismiss() }

        sheetBinding.btnSimpanUser.setOnClickListener {
            val nama = sheetBinding.etNamaUser.text.toString().trim()
            val username = sheetBinding.etUsernameUser.text.toString().trim()
            val divisi = sheetBinding.etDivisiUser.text.toString().trim()
            val password = sheetBinding.etPasswordUser.text.toString().trim()

            if (nama.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Nama, Username, dan Kata Sandi wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Kata sandi minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val body = mutableMapOf<String, Any>(
                "name" to nama,
                "username" to username,
                "password" to password,
                "role" to selectedRole
            )
            if (divisi.isNotEmpty()) {
                body["divisi"] = divisi
            }

            sheetBinding.btnSimpanUser.isEnabled = false
            lifecycleScope.launch {
                try {
                    val api = ApiConfig.getApiService(this@KelolaPenggunaActivity)
                    val res = api.tambahUser(body)
                    if (res.isSuccessful) {
                        Toast.makeText(this@KelolaPenggunaActivity, "Akun $nama berhasil dibuat!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadPengguna()
                    } else {
                        val errorMsg = ErrorUtils.parseError(res)
                        Toast.makeText(this@KelolaPenggunaActivity, errorMsg, Toast.LENGTH_LONG).show()
                        sheetBinding.btnSimpanUser.isEnabled = true
                    }
                } catch (t: Throwable) {
                    Toast.makeText(this@KelolaPenggunaActivity, "Gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                    sheetBinding.btnSimpanUser.isEnabled = true
                }
            }
        }

        dialog.show()
    }

    private fun showEditPenggunaDialog(user: User) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetBinding = com.app.inventarisbarang.databinding.DialogTambahPenggunaBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvDialogTitle.text = "Edit Data Pengguna"
        sheetBinding.tvDialogSubtitle.text = "Perbarui akun @${user.username}"
        sheetBinding.ivDialogHeaderIcon.setImageResource(R.drawable.ic_edit)
        sheetBinding.btnSimpanUser.text = "Simpan Perubahan"

        sheetBinding.etNamaUser.setText(user.name)
        sheetBinding.etUsernameUser.setText(user.username)
        sheetBinding.etDivisiUser.setText(user.divisi ?: "")
        sheetBinding.tvPasswordLabel.text = "KATA SANDI BARU (OPSIONAL)"
        sheetBinding.etPasswordUser.hint = "Kosongkan jika tidak ingin mengubah"
        sheetBinding.tvPasswordHint.text = "Hanya isi jika ingin mengganti kata sandi akun"

        var selectedRole = user.role.lowercase()

        fun updateRoleUI() {
            if (selectedRole == "admin") {
                sheetBinding.btnRoleAdmin.setBackgroundResource(R.drawable.form_kondisi_selected_bg)
                sheetBinding.tvRoleAdminText.text = "✓ Administrator"
                sheetBinding.tvRoleAdminText.setTextColor(ContextCompat.getColor(this, R.color.primary))

                sheetBinding.btnRoleUser.setBackgroundResource(R.drawable.form_kondisi_unselected_bg)
                sheetBinding.tvRoleUserText.text = "Anggota"
                sheetBinding.tvRoleUserText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            } else {
                sheetBinding.btnRoleUser.setBackgroundResource(R.drawable.form_kondisi_selected_bg)
                sheetBinding.tvRoleUserText.text = "✓ Anggota"
                sheetBinding.tvRoleUserText.setTextColor(ContextCompat.getColor(this, R.color.primary))

                sheetBinding.btnRoleAdmin.setBackgroundResource(R.drawable.form_kondisi_unselected_bg)
                sheetBinding.tvRoleAdminText.text = "Administrator"
                sheetBinding.tvRoleAdminText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
        updateRoleUI()

        sheetBinding.btnRoleUser.setOnClickListener {
            selectedRole = "user"
            updateRoleUI()
        }

        sheetBinding.btnRoleAdmin.setOnClickListener {
            selectedRole = "admin"
            updateRoleUI()
        }

        var isPasswordVisible = false
        sheetBinding.btnToggleFormPassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                sheetBinding.etPasswordUser.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
                sheetBinding.btnToggleFormPassword.setImageResource(R.drawable.ic_visibility)
            } else {
                sheetBinding.etPasswordUser.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                sheetBinding.btnToggleFormPassword.setImageResource(R.drawable.ic_visibility_off)
            }
            sheetBinding.etPasswordUser.setSelection(sheetBinding.etPasswordUser.text?.length ?: 0)
        }

        sheetBinding.btnDialogClose.setOnClickListener { dialog.dismiss() }
        sheetBinding.btnBatalUser.setOnClickListener { dialog.dismiss() }

        sheetBinding.btnSimpanUser.setOnClickListener {
            val nama = sheetBinding.etNamaUser.text.toString().trim()
            val username = sheetBinding.etUsernameUser.text.toString().trim()
            val divisi = sheetBinding.etDivisiUser.text.toString().trim()
            val password = sheetBinding.etPasswordUser.text.toString().trim()

            if (nama.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Nama dan Username wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val body = mutableMapOf<String, Any>(
                "name" to nama,
                "username" to username,
                "role" to selectedRole
            )
            if (divisi.isNotEmpty()) {
                body["divisi"] = divisi
            }
            if (password.isNotEmpty()) {
                if (password.length < 6) {
                    Toast.makeText(this, "Kata sandi minimal 6 karakter", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                body["password"] = password
            }

            sheetBinding.btnSimpanUser.isEnabled = false
            lifecycleScope.launch {
                try {
                    val api = ApiConfig.getApiService(this@KelolaPenggunaActivity)
                    val res = api.updateUser(user.id, body)
                    if (res.isSuccessful) {
                        Toast.makeText(this@KelolaPenggunaActivity, "Perubahan $nama berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadPengguna()
                    } else {
                        val errorMsg = ErrorUtils.parseError(res)
                        Toast.makeText(this@KelolaPenggunaActivity, errorMsg, Toast.LENGTH_LONG).show()
                        sheetBinding.btnSimpanUser.isEnabled = true
                    }
                } catch (t: Throwable) {
                    Toast.makeText(this@KelolaPenggunaActivity, "Gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                    sheetBinding.btnSimpanUser.isEnabled = true
                }
            }
        }

        dialog.show()
    }

    private fun showResetPasswordDialog(user: User) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetBinding = com.app.inventarisbarang.databinding.BottomSheetResetPasswordBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val initials = if (user.name.isNotBlank()) {
            user.name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
        } else {
            user.username.take(2).uppercase()
        }
        sheetBinding.tvTargetAvatarInitial.text = initials.ifEmpty { "U" }
        sheetBinding.tvTargetNama.text = user.name.ifEmpty { user.username }

        val isAdmin = user.role.equals("admin", true)
        if (isAdmin) {
            sheetBinding.tvTargetRoleTag.text = "ADMIN"
            sheetBinding.tvTargetRoleTag.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
        } else {
            sheetBinding.tvTargetRoleTag.text = "ANGGOTA"
            sheetBinding.tvTargetRoleTag.backgroundTintList = ContextCompat.getColorStateList(this, R.color.secondary)
        }

        val divisiText = user.divisi?.ifEmpty { "Divisi Operasional" } ?: "Divisi Operasional"
        sheetBinding.tvTargetUsername.text = "@${user.username} • $divisiText"

        var isPasswordVisible = false
        sheetBinding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                sheetBinding.etNewPassword.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
                sheetBinding.btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            } else {
                sheetBinding.etNewPassword.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                sheetBinding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }
            sheetBinding.etNewPassword.setSelection(sheetBinding.etNewPassword.text?.length ?: 0)
        }

        sheetBinding.btnSheetClose.setOnClickListener { dialog.dismiss() }
        sheetBinding.btnBatalReset.setOnClickListener { dialog.dismiss() }

        sheetBinding.btnSimpanReset.setOnClickListener {
            val newPass = sheetBinding.etNewPassword.text.toString().trim()
            if (newPass.length < 6) {
                Toast.makeText(this, "Kata sandi baru minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sheetBinding.btnSimpanReset.isEnabled = false
            lifecycleScope.launch {
                try {
                    val api = ApiConfig.getApiService(this@KelolaPenggunaActivity)
                    val res = api.updateUser(user.id, mapOf("password" to newPass))
                    if (res.isSuccessful) {
                        Toast.makeText(this@KelolaPenggunaActivity, "Kata sandi @${user.username} berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this@KelolaPenggunaActivity, ErrorUtils.parseError(res), Toast.LENGTH_SHORT).show()
                        sheetBinding.btnSimpanReset.isEnabled = true
                    }
                } catch (t: Throwable) {
                    Toast.makeText(this@KelolaPenggunaActivity, "Gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                    sheetBinding.btnSimpanReset.isEnabled = true
                }
            }
        }

        dialog.show()
    }

    private fun confirmDeleteUser(user: User) {
        val currentUserId = sessionManager.getUserId()
        if (user.id == currentUserId) {
            Toast.makeText(this, "Anda tidak dapat menghapus akun Anda sendiri!", Toast.LENGTH_LONG).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Akun Pengguna")
            .setMessage("Yakin ingin menghapus akun @${user.username} (${user.name})? Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus Akun") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val api = ApiConfig.getApiService(this@KelolaPenggunaActivity)
                        val res = api.deleteUser(user.id)
                        if (res.isSuccessful) {
                            Toast.makeText(this@KelolaPenggunaActivity, "Akun @${user.username} berhasil dihapus", Toast.LENGTH_SHORT).show()
                            loadPengguna()
                        } else {
                            Toast.makeText(this@KelolaPenggunaActivity, ErrorUtils.parseError(res), Toast.LENGTH_SHORT).show()
                        }
                    } catch (t: Throwable) {
                        Toast.makeText(this@KelolaPenggunaActivity, "Gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
