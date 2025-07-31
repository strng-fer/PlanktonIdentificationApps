package com.example.planktondetectionapps.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

/**
 * Authentication manager for handling Firebase authentication and user roles
 * Fixed version with proper state management and cache handling
 */
class AuthManager private constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    private var currentAppUser: AppUser? = null
    private val authStateListeners = mutableListOf<(AppUser?) -> Unit>()
    private var sharedPreferences: SharedPreferences? = null
    private var isInitialized = false
    private var isProcessingAuthState = false // Add flag to prevent multiple auth state processing

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager().also { INSTANCE = it }
            }
        }

        private const val TAG = "AuthManager"
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_GUEST_UID = "guest_uid"
        private const val KEY_GUEST_DISPLAY_NAME = "guest_display_name"
        private const val KEY_AUTO_LOGIN_ENABLED = "auto_login_enabled"
        private const val KEY_LAST_AUTH_TYPE = "last_auth_type"
        private const val KEY_USER_EMAIL = "user_email" // Add user email persistence
        private const val KEY_USER_DISPLAY_NAME = "user_display_name" // Add user display name persistence
        private const val KEY_USER_ROLE = "user_role" // Add user role persistence
        private const val KEY_USER_UID = "user_uid" // Add user UID persistence
    }

    /**
     * Initialize with context for SharedPreferences
     */
    fun initialize(context: Context) {
        if (isInitialized) return

        Log.d(TAG, "Initializing AuthManager...")
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Try to restore session from SharedPreferences first
        restoreLastSession()

        // Set up Firebase Auth state listener
        setupFirebaseAuthListener()

        isInitialized = true
        Log.d(TAG, "AuthManager initialized successfully")
    }

    /**
     * Restore last authentication session from SharedPreferences
     */
    private fun restoreLastSession() {
        sharedPreferences?.let { prefs ->
            val lastAuthType = prefs.getString(KEY_LAST_AUTH_TYPE, null)
            Log.d(TAG, "Attempting to restore last session. Auth type: $lastAuthType")

            when (lastAuthType) {
                "firebase" -> {
                    // Try to restore Firebase user session from cache
                    val userEmail = prefs.getString(KEY_USER_EMAIL, null)
                    val userDisplayName = prefs.getString(KEY_USER_DISPLAY_NAME, null)
                    val userRole = prefs.getString(KEY_USER_ROLE, null)
                    val userUid = prefs.getString(KEY_USER_UID, null)

                    if (!userEmail.isNullOrEmpty() && !userDisplayName.isNullOrEmpty() &&
                        !userRole.isNullOrEmpty() && !userUid.isNullOrEmpty()) {

                        val role = UserRole.values().find { it.roleName == userRole } ?: UserRole.GUEST
                        val cachedUser = AppUser(
                            uid = userUid,
                            email = userEmail,
                            displayName = userDisplayName,
                            role = role,
                            isEmailVerified = true
                        )

                        currentAppUser = cachedUser
                        Log.d(TAG, "Firebase session restored from cache: $userEmail")
                        notifyAuthStateListeners()
                    }
                }
                "guest" -> {
                    // Restore guest session immediately
                    restoreGuestSession()
                }
                "logout" -> {
                    Log.d(TAG, "Last action was logout, not restoring session")
                }
                else -> {
                    Log.d(TAG, "No previous session found")
                }
            }
        }
    }

    /**
     * Save user session data to SharedPreferences for persistence
     */
    private fun saveUserSession(user: AppUser) {
        sharedPreferences?.edit()?.apply {
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_DISPLAY_NAME, user.displayName)
            putString(KEY_USER_ROLE, user.role.roleName)
            putString(KEY_USER_UID, user.uid)
            apply()
        }
        Log.d(TAG, "User session saved: ${user.email}")
    }

    /**
     * Clear Firebase user session data
     */
    private fun clearFirebaseUserSession() {
        sharedPreferences?.edit()?.apply {
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_DISPLAY_NAME)
            remove(KEY_USER_ROLE)
            remove(KEY_USER_UID)
            apply()
        }
        Log.d(TAG, "Firebase user session cleared")
    }

    /**
     * Setup Firebase Auth state listener with improved session handling
     */
    private fun setupFirebaseAuthListener() {
        auth.addAuthStateListener { firebaseAuth ->
            if (isProcessingAuthState) {
                Log.d(TAG, "Already processing auth state, skipping...")
                return@addAuthStateListener
            }

            val firebaseUser = firebaseAuth.currentUser
            Log.d(TAG, "Firebase Auth state changed. User: ${firebaseUser?.email ?: "null"}")

            isProcessingAuthState = true

            if (firebaseUser != null) {
                // Firebase user is authenticated
                val lastAuthType = getLastAuthType()

                if (lastAuthType != "logout") {
                    // Clear guest preferences when Firebase user is present
                    clearGuestPreferences()
                    saveLastAuthType("firebase")

                    // Check if we have cached user data
                    val cachedEmail = sharedPreferences?.getString(KEY_USER_EMAIL, null)
                    val cachedDisplayName = sharedPreferences?.getString(KEY_USER_DISPLAY_NAME, null)
                    val cachedRole = sharedPreferences?.getString(KEY_USER_ROLE, null)
                    val cachedUid = sharedPreferences?.getString(KEY_USER_UID, null)

                    if (cachedEmail == firebaseUser.email && cachedUid == firebaseUser.uid &&
                        !cachedDisplayName.isNullOrEmpty() && !cachedRole.isNullOrEmpty()) {
                        // Use cached user data for immediate loading
                        Log.d(TAG, "Using cached user data for: ${firebaseUser.email}")
                        val userRole = UserRole.values().find { it.roleName == cachedRole } ?: UserRole.GUEST
                        currentAppUser = AppUser(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email,
                            displayName = cachedDisplayName,
                            role = userRole,
                            isEmailVerified = firebaseUser.isEmailVerified
                        )
                        notifyAuthStateListeners()
                    } else {
                        // Load fresh data from Firestore and cache it
                        loadUserData(firebaseUser)
                    }
                } else {
                    Log.d(TAG, "User was logged out, not restoring Firebase session")
                }
            } else {
                // No Firebase user - check for guest session
                val lastAuthType = getLastAuthType()
                Log.d(TAG, "No Firebase user. Last auth type: $lastAuthType")

                if (lastAuthType == "guest" && currentAppUser == null) {
                    // Try to restore guest session
                    if (!restoreGuestSession()) {
                        currentAppUser = null
                        notifyAuthStateListeners()
                    }
                } else if (lastAuthType == "logout") {
                    // User explicitly logged out, clear Firebase session data
                    clearFirebaseUserSession()
                    currentAppUser = null
                    notifyAuthStateListeners()
                }
            }

            isProcessingAuthState = false
        }
    }

    /**
     * Save last authentication type
     */
    private fun saveLastAuthType(type: String) {
        sharedPreferences?.edit()?.apply {
            putString(KEY_LAST_AUTH_TYPE, type)
            apply()
        }
    }

    /**
     * Get last authentication type (public method for access from MainActivity)
     */
    fun getLastAuthType(): String? {
        return sharedPreferences?.getString(KEY_LAST_AUTH_TYPE, null)
    }

    /**
     * Restore guest session from SharedPreferences
     */
    private fun restoreGuestSession(): Boolean {
        sharedPreferences?.let { prefs ->
            val isGuest = prefs.getBoolean(KEY_IS_GUEST, false)
            val guestUid = prefs.getString(KEY_GUEST_UID, null)
            val guestDisplayName = prefs.getString(KEY_GUEST_DISPLAY_NAME, "Guest User")
            val autoLoginEnabled = prefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, true)

            Log.d(TAG, "Attempting to restore guest session:")
            Log.d(TAG, "  isGuest: $isGuest")
            Log.d(TAG, "  guestUid: $guestUid")
            Log.d(TAG, "  autoLoginEnabled: $autoLoginEnabled")

            if (isGuest && !guestUid.isNullOrEmpty() && autoLoginEnabled) {
                val guestUser = AppUser(
                    uid = guestUid,
                    email = null,
                    displayName = guestDisplayName,
                    role = UserRole.GUEST,
                    isEmailVerified = false
                )

                currentAppUser = guestUser
                saveLastAuthType("guest")
                notifyAuthStateListeners()
                Log.d(TAG, "Guest session restored successfully: $guestUid")
                return true
            }
        }
        Log.d(TAG, "No guest session to restore")
        return false
    }

    /**
     * Save guest session to SharedPreferences
     */
    private fun saveGuestSession(guestUser: AppUser) {
        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_IS_GUEST, true)
            putString(KEY_GUEST_UID, guestUser.uid)
            putString(KEY_GUEST_DISPLAY_NAME, guestUser.displayName)
            putBoolean(KEY_AUTO_LOGIN_ENABLED, true)
            apply()
        }
        saveLastAuthType("guest")
        Log.d(TAG, "Guest session saved: ${guestUser.uid}")
    }

    /**
     * Clear guest session preferences
     */
    private fun clearGuestPreferences() {
        sharedPreferences?.edit()?.apply {
            remove(KEY_IS_GUEST)
            remove(KEY_GUEST_UID)
            remove(KEY_GUEST_DISPLAY_NAME)
            apply()
        }
        Log.d(TAG, "Guest preferences cleared")
    }

    /**
     * Clear all authentication data
     */
    private fun clearAllAuthData() {
        sharedPreferences?.edit()?.apply {
            clear()
            apply()
        }
        Log.d(TAG, "All auth data cleared")
    }

    /**
     * Enable or disable auto-login
     */
    fun setAutoLoginEnabled(enabled: Boolean) {
        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_AUTO_LOGIN_ENABLED, enabled)
            apply()
        }
    }

    /**
     * Check if auto-login is enabled
     */
    fun isAutoLoginEnabled(): Boolean {
        return sharedPreferences?.getBoolean(KEY_AUTO_LOGIN_ENABLED, true) ?: true
    }

    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<AppUser> {
        return try {
            Log.d(TAG, "Attempting to sign in with email: $email")

            // Clear any existing guest session before Firebase login
            clearGuestPreferences()

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Authentication failed - no user returned")

            Log.d(TAG, "Firebase sign in successful for: ${firebaseUser.email}")

            // Update last login time
            updateLastLoginTime(firebaseUser.uid)

            // Load and return user data
            val appUser = loadUserDataSync(firebaseUser)
            Log.d(TAG, "User data loaded successfully: ${appUser.displayName}, Role: ${appUser.role}")

            Result.success(appUser)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed for email: $email", e)
            Result.failure(e)
        }
    }

    /**
     * Register new user with email and password
     */
    suspend fun register(email: String, password: String, displayName: String, role: UserRole = UserRole.GUEST): Result<AppUser> {
        return try {
            Log.d(TAG, "Attempting to register user with email: $email")

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Registration failed")

            // Create user profile in Firestore
            val appUser = AppUser.fromFirebaseUser(firebaseUser, role).copy(
                displayName = displayName
            )

            // Save to Firestore
            usersCollection.document(firebaseUser.uid).set(appUser.toMap()).await()

            currentAppUser = appUser
            saveLastAuthType("firebase")
            notifyAuthStateListeners()

            Log.d(TAG, "User registered successfully: $displayName")
            Result.success(appUser)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            Result.failure(e)
        }
    }

    /**
     * Login as guest user (no authentication required)
     * Now with persistent session support
     */
    fun loginAsGuest(): AppUser {
        Log.d(TAG, "Attempting guest login...")

        // Check if we have existing guest session
        sharedPreferences?.let { prefs ->
            val existingGuestUid = prefs.getString(KEY_GUEST_UID, null)
            val existingDisplayName = prefs.getString(KEY_GUEST_DISPLAY_NAME, "Guest User")

            if (!existingGuestUid.isNullOrEmpty()) {
                val guestUser = AppUser(
                    uid = existingGuestUid,
                    email = null,
                    displayName = existingDisplayName,
                    role = UserRole.GUEST,
                    isEmailVerified = false
                )

                currentAppUser = guestUser
                saveLastAuthType("guest")
                notifyAuthStateListeners()
                Log.d(TAG, "Existing guest session restored: $existingGuestUid")
                return guestUser
            }
        }

        // Create new guest session
        val guestUser = AppUser(
            uid = "guest_${System.currentTimeMillis()}",
            email = null,
            displayName = "Guest User",
            role = UserRole.GUEST,
            isEmailVerified = false
        )

        currentAppUser = guestUser
        saveGuestSession(guestUser)
        notifyAuthStateListeners()

        Log.d(TAG, "New guest user created and saved: ${guestUser.uid}")
        return guestUser
    }

    /**
     * Sign out current user
     * Now properly clears all session data
     */
    fun signOut() {
        Log.d(TAG, "Signing out user...")

        // Mark as logout to prevent auto-restore
        saveLastAuthType("logout")

        // Sign out from Firebase if authenticated
        if (auth.currentUser != null) {
            Log.d(TAG, "Signing out from Firebase")
            auth.signOut()
        }

        // Clear all saved session data
        clearAllAuthData()
        currentAppUser = null
        notifyAuthStateListeners()

        Log.d(TAG, "User signed out and all session data cleared")
    }

    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): AppUser? = currentAppUser

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean = currentAppUser != null

    /**
     * Check if current user is guest
     */
    fun isGuestUser(): Boolean {
        return currentAppUser?.role == UserRole.GUEST && currentAppUser?.uid?.startsWith("guest_") == true
    }

    /**
     * Add authentication state listener
     */
    fun addAuthStateListener(listener: (AppUser?) -> Unit) {
        authStateListeners.add(listener)
        // Immediately notify with current state
        listener(currentAppUser)
    }

    /**
     * Remove authentication state listener
     */
    fun removeAuthStateListener(listener: (AppUser?) -> Unit) {
        authStateListeners.remove(listener)
    }

    /**
     * Load user data from Firestore
     */
    private fun loadUserData(firebaseUser: FirebaseUser) {
        usersCollection.document(firebaseUser.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val appUser = AppUser.fromMap(document.data ?: emptyMap()) ?: AppUser.fromFirebaseUser(firebaseUser)
                    currentAppUser = appUser
                    // Save user session for persistence
                    saveUserSession(appUser)
                } else {
                    // Create default user profile if doesn't exist
                    val appUser = AppUser.fromFirebaseUser(firebaseUser)
                    usersCollection.document(firebaseUser.uid).set(appUser.toMap())
                    currentAppUser = appUser
                    saveUserSession(appUser)
                }
                notifyAuthStateListeners()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to load user data", exception)
                // Create default user profile on failure
                val appUser = AppUser.fromFirebaseUser(firebaseUser)
                currentAppUser = appUser
                saveUserSession(appUser)
                notifyAuthStateListeners()
            }
    }

    /**
     * Load user data synchronously for use in suspend functions
     */
    private suspend fun loadUserDataSync(firebaseUser: FirebaseUser): AppUser {
        return try {
            val document = usersCollection.document(firebaseUser.uid).get().await()
            if (document.exists()) {
                AppUser.fromMap(document.data ?: emptyMap()) ?: AppUser.fromFirebaseUser(firebaseUser)
            } else {
                // Create default user profile if doesn't exist
                val appUser = AppUser.fromFirebaseUser(firebaseUser)
                usersCollection.document(firebaseUser.uid).set(appUser.toMap()).await()
                appUser
            }.also {
                currentAppUser = it
                // Save user session for persistence
                saveUserSession(it)
                notifyAuthStateListeners()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load user data", e)
            // Return default user profile on failure
            AppUser.fromFirebaseUser(firebaseUser).also {
                currentAppUser = it
                saveUserSession(it)
                notifyAuthStateListeners()
            }
        }
    }

    /**
     * Update last login time
     */
    private fun updateLastLoginTime(uid: String) {
        usersCollection.document(uid).update("lastLoginAt", Timestamp.now())
            .addOnFailureListener { exception ->
                Log.w(TAG, "Failed to update last login time", exception)
            }
    }

    /**
     * Notify all auth state listeners
     */
    private fun notifyAuthStateListeners() {
        authStateListeners.forEach { listener ->
            try {
                listener(currentAppUser)
            } catch (e: Exception) {
                Log.e(TAG, "Error in auth state listener", e)
            }
        }
    }

    /**
     * Update user role (admin only)
     */
    suspend fun updateUserRole(userId: String, newRole: UserRole): Result<Unit> {
        return try {
            val currentUser = getCurrentUser()
            if (currentUser?.role != UserRole.ADMIN) {
                throw SecurityException("Only admins can update user roles")
            }

            usersCollection.document(userId).update("role", newRole.roleName).await()

            // If updating current user's role, reload data
            if (userId == currentUser.uid) {
                auth.currentUser?.let { loadUserData(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user role", e)
            Result.failure(e)
        }
    }
}
