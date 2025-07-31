package com.example.planktondetectionapps.auth

/**
 * Enum representing different user roles and their permissions
 */
enum class UserRole(val roleName: String) {
    GUEST("Guest"),
    EXPERT("Expert"),
    ADMIN("Admin");

    /**
     * Check if this role can perform image classification (gallery and camera)
     * All roles can classify
     */
    fun canClassify(): Boolean {
        return true // All users can classify
    }

    /**
     * Check if this role can view their own classification history
     * All roles can view their own history
     */
    fun canViewHistory(): Boolean {
        return true // All users can view their own history
    }

    /**
     * Check if this role can provide feedback on classifications
     * Only Expert and Admin can provide feedback
     */
    fun canProvideFeedback(): Boolean {
        return this == EXPERT || this == ADMIN
    }

    /**
     * Check if this role can access all features including feedback
     * Only Expert and Admin can access all features
     */
    fun canAccessAllFeatures(): Boolean {
        return this == EXPERT || this == ADMIN
    }

    /**
     * Check if this role can download all logs from database
     * Only Admin can download all logs
     */
    fun canDownloadAllLogs(): Boolean {
        return this == ADMIN
    }

    /**
     * Check if this role has admin privileges
     */
    fun isAdmin(): Boolean {
        return this == ADMIN
    }

    /**
     * Check if this role is expert level
     */
    fun isExpert(): Boolean {
        return this == EXPERT
    }

    /**
     * Check if this role is guest
     */
    fun isGuest(): Boolean {
        return this == GUEST
    }

    companion object {
        fun fromString(role: String?): UserRole {
            return values().find { it.roleName.equals(role, ignoreCase = true) } ?: GUEST // Default to GUEST instead of USER
        }
    }
}
