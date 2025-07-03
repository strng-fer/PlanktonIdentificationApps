package com.example.planktondetectionapps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    private lateinit var backButton: ImageButton
    private lateinit var versionText: TextView
    private lateinit var developerContact: LinearLayout
    private lateinit var githubLink: LinearLayout
    private lateinit var emailContact: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        initializeViews()
        setupListeners()
        loadAppInfo()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        versionText = findViewById(R.id.versionText)
        developerContact = findViewById(R.id.developerContact)
        githubLink = findViewById(R.id.githubLink)
        emailContact = findViewById(R.id.emailContact)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        developerContact.setOnClickListener {
            openWebsite("https://github.com/strng-fer/PlanktonIdentificationApps/blob/main/README.md#contact")
        }

        githubLink.setOnClickListener {
            openWebsite("https://github.com/strng-fer/PlanktonIdentificationApps")
        }

        emailContact.setOnClickListener {
            sendEmail()
        }
    }

    private fun loadAppInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = "Versi ${packageInfo.versionName}"
        } catch (e: Exception) {
            versionText.text = "Versi 1.0.0"
        }
    }

    private fun openWebsite(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:developer@planktondetection.com")
            putExtra(Intent.EXTRA_SUBJECT, "Plankton Detection App - Feedback")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }
}
