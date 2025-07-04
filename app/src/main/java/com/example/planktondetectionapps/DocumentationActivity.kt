package com.example.planktondetectionapps

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity untuk menampilkan dokumentasi dan panduan penggunaan aplikasi
 * Menampilkan cara penggunaan, informasi model, troubleshooting, dan FAQ
 */
class DocumentationActivity : AppCompatActivity() {

    // UI Components
    private lateinit var backButton: ImageButton
    private lateinit var howToUseSection: LinearLayout
    private lateinit var modelInfoSection: LinearLayout
    private lateinit var troubleshootingSection: LinearLayout
    private lateinit var faqSection: LinearLayout

    /**
     * Inisialisasi activity dan setup UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentation)

        initializeViews()
        setupListeners()
        setupExpandableContent()
    }

    /**
     * Inisialisasi semua view components
     */
    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        howToUseSection = findViewById(R.id.howToUseSection)
        modelInfoSection = findViewById(R.id.modelInfoSection)
        troubleshootingSection = findViewById(R.id.troubleshootingSection)
        faqSection = findViewById(R.id.faqSection)
    }

    /**
     * Setup listener untuk UI interactions
     */
    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        howToUseSection.setOnClickListener {
            toggleSection(howToUseSection)
        }

        modelInfoSection.setOnClickListener {
            toggleSection(modelInfoSection)
        }

        troubleshootingSection.setOnClickListener {
            toggleSection(troubleshootingSection)
        }

        faqSection.setOnClickListener {
            toggleSection(faqSection)
        }
    }

    /**
     * Setup konten expandable - semua section awalnya collapsed
     */
    private fun setupExpandableContent() {
        val sections = listOf(howToUseSection, modelInfoSection, troubleshootingSection, faqSection)
        sections.forEach { section ->
            val contentView = section.findViewWithTag<TextView>(CONTENT_TAG)
            contentView?.visibility = View.GONE
        }
    }

    /**
     * Toggle visibility section expandable
     */
    private fun toggleSection(section: LinearLayout) {
        val contentView = section.findViewWithTag<TextView>(CONTENT_TAG)
        val arrowView = section.findViewWithTag<TextView>(ARROW_TAG)

        if (contentView?.visibility == View.VISIBLE) {
            contentView.visibility = View.GONE
            arrowView?.text = ARROW_DOWN
        } else {
            contentView?.visibility = View.VISIBLE
            arrowView?.text = ARROW_UP
        }
    }

    companion object {
        private const val CONTENT_TAG = "content"
        private const val ARROW_TAG = "arrow"
        private const val ARROW_DOWN = "▼"
        private const val ARROW_UP = "▲"
    }
}
