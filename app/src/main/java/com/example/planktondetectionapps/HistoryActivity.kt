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
            onDeleteClick = { entry -> showDeleteConfirmation(entry) },
            onItemClick = { entry -> showClassificationDetailsDialog(entry) } // Add detailed view handler
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
                onDeleteClick = { entry -> showDeleteConfirmation(entry) },
                onItemClick = { entry -> showClassificationDetailsDialog(entry) } // Add detailed view handler
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

        // Get UI elements from dialog
        val feedbackComment = dialogView.findViewById<EditText>(R.id.feedbackComment)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val feedbackRadioGroup = dialogView.findViewById<RadioGroup>(R.id.feedbackRadioGroup)
        val correctRadio = dialogView.findViewById<RadioButton>(R.id.correctRadio)
        val incorrectRadio = dialogView.findViewById<RadioButton>(R.id.incorrectRadio)
        val neutralRadio = dialogView.findViewById<RadioButton>(R.id.neutralRadio)
        val correctClassSpinner = dialogView.findViewById<Spinner>(R.id.correctClassSpinner)
        val correctClassLabel = dialogView.findViewById<TextView>(R.id.correctClassLabel)

        // Set current prediction info
        val currentPrediction = dialogView.findViewById<TextView>(R.id.currentPrediction)
        val currentConfidence = dialogView.findViewById<TextView>(R.id.currentConfidence)

        currentPrediction?.text = entry.classificationResult
        currentConfidence?.text = "Tingkat Kepercayaan: ${(entry.confidence * 100).toInt()}%"

        // Load plankton labels and add "Unrecognize" option
        val planktonLabels = loadLabels(this).toMutableList()
        planktonLabels.add("Unrecognize")

        // Setup spinner with plankton labels
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            planktonLabels
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        correctClassSpinner?.adapter = spinnerAdapter

        // Pre-fill existing feedback if any
        feedbackComment?.setText(entry.userFeedback)

        // Pre-select radio button based on existing feedback
        when (entry.isCorrect) {
            true -> correctRadio?.isChecked = true
            false -> {
                incorrectRadio?.isChecked = true
                correctClassLabel?.visibility = View.VISIBLE
                correctClassSpinner?.visibility = View.VISIBLE
                // Pre-select the correct class if it exists in the spinner
                val correctClassIndex = planktonLabels.indexOf(entry.correctClass)
                if (correctClassIndex >= 0) {
                    correctClassSpinner?.setSelection(correctClassIndex)
                }
            }
            null -> neutralRadio?.isChecked = true
        }

        // Show/hide correct class label and spinner based on radio selection
        feedbackRadioGroup?.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.incorrectRadio -> {
                    correctClassLabel?.visibility = View.VISIBLE
                    correctClassSpinner?.visibility = View.VISIBLE
                }
                else -> {
                    correctClassLabel?.visibility = View.GONE
                    correctClassSpinner?.visibility = View.GONE
                }
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Make dialog background transparent to prevent overlap with custom rounded background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set button listeners
        submitButton?.setOnClickListener {
            val feedback = feedbackComment?.text?.toString()?.trim() ?: ""

            // Determine correctness based on radio selection
            val isCorrect = when (feedbackRadioGroup?.checkedRadioButtonId) {
                R.id.correctRadio -> true
                R.id.incorrectRadio -> false
                else -> null // neutral or no selection
            }

            // Get correct class if prediction is marked as incorrect
            val correctClass = if (isCorrect == false) {
                // Get selected item from spinner
                val selectedItem = correctClassSpinner?.selectedItem?.toString()?.trim()
                selectedItem ?: ""
            } else {
                ""
            }

            saveFeedback(entry, feedback, isCorrect, correctClass)
            dialog.dismiss()
        }

        cancelButton?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveFeedback(entry: HistoryEntry, feedback: String, isCorrect: Boolean?, correctClass: String) {
        Log.d("HistoryActivity", "=== saveFeedback() called ===")
        Log.d("HistoryActivity", "Entry ID: ${entry.id}")
        Log.d("HistoryActivity", "Feedback: '$feedback'")
        Log.d("HistoryActivity", "IsCorrect: $isCorrect")
        Log.d("HistoryActivity", "CorrectClass: '$correctClass'")

        if (historyManager.updateEntryFeedback(entry.id, feedback, isCorrect, correctClass)) {
            Toast.makeText(this, "Feedback berhasil disimpan", Toast.LENGTH_SHORT).show()
            Log.d("HistoryActivity", "Feedback saved successfully")
            loadHistoryData()
        } else {
            Toast.makeText(this, "Gagal menyimpan feedback", Toast.LENGTH_SHORT).show()
            Log.e("HistoryActivity", "Failed to save feedback")
        }
    }

    /**
     * Load labels from assets file
     */
    private fun loadLabels(context: android.content.Context): List<String> {
        val labels = mutableListOf<String>()
        try {
            context.assets.open("labels.txt").bufferedReader().useLines { lines ->
                lines.forEach { labels.add(it) }
            }
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            labels.add("Unknown")
        }
        return labels
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

        // Add feedback debug info
        debugInfo.appendLine("\n=== FEEDBACK DEBUG ===")
        val entriesWithIncorrectPrediction = historyManager.getAllHistoryEntries().filter {
            it.isCorrect == false && it.correctClass.isNotEmpty()
        }
        debugInfo.appendLine("Entries with incorrect predictions and correct class: ${entriesWithIncorrectPrediction.size}")
        entriesWithIncorrectPrediction.take(3).forEach { entry ->
            debugInfo.appendLine("  Entry ${entry.id}: predicted='${entry.classificationResult}', actual='${entry.correctClass}'")
        }

        // Add sample of all entries to see their feedback status
        debugInfo.appendLine("\n=== ALL ENTRIES SAMPLE ===")
        val allEntries = historyManager.getAllHistoryEntries().take(5)
        allEntries.forEach { entry ->
            debugInfo.appendLine("Entry ${entry.id}: feedback='${entry.userFeedback}', isCorrect=${entry.isCorrect}, correctClass='${entry.correctClass}'")
        }

        // Add RecyclerView debug info
        debugInfo.appendLine("\n=== RECYCLERVIEW DEBUG ===")
        debugInfo.appendLine("RecyclerView visibility: ${historyRecyclerView.visibility}")
        debugInfo.appendLine("RecyclerView width: ${historyRecyclerView.width}")
        debugInfo.appendLine("RecyclerView height: ${historyRecyclerView.height}")
        debugInfo.appendLine("RecyclerView child count: ${historyRecyclerView.childCount}")
        debugInfo.appendLine("Adapter item count: ${historyRecyclerView.adapter?.itemCount ?: "null"}")
        debugInfo.appendLine("Layout Manager: ${historyRecyclerView.layoutManager?.javaClass?.simpleName}")

        AlertDialog.Builder(this)
            .setTitle("Debug Information")
            .setMessage(debugInfo.toString())
            .setPositiveButton("OK", null)
            .setNeutralButton("Test Feedback") { _, _ ->
                testFeedbackDataIntegrity()
            }
            .setNegativeButton("Create Sample Data") { _, _ ->
                createSampleIncorrectFeedback()
            }
            .show()
    }

    private fun createTestFeedbackData() {
        // Find the first entry without feedback to add test data
        val entries = historyManager.getAllHistoryEntries()
        val entryToUpdate = entries.firstOrNull { it.userFeedback.isEmpty() }

        if (entryToUpdate != null) {
            val testFeedback = "Test feedback: Prediksi tidak sesuai dengan gambar yang diberikan."
            val testCorrectClass = "Diatom" // Test correct class

            Log.d("HistoryActivity", "Creating test feedback data for entry ${entryToUpdate.id}")
            Log.d("HistoryActivity", "Test data: isCorrect=false, correctClass='$testCorrectClass'")

            if (historyManager.updateEntryFeedback(entryToUpdate.id, testFeedback, false, testCorrectClass)) {
                Toast.makeText(this, "Test feedback data created successfully", Toast.LENGTH_SHORT).show()
                loadHistoryData()
            } else {
                Toast.makeText(this, "Failed to create test data", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No entries available for test data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testFeedbackDataIntegrity() {
        Log.d("HistoryActivity", "=== Testing Feedback Data Integrity ===")

        val entries = historyManager.getAllHistoryEntries()
        Log.d("HistoryActivity", "Total entries: ${entries.size}")

        // Count entries with different feedback states
        val withFeedback = entries.filter { it.userFeedback.isNotEmpty() }
        val correctPredictions = entries.filter { it.isCorrect == true }
        val incorrectPredictions = entries.filter { it.isCorrect == false }
        val incorrectWithCorrectClass = entries.filter { it.isCorrect == false && it.correctClass.isNotEmpty() }

        Log.d("HistoryActivity", "Entries with feedback: ${withFeedback.size}")
        Log.d("HistoryActivity", "Correct predictions: ${correctPredictions.size}")
        Log.d("HistoryActivity", "Incorrect predictions: ${incorrectPredictions.size}")
        Log.d("HistoryActivity", "Incorrect with correct class: ${incorrectWithCorrectClass.size}")

        // Log details of incorrect predictions with correct class
        incorrectWithCorrectClass.forEach { entry ->
            Log.d("HistoryActivity", "Entry ${entry.id}: predicted='${entry.classificationResult}', actual='${entry.correctClass}', feedback='${entry.userFeedback}'")
        }

        val testInfo = StringBuilder()
        testInfo.appendLine("=== FEEDBACK DATA INTEGRITY TEST ===")
        testInfo.appendLine("Total entries: ${entries.size}")
        testInfo.appendLine("With feedback: ${withFeedback.size}")
        testInfo.appendLine("Correct predictions: ${correctPredictions.size}")
        testInfo.appendLine("Incorrect predictions: ${incorrectPredictions.size}")
        testInfo.appendLine("Incorrect with correct class: ${incorrectWithCorrectClass.size}")
        testInfo.appendLine("")

        if (incorrectWithCorrectClass.isNotEmpty()) {
            testInfo.appendLine("Entries that should show actual classification:")
            incorrectWithCorrectClass.take(5).forEach { entry ->
                testInfo.appendLine("• ${entry.id}: ${entry.classificationResult} → ${entry.correctClass}")
            }
        } else {
            testInfo.appendLine("❌ NO ENTRIES with incorrect predictions and correct class found!")
            testInfo.appendLine("This is why actual classification is not showing.")
        }

        AlertDialog.Builder(this)
            .setTitle("Feedback Data Integrity Test")
            .setMessage(testInfo.toString())
            .setPositiveButton("OK", null)
            .setNeutralButton("Create Sample") { _, _ ->
                createSampleIncorrectFeedback()
            }
            .show()
    }

    private fun createSampleIncorrectFeedback() {
        val entries = historyManager.getAllHistoryEntries()
        if (entries.isNotEmpty()) {
            val entryToUpdate = entries.first()

            Log.d("HistoryActivity", "Creating sample incorrect feedback for entry: ${entryToUpdate.id}")
            Log.d("HistoryActivity", "Original prediction: ${entryToUpdate.classificationResult}")

            val sampleFeedback = "Sample: Gambar ini sebenarnya adalah Diatom, bukan ${entryToUpdate.classificationResult}"
            val sampleCorrectClass = "Diatom"

            // Force save the feedback
            val success = historyManager.updateEntryFeedback(
                entryId = entryToUpdate.id,
                feedback = sampleFeedback,
                isCorrect = false,
                correctClass = sampleCorrectClass
            )

            Log.d("HistoryActivity", "Sample feedback save result: $success")

            if (success) {
                Toast.makeText(this, "Sample incorrect feedback created", Toast.LENGTH_SHORT).show()
                // Apply filter to show only incorrect predictions
                filterSpinner.setSelection(4) // "Prediksi Salah"
                loadHistoryData()
            } else {
                Toast.makeText(this, "Failed to create sample feedback", Toast.LENGTH_SHORT).show()

                // Show debug info from HistoryManager
                val debugInfo = historyManager.getDebugInfo()
                AlertDialog.Builder(this)
                    .setTitle("Debug Info - Why Save Failed")
                    .setMessage(debugInfo.takeLast(2000)) // Show last 2000 chars
                    .setPositiveButton("OK", null)
                    .show()
            }
        } else {
            Toast.makeText(this, "No entries found to update", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showClassificationDetailsDialog(entry: HistoryEntry) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_classification_details, null)

        // Set basic information
        val detailImageView = dialogView.findViewById<ImageView>(R.id.detailImageView)
        val detailClassificationText = dialogView.findViewById<TextView>(R.id.detailClassificationText)
        val detailTimestampText = dialogView.findViewById<TextView>(R.id.detailTimestampText)
        val detailModelText = dialogView.findViewById<TextView>(R.id.detailModelText)

        // Set classification results table
        val detailPred1 = dialogView.findViewById<TextView>(R.id.detailPred1)
        val detailPred2 = dialogView.findViewById<TextView>(R.id.detailPred2)
        val detailPred3 = dialogView.findViewById<TextView>(R.id.detailPred3)
        val detailProb1 = dialogView.findViewById<TextView>(R.id.detailProb1)
        val detailProb2 = dialogView.findViewById<TextView>(R.id.detailProb2)
        val detailProb3 = dialogView.findViewById<TextView>(R.id.detailProb3)

        // Set feedback section
        val detailFeedbackCard = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.detailFeedbackCard)
        val detailFeedbackIcon = dialogView.findViewById<ImageView>(R.id.detailFeedbackIcon)
        val detailFeedbackStatus = dialogView.findViewById<TextView>(R.id.detailFeedbackStatus)
        val detailFeedbackText = dialogView.findViewById<TextView>(R.id.detailFeedbackText)
        val detailCorrectClass = dialogView.findViewById<TextView>(R.id.detailCorrectClass)
        val detailActualClassificationContainer = dialogView.findViewById<LinearLayout>(R.id.detailActualClassificationContainer)
        val detailActualClassText = dialogView.findViewById<TextView>(R.id.detailActualClassText)

        // Set buttons
        val detailFeedbackButton = dialogView.findViewById<Button>(R.id.detailFeedbackButton)
        val detailCloseButton = dialogView.findViewById<Button>(R.id.detailCloseButton)

        // Load and set image
        try {
            val imageFile = java.io.File(entry.imagePath)
            if (imageFile.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
                detailImageView.setImageBitmap(bitmap)
            } else {
                detailImageView.setImageResource(R.drawable.ic_image_placeholder)
            }
        } catch (e: Exception) {
            detailImageView.setImageResource(R.drawable.ic_image_placeholder)
        }

        // Set basic info
        detailClassificationText.text = entry.classificationResult
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        detailTimestampText.text = dateFormat.format(entry.timestamp)
        detailModelText.text = "Model: ${entry.modelUsed}"

        // Parse and display classification results
        try {
            // Parse the detailed results from entry (you may need to store more detailed results)
            // For now, we'll show the main result and simulate other results
            val topResult = entry.classificationResult
            val topConfidence = entry.confidence

            detailPred1.text = topResult
            detailProb1.text = "${(topConfidence * 100).toInt()}%"

            // For demonstration, create mock secondary results
            // In a real implementation, you'd store and retrieve the full classification results
            val planktonClasses = listOf("Copepod", "Diatom", "Dinoflagellate", "Foraminifera", "Radiolaria")
            val otherClasses = planktonClasses.filter { it != topResult }.take(2)

            if (otherClasses.size >= 2) {
                val remaining = 1.0f - topConfidence
                val secondConfidence = remaining * 0.7f
                val thirdConfidence = remaining * 0.3f

                detailPred2.text = otherClasses[0]
                detailProb2.text = "${(secondConfidence * 100).toInt()}%"

                detailPred3.text = otherClasses[1]
                detailProb3.text = "${(thirdConfidence * 100).toInt()}%"
            } else {
                detailPred2.text = "N/A"
                detailProb2.text = "0%"
                detailPred3.text = "N/A"
                detailProb3.text = "0%"
            }
        } catch (e: Exception) {
            Log.e("HistoryActivity", "Error parsing classification results", e)
            detailPred1.text = entry.classificationResult
            detailProb1.text = "${(entry.confidence * 100).toInt()}%"
            detailPred2.text = "N/A"
            detailProb2.text = "0%"
            detailPred3.text = "N/A"
            detailProb3.text = "0%"
        }

        // Handle feedback display
        if (entry.userFeedback.isNotEmpty()) {
            detailFeedbackCard.visibility = View.VISIBLE
            detailFeedbackText.text = entry.userFeedback
            detailFeedbackButton.text = "Edit Feedback"

            when (entry.isCorrect) {
                true -> {
                    detailFeedbackIcon.setImageResource(R.drawable.ic_check_circle)
                    detailFeedbackIcon.setColorFilter(getColor(android.R.color.holo_green_dark))
                    detailFeedbackStatus.text = "Prediksi Benar"
                    detailFeedbackStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    detailCorrectClass.visibility = View.GONE
                    detailActualClassificationContainer.visibility = View.GONE
                }
                false -> {
                    detailFeedbackIcon.setImageResource(R.drawable.ic_error_circle)
                    detailFeedbackIcon.setColorFilter(getColor(android.R.color.holo_red_dark))
                    detailFeedbackStatus.text = "Prediksi Salah"
                    detailFeedbackStatus.setTextColor(getColor(android.R.color.holo_red_dark))

                    if (entry.correctClass.isNotEmpty()) {
                        detailCorrectClass.visibility = View.VISIBLE
                        detailCorrectClass.text = "Kelas yang benar: ${entry.correctClass}"

                        // Show actual classification
                        detailActualClassificationContainer.visibility = View.VISIBLE
                        detailActualClassText.text = entry.correctClass
                    } else {
                        detailCorrectClass.visibility = View.GONE
                        detailActualClassificationContainer.visibility = View.GONE
                    }
                }
                null -> {
                    detailFeedbackIcon.setImageResource(R.drawable.ic_help_circle)
                    detailFeedbackIcon.setColorFilter(getColor(android.R.color.darker_gray))
                    detailFeedbackStatus.text = "Menunggu Verifikasi"
                    detailFeedbackStatus.setTextColor(getColor(android.R.color.darker_gray))
                    detailCorrectClass.visibility = View.GONE
                    detailActualClassificationContainer.visibility = View.GONE
                }
            }
        } else {
            detailFeedbackCard.visibility = View.GONE
            detailFeedbackButton.text = "Add Feedback"
        }

        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Make dialog background transparent to prevent overlap with custom rounded background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set button listeners
        detailFeedbackButton.setOnClickListener {
            dialog.dismiss()
            showFeedbackDialog(entry)
        }

        detailCloseButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
