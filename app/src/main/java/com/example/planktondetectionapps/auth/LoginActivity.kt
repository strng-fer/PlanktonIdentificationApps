package com.example.planktondetectionapps.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.planktondetectionapps.MainActivity
import com.example.planktondetectionapps.R
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Login activity for user authentication
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSignIn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvRegister: TextView
    private lateinit var tvGuestAccess: TextView

    private val authManager = AuthManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupListeners()

        // Check if user is already authenticated
        if (authManager.isAuthenticated()) {
            navigateToMainActivity()
        }
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        progressBar = findViewById(R.id.progressBar)
        tvRegister = findViewById(R.id.tvRegister)
        tvGuestAccess = findViewById(R.id.tvGuestAccess)
    }

    private fun setupListeners() {
        btnSignIn.setOnClickListener {
            signIn()
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvGuestAccess.setOnClickListener {
            continueAsGuest()
        }
    }

    private fun signIn() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            val result = authManager.signIn(email, password)

            showLoading(false)

            result.fold(
                onSuccess = { user: AppUser ->
                    Toast.makeText(this@LoginActivity,
                        "Welcome back, ${user.displayName}! Role: ${user.getUserRole().roleName}",
                        Toast.LENGTH_LONG).show()
                    navigateToMainActivity()
                },
                onFailure = { exception: Throwable ->
                    Toast.makeText(this@LoginActivity,
                        "Sign in failed: ${exception.message}",
                        Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun continueAsGuest() {
        lifecycleScope.launch {
            // Create anonymous guest account
            val guestEmail = "guest_${System.currentTimeMillis()}@temp.com"
            val guestPassword = "temppassword123"

            showLoading(true)

            val result = authManager.register(
                email = guestEmail,
                password = guestPassword,
                displayName = "Guest User",
                role = UserRole.GUEST
            )

            showLoading(false)

            result.fold(
                onSuccess = { user ->
                    Toast.makeText(this@LoginActivity,
                        "Welcome, Guest! You can upload images for detection.",
                        Toast.LENGTH_LONG).show()
                    navigateToMainActivity()
                },
                onFailure = { exception ->
                    Toast.makeText(this@LoginActivity,
                        "Failed to create guest account: ${exception.message}",
                        Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSignIn.isEnabled = !isLoading
        tvRegister.isEnabled = !isLoading
        tvGuestAccess.isEnabled = !isLoading
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
