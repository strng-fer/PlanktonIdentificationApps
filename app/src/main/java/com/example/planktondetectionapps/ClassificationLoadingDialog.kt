package com.example.planktondetectionapps

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/**
 * Custom loading dialog for plankton classification process
 * Shows progress through multiple AI models with unique animations
 */
class ClassificationLoadingDialog(private val context: Context) {

    private var dialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null
    private var currentModelText: TextView? = null
    private var modelStatusContainer: LinearLayout? = null
    private var funFactText: TextView? = null

    // Animation views
    private var outerCircle: ImageView? = null
    private var innerCircle: ImageView? = null
    private var particle1: ImageView? = null
    private var particle2: ImageView? = null
    private var particle3: ImageView? = null

    // Model names for display
    private val modelNames = listOf(
        "MobileNet V3 Small",
        "MobileNet V3 Large",
        "ResNet50 V2",
        "ResNet101 V2",
        "EfficientNet V1 B0",
        "EfficientNet V2 B0",
        "ConvNeXt Tiny",
        "DenseNet 121",
        "Inception V3"
    )

    // Fun facts about plankton
    private val funFacts = listOf(
        "💡 Tahukah kamu? Plankton menghasilkan lebih dari 50% oksigen di atmosfer bumi!",
        "🌊 Plankton adalah dasar dari rantai makanan di lautan!",
        "🔬 Satu tetes air laut bisa mengandung jutaan plankton!",
        "🌍 Plankton berperan penting dalam siklus karbon global!",
        "✨ Beberapa plankton dapat bercahaya di kegelapan (bioluminescence)!",
        "🦐 Plankton terbesar adalah ubur-ubur yang bisa mencapai diameter 2 meter!",
        "🎯 AI dapat mengidentifikasi lebih dari 100 spesies plankton berbeda!",
        "🌡️ Plankton sangat sensitif terhadap perubahan suhu air laut!",
        "🔄 Plankton bermigrasi vertikal setiap hari mengikuti cahaya matahari!"
    )

    private var currentProgress = 0
    private var currentFactIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private var factUpdateRunnable: Runnable? = null

    fun show() {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_classification_loading, null)

        // Initialize views
        progressBar = view.findViewById(R.id.progressBar)
        progressText = view.findViewById(R.id.progressText)
        currentModelText = view.findViewById(R.id.currentModelText)
        modelStatusContainer = view.findViewById(R.id.modelStatusContainer)
        funFactText = view.findViewById(R.id.funFactText)

        // Animation views
        outerCircle = view.findViewById(R.id.outerCircle)
        innerCircle = view.findViewById(R.id.innerCircle)
        particle1 = view.findViewById(R.id.particle1)
        particle2 = view.findViewById(R.id.particle2)
        particle3 = view.findViewById(R.id.particle3)

        // Setup animations
        setupAnimations()

        // Setup model status list
        setupModelStatusList()

        // Setup fun fact rotation
        setupFunFactRotation()

        builder.setView(view)
        builder.setCancelable(false)

        dialog = builder.create()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.show()
    }

    private fun setupAnimations() {
        // Start rotating animations
        outerCircle?.let {
            val rotateAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate_clockwise)
            it.startAnimation(rotateAnimation)
        }

        innerCircle?.let {
            val rotateAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate_counter_clockwise)
            it.startAnimation(rotateAnimation)
        }

        // Start particle animations with delays
        particle1?.let {
            val particleAnimation = AnimationUtils.loadAnimation(context, R.anim.particle_float)
            it.startAnimation(particleAnimation)
        }

        particle2?.let {
            val particleAnimation = AnimationUtils.loadAnimation(context, R.anim.particle_float)
            handler.postDelayed({ it.startAnimation(particleAnimation) }, 500)
        }

        particle3?.let {
            val particleAnimation = AnimationUtils.loadAnimation(context, R.anim.particle_float)
            handler.postDelayed({ it.startAnimation(particleAnimation) }, 1000)
        }
    }

    private fun setupModelStatusList() {
        modelStatusContainer?.removeAllViews()

        for (i in modelNames.indices) {
            val statusView = LayoutInflater.from(context).inflate(
                android.R.layout.simple_list_item_1,
                modelStatusContainer,
                false
            )

            val textView = statusView.findViewById<TextView>(android.R.id.text1)
            textView.text = "⏳ ${modelNames[i]}"
            textView.textSize = 10f
            textView.setTextColor(context.getColor(android.R.color.darker_gray))

            modelStatusContainer?.addView(statusView)
        }
    }

    private fun setupFunFactRotation() {
        factUpdateRunnable = object : Runnable {
            override fun run() {
                if (currentFactIndex < funFacts.size) {
                    funFactText?.text = funFacts[currentFactIndex]
                    currentFactIndex = (currentFactIndex + 1) % funFacts.size
                    handler.postDelayed(this, 3000) // Change fact every 3 seconds
                }
            }
        }
        handler.post(factUpdateRunnable!!)
    }

    fun updateProgress(modelIndex: Int, modelName: String) {
        currentProgress = modelIndex + 1

        // Update progress bar
        progressBar?.progress = currentProgress
        progressText?.text = "$currentProgress / ${modelNames.size} Model Selesai"

        // Update current model text
        currentModelText?.text = "Memproses dengan $modelName..."

        // Update model status list
        updateModelStatusList(modelIndex)
    }

    private fun updateModelStatusList(completedIndex: Int) {
        modelStatusContainer?.let { container ->
            for (i in 0 until container.childCount) {
                val statusView = container.getChildAt(i)
                val textView = statusView.findViewById<TextView>(android.R.id.text1)

                when {
                    i < completedIndex -> {
                        textView.text = "✅ ${modelNames[i]}"
                        textView.setTextColor(context.getColor(android.R.color.holo_green_dark))
                    }
                    i == completedIndex -> {
                        textView.text = "🔄 ${modelNames[i]}"
                        textView.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                    }
                    else -> {
                        textView.text = "⏳ ${modelNames[i]}"
                        textView.setTextColor(context.getColor(android.R.color.darker_gray))
                    }
                }
            }
        }
    }

    fun updateFinalProcessing() {
        currentModelText?.text = "Menghitung hasil voting mayoritas..."
        progressBar?.progress = modelNames.size
        progressText?.text = "${modelNames.size} / ${modelNames.size} Model Selesai"

        // Mark all models as completed
        modelStatusContainer?.let { container ->
            for (i in 0 until container.childCount) {
                val statusView = container.getChildAt(i)
                val textView = statusView.findViewById<TextView>(android.R.id.text1)
                textView.text = "✅ ${modelNames[i]}"
                textView.setTextColor(context.getColor(android.R.color.holo_green_dark))
            }
        }
    }

    fun updateSingleModelProcessing(modelName: String) {
        currentModelText?.text = "Memproses dengan $modelName..."
        progressBar?.progress = 1
        progressBar?.max = 1
        progressText?.text = "Memproses model tunggal..."

        // Hide model status list for single model processing
        modelStatusContainer?.visibility = View.GONE

        // Show a different message for single model
        funFactText?.text = "⚡ Memproses dengan model $modelName untuk hasil yang cepat dan akurat!"
    }

    fun dismiss() {
        factUpdateRunnable?.let { handler.removeCallbacks(it) }
        dialog?.dismiss()
        dialog = null
    }

    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }
}
