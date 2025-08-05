package com.example.planktondetectionapps

import com.example.planktondetectionapps.location.LocationManager
import java.util.Date

/**
 * Data class untuk menyimpan entry riwayat klasifikasi dengan informasi lokasi
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
    val correctClass: String = "", // Jika user memberikan koreksi
    // Location information
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationAccuracy: Float = 0f,
    val fullAddress: String = "",
    val clusteredLocation: String = "", // Lokasi yang sudah di-cluster (kota/kecamatan/kelurahan)
    val locationLevel: String = "" // Level clustering (VILLAGE, SUB_DISTRICT, DISTRICT, CITY, UNKNOWN)
) {
    /**
     * Mengonversi entry ke format CSV dengan informasi lokasi
     */
    fun toCsvRow(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        // Escape quotes in text fields to prevent CSV parsing issues
        val escapedFeedback = userFeedback.replace("\"", "\"\"")
        val escapedCorrectClass = correctClass.replace("\"", "\"\"")
        val escapedResult = classificationResult.replace("\"", "\"\"")
        val escapedImagePath = imagePath.replace("\"", "\"\"")
        val escapedModel = modelUsed.replace("\"", "\"\"")
        val escapedAddress = fullAddress.replace("\"", "\"\"")
        val escapedClusteredLocation = clusteredLocation.replace("\"", "\"\"")

        return "$id,${dateFormat.format(timestamp)},\"$escapedImagePath\",\"$escapedResult\",$confidence,\"$escapedModel\",\"$escapedFeedback\",${isCorrect ?: ""},\"$escapedCorrectClass\",$latitude,$longitude,$locationAccuracy,\"$escapedAddress\",\"$escapedClusteredLocation\",$locationLevel"
    }

    companion object {
        /**
         * Header untuk file CSV dengan informasi lokasi
         */
        fun getCsvHeader(): String {
            return "ID,Timestamp,Image Path,Classification Result,Confidence,Model Used,User Feedback,Is Correct,Correct Class,Latitude,Longitude,Location Accuracy,Full Address,Clustered Location,Location Level"
        }

        /**
         * Membuat entry dari baris CSV
         */
        fun fromCsvRow(csvRow: String): HistoryEntry? {
            return try {
                android.util.Log.d("HistoryEntry", "Parsing CSV row: $csvRow")

                // Split CSV dengan handling untuk quoted strings
                val parts = mutableListOf<String>()
                var currentPart = StringBuilder()
                var insideQuotes = false
                var i = 0

                while (i < csvRow.length) {
                    val char = csvRow[i]
                    when {
                        char == '"' -> {
                            insideQuotes = !insideQuotes
                        }
                        char == ',' && !insideQuotes -> {
                            parts.add(currentPart.toString().trim())
                            currentPart = StringBuilder()
                        }
                        else -> {
                            currentPart.append(char)
                        }
                    }
                    i++
                }
                parts.add(currentPart.toString().trim()) // Add last part

                android.util.Log.d("HistoryEntry", "Parsed ${parts.size} parts: $parts")

                if (parts.size >= 6) { // Minimum required fields
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    val entry = HistoryEntry(
                        id = parts[0],
                        timestamp = try { dateFormat.parse(parts[1]) } catch (e: Exception) { Date() } ?: Date(),
                        imagePath = parts[2],
                        classificationResult = parts[3],
                        confidence = try { parts[4].toFloat() } catch (e: Exception) { 0f },
                        modelUsed = parts[5],
                        userFeedback = if (parts.size > 6) parts[6].replace("\"", "") else "",
                        isCorrect = if (parts.size > 7 && parts[7].isNotEmpty()) {
                            try { parts[7].toBoolean() } catch (e: Exception) { null }
                        } else null,
                        correctClass = if (parts.size > 8) parts[8].replace("\"", "") else "",
                        // Location fields (backwards compatibility)
                        latitude = if (parts.size > 9) try { parts[9].toDouble() } catch (e: Exception) { 0.0 } else 0.0,
                        longitude = if (parts.size > 10) try { parts[10].toDouble() } catch (e: Exception) { 0.0 } else 0.0,
                        locationAccuracy = if (parts.size > 11) try { parts[11].toFloat() } catch (e: Exception) { 0f } else 0f,
                        fullAddress = if (parts.size > 12) parts[12].replace("\"", "") else "",
                        clusteredLocation = if (parts.size > 13) parts[13].replace("\"", "") else "",
                        locationLevel = if (parts.size > 14) parts[14] else ""
                    )
                    android.util.Log.d("HistoryEntry", "Successfully created entry: ${entry.id}")
                    entry
                } else {
                    android.util.Log.e("HistoryEntry", "Invalid CSV row - not enough parts: ${parts.size}")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryEntry", "Error parsing CSV row: $csvRow", e)
                null
            }
        }

        /**
         * Create HistoryEntry with location info
         */
        fun createWithLocation(
            id: String,
            timestamp: Date,
            imagePath: String,
            classificationResult: String,
            confidence: Float,
            modelUsed: String,
            locationInfo: LocationManager.LocationInfo?
        ): HistoryEntry {
            return HistoryEntry(
                id = id,
                timestamp = timestamp,
                imagePath = imagePath,
                classificationResult = classificationResult,
                confidence = confidence,
                modelUsed = modelUsed,
                latitude = locationInfo?.latitude ?: 0.0,
                longitude = locationInfo?.longitude ?: 0.0,
                locationAccuracy = locationInfo?.accuracy ?: 0f,
                fullAddress = locationInfo?.address ?: "",
                clusteredLocation = locationInfo?.clusteredLocation ?: "",
                locationLevel = locationInfo?.locationLevel?.name ?: ""
            )
        }
    }
}
