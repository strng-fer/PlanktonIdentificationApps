package com.example.planktondetectionapps

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity untuk pengaturan aplikasi
 * Menangani preferensi pengguna seperti vibration, sound, dan save location
 */
class SettingsActivity : AppCompatActivity() {

    // UI Components
    private lateinit var backButton: ImageButton
    private lateinit var vibrationSwitch: Switch
    private lateinit var soundSwitch: Switch
    private lateinit var saveLocationSwitch: Switch

    /**
     * Inisialisasi activity dan setup UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initializeViews()
        setupListeners()
        loadSettings()
    }

    /**
     * Inisialisasi semua view components
     */
    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        vibrationSwitch = findViewById(R.id.vibrationSwitch)
        soundSwitch = findViewById(R.id.soundSwitch)
        saveLocationSwitch = findViewById(R.id.saveLocationSwitch)
    }

    /**
     * Setup listener untuk UI interactions
     */
    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        vibrationSwitch.setOnCheckedChangeListener { _, _ ->
            saveSettings()
        }

        soundSwitch.setOnCheckedChangeListener { _, _ ->
            saveSettings()
        }

        saveLocationSwitch.setOnCheckedChangeListener { _, _ ->
            saveSettings()
        }
    }

    /**
     * Load pengaturan yang tersimpan dari SharedPreferences
     */
    private fun loadSettings() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        vibrationSwitch.isChecked = sharedPref.getBoolean(PREF_VIBRATION, true)
        soundSwitch.isChecked = sharedPref.getBoolean(PREF_SOUND, true)
        saveLocationSwitch.isChecked = sharedPref.getBoolean(PREF_SAVE_TO_GALLERY, true)
    }

    /**
     * Simpan pengaturan ke SharedPreferences
     */
    private fun saveSettings() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(PREF_VIBRATION, vibrationSwitch.isChecked)
            putBoolean(PREF_SOUND, soundSwitch.isChecked)
            putBoolean(PREF_SAVE_TO_GALLERY, saveLocationSwitch.isChecked)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "PlanktonDetectionSettings"
        private const val PREF_VIBRATION = "vibration_enabled"
        private const val PREF_SOUND = "sound_enabled"
        private const val PREF_SAVE_TO_GALLERY = "save_to_gallery"
    }
}
