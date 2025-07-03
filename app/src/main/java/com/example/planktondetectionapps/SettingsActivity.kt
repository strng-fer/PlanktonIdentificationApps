package com.example.planktondetectionapps

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var backButton: ImageButton
    private lateinit var vibrationSwitch: Switch
    private lateinit var soundSwitch: Switch
    private lateinit var saveLocationSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initializeViews()
        setupListeners()
        loadSettings()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        vibrationSwitch = findViewById(R.id.vibrationSwitch)
        soundSwitch = findViewById(R.id.soundSwitch)
        saveLocationSwitch = findViewById(R.id.saveLocationSwitch)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        vibrationSwitch.setOnCheckedChangeListener { _, _ -> saveSettings() }
        soundSwitch.setOnCheckedChangeListener { _, _ -> saveSettings() }
        saveLocationSwitch.setOnCheckedChangeListener { _, _ -> saveSettings() }
    }

    private fun loadSettings() {
        val sharedPref = getSharedPreferences("PlanktonDetectionSettings", MODE_PRIVATE)

        // Load other settings
        vibrationSwitch.isChecked = sharedPref.getBoolean("vibration_enabled", true)
        soundSwitch.isChecked = sharedPref.getBoolean("sound_enabled", true)
        saveLocationSwitch.isChecked = sharedPref.getBoolean("save_to_gallery", true)
    }

    private fun saveSettings() {
        val sharedPref = getSharedPreferences("PlanktonDetectionSettings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("vibration_enabled", vibrationSwitch.isChecked)
            putBoolean("sound_enabled", soundSwitch.isChecked)
            putBoolean("save_to_gallery", saveLocationSwitch.isChecked)
            apply()
        }
    }
}
