package com.example.planktondetectionapps.auth

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser

/**
 * Data class representing an application user with role and metadata
 */
data class AppUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val role: UserRole = UserRole.BASIC, // Changed from GUEST to BASIC
    val createdAt: Timestamp = Timestamp.now(),
    val lastLoginAt: Timestamp = Timestamp.now(),
    val isEmailVerified: Boolean = false
) {
    /**
     * Get the user's role
     */
    fun getUserRole(): UserRole = role

    /**
     * Convert to a map for Firestore storage
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "role" to role.roleName,
            "createdAt" to createdAt,
            "lastLoginAt" to lastLoginAt,
            "isEmailVerified" to isEmailVerified
        )
    }

    companion object {
        /**
         * Create AppUser from FirebaseUser
         */
        fun fromFirebaseUser(firebaseUser: FirebaseUser, role: UserRole = UserRole.BASIC): AppUser { // Changed default from GUEST to BASIC
            return AppUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@"),
                role = role,
                isEmailVerified = firebaseUser.isEmailVerified
            )
        }

        /**
         * Create AppUser from Firestore document data
         */
        fun fromMap(data: Map<String, Any?>): AppUser? {
            return try {
                AppUser(
                    uid = data["uid"] as? String ?: return null,
                    email = data["email"] as? String,
                    displayName = data["displayName"] as? String,
                    role = UserRole.fromString(data["role"] as? String),
                    createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                    lastLoginAt = data["lastLoginAt"] as? Timestamp ?: Timestamp.now(),
                    isEmailVerified = data["isEmailVerified"] as? Boolean ?: false
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
