package com.example.planktondetectionapps.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

/**
 * Authentication manager for handling Firebase authentication and user roles
 */
class AuthManager private constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    private var currentAppUser: AppUser? = null
    private val authStateListeners = mutableListOf<(AppUser?) -> Unit>()

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager().also { INSTANCE = it }
            }
        }

        private const val TAG = "AuthManager"
    }

    init {
        // Listen to Firebase Auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // User is signed in, load user data from Firestore
                loadUserData(firebaseUser)
            } else {
                // User is signed out
                currentAppUser = null
                notifyAuthStateListeners()
            }
        }
    }

    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<AppUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Authentication failed")

            // Update last login time
            updateLastLoginTime(firebaseUser.uid)

            // Load and return user data
            val appUser = loadUserDataSync(firebaseUser)
            Result.success(appUser)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Register new user with email and password
     */
    suspend fun register(email: String, password: String, displayName: String, role: UserRole = UserRole.USER): Result<AppUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Registration failed")

            // Create user profile in Firestore
            val appUser = AppUser.fromFirebaseUser(firebaseUser, role).copy(
                displayName = displayName
            )

            // Save to Firestore
            usersCollection.document(firebaseUser.uid).set(appUser.toMap()).await()

            currentAppUser = appUser
            notifyAuthStateListeners()

            Result.success(appUser)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
        currentAppUser = null
        notifyAuthStateListeners()
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
                    val appUser = AppUser.fromMap(document.data ?: emptyMap())
                    currentAppUser = appUser
                } else {
                    // Create default user profile if doesn't exist
                    val appUser = AppUser.fromFirebaseUser(firebaseUser)
                    usersCollection.document(firebaseUser.uid).set(appUser.toMap())
                    currentAppUser = appUser
                }
                notifyAuthStateListeners()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to load user data", exception)
                // Create default user profile on failure
                val appUser = AppUser.fromFirebaseUser(firebaseUser)
                currentAppUser = appUser
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
                notifyAuthStateListeners()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load user data", e)
            // Return default user profile on failure
            AppUser.fromFirebaseUser(firebaseUser).also {
                currentAppUser = it
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
