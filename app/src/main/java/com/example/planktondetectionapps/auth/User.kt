package com.example.planktondetectionapps.auth

import com.google.firebase.Timestamp

/**
 * User data class for admin functionality
 * Represents a user with basic information for admin viewing
 */
data class User(
    val uid: String,
    val email: String?,
    val displayName: String,
    val role: UserRole,
    val createdAt: Timestamp,
    val lastLoginAt: Timestamp
)
