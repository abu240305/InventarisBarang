package com.app.inventarisbarang.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityRegisterBinding
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.utils.ErrorUtils
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isCoveringEyes = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }

        setupBearInteractions()
        setupListeners()
    }

    private fun setupBearInteractions() {
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            setBearState(coverEyes = hasFocus)
        }
    }

    private fun setBearState(coverEyes: Boolean) {
        if (isCoveringEyes == coverEyes) return
        isCoveringEyes = coverEyes

        binding.ivBearChar.animate()
            .scaleX(0.88f)
            .scaleY(0.88f)
            .setDuration(110)
            .withEndAction {
                if (coverEyes) {
                    binding.ivBearChar.setImageResource(R.drawable.bear_cover_eyes)
                } else {
                    binding.ivBearChar.setImageResource(R.drawable.bear_normal)
                }
                binding.ivBearChar.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.2f))
                    .setDuration(220)
                    .start()
            }
            .start()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            handleRegister()
        }

        binding.tvLoginLink.setOnClickListener {
            finish()
        }
    }

    private fun handleRegister() {
        val name = binding.etName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Nama lengkap tidak boleh kosong", Toast.LENGTH_SHORT).show()
            binding.etName.requestFocus()
            return
        }

        if (username.isEmpty()) {
            Toast.makeText(this, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show()
            binding.etUsername.requestFocus()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            binding.etPassword.requestFocus()
            return
        }

        showLoading(true)

        val params = mutableMapOf(
            "name" to name,
            "username" to username,
            "password" to password,
        )
        if (email.isNotEmpty()) {
            params["email"] = email
        }

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@RegisterActivity)
                val response = apiService.register(params)

                showLoading(false)

                if (response.isSuccessful) {
                    val body = response.body()
                    Toast.makeText(
                        this@RegisterActivity,
                        body?.message ?: "Pendaftaran berhasil! Silakan masuk.",
                        Toast.LENGTH_LONG,
                    ).show()
                    finish()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                showLoading(false)
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
    }
}
