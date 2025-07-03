package com.example.planktondetectionapps

import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DocumentationActivity : AppCompatActivity() {
    private lateinit var backButton: ImageButton
    private lateinit var howToUseSection: LinearLayout
    private lateinit var modelInfoSection: LinearLayout
    private lateinit var troubleshootingSection: LinearLayout
    private lateinit var faqSection: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentation)

        initializeViews()
        setupListeners()
        setupExpandableContent()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        howToUseSection = findViewById(R.id.howToUseSection)
        modelInfoSection = findViewById(R.id.modelInfoSection)
        troubleshootingSection = findViewById(R.id.troubleshootingSection)
        faqSection = findViewById(R.id.faqSection)
    }

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

    private fun setupExpandableContent() {
        // Initially collapse all content sections
        val sections = listOf(howToUseSection, modelInfoSection, troubleshootingSection, faqSection)
        sections.forEach { section ->
            val contentView = section.findViewWithTag<TextView>("content")
            contentView?.visibility = android.view.View.GONE
        }
    }

    private fun toggleSection(section: LinearLayout) {
        val contentView = section.findViewWithTag<TextView>("content")
        val arrowView = section.findViewWithTag<TextView>("arrow")

        if (contentView?.visibility == android.view.View.VISIBLE) {
            contentView.visibility = android.view.View.GONE
            arrowView?.text = "▼"
        } else {
            contentView?.visibility = android.view.View.VISIBLE
            arrowView?.text = "▲"
        }
    }
}
