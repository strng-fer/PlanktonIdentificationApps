package com.example.planktondetectionapps.auth

/**
 * Enum representing different user roles and their permissions
 */
enum class UserRole(val roleName: String) {
    GUEST("Guest"),
    USER("User"),
    RESEARCHER("Researcher"),
    ADMIN("Admin");

    /**
     * Check if this role can upload images
     */
    fun canUpload(): Boolean {
        return this != GUEST
    }

    /**
     * Check if this role can access feedback functionality
     */
    fun canAccessFeedback(): Boolean {
        return this == RESEARCHER || this == ADMIN
    }

    /**
     * Check if this role has admin privileges
     */
    fun isAdmin(): Boolean {
        return this == ADMIN
    }

    companion object {
        fun fromString(role: String?): UserRole {
            return values().find { it.roleName.equals(role, ignoreCase = true) } ?: USER
        }
    }
}
