package com.example.planktondetectionapps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
        Log.d("HistoryActivity", "=== setupRecyclerView() called ===")

        historyAdapter = HistoryAdapter(
            context = this,
            historyList = currentHistoryList,
            onFeedbackClick = { entry -> showFeedbackDialog(entry) },
            onDeleteClick = { entry -> showDeleteConfirmation(entry) }
        )

        Log.d("HistoryActivity", "Created adapter with initial list size: ${currentHistoryList.size}")

        historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
            // Add these properties to ensure RecyclerView works properly
            setHasFixedSize(false)
            isNestedScrollingEnabled = true
        }

        Log.d("HistoryActivity", "RecyclerView setup complete")
        Log.d("HistoryActivity", "RecyclerView adapter set: ${historyRecyclerView.adapter != null}")
        Log.d("HistoryActivity", "RecyclerView layoutManager set: ${historyRecyclerView.layoutManager != null}")
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }

        exportButton.setOnClickListener {
            checkStoragePermissionAndExport()
        }

        clearAllButton.setOnClickListener {
            showClearAllConfirmation()
        }

        // Add debug button (long press on export button)
        exportButton.setOnLongClickListener {
            showDebugInfo()
            true
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

        // Temporarily disable listener to prevent auto-filtering during setup
        filterSpinner.onItemSelectedListener = null
        filterSpinner.setSelection(0) // Set to "Semua Riwayat" without triggering filter

        // Set listener after initial setup
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Only apply filter if this is a user selection, not initial setup
                if (parent != null) {
                    applyFilter(position)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadHistoryData() {
        Log.d("HistoryActivity", "=== loadHistoryData() called ===")
        try {
            Log.d("HistoryActivity", "Getting all history entries from HistoryManager...")
            val allEntries = historyManager.getAllHistoryEntries()
            Log.d("HistoryActivity", "Retrieved ${allEntries.size} entries from HistoryManager")

            // Debug: Log each entry received
            allEntries.forEachIndexed { index, entry ->
                Log.d("HistoryActivity", "Entry $index: ID=${entry.id}, Result=${entry.classificationResult}")
            }

            currentHistoryList.clear()
            Log.d("HistoryActivity", "Cleared currentHistoryList, size now: ${currentHistoryList.size}")

            currentHistoryList.addAll(allEntries)
            Log.d("HistoryActivity", "Added ${allEntries.size} entries to currentHistoryList")
            Log.d("HistoryActivity", "currentHistoryList final size: ${currentHistoryList.size}")

            // Log first few entries for debugging
            if (currentHistoryList.isNotEmpty()) {
                Log.d("HistoryActivity", "Sample entries in currentHistoryList:")
                currentHistoryList.take(3).forEachIndexed { index, entry ->
                    Log.d("HistoryActivity", "  Entry $index: ID=${entry.id}, Result=${entry.classificationResult}, Confidence=${entry.confidence}")
                }
            } else {
                Log.w("HistoryActivity", "currentHistoryList is empty after adding entries!")
                Log.w("HistoryActivity", "Original allEntries size was: ${allEntries.size}")
            }

            updateStatistics()
            updateUI()
            Log.d("HistoryActivity", "=== loadHistoryData() finished ===")
        } catch (e: Exception) {
            Log.e("HistoryActivity", "Error loading history: ${e.message}", e)
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
        Log.d("HistoryActivity", "=== updateUI() called ===")
        Log.d("HistoryActivity", "currentHistoryList.size: ${currentHistoryList.size}")

        if (currentHistoryList.isEmpty()) {
            Log.d("HistoryActivity", "Showing empty state layout")
            historyRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            Log.d("HistoryActivity", "Showing history recycler view with ${currentHistoryList.size} items")
            historyRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE

            // Check RecyclerView state
            Log.d("HistoryActivity", "RecyclerView visibility: ${historyRecyclerView.visibility}")
            Log.d("HistoryActivity", "RecyclerView width: ${historyRecyclerView.width}, height: ${historyRecyclerView.height}")
            Log.d("HistoryActivity", "RecyclerView layoutManager: ${historyRecyclerView.layoutManager}")
            Log.d("HistoryActivity", "RecyclerView adapter: ${historyRecyclerView.adapter}")

            // Recreate adapter with fresh data to ensure it works
            historyAdapter = HistoryAdapter(
                context = this,
                historyList = currentHistoryList.toMutableList(), // Create a new list
                onFeedbackClick = { entry -> showFeedbackDialog(entry) },
                onDeleteClick = { entry -> showDeleteConfirmation(entry) }
            )

            // Set the new adapter
            historyRecyclerView.adapter = historyAdapter

            Log.d("HistoryActivity", "New adapter created and set with ${historyAdapter.itemCount} items")

            // Force layout refresh
            historyRecyclerView.post {
                Log.d("HistoryActivity", "RecyclerView post-layout: width=${historyRecyclerView.width}, height=${historyRecyclerView.height}")
                Log.d("HistoryActivity", "RecyclerView child count: ${historyRecyclerView.childCount}")
                historyRecyclerView.requestLayout()
            }
        }
    }

    private fun applyFilter(filterType: Int) {
        Log.d("HistoryActivity", "=== applyFilter() called with filterType: $filterType ===")

        val allEntries = historyManager.getAllHistoryEntries()
        Log.d("HistoryActivity", "Got ${allEntries.size} entries from historyManager")

        val filteredEntries = when (filterType) {
            0 -> {
                Log.d("HistoryActivity", "Filter: Semua Riwayat")
                allEntries
            }
            1 -> {
                Log.d("HistoryActivity", "Filter: Dengan Feedback")
                val filtered = allEntries.filter { it.userFeedback.isNotEmpty() }
                Log.d("HistoryActivity", "Found ${filtered.size} entries with feedback")
                filtered
            }
            2 -> {
                Log.d("HistoryActivity", "Filter: Tanpa Feedback")
                val filtered = allEntries.filter { it.userFeedback.isEmpty() }
                Log.d("HistoryActivity", "Found ${filtered.size} entries without feedback")
                filtered
            }
            3 -> {
                Log.d("HistoryActivity", "Filter: Prediksi Benar")
                val filtered = allEntries.filter { it.isCorrect == true }
                Log.d("HistoryActivity", "Found ${filtered.size} entries with correct predictions")
                filtered
            }
            4 -> {
                Log.d("HistoryActivity", "Filter: Prediksi Salah")
                val filtered = allEntries.filter { it.isCorrect == false }
                Log.d("HistoryActivity", "Found ${filtered.size} entries with incorrect predictions")
                filtered
            }
            5 -> {
                Log.d("HistoryActivity", "Filter: Terbaru Dahulu")
                allEntries.sortedByDescending { it.timestamp }
            }
            6 -> {
                Log.d("HistoryActivity", "Filter: Terlama Dahulu")
                allEntries.sortedBy { it.timestamp }
            }
            else -> {
                Log.d("HistoryActivity", "Filter: Default (all entries)")
                allEntries
            }
        }

        Log.d("HistoryActivity", "Filtered result: ${filteredEntries.size} entries")

        currentHistoryList.clear()
        currentHistoryList.addAll(filteredEntries)

        Log.d("HistoryActivity", "Updated currentHistoryList size: ${currentHistoryList.size}")
        updateUI()
        Log.d("HistoryActivity", "=== applyFilter() finished ===")
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

    private fun showDebugInfo() {
        val debugInfo = StringBuilder()
        debugInfo.appendLine("=== DEBUG HISTORY INFO ===")
        debugInfo.appendLine("CSV File Path: ${historyManager.getCsvFilePath()}")
        debugInfo.appendLine("Total Entries: ${historyManager.getAllHistoryEntries().size}")
        debugInfo.appendLine("Current List Size: ${currentHistoryList.size}")
        debugInfo.appendLine("Selected Filter: ${filterSpinner.selectedItem}")

        val stats = historyManager.getFeedbackStats()
        debugInfo.appendLine("Entries with Feedback: ${stats.entriesWithFeedback}")
        debugInfo.appendLine("Accuracy: ${stats.accuracyPercentage.toInt()}%")

        // Add RecyclerView debug info
        debugInfo.appendLine("\n=== RECYCLERVIEW DEBUG ===")
        debugInfo.appendLine("RecyclerView visibility: ${historyRecyclerView.visibility}")
        debugInfo.appendLine("RecyclerView width: ${historyRecyclerView.width}")
        debugInfo.appendLine("RecyclerView height: ${historyRecyclerView.height}")
        debugInfo.appendLine("RecyclerView child count: ${historyRecyclerView.childCount}")
        debugInfo.appendLine("Adapter item count: ${historyRecyclerView.adapter?.itemCount ?: "null"}")
        debugInfo.appendLine("Layout Manager: ${historyRecyclerView.layoutManager?.javaClass?.simpleName}")

        // Add test result
        debugInfo.appendLine("\n=== SAVE/LOAD TEST ===")
        debugInfo.appendLine(historyManager.testSaveLoad())

        debugInfo.appendLine("\n=== RECENT DEBUG LOGS ===")
        val fullDebugInfo = historyManager.getDebugInfo()
        val recentLogs = fullDebugInfo.lines().takeLast(15).joinToString("\n")
        debugInfo.appendLine(recentLogs)

        AlertDialog.Builder(this)
            .setTitle("Debug Information")
            .setMessage(debugInfo.toString())
            .setPositiveButton("OK", null)
            .setNeutralButton("Full Logs") { _, _ ->
                showFullDebugLogs()
            }
            .setNegativeButton("Force Refresh") { _, _ ->
                // Force recreate everything
                loadHistoryData()
                Toast.makeText(this, "Forced refresh completed", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showFullDebugLogs() {
        val fullLogs = historyManager.getDebugInfo()
        AlertDialog.Builder(this)
            .setTitle("Full Debug Logs")
            .setMessage(fullLogs)
            .setPositiveButton("OK", null)
            .show()
    }
}
