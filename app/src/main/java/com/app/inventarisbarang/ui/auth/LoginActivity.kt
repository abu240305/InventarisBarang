package com.app.inventarisbarang.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ActivityLoginBinding
import com.app.inventarisbarang.model.User
import com.app.inventarisbarang.network.ApiConfig
import com.app.inventarisbarang.ui.dashboard.DashboardActivity
import com.app.inventarisbarang.utils.ErrorUtils
import com.app.inventarisbarang.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private var isCoveringEyes = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(this)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }

        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setupBearInteractions()
        setupListeners()
    }

    private fun setupBearInteractions() {
        // When password field gains focus, bear covers its eyes!
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                setBearState(coverEyes = true)
            } else {
                setBearState(coverEyes = false)
            }
        }

        binding.etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                setBearState(coverEyes = false)
            }
        }
    }

    private fun setBearState(coverEyes: Boolean) {
        if (isCoveringEyes == coverEyes) return
        isCoveringEyes = coverEyes

        // Smooth 3D pop & bounce switch animation
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
        binding.btnLogin.setOnClickListener {
            handleLogin()
        }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "Username atau email tidak boleh kosong", Toast.LENGTH_SHORT).show()
            binding.etUsername.requestFocus()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            binding.etPassword.requestFocus()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@LoginActivity)
                val response = apiService.login(
                    mapOf(
                        "username" to username,
                        "password" to password,
                    ),
                )

                showLoading(false)

                if (response.isSuccessful) {
                    val body = response.body()
                    val loginData = body?.data
                    val token = loginData?.token ?: ""
                    val user = loginData?.user ?: User(username = username, name = username)

                    sessionManager.saveLoginSession(token, user)
                    Toast.makeText(
                        this@LoginActivity,
                        body?.message ?: "Login berhasil! Selamat datang.",
                        Toast.LENGTH_SHORT,
                    ).show()

                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                } else {
                    val errorMsg = ErrorUtils.parseError(response)
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                showLoading(false)
                val errorMsg = ErrorUtils.parseThrowable(t)
                Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }
}
