package com.example.planktondetectionapps.database

import android.util.Log
import com.example.planktondetectionapps.HistoryEntry
import com.example.planktondetectionapps.auth.AuthManager
import com.example.planktondetectionapps.auth.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

/**
 * Service untuk mengelola integrasi database dengan Firebase Firestore
 */
class DatabaseService private constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val classificationsCollection = firestore.collection("classifications")
    private val authManager = AuthManager.getInstance()

    companion object {
        @Volatile
        private var INSTANCE: DatabaseService? = null

        fun getInstance(): DatabaseService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseService().also { INSTANCE = it }
            }
        }

        private const val TAG = "DatabaseService"
    }

    /**
     * Menyimpan hasil klasifikasi ke database
     * Dipanggil otomatis untuk semua user (guest, expert, admin)
     */
    suspend fun saveClassificationToDatabase(entry: HistoryEntry): Result<String> {
        return try {
            val currentUser = authManager.getCurrentUser()
            val userId = currentUser?.uid ?: "guest_${System.currentTimeMillis()}"
            val userRole = currentUser?.role?.roleName ?: UserRole.BASIC.roleName // Changed from GUEST to BASIC

            val classificationData = mapOf(
                "id" to entry.id,
                "userId" to userId,
                "userRole" to userRole,
                "imagePath" to entry.imagePath,
                "classificationResult" to entry.classificationResult,
                "confidence" to entry.confidence,
                "modelUsed" to entry.modelUsed,
                "timestamp" to Timestamp(entry.timestamp),
                "userFeedback" to entry.userFeedback,
                "isCorrect" to entry.isCorrect,
                "correctClass" to entry.correctClass,
                "isUpdated" to false,
                "createdAt" to Timestamp.now()
            )

            // Simpan ke collection dengan document ID yang sama dengan entry ID
            classificationsCollection.document(entry.id).set(classificationData).await()

            Log.d(TAG, "Classification saved to database: ${entry.id} for user: $userId")
            Result.success(entry.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save classification to database", e)
            Result.failure(e)
        }
    }

    /**
     * Update hasil klasifikasi di database (untuk feedback expert)
     */
    suspend fun updateClassificationInDatabase(entry: HistoryEntry): Result<Unit> {
        return try {
            val currentUser = authManager.getCurrentUser()
            val userId = currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))

            val updateData = mapOf(
                "userFeedback" to entry.userFeedback,
                "isCorrect" to entry.isCorrect,
                "correctClass" to entry.correctClass,
                "isUpdated" to true,
                "updatedBy" to userId,
                "updatedAt" to Timestamp.now()
            )

            classificationsCollection.document(entry.id).update(updateData).await()

            Log.d(TAG, "Classification updated in database: ${entry.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update classification in database", e)
            Result.failure(e)
        }
    }

    /**
     * Mengambil semua log klasifikasi dari database (hanya untuk admin)
     */
    suspend fun getAllClassificationsFromDatabase(): Result<List<Map<String, Any>>> {
        return try {
            val currentUser = authManager.getCurrentUser()

            // Cek apakah user adalah admin
            if (currentUser?.role != UserRole.ADMIN) {
                return Result.failure(SecurityException("Only admin can access all classifications"))
            }

            val querySnapshot = classificationsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val classifications = querySnapshot.documents.mapNotNull { document ->
                document.data?.plus("documentId" to document.id)
            }

            Log.d(TAG, "Retrieved ${classifications.size} classifications from database")
            Result.success(classifications)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all classifications from database", e)
            Result.failure(e)
        }
    }

    /**
     * Mengambil log klasifikasi berdasarkan user ID
     */
    suspend fun getClassificationsByUserId(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val querySnapshot = classificationsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val classifications = querySnapshot.documents.mapNotNull { document ->
                document.data?.plus("documentId" to document.id)
            }

            Log.d(TAG, "Retrieved ${classifications.size} classifications for user: $userId")
            Result.success(classifications)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get classifications for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Export semua data klasifikasi ke format CSV (untuk admin)
     */
    suspend fun exportAllClassificationsToCSV(): Result<String> {
        return try {
            val result = getAllClassificationsFromDatabase()
            if (!result.isSuccess) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Failed to get classifications"))
            }

            val classifications = result.getOrNull() ?: emptyList()
            val csvContent = buildString {
                // Header
                appendLine("ID,User ID,User Role,Image Path,Classification Result,Confidence,Model Used,Timestamp,User Feedback,Is Correct,Correct Class,Is Updated,Updated By,Created At,Updated At")

                // Data rows
                classifications.forEach { classification ->
                    val id = classification["id"] ?: ""
                    val userId = classification["userId"] ?: ""
                    val userRole = classification["userRole"] ?: ""
                    val imagePath = classification["imagePath"] ?: ""
                    val classificationResult = classification["classificationResult"] ?: ""
                    val confidence = classification["confidence"] ?: ""
                    val modelUsed = classification["modelUsed"] ?: ""
                    val timestamp = classification["timestamp"] ?: ""
                    val userFeedback = classification["userFeedback"] ?: ""
                    val isCorrect = classification["isCorrect"] ?: ""
                    val correctClass = classification["correctClass"] ?: ""
                    val isUpdated = classification["isUpdated"] ?: false
                    val updatedBy = classification["updatedBy"] ?: ""
                    val createdAt = classification["createdAt"] ?: ""
                    val updatedAt = classification["updatedAt"] ?: ""

                    appendLine("\"$id\",\"$userId\",\"$userRole\",\"$imagePath\",\"$classificationResult\",\"$confidence\",\"$modelUsed\",\"$timestamp\",\"$userFeedback\",\"$isCorrect\",\"$correctClass\",\"$isUpdated\",\"$updatedBy\",\"$createdAt\",\"$updatedAt\"")
                }
            }

            Log.d(TAG, "Generated CSV export with ${classifications.size} records")
            Result.success(csvContent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export classifications to CSV", e)
            Result.failure(e)
        }
    }
}
