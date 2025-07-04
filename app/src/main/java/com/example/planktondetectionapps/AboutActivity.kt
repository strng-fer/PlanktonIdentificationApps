package com.example.planktondetectionapps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity untuk menampilkan informasi tentang aplikasi
 * Menampilkan detail aplikasi, fitur, dan kontak developer
 */
class AboutActivity : AppCompatActivity() {

    // UI Components
    private lateinit var backButton: ImageButton
    private lateinit var versionText: TextView
    private lateinit var developerContact: LinearLayout
    private lateinit var githubLink: LinearLayout
    private lateinit var emailContact: LinearLayout

    /**
     * Inisialisasi activity dan setup UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        initializeViews()
        setupListeners()
        loadAppInfo()
    }

    /**
     * Inisialisasi semua view components
     */
    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        versionText = findViewById(R.id.versionText)
        developerContact = findViewById(R.id.developerContact)
        githubLink = findViewById(R.id.githubLink)
        emailContact = findViewById(R.id.emailContact)
    }

    /**
     * Setup listener untuk UI interactions
     */
    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        developerContact.setOnClickListener {
            openWebsite(DEVELOPER_CONTACT_URL)
        }

        githubLink.setOnClickListener {
            openWebsite(GITHUB_REPO_URL)
        }

        emailContact.setOnClickListener {
            sendEmail()
        }
    }

    /**
     * Load informasi aplikasi dari PackageManager
     */
    private fun loadAppInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = "Versi ${packageInfo.versionName}"
        } catch (e: Exception) {
            versionText.text = "Versi 1.0.0"
        }
    }

    /**
     * Buka website di browser
     */
    private fun openWebsite(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    /**
     * Buka aplikasi email untuk mengirim feedback
     */
    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$DEVELOPER_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "Plankton Detection App - Feedback")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    companion object {
        private const val DEVELOPER_CONTACT_URL = "https://github.com/strng-fer/PlanktonIdentificationApps?tab=readme-ov-file#-contact"
        private const val GITHUB_REPO_URL = "https://github.com/strng-fer/PlanktonIdentificationApps"
        private const val DEVELOPER_EMAIL = "developer@planktondetection.com"
    }
}
