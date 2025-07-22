package com.example.planktondetectionapps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Activity untuk menampilkan riwayat klasifikasi plankton
 */
class HistoryActivity : AppCompatActivity() {

    // UI Components
    private lateinit var backButton: ImageButton
    private lateinit var exportButton: ImageButton
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var totalClassificationsText: TextView
    private lateinit var feedbackCountText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var filterSpinner: Spinner
    private lateinit var clearAllButton: Button

    // Data Management
    private lateinit var historyManager: HistoryManager
    private lateinit var historyAdapter: HistoryAdapter
    private var currentHistoryList = mutableListOf<HistoryEntry>()

    // Permission launcher
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            exportCsvFile()
        } else {
            Toast.makeText(this, "Permission required to export file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContentView(R.layout.activity_history)

        try {
            initializeViews()
            setupRecyclerView()
            setupListeners()
            setupFilterSpinner()

            historyManager = HistoryManager(this)
            loadHistoryData()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing history: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        exportButton = findViewById(R.id.exportButton)
        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        totalClassificationsText = findViewById(R.id.totalClassificationsText)
        feedbackCountText = findViewById(R.id.feedbackCountText)
        accuracyText = findViewById(R.id.accuracyText)
        filterSpinner = findViewById(R.id.filterSpinner)
        clearAllButton = findViewById(R.id.clearAllButton)
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            context = this,
            historyList = currentHistoryList,
            onFeedbackClick = { entry -> showFeedbackDialog(entry) },
            onDeleteClick = { entry -> showDeleteConfirmation(entry) }
        )

        historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }

        exportButton.setOnClickListener {
            checkStoragePermissionAndExport()
        }

        clearAllButton.setOnClickListener {
            showClearAllConfirmation()
        }
    }

    private fun setupFilterSpinner() {
        val filterOptions = arrayOf(
            "Semua Riwayat",
            "Dengan Feedback",
            "Tanpa Feedback",
            "Prediksi Benar",
            "Prediksi Salah",
            "Terbaru Dahulu",
            "Terlama Dahulu"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = adapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilter(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadHistoryData() {
        try {
            val allEntries = historyManager.getAllHistoryEntries()
            currentHistoryList.clear()
            currentHistoryList.addAll(allEntries)

            updateStatistics()
            updateUI()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading history: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatistics() {
        val stats = historyManager.getFeedbackStats()

        totalClassificationsText.text = stats.totalEntries.toString()
        feedbackCountText.text = stats.entriesWithFeedback.toString()
        accuracyText.text = "${stats.accuracyPercentage.toInt()}%"
    }

    private fun updateUI() {
        if (currentHistoryList.isEmpty()) {
            historyRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            historyRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            historyAdapter.updateData(currentHistoryList)
        }
    }

    private fun applyFilter(filterType: Int) {
        val allEntries = historyManager.getAllHistoryEntries()
        val filteredEntries = when (filterType) {
            0 -> allEntries // Semua Riwayat
            1 -> allEntries.filter { it.userFeedback.isNotEmpty() } // Dengan Feedback
            2 -> allEntries.filter { it.userFeedback.isEmpty() } // Tanpa Feedback
            3 -> allEntries.filter { it.isCorrect == true } // Prediksi Benar
            4 -> allEntries.filter { it.isCorrect == false } // Prediksi Salah
            5 -> allEntries.sortedByDescending { it.timestamp } // Terbaru Dahulu
            6 -> allEntries.sortedBy { it.timestamp } // Terlama Dahulu
            else -> allEntries
        }

        currentHistoryList.clear()
        currentHistoryList.addAll(filteredEntries)
        updateUI()
    }

    private fun showFeedbackDialog(entry: HistoryEntry) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback, null)

        // Use existing UI elements that we know exist
        val feedbackComment = dialogView.findViewById<EditText>(R.id.feedbackComment)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        // Set current prediction info
        val currentPrediction = dialogView.findViewById<TextView>(R.id.currentPrediction)
        val currentConfidence = dialogView.findViewById<TextView>(R.id.currentConfidence)

        currentPrediction?.text = entry.classificationResult
        currentConfidence?.text = "Tingkat Kepercayaan: ${(entry.confidence * 100).toInt()}%"

        // Pre-fill existing feedback if any
        feedbackComment?.setText(entry.userFeedback)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Feedback untuk Klasifikasi")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set button listeners
        submitButton?.setOnClickListener {
            val feedback = feedbackComment?.text?.toString()?.trim() ?: ""
            saveFeedback(entry, feedback, null, "")
            dialog.dismiss()
        }

        cancelButton?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveFeedback(entry: HistoryEntry, feedback: String, isCorrect: Boolean?, correctClass: String) {
        if (historyManager.updateEntryFeedback(entry.id, feedback, isCorrect, correctClass)) {
            Toast.makeText(this, "Feedback berhasil disimpan", Toast.LENGTH_SHORT).show()
            loadHistoryData()
        } else {
            Toast.makeText(this, "Gagal menyimpan feedback", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation(entry: HistoryEntry) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Riwayat")
            .setMessage("Apakah Anda yakin ingin menghapus riwayat ini?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteHistoryEntry(entry)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteHistoryEntry(entry: HistoryEntry) {
        if (historyManager.deleteEntry(entry.id)) {
            historyAdapter.removeItem(entry)
            updateStatistics()
            Toast.makeText(this, "Riwayat berhasil dihapus", Toast.LENGTH_SHORT).show()

            // Update UI if list becomes empty
            if (currentHistoryList.isEmpty()) {
                updateUI()
            }
        } else {
            Toast.makeText(this, "Gagal menghapus riwayat", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showClearAllConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Semua Riwayat")
            .setMessage("Apakah Anda yakin ingin menghapus semua riwayat? Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus Semua") { _, _ ->
                clearAllHistory()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun clearAllHistory() {
        try {
            currentHistoryList.forEach { entry ->
                historyManager.deleteEntry(entry.id)
            }
            currentHistoryList.clear()
            historyAdapter.updateData(emptyList())
            updateStatistics()
            updateUI()
            Toast.makeText(this, "Semua riwayat berhasil dihapus", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menghapus riwayat: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkStoragePermissionAndExport() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                exportCsvFile()
            }
            else -> {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun exportCsvFile() {
        try {
            val exportedFile = historyManager.exportCsvToDownloads()
            if (exportedFile != null) {
                Toast.makeText(
                    this,
                    "File CSV berhasil diekspor ke: ${exportedFile.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "Gagal mengekspor file CSV", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
