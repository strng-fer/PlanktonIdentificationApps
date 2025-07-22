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
        // Buat file di direktori Documents aplikasi
        val documentsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PlanktonHistory")
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }
        csvFile = File(documentsDir, csvFileName)

        // Buat header jika file belum ada
        if (!csvFile.exists()) {
            createCsvFile()
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
            }
            Log.d("HistoryManager", "CSV file created at: ${csvFile.absolutePath}")
        } catch (e: IOException) {
            Log.e("HistoryManager", "Error creating CSV file", e)
        }
    }

    /**
     * Menyimpan entry baru ke CSV
     */
    fun saveHistoryEntry(entry: HistoryEntry): Boolean {
        return try {
            FileWriter(csvFile, true).use { writer ->
                writer.append(entry.toCsvRow())
                writer.append("\n")
            }
            Log.d("HistoryManager", "History entry saved: ${entry.id}")
            true
        } catch (e: IOException) {
            Log.e("HistoryManager", "Error saving history entry", e)
            false
        }
    }

    /**
     * Membaca semua entry dari CSV
     */
    fun getAllHistoryEntries(): List<HistoryEntry> {
        val entries = mutableListOf<HistoryEntry>()

        try {
            if (csvFile.exists()) {
                csvFile.readLines().drop(1).forEach { line -> // Skip header
                    if (line.isNotBlank()) {
                        HistoryEntry.fromCsvRow(line)?.let { entry ->
                            entries.add(entry)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HistoryManager", "Error reading history entries", e)
        }

        // Sort by timestamp (newest first)
        return entries.sortedByDescending { it.timestamp }
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
