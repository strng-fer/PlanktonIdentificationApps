package com.example.planktondetectionapps

import java.util.Date

/**
 * Data class untuk menyimpan entry riwayat klasifikasi
 */
data class HistoryEntry(
    val id: String,
    val timestamp: Date,
    val imagePath: String,
    val classificationResult: String,
    val confidence: Float,
    val modelUsed: String,
    val userFeedback: String = "", // Empty jika belum ada feedback
    val isCorrect: Boolean? = null, // null jika belum ada feedback, true/false jika sudah ada
    val correctClass: String = "" // Jika user memberikan koreksi
) {
    /**
     * Mengonversi entry ke format CSV
     */
    fun toCsvRow(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return "$id,${dateFormat.format(timestamp)},$imagePath,$classificationResult,$confidence,$modelUsed,\"$userFeedback\",${isCorrect ?: ""},\"$correctClass\""
    }

    companion object {
        /**
         * Header untuk file CSV
         */
        fun getCsvHeader(): String {
            return "ID,Timestamp,Image Path,Classification Result,Confidence,Model Used,User Feedback,Is Correct,Correct Class"
        }

        /**
         * Membuat entry dari baris CSV
         */
        fun fromCsvRow(csvRow: String): HistoryEntry? {
            return try {
                val parts = csvRow.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                if (parts.size >= 9) {
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    HistoryEntry(
                        id = parts[0],
                        timestamp = dateFormat.parse(parts[1]) ?: Date(),
                        imagePath = parts[2],
                        classificationResult = parts[3],
                        confidence = parts[4].toFloat(),
                        modelUsed = parts[5],
                        userFeedback = parts[6].replace("\"", ""),
                        isCorrect = if (parts[7].isEmpty()) null else parts[7].toBoolean(),
                        correctClass = parts[8].replace("\"", "")
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
