package com.example.planktondetectionapps

import android.content.Context
import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * Main activity for the Plankton Detection Application
 * Handles camera capture, gallery selection, and AI model inference for plankton classification
 */
class MainActivity : AppCompatActivity() {

    /**
     * Enum untuk memilih jenis model AI yang akan digunakan
     */
    enum class ModelType {
        MOBILENET_V3_SMALL,
        MOBILENET_V3_LARGE,
        RESNET50_V2,
        RESNET101_V2,
        EFFICIENTNET_V1_B0,
        EFFICIENTNET_V2_B0,
        CONVNEXT_TINY,
        DENSENET121,
        INCEPTION_V3,
        MAJORITY_VOTING
    }

    // UI Components
    private var result: TextView? = null
    private var confidence: LinearLayout? = null
    private var defaultMessage: TextView? = null
    private var resultsTable: android.widget.TableLayout? = null
    private var modelInfo: TextView? = null
    private var pred1: TextView? = null
    private var pred2: TextView? = null
    private var pred3: TextView? = null
    private var prob1: TextView? = null
    private var prob2: TextView? = null
    private var prob3: TextView? = null
    private var imageView: ImageView? = null
    private var picture: Button? = null
    private var galleryButton: Button? = null
    private var saveButton: Button? = null
    private var feedbackButton: Button? = null

    // Custom dropdown UI elements
    private var customDropdownContainer: LinearLayout? = null
    private var dropdownOptions: LinearLayout? = null
    private var dropdownArrow: ImageView? = null
    private var selectedModelName: TextView? = null
    private var selectedModelDescription: TextView? = null
    private var option1: LinearLayout? = null
    private var option2: LinearLayout? = null
    private var option3: LinearLayout? = null
    private var option4: LinearLayout? = null
    private var option5: LinearLayout? = null
    private var option6: LinearLayout? = null
    private var option7: LinearLayout? = null
    private var option8: LinearLayout? = null
    private var option9: LinearLayout? = null

    // Navigation menu UI elements
    private var menuButton: android.widget.ImageButton? = null
    private var navigationMenu: LinearLayout? = null
    private var settingsOption: LinearLayout? = null
    private var historyOption: LinearLayout? = null
    private var aboutOption: LinearLayout? = null
    private var documentationOption: LinearLayout? = null
    private var isNavigationMenuOpen = false

    // AI Model configuration
    private var imageSize: Int = 224
    private var selectedModel: ModelType = ModelType.MOBILENET_V3_SMALL
    private var isDropdownOpen = false

    // Classification data storage
    private var currentBitmap: Bitmap? = null
    private var currentClassificationResult: String? = null
    private var currentConfidence: Float = 0f
    private var currentPhotoUri: Uri? = null
    private var currentHistoryEntryId: String? = null // Track current history entry for feedback

    // History Manager
    private lateinit var historyManager: HistoryManager

    // Activity result launchers
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var batchGalleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<String>

    /**
     * Inisialisasi activity dan setup UI components
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode for the entire application
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContentView(R.layout.activity_main)

        initializeViews()
        initializeLaunchers()
        setupCustomDropdown()
        setupNavigationMenu()
        setupButtonListeners()

        // Initialize history manager
        historyManager = HistoryManager(this)

        // Delay welcome dialog until after layout is completely finished
        // This prevents any flicker by ensuring everything is ready
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(object :
            android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remove listener to prevent multiple calls
                window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // Now show welcome dialog when everything is fully rendered
                showWelcomeDialog()
            }
        })
    }

    /**
     * Inisialisasi semua view components
     */
    private fun initializeViews() {
        result = findViewById(R.id.result)
        confidence = findViewById(R.id.confidence)
        defaultMessage = findViewById(R.id.defaultMessage)
        resultsTable = findViewById(R.id.resultsTable)
        modelInfo = findViewById(R.id.modelInfo)
        pred1 = findViewById(R.id.pred1)
        pred2 = findViewById(R.id.pred2)
        pred3 = findViewById(R.id.pred3)
        prob1 = findViewById(R.id.prob1)
        prob2 = findViewById(R.id.prob2)
        prob3 = findViewById(R.id.prob3)
        imageView = findViewById(R.id.imageView)
        picture = findViewById(R.id.button)
        galleryButton = findViewById(R.id.galleryButton)
        saveButton = findViewById(R.id.saveButton)
        feedbackButton = findViewById(R.id.feedbackButton)

        // Setup click listener untuk result TextView
        result?.setOnClickListener {
            if (!currentClassificationResult.isNullOrEmpty()) {
                PlanktonInfoManager.showPlanktonInfoPopup(this, currentClassificationResult!!)
            }
        }

        // Membuat result TextView terlihat clickable dengan styling yang tepat
        result?.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
        result?.setPadding(12, 8, 12, 8)
        result?.isClickable = true
        result?.isFocusable = true

        // Tambahkan visual hint bahwa text bisa diklik
        result?.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    view.alpha = 0.7f
                    false
                }

                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    view.alpha = 1.0f
                    false
                }

                else -> false
            }
        }

        // Initialize custom dropdown elements
        customDropdownContainer = findViewById(R.id.customDropdownContainer)
        dropdownOptions = findViewById(R.id.dropdownOptions)
        dropdownArrow = findViewById(R.id.dropdownArrow)
        selectedModelName = findViewById(R.id.selectedModelName)
        selectedModelDescription = findViewById(R.id.selectedModelDescription)
        option1 = findViewById(R.id.option1)
        option2 = findViewById(R.id.option2)
        option3 = findViewById(R.id.option3)
        option4 = findViewById(R.id.option4)
        option5 = findViewById(R.id.option5)
        option6 = findViewById(R.id.option6)
        option7 = findViewById(R.id.option7)
        option8 = findViewById(R.id.option8)
        option9 = findViewById(R.id.option9)

        // Initialize navigation menu elements
        menuButton = findViewById(R.id.menuButton)
        navigationMenu = findViewById(R.id.navigationMenu)
        settingsOption = findViewById(R.id.settingsOption)
        aboutOption = findViewById(R.id.aboutOption)
        documentationOption = findViewById(R.id.documentationOption)
        historyOption = findViewById(R.id.historyOption)
    }

    /**
     * Setup listener untuk button clicks
     */
    private fun setupButtonListeners() {
        picture?.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                showCameraSelectionDialog()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        galleryButton?.setOnClickListener {
            showGallerySelectionDialog()
        }

        saveButton?.setOnClickListener {
            if (currentBitmap != null && currentClassificationResult != null) {
                checkStoragePermissionAndSave()
            } else {
                showError("Tidak ada gambar atau hasil klasifikasi untuk disimpan.")
            }
        }

        feedbackButton?.setOnClickListener {
            showFeedbackDialog()
        }
    }

    /**
     * Setup dropdown untuk pemilihan model AI
     */
    private fun setupCustomDropdown() {
        customDropdownContainer?.setOnClickListener {
            toggleDropdown()
        }

        option1?.setOnClickListener {
            selectModel(
                ModelType.MOBILENET_V3_SMALL,
                "MobileNetV3 Small",
                "Model ringan dengan performa cepat"
            )
        }

        option2?.setOnClickListener {
            selectModel(
                ModelType.MOBILENET_V3_LARGE,
                "MobileNetV3 Large",
                "Model ringan dengan performa cepat dan lebih akurat"
            )
        }

        option3?.setOnClickListener {
            selectModel(
                ModelType.RESNET50_V2,
                "ResNet50 V2 (300 Data)",
                "Model menengah dengan akurasi tinggi"
            )
        }

        option4?.setOnClickListener {
            selectModel(
                ModelType.RESNET101_V2,
                "ResNet101 V2 (300 Data)",
                "Model tinggi dengan keakuratan sangat tinggi"
            )
        }

        option5?.setOnClickListener {
            selectModel(
                ModelType.EFFICIENTNET_V1_B0,
                "EfficientNet V1 B0 (300 Data)",
                "Model dengan efisiensi optimal"
            )
        }

        option6?.setOnClickListener {
            selectModel(
                ModelType.EFFICIENTNET_V2_B0,
                "EfficientNet V2 B0 (300 Data)",
                "Model terbaru dengan efisiensi optimal"
            )
        }

        option7?.setOnClickListener {
            selectModel(
                ModelType.CONVNEXT_TINY,
                "ConvNext Tiny",
                "Model modern dengan arsitektur ConvNext"
            )
        }

        option8?.setOnClickListener {
            selectModel(
                ModelType.DENSENET121,
                "DenseNet121",
                "Model dengan koneksi dense yang efisien"
            )
        }

        option9?.setOnClickListener {
            selectModel(
                ModelType.MAJORITY_VOTING,
                "Majority Voting",
                "Ensemble dari 9 model untuk akurasi maksimal"
            )
        }
    }

    /**
     * Toggle visibility dropdown model selection
     */
    private fun toggleDropdown() {
        if (isDropdownOpen) {
            dropdownOptions?.visibility = View.GONE
            dropdownArrow?.rotation = 0f
            isDropdownOpen = false
        } else {
            dropdownOptions?.visibility = View.VISIBLE
            dropdownArrow?.rotation = 180f
            isDropdownOpen = true
        }
    }

    /**
     * Pilih model AI yang akan digunakan
     */
    private fun selectModel(modelType: ModelType, modelName: String, modelDescription: String) {
        selectedModel = modelType
        selectedModelName?.text = modelName
        selectedModelDescription?.text = modelDescription

        // Close dropdown after selection
        dropdownOptions?.visibility = View.GONE
        dropdownArrow?.rotation = 0f
        isDropdownOpen = false

        Toast.makeText(this, "Model dipilih: $modelName", Toast.LENGTH_SHORT).show()
    }

    /**
     * Cek permission storage dan simpan gambar
     */
    private fun checkStoragePermissionAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGallery()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                saveImageToGallery()
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * Tampilkan dialog selamat datang saat aplikasi pertama kali dibuka
     */
    private fun showWelcomeDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_welcome, null)

        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(false)

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val startButton = dialogView.findViewById<Button>(R.id.welcomeStartButton)
        startButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Tampilkan dialog error dengan pesan tertentu
     */
    private fun showErrorDialog(message: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Gagal")
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert)
        dialogBuilder.create().show()
    }

    /**
     * Tampilkan dialog sukses dengan pesan tertentu
     */
    private fun showSuccessDialog(message: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Berhasil Disimpan")
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.setIcon(android.R.drawable.ic_dialog_info)
        dialogBuilder.create().show()
    }

    /**
     * Inisialisasi ActivityResultLaunchers untuk kamera, galeri, dan permissions
     */
    private fun initializeLaunchers() {
        // Camera launcher - using full resolution capture with file output
        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    try {
                        currentPhotoUri?.let { photoUri ->
                            android.util.Log.d(
                                "PlanktonDebug",
                                "Loading full resolution image from URI"
                            )
                            val bitmap = loadHighQualityImageFromUri(photoUri)
                            android.util.Log.d(
                                "PlanktonDebug",
                                "Full resolution camera image size: ${bitmap.width}x${bitmap.height}"
                            )

                            currentBitmap = bitmap
                            imageView?.setImageBitmap(bitmap)

                            val aiImage = createHighQualitySquareImage(bitmap, imageSize)
                            classifyImage(aiImage)

                            cleanupTempFile(photoUri)
                            return@registerForActivityResult
                        }

                        // Fallback to thumbnail if no URI
                        val extras = result.data?.extras

                        @Suppress("DEPRECATION")
                        val photo = extras?.get("data") as? Bitmap

                        if (photo != null) {
                            android.util.Log.d(
                                "PlanktonDebug",
                                "Fallback to thumbnail - size: ${photo.width}x${photo.height}"
                            )
                            showError("Kualitas gambar rendah (thumbnail). Menggunakan kualitas yang tersedia.")

                            currentBitmap = photo
                            imageView?.setImageBitmap(photo)
                            val aiImage = createHighQualitySquareImage(photo, imageSize)
                            classifyImage(aiImage)
                        } else {
                            showError("Gagal mengambil gambar dari kamera.")
                        }
                    } catch (e: Exception) {
                        showError("Error processing image: ${e.message}")
                        currentPhotoUri?.let { cleanupTempFile(it) }
                    }
                } else {
                    showError("Pengambilan gambar dibatalkan.")
                    currentPhotoUri?.let { cleanupTempFile(it) }
                }
            }

        // Gallery launcher
        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    try {
                        val imageUri = result.data?.data
                        if (imageUri != null) {
                            val bitmap = loadHighQualityImageFromUri(imageUri)
                            android.util.Log.d(
                                "PlanktonDebug",
                                "Gallery image size: ${bitmap.width}x${bitmap.height}"
                            )

                            currentBitmap = bitmap
                            imageView?.setImageBitmap(bitmap)

                            val aiImage = createHighQualitySquareImage(bitmap, imageSize)
                            classifyImage(aiImage)
                        } else {
                            showError("Gagal mengambil gambar dari galeri.")
                        }
                    } catch (e: Exception) {
                        showError("Error processing gallery image: ${e.message}")
                    }
                } else {
                    showError("Pemilihan gambar dibatalkan.")
                }
            }

        // Batch gallery launcher
        batchGalleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    try {
                        val imageUris = mutableListOf<Uri>()

                        // Handle multiple selection
                        val clipData = result.data?.clipData
                        if (clipData != null) {
                            // Multiple images selected
                            for (i in 0 until clipData.itemCount) {
                                val item = clipData.getItemAt(i)
                                imageUris.add(item.uri)
                            }
                        } else {
                            // Single image selected (fallback)
                            result.data?.data?.let { uri ->
                                imageUris.add(uri)
                            }
                        }

                        if (imageUris.isNotEmpty()) {
                            // Launch batch processing activity
                            val intent = Intent(this, BatchProcessingActivity::class.java)
                            intent.putParcelableArrayListExtra("imageUris", ArrayList(imageUris))
                            intent.putExtra("selectedModel", selectedModel)
                            startActivity(intent)
                        } else {
                            showError("Tidak ada gambar yang dipilih.")
                        }
                    } catch (e: Exception) {
                        showError("Error processing gallery images: ${e.message}")
                    }
                } else {
                    showError("Pemilihan gambar dibatalkan.")
                }
            }

        // Permission launcher
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    showCameraSelectionDialog()
                } else {
                    showError("Izin kamera diperlukan untuk mengambil foto.")
                }
            }

        // Storage permission launcher
        storagePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    saveImageToGallery()
                } else {
                    showError("Izin penyimpanan diperlukan untuk menyimpan gambar ke galeri.")
                }
            }
    }

    /**
     * Tampilkan pesan error dan reset UI
     */
    private fun showError(message: String) {
        showErrorDialog(message)
        result?.text = "Terjadi kesalahan"
        confidence?.visibility = View.GONE
        saveButton?.isEnabled = false
    }

    /**
     * Load labels dari file assets
     */
    private fun loadLabels(context: Context): List<String> {
        val labels = mutableListOf<String>()
        try {
            context.assets.open("labels.txt").bufferedReader().useLines { lines ->
                lines.forEach { labels.add(it) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            labels.add("Unknown")
        }
        return labels
    }

    /**
     * Klasifikasi gambar menggunakan model AI yang dipilih
     */
    private fun classifyImage(image: Bitmap) {
        try {
            android.util.Log.d("PlanktonDebug", "=== CLASSIFICATION START ===")
            android.util.Log.d("PlanktonDebug", "Selected model: ${selectedModel.name}")

            // Special handling for majority voting
            if (selectedModel == ModelType.MAJORITY_VOTING) {
                performMajorityVoting(image)
                return
            }

            // Show loading dialog for single model classification too
            val loadingDialog = ClassificationLoadingDialog(this)
            loadingDialog.show()

            // Update dialog to show single model processing
            loadingDialog.updateSingleModelProcessing(formatModelName(selectedModel))

            // Process in background thread
            Thread {
                try {
                    val inputFeature0 =
                        TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

                    // Choose preprocessing based on model type
                    val byteBuffer = when (selectedModel) {
                        ModelType.MOBILENET_V3_SMALL -> preprocessImageForMobileNetV3BuildIn(image)
                        ModelType.MOBILENET_V3_LARGE -> preprocessImageForMobileNetV3BuildIn(image)
                        ModelType.RESNET50_V2 -> preprocessImageForResNetV2(image)
                        ModelType.RESNET101_V2 -> preprocessImageForResNetV2(image)
                        ModelType.EFFICIENTNET_V1_B0 -> preprocessImageForEfficientNetBuildIn(image)
                        ModelType.EFFICIENTNET_V2_B0 -> preprocessImageForEfficientNetBuildIn(image)
                        ModelType.CONVNEXT_TINY -> preprocessImageForConvNext(image)
                        ModelType.DENSENET121 -> preprocessImageForDenseNet(image)
                        ModelType.INCEPTION_V3 -> preprocessImageForInception(image)
                        ModelType.MAJORITY_VOTING -> throw IllegalStateException("Majority voting should be handled separately")
                    }
                    inputFeature0.loadBuffer(byteBuffer)

                    // Run inference with selected model
                    val confidences = try {
                        when (selectedModel) {
                            ModelType.MOBILENET_V3_SMALL -> {
                                try {
                                    val model =
                                        com.example.planktondetectionapps.ml.MobileNetV3Small.newInstance(
                                            applicationContext
                                        )
                                    val outputs = model.process(inputFeature0)
                                    val result = outputs.outputFeature0AsTensorBuffer.floatArray
                                    model.close()
                                    result
                                } catch (e: Exception) {
                                    android.util.Log.e(
                                        "PlanktonDebug",
                                        "MobileNetV3Small model not found",
                                        e
                                    )
                                    runOnUiThread {
                                        loadingDialog.dismiss()
                                        showError("Model MobileNetV3Small tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                                    }
                                    return@Thread
                                }
                            }

                            ModelType.MOBILENET_V3_LARGE -> {
                                try {
                                    val model =
                                        com.example.planktondetectionapps.ml.MobileNetV3LargeWith300Data.newInstance(
                                            applicationContext
                                        )
                                    val outputs = model.process(inputFeature0)
                                    val result = outputs.outputFeature0AsTensorBuffer.floatArray
                                    model.close()
                                    result
                                } catch (e: Exception) {
                                    android.util.Log.e(
                                        "PlanktonDebug",
                                        "MobileNetV3Small model not found",
                                        e
                                    )
                                    runOnUiThread {
                                        loadingDialog.dismiss()
                                        showError("Model MobileNetV3Large tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                                    }
                                    return@Thread
                                }
                            }

                            ModelType.RESNET50_V2 -> {
                                try {
                                    val modelClass = try {
                                        Class.forName("com.example.planktondetectionapps.ml.ResNet50V2")
                                    } catch (_: ClassNotFoundException) {
                                        try {
                                            Class.forName("com.example.planktondetectionapps.ml.ResNet50V2with300Data")
                                        } catch (_: ClassNotFoundException) {
                                            Class.forName("com.example.planktondetectionapps.ml.Resnet50v2")
                                        }
                                    }

                                    val modelInstance =
                                        modelClass.getMethod("newInstance", Context::class.java)
                                            .invoke(null, applicationContext)
                                    val processMethod =
                                        modelClass.getMethod("process", TensorBuffer::class.java)
                                    val outputs = processMethod.invoke(modelInstance, inputFeature0)
                                    val outputMethod =
                                        outputs::class.java.getMethod("getOutputFeature0AsTensorBuffer")
                                    val tensorBuffer = outputMethod.invoke(outputs) as TensorBuffer
                                    val result = tensorBuffer.floatArray

                                    val closeMethod = modelClass.getMethod("close")
                                    closeMethod.invoke(modelInstance)

                                    result
                                } catch (e: Exception) {
                                    android.util.Log.e(
                                        "PlanktonDebug",
                                        "ResNet50V2 model not found",
                                        e
                                    )
                                    runOnUiThread {
                                        loadingDialog.dismiss()
                                        showError("Model ResNet50V2 tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                                    }
                                    return@Thread
                                }
                            }

                            ModelType.RESNET101_V2 -> {
                                try {
                                    val modelClass = try {
                                        Class.forName("com.example.planktondetectionapps.ml.ResNet101V2")
                                    } catch (_: ClassNotFoundException) {
                                        try {
                                            Class.forName("com.example.planktondetectionapps.ml.ResNet101V2with300Data")
                                        } catch (_: ClassNotFoundException) {
                                            Class.forName("com.example.planktondetectionapps.ml.Resnet101v2")
                                        }
                                    }

                                    val modelInstance =
                                        modelClass.getMethod("newInstance", Context::class.java)
                                            .invoke(null, applicationContext)
                                    val processMethod =
                                        modelClass.getMethod("process", TensorBuffer::class.java)
                                    val outputs = processMethod.invoke(modelInstance, inputFeature0)
                                    val outputMethod =
                                        outputs::class.java.getMethod("getOutputFeature0AsTensorBuffer")
                                    val tensorBuffer = outputMethod.invoke(outputs) as TensorBuffer
                                    val result = tensorBuffer.floatArray

                                    val closeMethod = modelClass.getMethod("close")
                                    closeMethod.invoke(modelInstance)

                                    result
                                } catch (e: Exception) {
                                    android.util.Log.e(
                                        "PlanktonDebug",
                                        "ResNet101V2 model not found",
                                        e
                                    )
                                    runOnUiThread {
                                        loadingDialog.dismiss()
                                        showError("Model ResNet101V2 tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                                    }
                                    return@Thread
                                }
                            }

                            ModelType.EFFICIENTNET_V1_B0 -> {
                                try {
                                    val modelClass = try {
                                        Class.forName("com.example.planktondetectionapps.ml.EfficientNetV1")
                                    } catch (_: ClassNotFoundException) {
                                        try {
                                            Class.forName("com.example.planktondetectionapps.ml.EfficientNetV1with300Data")
                                        } catch (_: ClassNotFoundException) {
                                            Class.forName("com.example.planktondetectionapps.ml.Efficientnetv1")
                                        }
                                    }

                                    val modelInstance =
                                        modelClass.getMethod("newInstance", Context::class.java)
                                            .invoke(null, applicationContext)
                                    val processMethod =
                                        modelClass.getMethod("process", TensorBuffer::class.java)
                                    val outputs = processMethod.invoke(modelInstance, inputFeature0)
                                    val outputMethod =
                                        outputs::class.java.getMethod("getOutputFeature0AsTensorBuffer")
                                    val tensorBuffer = outputMethod.invoke(outputs) as TensorBuffer
                                    val result = tensorBuffer.floatArray

                                    val closeMethod = modelClass.getMethod("close")
                                    closeMethod.invoke(modelInstance)

                                    result
                                } catch (e: Exception) {
                                    android.util.Log.e(
                                        "PlanktonDebug",
                                        "EfficientNetV1B0 model not found",
                                        e
                                    )
                                    runOnUiThread {
                                        loadingDialog.dismiss()
                                        showError("Model EfficientNetV1B0 tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                                    }
                                    return@Thread
                                }
                            }

                            ModelType.EFFICIENTNET_V2_B0 -> {
                                try {
                                    val modelClass = try {
                                        Class.forName("com.example.planktondetectionapps.ml.EfficientNetV2B0")
                                    } catch (_: ClassNotFoundException) {
                                        try {
                                            Class.forName("com.example.planktondetectionapps.ml.EfficientNetV2B0with300Data")
                                        } catch (_: ClassNotFoundException) {
                                            Class.forName("com.example.planktondetectionapps.ml.Efficientnetv2b0")
                                        }
                                    }

                                    val modelInstance =
                                        modelClass.getMethod("newInstance", Context::class.java)
                                            .invoke(null, applicationContext)
                                    val processMethod =
                                        modelClass.getMethod("process", TensorBuffer::class.java)
                                    val outputs = processMethod.invoke(modelInstance, inputFeature0)
                                    val outputMethod =
                                        outputs::class.java.getMethod("getOutputFeature0AsTensorBuffer")
                                    val tensorBuffer = outputMethod.invoke(outputs) as TensorBuffer
                                    val result = tensorBuffer.floatArray

                                    val closeMethod = modelClass.getMethod("close")
                                    closeMethod.invoke(modelInstance)

                                    result
                                } catch (e: Exception) {
                                    android.util.Log.e(
                                        "PlanktonDebug",
                                        "EfficientNetV2B0 model not found",
                                        e
                                    )
                                    runOnUiThread {
                                        loadingDialog.dismiss()
                                        showError("Model EfficientNetV2B0 tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                                    }
                                    return@Thread
                                }
                            }

                            ModelType.CONVNEXT_TINY -> {
                                try {
                                    val model =
                                        com.example.planktondetectionapps.ml.ConvNeXtTinywith300Data.newInstance(
                                            applicationContext
                                        )
                                    val outputs = model.process(inputFeature0)
                                    val result = outputs.outputFeature0AsTensorBuffer.floatArray
                                    model.close()
                                    result
                                } catch (e: Exception) {
                                    android.util.Log.e(
                                        "PlanktonDebug",
                                        "ConvNextTiny model not found",
                                        e
                                    )
                                    runOnUiThread {
                                        loadingDialog.dismiss()
                                        showError("Model ConvNextTiny tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                                    }
                                    return@Thread
                                }
                            }

                            ModelType.DENSENET121 -> {
                                try {
                                    val model =
                                        com.example.planktondetectionapps.ml.DenseNet121with300Data.newInstance(
                                            applicationContext
                                        )
                                    val outputs = model.process(inputFeature0)
                                    val result = outputs.outputFeature0AsTensorBuffer.floatArray
                                    model.close()
                                    result
                                } catch (e: Exception) {
                                    android.util.Log.e(
                                        "PlanktonDebug",
                                        "DenseNet121 model not found",
                                        e
                                    )
                                    runOnUiThread {
                                        loadingDialog.dismiss()
                                        showError("Model DenseNet121 tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                                    }
                                    return@Thread
                                }
                            }

                            ModelType.INCEPTION_V3 -> {
                                try {
                                    val model =
                                        com.example.planktondetectionapps.ml.InceptionV3with300Data.newInstance(
                                            applicationContext
                                        )
                                    val outputs = model.process(inputFeature0)
                                    val result = outputs.outputFeature0AsTensorBuffer.floatArray
                                    model.close()
                                    result
                                } catch (e: Exception) {
                                    android.util.Log.e(
                                        "PlanktonDebug",
                                        "InceptionV3 model not found",
                                        e
                                    )
                                    runOnUiThread {
                                        loadingDialog.dismiss()
                                        showError("Model InceptionV3 tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                                    }
                                    return@Thread
                                }
                            }

                            ModelType.MAJORITY_VOTING -> {
                                // This should never be reached as majority voting is handled separately
                                throw IllegalStateException("Majority voting should be handled separately")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PlanktonDebug", "Error running model inference", e)
                        runOnUiThread {
                            loadingDialog.dismiss()
                            showError("Error menjalankan inferensi model: ${e.message}")
                        }
                        return@Thread
                    }

                    // Process results on UI thread
                    runOnUiThread {
                        // Process classification results first, then dismiss dialog
                        processClassificationResults(confidences) {
                            // Dismiss dialog only after UI updates are complete
                            loadingDialog.dismiss()
                        }
                    }

                } catch (e: Exception) {
                    android.util.Log.e("PlanktonDebug", "Error in background classification", e)
                    runOnUiThread {
                        showError("Error classifying image with ${selectedModel.name}: ${e.message}")
                    }
                }
            }.start()

        } catch (e: Exception) {
            android.util.Log.e("PlanktonDebug", "Error in classifyImage", e)
            showError("Error classifying image with ${selectedModel.name}: ${e.message}")
        }
    }

    /**
     * Proses hasil klasifikasi dan update UI
     */
    private fun processClassificationResults(confidences: FloatArray, onComplete: (() -> Unit)? = null) {
        android.util.Log.d("PlanktonDebug", "=== MODEL OUTPUT DEBUG ===")
        android.util.Log.d("PlanktonDebug", "Total classes: ${confidences.size}")

        val sumConfidences = confidences.sum()
        android.util.Log.d("PlanktonDebug", "Sum of all confidences: $sumConfidences")

        val finalConfidences = if (sumConfidences > 0.99 && sumConfidences < 1.01) {
            android.util.Log.d("PlanktonDebug", "Using raw output (already probabilities)")
            confidences
        } else {
            android.util.Log.d("PlanktonDebug", "Applying softmax to raw logits")
            applySoftmax(confidences)
        }

        var maxPos = 0
        var maxConfidence = 0f
        for (i in finalConfidences.indices) {
            if (finalConfidences[i] > maxConfidence) {
                maxConfidence = finalConfidences[i]
                maxPos = i
            }
        }

        val classes = loadLabels(this)
        android.util.Log.d("PlanktonDebug", "Predicted class index: $maxPos")
        android.util.Log.d("PlanktonDebug", "Max confidence: $maxConfidence")

        if (maxPos < classes.size) {
            currentClassificationResult = classes[maxPos]
            currentConfidence = maxConfidence

            result?.text = classes[maxPos]

            // Update UI for table results
            if (confidence?.visibility != View.VISIBLE) {
                confidence?.visibility = View.VISIBLE
            }

            defaultMessage?.visibility = View.GONE
            resultsTable?.visibility = View.VISIBLE

            // Update model info with formatted name and show it
            modelInfo?.text = "Model: ${formatModelName(selectedModel)}"
            modelInfo?.visibility = View.VISIBLE

            // Update predictions and probabilities
            val top3 = finalConfidences.mapIndexed { index, confidence ->
                Pair(index, confidence)
            }.sortedByDescending { it.second }.take(3)

            if (top3.size > 0) {
                pred1?.text = classes[top3[0].first]
                prob1?.text = String.format(Locale.getDefault(), "%.1f%%", top3[0].second * 100)
            }
            if (top3.size > 1) {
                pred2?.text = classes[top3[1].first]
                prob2?.text = String.format(Locale.getDefault(), "%.1f%%", top3[1].second * 100)
            }
            if (top3.size > 2) {
                pred3?.text = classes[top3[2].first]
                prob3?.text = String.format(Locale.getDefault(), "%.1f%%", top3[2].second * 100)
            }

            saveButton?.isEnabled = true

            // Show and enable feedback section
            val feedbackSection = findViewById<LinearLayout>(R.id.feedbackSection)
            feedbackSection?.visibility = View.VISIBLE

            feedbackButton?.visibility = View.VISIBLE
            feedbackButton?.isEnabled = true

            // Save to history automatically
            saveToHistory()

            // Add delay to ensure all UI updates are complete before calling onComplete
            Handler(Looper.getMainLooper()).postDelayed({
                onComplete?.invoke()
            }, 400) // 400ms delay to ensure smooth transition and all UI updates are complete

        } else {
            showError("Error: Invalid classification result")
            onComplete?.invoke()
        }
    }

    /**
     * Format model name untuk ditampilkan dengan lebih rapi
     */
    private fun formatModelName(modelType: ModelType): String {
        return when (modelType) {
            ModelType.MOBILENET_V3_SMALL -> "MobileNet V3 Small"
            ModelType.MOBILENET_V3_LARGE -> "MobileNet V3 Large"
            ModelType.RESNET50_V2 -> "ResNet50 V2"
            ModelType.RESNET101_V2 -> "ResNet101 V2"
            ModelType.EFFICIENTNET_V1_B0 -> "EfficientNet V1 B0"
            ModelType.EFFICIENTNET_V2_B0 -> "EfficientNet V2 B0"
            ModelType.CONVNEXT_TINY -> "ConvNeXt Tiny"
            ModelType.DENSENET121 -> "DenseNet121"
            ModelType.INCEPTION_V3 -> "Inception V3"
            ModelType.MAJORITY_VOTING -> "Majority Voting (9 Models)"
        }
    }

    /**
     * Apply softmax function to convert logits to probabilities
     */
    private fun applySoftmax(logits: FloatArray): FloatArray {
        val result = FloatArray(logits.size)
        val maxLogit = logits.maxOrNull() ?: 0f

        var sumExp = 0f
        for (i in logits.indices) {
            result[i] = kotlin.math.exp(logits[i] - maxLogit)
            sumExp += result[i]
        }

        for (i in result.indices) {
            result[i] = result[i] / sumExp
        }

        return result
    }

    /**
     * Preprocessing untuk MobileNetV3 dengan built-in preprocessing
     */
    private fun preprocessImageForMobileNetV3BuildIn(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d(
            "PlanktonDebug",
            "Processing image for MobileNetV3 with built-in preprocessing"
        )

        var pixel = 0
        for (y in 0 until imageSize) {
            for (x in 0 until imageSize) {
                val value = intValues[pixel++]

                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                byteBuffer.putFloat(red.toFloat())
                byteBuffer.putFloat(green.toFloat())
                byteBuffer.putFloat(blue.toFloat())
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Preprocessing untuk ResNetV2 dengan normalisasi [-1, 1]
     */
    private fun preprocessImageForResNetV2(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d("PlanktonDebug", "Using ResNetV2 preprocessing: scaling to [-1, 1]")

        var pixel = 0
        for (y in 0 until imageSize) {
            for (x in 0 until imageSize) {
                val value = intValues[pixel++]

                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                byteBuffer.putFloat((red / 127.5f) - 1.0f)
                byteBuffer.putFloat((green / 127.5f) - 1.0f)
                byteBuffer.putFloat((blue / 127.5f) - 1.0f)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Preprocessing untuk EfficientNetV2 dengan built-in preprocessing
     */
    private fun preprocessImageForEfficientNetBuildIn(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d(
            "PlanktonDebug",
            "Processing image for EfficientNetV2 with built-in preprocessing"
        )

        var pixel = 0
        for (y in 0 until imageSize) {
            for (x in 0 until imageSize) {
                val value = intValues[pixel++]

                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                byteBuffer.putFloat(red.toFloat())
                byteBuffer.putFloat(green.toFloat())
                byteBuffer.putFloat(blue.toFloat())
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Preprocessing untuk ConvNext dengan normalisasi [-1, 1]
     */
    private fun preprocessImageForConvNext(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d("PlanktonDebug", "Using ConvNext preprocessing: scaling to [-1, 1]")

        var pixel = 0
        for (y in 0 until imageSize) {
            for (x in 0 until imageSize) {
                val value = intValues[pixel++]

                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                byteBuffer.putFloat((red / 127.5f) - 1.0f)
                byteBuffer.putFloat((green / 127.5f) - 1.0f)
                byteBuffer.putFloat((blue / 127.5f) - 1.0f)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Preprocessing untuk DenseNet dengan normalisasi [-1, 1]
     */
    private fun preprocessImageForDenseNet(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d("PlanktonDebug", "Using DenseNet preprocessing: scaling to [-1, 1]")

        var pixel = 0
        for (y in 0 until imageSize) {
            for (x in 0 until imageSize) {
                val value = intValues[pixel++]

                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                byteBuffer.putFloat((red / 127.5f) - 1.0f)
                byteBuffer.putFloat((green / 127.5f) - 1.0f)
                byteBuffer.putFloat((blue / 127.5f) - 1.0f)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Preprocessing untuk Inception dengan ukuran tetap 299x299
     */
    private fun preprocessImageForInception(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 299 * 299 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, 299, 299, true)
        val intValues = IntArray(299 * 299)
        scaledBitmap.getPixels(intValues, 0, 299, 0, 0, 299, 299)

        android.util.Log.d(
            "PlanktonDebug",
            "Processing image for Inception with fixed size 299x299"
        )

        var pixel = 0
        for (y in 0 until 299) {
            for (x in 0 until 299) {
                val value = intValues[pixel++]

                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                byteBuffer.putFloat((red / 127.5f) - 1.0f)
                byteBuffer.putFloat((green / 127.5f) - 1.0f)
                byteBuffer.putFloat((blue / 127.5f) - 1.0f)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Tampilkan dialog pemilihan kamera
     */
    private fun showCameraSelectionDialog() {
        try {
            val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            val availableCameras = getAvailableCameras(cameraManager)

            if (availableCameras.isEmpty()) {
                launchDefaultCamera()
                return
            }

            val filteredCameras = availableCameras.filter { camera ->
                camera.second == "Kamera Default"
            }

            if (filteredCameras.size == 1) {
                launchDefaultCamera()
                return
            }

            if (filteredCameras.isEmpty()) {
                launchDefaultCamera()
                return
            }

            val cameraNames = filteredCameras.map { it.second }.toTypedArray()

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Pilih Kamera")
                .setIcon(R.drawable.ic_camera_capture)
                .setItems(cameraNames) { _, _ ->
                    launchDefaultCamera()
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } catch (_: Exception) {
            launchDefaultCamera()
        }
    }

    /**
     * Dapatkan list kamera yang tersedia
     */
    private fun getAvailableCameras(cameraManager: CameraManager): List<Pair<String, String>> {
        val cameras = mutableListOf<Pair<String, String>>()

        try {
            val cameraIdList = cameraManager.cameraIdList

            for (cameraId in cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                when (facing) {
                    CameraCharacteristics.LENS_FACING_BACK -> {
                        cameras.add(Pair(cameraId, "Kamera Default"))
                    }
                }
            }
        } catch (_: Exception) {
            cameras.add(Pair("0", "Kamera Default"))
        }

        return cameras
    }

    /**
     * Launch kamera default
     */
    private fun launchDefaultCamera() {
        try {
            val photoFile = createImageFile()
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            cameraLauncher.launch(cameraIntent)
            Toast.makeText(this, "Membuka kamera untuk foto resolusi penuh...", Toast.LENGTH_SHORT)
                .show()
        } catch (e: Exception) {
            showError("Error launching camera: ${e.message}")
        }
    }

    /**
     * Buat file temporary untuk capture kamera
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    /**
     * Hapus file temporary setelah diproses
     */
    private fun cleanupTempFile(uri: Uri) {
        try {
            val file = File(uri.path ?: return)
            if (file.exists()) {
                file.delete()
                android.util.Log.d("PlanktonDebug", "Temporary file cleaned up: ${file.name}")
            }
        } catch (e: Exception) {
            android.util.Log.e("PlanktonDebug", "Error cleaning up temp file", e)
        }
    }

    /**
     * Simpan gambar hasil klasifikasi ke galeri
     */
    private fun saveImageToGallery() {
        if (currentBitmap == null || currentClassificationResult == null) {
            showError("Tidak ada gambar atau hasil klasifikasi untuk disimpan.")
            return
        }

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${currentClassificationResult}_${timeStamp}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveImageToGalleryModern(fileName)
            } else {
                saveImageToGalleryLegacy(fileName)
            }
        } catch (e: Exception) {
            android.util.Log.e("PlanktonDebug", "Error saving image to gallery", e)
            showError("Gagal menyimpan gambar: ${e.message}")
        }
    }

    /**
     * Simpan gambar ke galeri untuk Android 10+
     */
    private fun saveImageToGalleryModern(fileName: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/PlanktonDetection"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
            outputStream?.use { stream ->
                currentBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)

            showSuccessDialog(
                "Gambar berhasil disimpan ke galeri dengan nama:\n$fileName\n\nKlasifikasi: $currentClassificationResult\nTingkat kepercayaan: ${
                    String.format(
                        Locale.getDefault(),
                        "%.1f%%",
                        currentConfidence * 100
                    )
                }"
            )
        } else {
            showError("Gagal menyimpan gambar ke galeri.")
        }
    }

    /**
     * Simpan gambar ke galeri untuk Android 9 dan bawah
     */
    private fun saveImageToGalleryLegacy(fileName: String) {
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val planktonDir = File(picturesDir, "PlanktonDetection")

        if (!planktonDir.exists()) {
            planktonDir.mkdirs()
        }

        val imageFile = File(planktonDir, fileName)
        val outputStream = FileOutputStream(imageFile)

        outputStream.use { stream ->
            currentBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        // Notify media scanner about the new file
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = Uri.fromFile(imageFile)
        sendBroadcast(mediaScanIntent)

        showSuccessDialog(
            "Gambar berhasil disimpan ke galeri dengan nama:\n$fileName\n\nKlasifikasi: $currentClassificationResult\nTingkat kepercayaan: ${
                String.format(
                    Locale.getDefault(),
                    "%.1f%%",
                    currentConfidence * 100
                )
            }"
        )
    }

    /**
     * Load gambar berkualitas tinggi dari URI
     */
    private fun loadHighQualityImageFromUri(uri: Uri): Bitmap {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inSampleSize = 1
                    inMutable = true
                }
                val inputStream = contentResolver.openInputStream(uri)
                android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                    ?: throw Exception("Failed to decode image from URI")
            }
        } catch (e: Exception) {
            android.util.Log.e("PlanktonDebug", "Error loading image from URI", e)
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    /**
     * Buat gambar persegi berkualitas tinggi untuk AI processing
     */
    private fun createHighQualitySquareImage(originalBitmap: Bitmap, targetSize: Int): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height

        val sourceBitmap = if (originalBitmap.config != Bitmap.Config.ARGB_8888) {
            originalBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            originalBitmap
        }

        val squareSize = min(width, height)
        val xOffset = (width - squareSize) / 2
        val yOffset = (height - squareSize) / 2

        val squareBitmap = Bitmap.createBitmap(
            sourceBitmap,
            xOffset,
            yOffset,
            squareSize,
            squareSize
        )

        if (squareSize == targetSize) {
            return squareBitmap
        }

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val scaledBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(scaledBitmap)

        val srcRect = android.graphics.Rect(0, 0, squareSize, squareSize)
        val dstRect = android.graphics.Rect(0, 0, targetSize, targetSize)

        canvas.drawBitmap(squareBitmap, srcRect, dstRect, paint)

        return scaledBitmap
    }

    /**
     * Setup navigation menu functionality
     */
    private fun setupNavigationMenu() {
        navigationMenu?.visibility = View.GONE

        menuButton?.setOnClickListener {
            if (isNavigationMenuOpen) {
                navigationMenu?.visibility = View.GONE
                isNavigationMenuOpen = false
            } else {
                navigationMenu?.visibility = View.VISIBLE
                isNavigationMenuOpen = true
            }
        }

        settingsOption?.setOnClickListener {
            navigationMenu?.visibility = View.GONE
            isNavigationMenuOpen = false
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        aboutOption?.setOnClickListener {
            navigationMenu?.visibility = View.GONE
            isNavigationMenuOpen = false
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        documentationOption?.setOnClickListener {
            navigationMenu?.visibility = View.GONE
            isNavigationMenuOpen = false
            val intent = Intent(this, DocumentationActivity::class.java)
            startActivity(intent)
        }

        historyOption?.setOnClickListener {
            navigationMenu?.visibility = View.GONE
            isNavigationMenuOpen = false
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Tampilkan dialog pemilihan galeri (single atau batch) dengan UI modern
     */
    private fun showGallerySelectionDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_gallery_selection, null)

        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(true)

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Setup click listeners for options
        val singleImageOption = dialogView.findViewById<LinearLayout>(R.id.singleImageOption)
        val batchProcessingOption =
            dialogView.findViewById<LinearLayout>(R.id.batchProcessingOption)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        singleImageOption.setOnClickListener {
            dialog.dismiss()
            // Single image selection
            val galleryIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(galleryIntent)
        }

        batchProcessingOption.setOnClickListener {
            dialog.dismiss()
            // Batch image selection
            val batchGalleryIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            batchGalleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            batchGalleryLauncher.launch(batchGalleryIntent)
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Perform majority voting for classification
     */
    private fun performMajorityVoting(image: Bitmap) {
        // Show loading dialog
        val loadingDialog = ClassificationLoadingDialog(this)
        loadingDialog.show()

        // Process in background thread to avoid blocking UI
        Thread {
            try {
                // Prepare the image for all models in the ensemble
                val inputFeature0 =
                    TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
                val byteBuffer = preprocessImageForMobileNetV3BuildIn(image)
                inputFeature0.loadBuffer(byteBuffer)

                // Collect predictions from all models
                val predictions = mutableListOf<FloatArray>()

                try {
                    // MobileNetV3 Small
                    runOnUiThread { loadingDialog.updateProgress(0, "MobileNet V3 Small") }
                    val model1 = com.example.planktondetectionapps.ml.MobileNetV3Small.newInstance(
                        applicationContext
                    )
                    val output1 = model1.process(inputFeature0)
                    predictions.add(output1.outputFeature0AsTensorBuffer.floatArray)
                    model1.close()
                } catch (e: Exception) {
                    android.util.Log.e("PlanktonDebug", "Error running MobileNetV3Small", e)
                }

                try {
                    // MobileNetV3 Large
                    runOnUiThread { loadingDialog.updateProgress(1, "MobileNet V3 Large") }
                    val model2 =
                        com.example.planktondetectionapps.ml.MobileNetV3LargeWith300Data.newInstance(
                            applicationContext
                        )
                    val output2 = model2.process(inputFeature0)
                    predictions.add(output2.outputFeature0AsTensorBuffer.floatArray)
                    model2.close()
                } catch (e: Exception) {
                    android.util.Log.e("PlanktonDebug", "Error running MobileNetV3Large", e)
                }

                try {
                    // ResNet50 V2
                    runOnUiThread { loadingDialog.updateProgress(2, "ResNet50 V2") }
                    val model3 =
                        com.example.planktondetectionapps.ml.ResNet50V2with300Data.newInstance(
                            applicationContext
                        )
                    val output3 = model3.process(inputFeature0)
                    predictions.add(output3.outputFeature0AsTensorBuffer.floatArray)
                    model3.close()
                } catch (e: Exception) {
                    android.util.Log.e("PlanktonDebug", "Error running ResNet50V2", e)
                }

                try {
                    // ResNet101 V2
                    runOnUiThread { loadingDialog.updateProgress(3, "ResNet101 V2") }
                    val model4 =
                        com.example.planktondetectionapps.ml.ResNet101V2with300Data.newInstance(
                            applicationContext
                        )
                    val output4 = model4.process(inputFeature0)
                    predictions.add(output4.outputFeature0AsTensorBuffer.floatArray)
                    model4.close()
                } catch (e: Exception) {
                    android.util.Log.e("PlanktonDebug", "Error running ResNet101V2", e)
                }

                try {
                    // EfficientNet V1 B0
                    runOnUiThread { loadingDialog.updateProgress(4, "EfficientNet V1 B0") }
                    val model5 =
                        com.example.planktondetectionapps.ml.EfficientNetV1with300Data.newInstance(
                            applicationContext
                        )
                    val output5 = model5.process(inputFeature0)
                    predictions.add(output5.outputFeature0AsTensorBuffer.floatArray)
                    model5.close()
                } catch (e: Exception) {
                    android.util.Log.e("PlanktonDebug", "Error running EfficientNetV1B0", e)
                }

                try {
                    // EfficientNet V2 B0
                    runOnUiThread { loadingDialog.updateProgress(5, "EfficientNet V2 B0") }
                    val model6 =
                        com.example.planktondetectionapps.ml.EfficientNetV2B0with300Data.newInstance(
                            applicationContext
                        )
                    val output6 = model6.process(inputFeature0)
                    predictions.add(output6.outputFeature0AsTensorBuffer.floatArray)
                    model6.close()
                } catch (e: Exception) {
                    android.util.Log.e("PlanktonDebug", "Error running EfficientNetV2B0", e)
                }

                try {
                    // ConvNext Tiny
                    runOnUiThread { loadingDialog.updateProgress(6, "ConvNeXt Tiny") }
                    val model7 =
                        com.example.planktondetectionapps.ml.ConvNeXtTinywith300Data.newInstance(
                            applicationContext
                        )
                    val output7 = model7.process(inputFeature0)
                    predictions.add(output7.outputFeature0AsTensorBuffer.floatArray)
                    model7.close()
                } catch (e: Exception) {
                    android.util.Log.e("PlanktonDebug", "Error running ConvNextTiny", e)
                }

                try {
                    // DenseNet 121
                    runOnUiThread { loadingDialog.updateProgress(7, "DenseNet 121") }
                    val model8 =
                        com.example.planktondetectionapps.ml.DenseNet121with300Data.newInstance(
                            applicationContext
                        )
                    val output8 = model8.process(inputFeature0)
                    predictions.add(output8.outputFeature0AsTensorBuffer.floatArray)
                    model8.close()
                } catch (e: Exception) {
                    android.util.Log.e("PlanktonDebug", "Error running DenseNet121", e)
                }

                try {
                    // Inception V3
                    runOnUiThread { loadingDialog.updateProgress(8, "Inception V3") }
                    val model9 =
                        com.example.planktondetectionapps.ml.InceptionV3with300Data.newInstance(
                            applicationContext
                        )
                    val output9 = model9.process(inputFeature0)
                    predictions.add(output9.outputFeature0AsTensorBuffer.floatArray)
                    model9.close()
                } catch (e: Exception) {
                    android.util.Log.e("PlanktonDebug", "Error running InceptionV3", e)
                }

                // Update dialog for final processing
                runOnUiThread { loadingDialog.updateFinalProcessing() }

                // Perform majority voting
                val finalPrediction = FloatArray(predictions[0].size)
                for (i in predictions.indices) {
                    for (j in predictions[i].indices) {
                        finalPrediction[j] += predictions[i][j]
                    }
                }

                // Get the class with the highest vote
                var maxPos = 0
                var maxVote = 0f
                val classVotes = finalPrediction.mapIndexed { index, value -> Pair(index, value) }
                    .sortedByDescending { it.second }
                maxPos = classVotes[0].first
                maxVote = classVotes[0].second

                // If there's a tie, use the confidence to decide
                if (classVotes.size > 1 && maxVote == classVotes[1].second) {
                    // Tie-breaking logic: choose the class with the highest confidence among the tied classes
                    maxPos = classVotes.filter { it.second == maxVote }.map { it.first }.maxOrNull()
                        ?: maxPos
                }

                val classes = loadLabels(this)
                if (maxPos < classes.size) {
                    currentClassificationResult = classes[maxPos]
                    currentConfidence = maxVote / predictions.size

                    // Update UI on main thread
                    runOnUiThread {
                        result?.text = classes[maxPos]

                        // Update UI for table results
                        if (confidence?.visibility != View.VISIBLE) {
                            confidence?.visibility = View.VISIBLE
                        }

                        defaultMessage?.visibility = View.GONE
                        resultsTable?.visibility = View.VISIBLE

                        // Update model info with formatted name and show it
                        modelInfo?.text = "Model: ${formatModelName(selectedModel)}"
                        modelInfo?.visibility = View.VISIBLE

                        // Update predictions and probabilities
                        val top3 = finalPrediction.mapIndexed { index, confidence ->
                            Pair(index, confidence)
                        }.sortedByDescending { it.second }.take(3)

                        if (top3.size > 0) {
                            pred1?.text = classes[top3[0].first]
                            prob1?.text =
                                String.format(Locale.getDefault(), "%.1f%%", top3[0].second * 100)
                        }
                        if (top3.size > 1) {
                            pred2?.text = classes[top3[1].first]
                            prob2?.text =
                                String.format(Locale.getDefault(), "%.1f%%", top3[1].second * 100)
                        }
                        if (top3.size > 2) {
                            pred3?.text = classes[top3[2].first]
                            prob3?.text =
                                String.format(Locale.getDefault(), "%.1f%%", top3[2].second * 100)
                        }

                        saveButton?.isEnabled = true
                    }
                }

                // Dismiss loading dialog after a short delay to show completion
                runOnUiThread {
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadingDialog.dismiss()
                    }, 1000)
                }

            } catch (e: Exception) {
                android.util.Log.e("PlanktonDebug", "Error in classification", e)
                runOnUiThread {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this,
                        "Error during classification: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    /**
     * Tampilkan dialog feedback
     */
    private fun showFeedbackDialog() {
        if (currentClassificationResult == null || currentBitmap == null) {
            Toast.makeText(this, "Tidak ada hasil klasifikasi untuk diberikan feedback", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback, null)

        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(true)

        val dialog = dialogBuilder.create()
        // Set transparent background to show the rounded corners from the layout
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Get dialog elements
        val currentPrediction = dialogView.findViewById<TextView>(R.id.currentPrediction)
        val currentConfidenceText = dialogView.findViewById<TextView>(R.id.currentConfidence)
        val correctLabelSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.correctLabelSpinner)
        val feedbackComment = dialogView.findViewById<android.widget.EditText>(R.id.feedbackComment)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        // Display current prediction
        currentPrediction.text = currentClassificationResult
        currentConfidenceText.text = "Tingkat Kepercayaan: ${String.format(Locale.getDefault(), "%.1f%%", currentConfidence * 100)}"

        // Load and populate spinner with plankton labels
        val labels = loadLabels(this)
        val spinnerAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        correctLabelSpinner.adapter = spinnerAdapter

        // Pre-select the current prediction in spinner if it exists
        val currentPredictionIndex = labels.indexOf(currentClassificationResult)
        if (currentPredictionIndex >= 0) {
            correctLabelSpinner.setSelection(currentPredictionIndex)
        }

        // Setup click listeners
        submitButton.setOnClickListener {
            val selectedCorrectLabel = correctLabelSpinner.selectedItem.toString()
            val commentText = feedbackComment.text.toString().trim()

            // Validate selection
            if (selectedCorrectLabel.isEmpty()) {
                Toast.makeText(this, "Silakan pilih label yang benar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update feedback in history entry instead of separate file
            updateHistoryFeedback(
                correctLabel = selectedCorrectLabel,
                comment = commentText
            )

            Toast.makeText(this, "Feedback berhasil disimpan. Terima kasih!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Update feedback in the current history entry
     */
    private fun updateHistoryFeedback(correctLabel: String, comment: String) {
        if (currentHistoryEntryId.isNullOrEmpty()) {
            Toast.makeText(this, "Tidak dapat menyimpan feedback. Silakan lakukan klasifikasi terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            Log.d("MainActivity", "Updating feedback for entry ID: $currentHistoryEntryId")
            Log.d("MainActivity", "Correct label: $correctLabel")
            Log.d("MainActivity", "Comment: $comment")

            // Determine if the prediction is correct
            val isCorrect = currentClassificationResult == correctLabel

            // Update the history entry using HistoryManager
            val updateSuccess = historyManager.updateEntryFeedback(
                entryId = currentHistoryEntryId!!,
                feedback = comment,
                isCorrect = isCorrect,
                correctClass = correctLabel
            )

            if (updateSuccess) {
                Log.d("MainActivity", "✅ Feedback updated successfully!")
                Toast.makeText(this, "Feedback berhasil disimpan. Terima kasih!", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("MainActivity", "❌ Failed to update feedback")
                Toast.makeText(this, "Gagal menyimpan feedback", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating feedback", e)
            Toast.makeText(this, "Error menyimpan feedback: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Save current classification result to history
     */
    private fun saveToHistory() {
        Log.d("PlanktonHistory", "=== saveToHistory() called ===")
        Log.d("PlanktonHistory", "currentClassificationResult: $currentClassificationResult")
        Log.d("PlanktonHistory", "currentBitmap is null: ${currentBitmap == null}")
        Log.d("PlanktonHistory", "currentConfidence: $currentConfidence")
        Log.d("PlanktonHistory", "selectedModel: $selectedModel")

        if (currentClassificationResult != null && currentBitmap != null) {
            try {
                Log.d("PlanktonHistory", "Attempting to save image to internal storage...")
                // Save image to internal storage first
                val imageFile = saveImageToInternalStorage()
                Log.d("PlanktonHistory", "Image saved to: ${imageFile?.absolutePath}")

                if (imageFile != null && imageFile.exists()) {
                    // Create history entry
                    val historyEntry = HistoryEntry(
                        id = System.currentTimeMillis().toString(),
                        timestamp = Date(),
                        imagePath = imageFile.absolutePath,
                        classificationResult = currentClassificationResult!!,
                        confidence = currentConfidence,
                        modelUsed = formatModelName(selectedModel)
                    )

                    Log.d("PlanktonHistory", "Created HistoryEntry:")
                    Log.d("PlanktonHistory", "  ID: ${historyEntry.id}")
                    Log.d("PlanktonHistory", "  Result: ${historyEntry.classificationResult}")
                    Log.d("PlanktonHistory", "  Confidence: ${historyEntry.confidence}")
                    Log.d("PlanktonHistory", "  Model: ${historyEntry.modelUsed}")
                    Log.d("PlanktonHistory", "  Image path: ${historyEntry.imagePath}")

                    // Save to CSV
                    Log.d("PlanktonHistory", "Attempting to save to HistoryManager...")
                    val saveSuccess = historyManager.saveHistoryEntry(historyEntry)
                    Log.d("PlanktonHistory", "Save result: $saveSuccess")

                    if (saveSuccess) {
                        // Store the entry ID for feedback updates
                        currentHistoryEntryId = historyEntry.id
                        Log.d("PlanktonHistory", "✅ History entry saved successfully! ID: ${historyEntry.id}")

                        // Verify by reading back all entries
                        val allEntries = historyManager.getAllHistoryEntries()
                        Log.d("PlanktonHistory", "📊 Total entries after save: ${allEntries.size}")

                        // Show success message to user
                        runOnUiThread {
                            Toast.makeText(this, "Klasifikasi disimpan ke riwayat", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("PlanktonHistory", "❌ Failed to save history entry")
                        runOnUiThread {
                            Toast.makeText(this, "Gagal menyimpan ke riwayat", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("PlanktonHistory", "❌ Failed to save image file or file doesn't exist")
                    runOnUiThread {
                        Toast.makeText(this, "Gagal menyimpan gambar riwayat", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PlanktonHistory", "❌ Exception in saveToHistory()", e)
                runOnUiThread {
                    Toast.makeText(this, "Error menyimpan riwayat: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Log.w("PlanktonHistory", "⚠️ Cannot save to history - missing required data:")
            Log.w("PlanktonHistory", "  currentClassificationResult is null: ${currentClassificationResult == null}")
            Log.w("PlanktonHistory", "  currentBitmap is null: ${currentBitmap == null}")
        }
        Log.d("PlanktonHistory", "=== saveToHistory() finished ===")
    }

    /**
     * Save current bitmap to internal storage for history
     */
    private fun saveImageToInternalStorage(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${currentClassificationResult}_${timeStamp}.png"

            // Create history images directory
            val historyDir = File(filesDir, "history_images")
            if (!historyDir.exists()) {
                historyDir.mkdirs()
            }

            val imageFile = File(historyDir, fileName)
            val outputStream = FileOutputStream(imageFile)

            outputStream.use { stream ->
                currentBitmap!!.compress(Bitmap.CompressFormat.PNG, 90, stream)
            }

            imageFile
        } catch (e: Exception) {
            android.util.Log.e("PlanktonHistory", "Error saving image to internal storage", e)
            null
        }
    }
}
