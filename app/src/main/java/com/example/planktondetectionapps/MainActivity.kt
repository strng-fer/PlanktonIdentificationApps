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
import android.provider.MediaStore
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
        EFFICIENTNET_V2_B0
    }

    // UI Components
    private var result: TextView? = null
    private var confidence: TextView? = null
    private var imageView: ImageView? = null
    private var picture: Button? = null
    private var galleryButton: Button? = null
    private var saveButton: Button? = null

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

    // Navigation menu UI elements
    private var menuButton: android.widget.ImageButton? = null
    private var navigationMenu: LinearLayout? = null
    private var settingsOption: LinearLayout? = null
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

    // Activity result launchers
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
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
        showWelcomeDialog()
        initializeLaunchers()
        setupCustomDropdown()
        setupNavigationMenu()
        setupButtonListeners()
    }

    /**
     * Inisialisasi semua view components
     */
    private fun initializeViews() {
        result = findViewById(R.id.result)
        confidence = findViewById(R.id.confidence)
        imageView = findViewById(R.id.imageView)
        picture = findViewById(R.id.button)
        galleryButton = findViewById(R.id.galleryButton)
        saveButton = findViewById(R.id.saveButton)

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

        // Initialize navigation menu elements
        menuButton = findViewById(R.id.menuButton)
        navigationMenu = findViewById(R.id.navigationMenu)
        settingsOption = findViewById(R.id.settingsOption)
        aboutOption = findViewById(R.id.aboutOption)
        documentationOption = findViewById(R.id.documentationOption)
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
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(galleryIntent)
        }

        saveButton?.setOnClickListener {
            if (currentBitmap != null && currentClassificationResult != null) {
                checkStoragePermissionAndSave()
            } else {
                showError("Tidak ada gambar atau hasil klasifikasi untuk disimpan.")
            }
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
            selectModel(ModelType.MOBILENET_V3_SMALL, "MobileNetV3 Small", "Model ringan dengan performa cepat")
        }

        option2?.setOnClickListener {
            selectModel(ModelType.MOBILENET_V3_LARGE, "MobileNetV3 Large", "Model ringan dengan performa cepat dan lebih akurat")
        }

        option3?.setOnClickListener {
            selectModel(ModelType.RESNET50_V2, "ResNet50 V2 (300 Data)", "Model menengah dengan akurasi tinggi")
        }

        option4?.setOnClickListener {
            selectModel(ModelType.EFFICIENTNET_V2_B0, "EfficientNet V2 B0 (300 Data)", "Model terbaru dengan efisiensi optimal")
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
                == PackageManager.PERMISSION_GRANTED) {
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
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    currentPhotoUri?.let { photoUri ->
                        android.util.Log.d("PlanktonDebug", "Loading full resolution image from URI")
                        val bitmap = loadHighQualityImageFromUri(photoUri)
                        android.util.Log.d("PlanktonDebug", "Full resolution camera image size: ${bitmap.width}x${bitmap.height}")

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
                        android.util.Log.d("PlanktonDebug", "Fallback to thumbnail - size: ${photo.width}x${photo.height}")
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
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                try {
                    val imageUri = result.data?.data
                    if (imageUri != null) {
                        val bitmap = loadHighQualityImageFromUri(imageUri)
                        android.util.Log.d("PlanktonDebug", "Gallery image size: ${bitmap.width}x${bitmap.height}")

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

        // Permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                showCameraSelectionDialog()
            } else {
                showError("Izin kamera diperlukan untuk mengambil foto.")
            }
        }

        // Storage permission launcher
        storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
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
        confidence?.text = "Silakan coba lagi"
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

            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

            // Choose preprocessing based on model type
            val byteBuffer = when (selectedModel) {
                ModelType.MOBILENET_V3_SMALL -> preprocessImageForMobileNetV3BuildIn(image)
                ModelType.MOBILENET_V3_LARGE -> preprocessImageForMobileNetV3BuildIn(image)
                ModelType.RESNET50_V2 -> preprocessImageForResNetV2(image)
                ModelType.EFFICIENTNET_V2_B0 -> preprocessImageForEfficientNetV2BuildIn(image)
            }
            inputFeature0.loadBuffer(byteBuffer)

            // Run inference with selected model
            val confidences = try {
                when (selectedModel) {
                    ModelType.MOBILENET_V3_SMALL -> {
                        try {
                            val model = com.example.planktondetectionapps.ml.MobileNetV3Small.newInstance(applicationContext)
                            val outputs = model.process(inputFeature0)
                            val result = outputs.outputFeature0AsTensorBuffer.floatArray
                            model.close()
                            result
                        } catch (e: Exception) {
                            android.util.Log.e("PlanktonDebug", "MobileNetV3Small model not found", e)
                            showError("Model MobileNetV3Small tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                            return
                        }
                    }

                    ModelType.MOBILENET_V3_LARGE -> {
                        try {
                            val model = com.example.planktondetectionapps.ml.MobileNetV3LargeWith300Data.newInstance(applicationContext)
                            val outputs = model.process(inputFeature0)
                            val result = outputs.outputFeature0AsTensorBuffer.floatArray
                            model.close()
                            result
                        } catch (e: Exception) {
                            android.util.Log.e("PlanktonDebug", "MobileNetV3Small model not found", e)
                            showError("Model MobileNetV3Large tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                            return
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

                            val modelInstance = modelClass.getMethod("newInstance", Context::class.java)
                                .invoke(null, applicationContext)
                            val processMethod = modelClass.getMethod("process", TensorBuffer::class.java)
                            val outputs = processMethod.invoke(modelInstance, inputFeature0)
                            val outputMethod = outputs::class.java.getMethod("getOutputFeature0AsTensorBuffer")
                            val tensorBuffer = outputMethod.invoke(outputs) as TensorBuffer
                            val result = tensorBuffer.floatArray

                            val closeMethod = modelClass.getMethod("close")
                            closeMethod.invoke(modelInstance)

                            result
                        } catch (e: Exception) {
                            android.util.Log.e("PlanktonDebug", "ResNet50V2 model not found", e)
                            showError("Model ResNet50V2 tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                            return
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

                            val modelInstance = modelClass.getMethod("newInstance", Context::class.java)
                                .invoke(null, applicationContext)
                            val processMethod = modelClass.getMethod("process", TensorBuffer::class.java)
                            val outputs = processMethod.invoke(modelInstance, inputFeature0)
                            val outputMethod = outputs::class.java.getMethod("getOutputFeature0AsTensorBuffer")
                            val tensorBuffer = outputMethod.invoke(outputs) as TensorBuffer
                            val result = tensorBuffer.floatArray

                            val closeMethod = modelClass.getMethod("close")
                            closeMethod.invoke(modelInstance)

                            result
                        } catch (e: Exception) {
                            android.util.Log.e("PlanktonDebug", "EfficientNetV2B0 model not found", e)
                            showError("Model EfficientNetV2B0 tidak ditemukan. Pastikan file model sudah ditambahkan ke folder ml/")
                            return
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlanktonDebug", "Error running model inference", e)
                showError("Error menjalankan inferensi model: ${e.message}")
                return
            }

            // Process results
            processClassificationResults(confidences)

        } catch (e: Exception) {
            android.util.Log.e("PlanktonDebug", "Error in classifyImage", e)
            showError("Error classifying image with ${selectedModel.name}: ${e.message}")
        }
    }

    /**
     * Proses hasil klasifikasi dan update UI
     */
    private fun processClassificationResults(confidences: FloatArray) {
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

            val classConfidencePairs = mutableListOf<Pair<Int, Float>>()
            for (i in finalConfidences.indices) {
                classConfidencePairs.add(Pair(i, finalConfidences[i]))
            }

            classConfidencePairs.sortByDescending { it.second }

            val top3 = classConfidencePairs.take(3)
            var s = "Model: ${selectedModel.name}\nTop 3 Predictions:\n"
            for ((index, conf) in top3) {
                if (index < classes.size) {
                    s += String.format(Locale.getDefault(), "%s: %.1f%%\n", classes[index], conf * 100)
                }
            }

            confidence?.text = s
            saveButton?.isEnabled = true
        } else {
            showError("Error: Invalid classification result")
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

        android.util.Log.d("PlanktonDebug", "Processing image for MobileNetV3 with built-in preprocessing")

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
    private fun preprocessImageForEfficientNetV2BuildIn(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d("PlanktonDebug", "Processing image for EfficientNetV2 with built-in preprocessing")

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
            Toast.makeText(this, "Membuka kamera untuk foto resolusi penuh...", Toast.LENGTH_SHORT).show()
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
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PlanktonDetection")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
            outputStream?.use { stream ->
                currentBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)

            showSuccessDialog(
                "Gambar berhasil disimpan ke galeri dengan nama:\n$fileName\n\nKlasifikasi: $currentClassificationResult\nTingkat kepercayaan: ${String.format(Locale.getDefault(), "%.1f%%", currentConfidence * 100)}"
            )
        } else {
            showError("Gagal menyimpan gambar ke galeri.")
        }
    }

    /**
     * Simpan gambar ke galeri untuk Android 9 dan bawah
     */
    private fun saveImageToGalleryLegacy(fileName: String) {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
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
            "Gambar berhasil disimpan ke galeri dengan nama:\n$fileName\n\nKlasifikasi: $currentClassificationResult\nTingkat kepercayaan: ${String.format(Locale.getDefault(), "%.1f%%", currentConfidence * 100)}"
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
    }
}
