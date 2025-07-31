package com.example.planktondetectionapps.admin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.planktondetectionapps.R
import com.example.planktondetectionapps.auth.AuthManager
import com.example.planktondetectionapps.auth.UserRole
import com.example.planktondetectionapps.database.DatabaseService
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity khusus untuk admin yang memungkinkan mengunduh semua log dari database
 */
class AdminActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var btnDownloadAllLogs: Button
    private lateinit var btnRefreshStats: Button
    private lateinit var btnViewAllUsers: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvDownloadStatus: TextView
    private lateinit var tvStatistics: TextView

    private val authManager = AuthManager.getInstance()
    private val databaseService = DatabaseService.getInstance()

    companion object {
        private const val TAG = "AdminActivity"
        private const val STORAGE_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Cek apakah user adalah admin
        val currentUser = authManager.getCurrentUser()
        if (currentUser?.role != UserRole.ADMIN) {
            Toast.makeText(this, "Access denied. Admin privileges required.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initViews()
        setupUI()
        loadStatistics()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        btnDownloadAllLogs = findViewById(R.id.btnDownloadAllLogs)
        btnRefreshStats = findViewById(R.id.btnRefreshStats)
        btnViewAllUsers = findViewById(R.id.btnViewAllUsers)
        progressBar = findViewById(R.id.progressBar)
        tvDownloadStatus = findViewById(R.id.tvDownloadStatus)
        tvStatistics = findViewById(R.id.tvStatistics)
    }

    private fun setupUI() {
        toolbar.title = "Admin Panel"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btnDownloadAllLogs.setOnClickListener {
            checkPermissionAndDownload()
        }

        btnRefreshStats.setOnClickListener {
            loadStatistics()
        }

        btnViewAllUsers.setOnClickListener {
            // TODO: Implement user management functionality
            Toast.makeText(this@AdminActivity, "User management coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionAndDownload() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
            downloadAllLogs()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadAllLogs()
            } else {
                Toast.makeText(this, "Storage permission required to download logs", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun downloadAllLogs() {
        btnDownloadAllLogs.isEnabled = false
        progressBar.visibility = View.VISIBLE
        tvDownloadStatus.text = "Downloading all classification logs..."

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting download of all classification logs")

                val result = databaseService.exportAllClassificationsToCSV()

                if (result.isSuccess) {
                    val csvContent = result.getOrNull()
                    if (csvContent != null) {
                        saveCSVToDownloads(csvContent)
                    } else {
                        showError("No data available to export")
                    }
                } else {
                    showError("Failed to export data: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading logs", e)
                showError("Error downloading logs: ${e.message}")
            } finally {
                btnDownloadAllLogs.isEnabled = true
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun saveCSVToDownloads(csvContent: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "plankton_all_classifications_$timestamp.csv"
            val file = File(downloadsDir, fileName)

            FileWriter(file).use { writer ->
                writer.write(csvContent)
            }

            tvDownloadStatus.text = "✅ Downloaded successfully to: ${file.absolutePath}"
            Toast.makeText(this, "File saved to Downloads: $fileName", Toast.LENGTH_LONG).show()

            Log.d(TAG, "CSV file saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving CSV file", e)
            showError("Error saving file: ${e.message}")
        }
    }

    private fun loadStatistics() {
        progressBar.visibility = View.VISIBLE
        tvStatistics.text = "Loading statistics..."

        lifecycleScope.launch {
            try {
                val result = databaseService.getAllClassificationsFromDatabase()

                if (result.isSuccess) {
                    val classifications = result.getOrNull() ?: emptyList()
                    displayStatistics(classifications)
                } else {
                    showError("Failed to load statistics: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading statistics", e)
                showError("Error loading statistics: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayStatistics(classifications: List<Map<String, Any>>) {
        val totalClassifications = classifications.size
        val guestClassifications = classifications.count { (it["userRole"] as? String) == "Guest" }
        val expertClassifications = classifications.count { (it["userRole"] as? String) == "Expert" }
        val adminClassifications = classifications.count { (it["userRole"] as? String) == "Admin" }
        val withFeedback = classifications.count { !(it["userFeedback"] as? String).isNullOrBlank() }
        val correctPredictions = classifications.count { it["isCorrect"] == true }
        val incorrectPredictions = classifications.count { it["isCorrect"] == false }

        val statisticsText = buildString {
            appendLine("📊 DATABASE STATISTICS")
            appendLine("═══════════════════════")
            appendLine("Total Classifications: $totalClassifications")
            appendLine("")
            appendLine("🔹 By User Role:")
            appendLine("   Guest: $guestClassifications")
            appendLine("   Expert: $expertClassifications")
            appendLine("   Admin: $adminClassifications")
            appendLine("")
            appendLine("🔹 Feedback Status:")
            appendLine("   With Feedback: $withFeedback")
            appendLine("   Without Feedback: ${totalClassifications - withFeedback}")
            if (withFeedback > 0) {
                val feedbackPercentage = (withFeedback * 100f) / totalClassifications
                appendLine("   Feedback Rate: ${String.format(Locale.getDefault(), "%.1f", feedbackPercentage)}%")
            }
            appendLine("")
            appendLine("🔹 Accuracy (from feedback):")
            appendLine("   Correct: $correctPredictions")
            appendLine("   Incorrect: $incorrectPredictions")
            if (withFeedback > 0) {
                val accuracyPercentage = (correctPredictions * 100f) / withFeedback
                appendLine("   Accuracy: ${String.format(Locale.getDefault(), "%.1f", accuracyPercentage)}%")
            }
        }

        tvStatistics.text = statisticsText
    }

    private fun showError(message: String) {
        tvDownloadStatus.text = "❌ Error: $message"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, message)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
