package com.example.planktondetectionapps

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager untuk mengelola penyimpanan dan pembacaan riwayat klasifikasi dalam format CSV
 */
class HistoryManager(private val context: Context) {

    private val csvFileName = "plankton_classification_history.csv"
    private val csvFile: File

    init {
        // Gunakan internal storage untuk lebih reliable
        val historyDir = File(context.filesDir, "PlanktonHistory")
        if (!historyDir.exists()) {
            val created = historyDir.mkdirs()
            Log.d("HistoryManager", "History directory created: $created at ${historyDir.absolutePath}")
        }
        csvFile = File(historyDir, csvFileName)
        Log.d("HistoryManager", "CSV file path: ${csvFile.absolutePath}")

        // Panggil writeDebugLog setelah csvFile diinisialisasi
        writeDebugLog("INIT: History directory path: ${historyDir.absolutePath}")
        writeDebugLog("INIT: CSV file path: ${csvFile.absolutePath}")

        // Buat header jika file belum ada
        if (!csvFile.exists()) {
            createCsvFile()
            writeDebugLog("INIT: Created new CSV file")
        } else {
            Log.d("HistoryManager", "CSV file already exists with ${csvFile.length()} bytes")
            writeDebugLog("INIT: CSV file exists with ${csvFile.length()} bytes")
        }
    }

    /**
     * Membuat file CSV dengan header
     */
    private fun createCsvFile() {
        try {
            FileWriter(csvFile).use { writer ->
                writer.append(HistoryEntry.getCsvHeader())
                writer.append("\n")
                writer.flush()
            }
            Log.d("HistoryManager", "CSV file created successfully at: ${csvFile.absolutePath}")
        } catch (e: IOException) {
            Log.e("HistoryManager", "Error creating CSV file", e)
        }
    }

    /**
     * Menyimpan entry baru ke CSV
     */
    fun saveHistoryEntry(entry: HistoryEntry): Boolean {
        return try {
            writeDebugLog("SAVE: Attempting to save entry: ${entry.id}")
            writeDebugLog("SAVE: Entry details - Result: ${entry.classificationResult}, Confidence: ${entry.confidence}, Model: ${entry.modelUsed}")

            Log.d("HistoryManager", "Attempting to save entry: ${entry.id}")
            FileWriter(csvFile, true).use { writer ->
                val csvRow = entry.toCsvRow()
                writeDebugLog("SAVE: CSV row: $csvRow")
                writer.append(csvRow)
                writer.append("\n")
                writer.flush()
            }

            val newFileSize = csvFile.length()
            Log.d("HistoryManager", "History entry saved successfully: ${entry.id}")
            Log.d("HistoryManager", "File size after save: $newFileSize bytes")
            writeDebugLog("SAVE: SUCCESS - Entry saved, new file size: $newFileSize bytes")
            true
        } catch (e: IOException) {
            Log.e("HistoryManager", "Error saving history entry: ${entry.id}", e)
            writeDebugLog("SAVE: ERROR - ${e.message}")
            false
        }
    }

    /**
     * Membaca semua entry dari CSV
     */
    fun getAllHistoryEntries(): List<HistoryEntry> {
        writeDebugLog("READ: Starting getAllHistoryEntries()")
        val entries = mutableListOf<HistoryEntry>()

        try {
            writeDebugLog("READ: CSV file exists: ${csvFile.exists()}, size: ${csvFile.length()} bytes")

            if (csvFile.exists() && csvFile.length() > 0) {
                Log.d("HistoryManager", "Reading CSV file: ${csvFile.absolutePath}, size: ${csvFile.length()} bytes")
                val lines = csvFile.readLines()
                Log.d("HistoryManager", "Total lines in CSV: ${lines.size}")
                writeDebugLog("READ: Total lines in CSV: ${lines.size}")

                lines.drop(1).forEachIndexed { index, line -> // Skip header
                    if (line.isNotBlank()) {
                        writeDebugLog("READ: Processing line ${index + 2}: $line")
                        try {
                            HistoryEntry.fromCsvRow(line)?.let { entry ->
                                entries.add(entry)
                                Log.d("HistoryManager", "Parsed entry: ${entry.id}")
                                writeDebugLog("READ: Successfully parsed entry: ${entry.id}")
                            } ?: run {
                                writeDebugLog("READ: Failed to parse line ${index + 2} - null result")
                            }
                        } catch (e: Exception) {
                            Log.e("HistoryManager", "Error parsing CSV line: $line", e)
                            writeDebugLog("READ: Error parsing line ${index + 2}: ${e.message}")
                        }
                    }
                }
                Log.d("HistoryManager", "Successfully loaded ${entries.size} history entries")
                writeDebugLog("READ: Successfully loaded ${entries.size} history entries")
            } else {
                Log.w("HistoryManager", "CSV file does not exist or is empty")
                writeDebugLog("READ: CSV file does not exist or is empty")
            }
        } catch (e: Exception) {
            Log.e("HistoryManager", "Error reading history entries", e)
            writeDebugLog("READ: ERROR - ${e.message}")
        }

        // Sort by timestamp (newest first)
        val sortedEntries = entries.sortedByDescending { it.timestamp }
        writeDebugLog("READ: Returning ${sortedEntries.size} sorted entries")
        return sortedEntries
    }

    /**
     * Update feedback untuk entry tertentu
     */
    fun updateEntryFeedback(entryId: String, feedback: String, isCorrect: Boolean?, correctClass: String = ""): Boolean {
        return try {
            val entries = getAllHistoryEntries().toMutableList()
            val entryIndex = entries.indexOfFirst { it.id == entryId }

            if (entryIndex != -1) {
                entries[entryIndex] = entries[entryIndex].copy(
                    userFeedback = feedback,
                    isCorrect = isCorrect,
                    correctClass = correctClass
                )

                // Rewrite entire file
                rewriteCsvFile(entries)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("HistoryManager", "Error updating entry feedback", e)
            false
        }
    }

    /**
     * Menulis ulang seluruh file CSV
     */
    private fun rewriteCsvFile(entries: List<HistoryEntry>) {
        FileWriter(csvFile).use { writer ->
            writer.append(HistoryEntry.getCsvHeader())
            writer.append("\n")

            entries.forEach { entry ->
                writer.append(entry.toCsvRow())
                writer.append("\n")
            }
        }
    }

    /**
     * Menghapus entry berdasarkan ID
     */
    fun deleteEntry(entryId: String): Boolean {
        return try {
            val entries = getAllHistoryEntries().toMutableList()
            val removed = entries.removeIf { it.id == entryId }

            if (removed) {
                rewriteCsvFile(entries)
            }

            removed
        } catch (e: Exception) {
            Log.e("HistoryManager", "Error deleting entry", e)
            false
        }
    }

    /**
     * Mendapatkan statistik feedback
     */
    fun getFeedbackStats(): FeedbackStats {
        val entries = getAllHistoryEntries()
        val totalEntries = entries.size
        val entriesWithFeedback = entries.count { it.userFeedback.isNotEmpty() }
        val correctPredictions = entries.count { it.isCorrect == true }
        val incorrectPredictions = entries.count { it.isCorrect == false }

        return FeedbackStats(
            totalEntries = totalEntries,
            entriesWithFeedback = entriesWithFeedback,
            correctPredictions = correctPredictions,
            incorrectPredictions = incorrectPredictions
        )
    }

    /**
     * Export CSV file ke Downloads
     */
    fun exportCsvToDownloads(): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(downloadsDir, "plankton_history_export_$timestamp.csv")

            csvFile.copyTo(exportFile, overwrite = true)
            exportFile
        } catch (e: Exception) {
            Log.e("HistoryManager", "Error exporting CSV", e)
            null
        }
    }

    /**
     * Mendapatkan path file CSV
     */
    fun getCsvFilePath(): String = csvFile.absolutePath

    /**
     * Debug function to write detailed logs to internal file
     */
    private fun writeDebugLog(message: String) {
        try {
            val debugFile = File(context.filesDir, "history_debug.txt")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            debugFile.appendText("[$timestamp] $message\n")
        } catch (e: Exception) {
            Log.e("HistoryManager", "Error writing debug log", e)
        }
    }

    /**
     * Get debug information
     */
    fun getDebugInfo(): String {
        return try {
            val debugFile = File(context.filesDir, "history_debug.txt")
            if (debugFile.exists()) {
                debugFile.readText()
            } else {
                "Debug file not found"
            }
        } catch (e: Exception) {
            "Error reading debug file: ${e.message}"
        }
    }

    /**
     * Test function to verify if save/load works properly
     */
    fun testSaveLoad(): String {
        return try {
            val testEntry = HistoryEntry(
                id = "TEST_${System.currentTimeMillis()}",
                timestamp = Date(),
                imagePath = "/test/path",
                classificationResult = "TestPlankton",
                confidence = 0.85f,
                modelUsed = "TestModel"
            )

            writeDebugLog("TEST: Creating test entry: ${testEntry.id}")

            // Test save
            val saveResult = saveHistoryEntry(testEntry)
            writeDebugLog("TEST: Save result: $saveResult")

            // Test load
            val allEntries = getAllHistoryEntries()
            writeDebugLog("TEST: Loaded ${allEntries.size} entries")

            val foundEntry = allEntries.find { it.id == testEntry.id }
            writeDebugLog("TEST: Found test entry: ${foundEntry != null}")

            if (saveResult && foundEntry != null) {
                "✅ SAVE/LOAD TEST PASSED\nSaved and retrieved test entry successfully"
            } else {
                "❌ SAVE/LOAD TEST FAILED\nSave: $saveResult, Found: ${foundEntry != null}"
            }
        } catch (e: Exception) {
            writeDebugLog("TEST: ERROR - ${e.message}")
            "❌ TEST EXCEPTION: ${e.message}"
        }
    }
}

/**
 * Data class untuk statistik feedback
 */
data class FeedbackStats(
    val totalEntries: Int,
    val entriesWithFeedback: Int,
    val correctPredictions: Int,
    val incorrectPredictions: Int
) {
    val feedbackPercentage: Float
        get() = if (totalEntries > 0) (entriesWithFeedback * 100f) / totalEntries else 0f

    val accuracyPercentage: Float
        get() = if (entriesWithFeedback > 0) (correctPredictions * 100f) / entriesWithFeedback else 0f
}
