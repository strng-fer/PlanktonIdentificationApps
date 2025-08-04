package com.example.planktondetectionapps.auth

/**
 * Enum representing different user roles and their permissions
 */
enum class UserRole(val roleName: String) {
    VIEWER("Viewer"),    // New guest role - can only view app, no classification
    BASIC("Basic"),      // Changed from GUEST - requires registration, can classify
    EXPERT("Expert"),
    ADMIN("Admin");

    /**
     * Check if this role can perform image classification (gallery and camera)
     * VIEWER cannot classify, others can
     */
    fun canClassify(): Boolean {
        return this != VIEWER // Only VIEWER cannot classify
    }

    /**
     * Check if this role can view their own classification history
     * VIEWER cannot view history (no classifications), others can
     */
    fun canViewHistory(): Boolean {
        return this != VIEWER // Only VIEWER cannot view history
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
     * Check if this role is basic (formerly guest but requires registration)
     */
    fun isBasic(): Boolean {
        return this == BASIC
    }

    /**
     * Check if this role is viewer (new guest role - view only)
     */
    fun isViewer(): Boolean {
        return this == VIEWER
    }

    companion object {
        fun fromString(role: String?): UserRole {
            return values().find { it.roleName.equals(role, ignoreCase = true) } ?: BASIC // Default to BASIC instead of old GUEST
        }
    }
}
