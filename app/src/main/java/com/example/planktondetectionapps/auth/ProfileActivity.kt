package com.example.planktondetectionapps.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.planktondetectionapps.R
import com.example.planktondetectionapps.admin.AdminActivity
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Profile activity for displaying user information and managing account
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var tvDisplayName: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var tvLastLogin: TextView
    private lateinit var llUploadPermission: LinearLayout
    private lateinit var llFeedbackPermission: LinearLayout
    private lateinit var llAdminPermission: LinearLayout
    private lateinit var ivUploadIcon: ImageView
    private lateinit var ivFeedbackIcon: ImageView
    private lateinit var ivAdminIcon: ImageView
    private lateinit var llAdminSection: LinearLayout
    private lateinit var btnManageUsers: Button
    private lateinit var btnSignOut: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        setupListeners()
        updateUI()
    }

    private fun initViews() {
        tvDisplayName = findViewById(R.id.tvDisplayName)
        tvUserRole = findViewById(R.id.tvUserRole)
        tvEmail = findViewById(R.id.tvEmail)
        tvMemberSince = findViewById(R.id.tvMemberSince)
        tvLastLogin = findViewById(R.id.tvLastLogin)
        llUploadPermission = findViewById(R.id.llUploadPermission)
        llFeedbackPermission = findViewById(R.id.llFeedbackPermission)
        llAdminPermission = findViewById(R.id.llAdminPermission)
        ivUploadIcon = findViewById(R.id.ivUploadIcon)
        ivFeedbackIcon = findViewById(R.id.ivFeedbackIcon)
        ivAdminIcon = findViewById(R.id.ivAdminIcon)
        llAdminSection = findViewById(R.id.llAdminSection)
        btnManageUsers = findViewById(R.id.btnManageUsers)
        btnSignOut = findViewById(R.id.btnSignOut)
    }

    private fun setupListeners() {
        btnSignOut.setOnClickListener {
            signOut()
        }

        btnManageUsers.setOnClickListener {
            // Launch AdminActivity
            val intent = Intent(this, AdminActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateUI() {
        val currentUser = com.example.planktondetectionapps.auth.AuthManager.getInstance().getCurrentUser()

        if (currentUser == null) {
            // User not logged in, redirect to login
            navigateToLogin()
            return
        }

        // Update user information
        tvDisplayName.text = if (currentUser.displayName?.isEmpty() != false) "User" else currentUser.displayName
        tvEmail.text = currentUser.email ?: "No email"

        val userRole = currentUser.role
        tvUserRole.text = userRole.roleName.uppercase()

        // Format dates
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        tvMemberSince.text = dateFormat.format(currentUser.createdAt.toDate())
        tvLastLogin.text = dateFormat.format(currentUser.lastLoginAt.toDate())

        // Update permissions display
        updatePermissionIcons(userRole)

        // Show admin section if user is admin
        llAdminSection.visibility = if (userRole.isAdmin()) View.VISIBLE else View.GONE
    }

    private fun updatePermissionIcons(userRole: UserRole) {
        // Classification permission - all users can classify
        val classifyEnabled = userRole.canClassify()
        ivUploadIcon.setColorFilter(
            ContextCompat.getColor(this,
                if (classifyEnabled) android.R.color.holo_green_dark else android.R.color.darker_gray
            )
        )
        llUploadPermission.alpha = if (classifyEnabled) 1.0f else 0.5f

        // Feedback permission - expert and admin
        val feedbackEnabled = userRole.canProvideFeedback()
        ivFeedbackIcon.setColorFilter(
            ContextCompat.getColor(this,
                if (feedbackEnabled) android.R.color.holo_blue_dark else android.R.color.darker_gray
            )
        )
        llFeedbackPermission.alpha = if (feedbackEnabled) 1.0f else 0.5f

        // Admin permission - only admin
        val adminEnabled = userRole.isAdmin()
        ivAdminIcon.setColorFilter(
            ContextCompat.getColor(this,
                if (adminEnabled) android.R.color.holo_red_dark else android.R.color.darker_gray
            )
        )
        llAdminPermission.alpha = if (adminEnabled) 1.0f else 0.5f
    }

    private fun signOut() {
        com.example.planktondetectionapps.auth.AuthManager.getInstance().signOut()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
