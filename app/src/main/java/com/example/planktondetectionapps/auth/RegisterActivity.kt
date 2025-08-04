package com.example.planktondetectionapps.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.planktondetectionapps.MainActivity
import com.example.planktondetectionapps.R
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Registration activity for new user accounts
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var etDisplayName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var rgRole: RadioGroup
    private lateinit var rbGuest: RadioButton
    private lateinit var rbExpert: RadioButton
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etDisplayName = findViewById(R.id.etDisplayName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        rgRole = findViewById(R.id.rgRole)
        rbGuest = findViewById(R.id.rbGuest)
        rbExpert = findViewById(R.id.rbExpert)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            registerUser()
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val displayName = etDisplayName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Validation
        if (displayName.isEmpty()) {
            etDisplayName.error = "Full name is required"
            return
        }

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            return
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            return
        }

        val selectedRole = when (rgRole.checkedRadioButtonId) {
            R.id.rbExpert -> UserRole.EXPERT
            R.id.rbGuest -> UserRole.BASIC  // Changed: rbGuest now maps to BASIC role
            else -> UserRole.BASIC // Default to BASIC instead of GUEST
        }

        showLoading(true)

        lifecycleScope.launch {
            val result = com.example.planktondetectionapps.auth.AuthManager.getInstance().register(
                email = email,
                password = password,
                displayName = displayName,
                role = selectedRole
            )

            showLoading(false)

            result.fold(
                onSuccess = { user: com.example.planktondetectionapps.auth.AppUser ->
                    Toast.makeText(this@RegisterActivity,
                        "Account created successfully! Welcome, ${user.displayName}!",
                        Toast.LENGTH_LONG).show()
                    navigateToMainActivity()
                },
                onFailure = { exception: Throwable ->
                    Toast.makeText(this@RegisterActivity,
                        "Registration failed: ${exception.message}",
                        Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !isLoading
        tvBackToLogin.isEnabled = !isLoading

        // Disable form fields
        etDisplayName.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading
        etPassword.isEnabled = !isLoading
        etConfirmPassword.isEnabled = !isLoading
        rgRole.isEnabled = !isLoading
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
