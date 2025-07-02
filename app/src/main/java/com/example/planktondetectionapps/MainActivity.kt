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

class MainActivity : AppCompatActivity() {
    // Model enum untuk memilih jenis model AI
    enum class ModelType {
        MOBILENET_V3_SMALL,
        RESNET50_V2,
        EFFICIENTNET_V2_B0
    }

    var result: TextView? = null
    var confidence: TextView? = null
    var imageView: ImageView? = null
    var picture: Button? = null
    var galleryButton: Button? = null
    var saveButton: Button? = null

    // Custom dropdown UI elements
    var customDropdownContainer: LinearLayout? = null
    var dropdownOptions: LinearLayout? = null
    var dropdownArrow: ImageView? = null
    var selectedModelName: TextView? = null
    var selectedModelDescription: TextView? = null
    var option1: LinearLayout? = null
    var option2: LinearLayout? = null
    var option3: LinearLayout? = null

    var imageSize: Int = 224

    // Variables to store current classification data for saving
    private var currentBitmap: Bitmap? = null
    private var currentClassificationResult: String? = null
    private var currentConfidence: Float = 0f
    private var currentPhotoUri: Uri? = null
    private var selectedModel: ModelType = ModelType.MOBILENET_V3_SMALL // Default model
    private var isDropdownOpen = false

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        result = findViewById<TextView>(R.id.result)
        confidence = findViewById<TextView>(R.id.confidence)
        imageView = findViewById<ImageView>(R.id.imageView)
        picture = findViewById<Button>(R.id.button)
        galleryButton = findViewById<Button>(R.id.galleryButton)
        saveButton = findViewById<Button>(R.id.saveButton)

        // Initialize custom dropdown elements
        customDropdownContainer = findViewById<LinearLayout>(R.id.customDropdownContainer)
        dropdownOptions = findViewById<LinearLayout>(R.id.dropdownOptions)
        dropdownArrow = findViewById<ImageView>(R.id.dropdownArrow)
        selectedModelName = findViewById<TextView>(R.id.selectedModelName)
        selectedModelDescription = findViewById<TextView>(R.id.selectedModelDescription)
        option1 = findViewById<LinearLayout>(R.id.option1)
        option2 = findViewById<LinearLayout>(R.id.option2)
        option3 = findViewById<LinearLayout>(R.id.option3)

        // Show welcome dialog when app starts
        showWelcomeDialog()

        // Initialize ActivityResultLaunchers
        initializeLaunchers()

        // Setup custom dropdown functionality
        setupCustomDropdown()

        picture?.setOnClickListener {
            // Launch camera if we have permission
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                showCameraSelectionDialog()
            } else {
                //Request camera permission if we don't have it.
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        galleryButton?.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(galleryIntent)
        }

        // Add save button click listener
        saveButton?.setOnClickListener {
            if (currentBitmap != null && currentClassificationResult != null) {
                checkStoragePermissionAndSave()
            } else {
                showError("Tidak ada gambar atau hasil klasifikasi untuk disimpan.")
            }
        }
    }

    private fun setupCustomDropdown() {
        // Toggle dropdown when container is clicked
        customDropdownContainer?.setOnClickListener {
            toggleDropdown()
        }

        // Set up option click listeners
        option1?.setOnClickListener {
            selectModel(ModelType.MOBILENET_V3_SMALL, "MobileNetV3 Small", "Model ringan dengan performa cepat")
        }

        option2?.setOnClickListener {
            selectModel(ModelType.RESNET50_V2, "ResNet50 V2 (300 Data)", "Model menengah dengan akurasi tinggi")
        }

        option3?.setOnClickListener {
            selectModel(ModelType.EFFICIENTNET_V2_B0, "EfficientNet V2 B0 (300 Data)", "Model terbaru dengan efisiensi optimal")
        }
    }

    private fun toggleDropdown() {
        if (isDropdownOpen) {
            // Close dropdown
            dropdownOptions?.visibility = View.GONE
            dropdownArrow?.rotation = 0f
            isDropdownOpen = false
        } else {
            // Open dropdown
            dropdownOptions?.visibility = View.VISIBLE
            dropdownArrow?.rotation = 180f
            isDropdownOpen = true
        }
    }

    private fun selectModel(modelType: ModelType, modelName: String, modelDescription: String) {
        selectedModel = modelType
        selectedModelName?.text = modelName
        selectedModelDescription?.text = modelDescription

        // Close dropdown after selection
        dropdownOptions?.visibility = View.GONE
        dropdownArrow?.rotation = 0f
        isDropdownOpen = false

        // Show selection feedback
        Toast.makeText(this, "Model dipilih: $modelName", Toast.LENGTH_SHORT).show()
    }

    private fun checkStoragePermissionAndSave() {
        // For Android 10 and above, WRITE_EXTERNAL_STORAGE permission is not needed
        // when using MediaStore API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGallery()
        } else {
            // For Android 9 and below, check for WRITE_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                saveImageToGallery()
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun showWelcomeDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Selamat Datang di Plankton Detection")
        dialogBuilder.setIcon(R.drawable.ic_microscope)
        dialogBuilder.setMessage(
            "Aplikasi ini menggunakan teknologi AI untuk mendeteksi dan mengklasifikasi jenis plankton.\n\n" +
            "• Ambil foto menggunakan kamera\n" +
            "• Pilih foto dari galeri\n" +
            "• Dapatkan hasil klasifikasi dengan tingkat kepercayaan\n" +
            "• Simpan hasil ke galeri dengan nama sesuai klasifikasi\n\n" +
            "Pastikan gambar plankton terlihat jelas untuk hasil terbaik!"
        )
        dialogBuilder.setPositiveButton("Mulai") { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.setCancelable(false)
        dialogBuilder.create().show()
    }

    private fun showErrorDialog(title: String, message: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(title)
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert)
        dialogBuilder.create().show()
    }

    private fun showSuccessDialog(title: String, message: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(title)
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.setIcon(android.R.drawable.ic_dialog_info)
        dialogBuilder.create().show()
    }

    private fun initializeLaunchers() {
        // Camera launcher - using full resolution capture with file output
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    // Check if we have a photo URI from full resolution capture
                    currentPhotoUri?.let { photoUri ->
                        android.util.Log.d("PlanktonDebug", "Loading full resolution image from URI")
                        val bitmap = loadHighQualityImageFromUri(photoUri)
                        android.util.Log.d("PlanktonDebug", "Full resolution camera image size: ${bitmap.width}x${bitmap.height}")

                        // Store high-quality image for preview and saving
                        currentBitmap = bitmap
                        imageView?.setImageBitmap(bitmap)

                        // Create AI processing image
                        val aiImage = createHighQualitySquareImage(bitmap, imageSize)
                        classifyImage(aiImage)

                        // Clean up temporary file
                        cleanupTempFile(photoUri)
                        return@registerForActivityResult
                    }

                    // Fallback to thumbnail if no URI (shouldn't happen with new implementation)
                    val extras = result.data?.extras
                    @Suppress("DEPRECATION")
                    val photo = extras?.get("data") as? Bitmap

                    if (photo != null) {
                        android.util.Log.d("PlanktonDebug", "Fallback to thumbnail - size: ${photo.width}x${photo.height}")
                        showError("Kualitas gambar rendah (thumbnail). Menggunakan kualitas yang tersedia.")

                        // Still process the thumbnail but warn user
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

                        // Store high-quality image for preview and saving
                        currentBitmap = bitmap
                        imageView?.setImageBitmap(bitmap)

                        // Create AI processing image
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

    private fun showError(message: String) {
        showErrorDialog("Gagal", message)
        result?.text = "Terjadi kesalahan"
        confidence?.text = "Silakan coba lagi"
        // Disable save button when there's an error
        saveButton?.isEnabled = false
    }

    fun loadLabels(context: Context): List<String> {
        val labels = mutableListOf<String>()
        try {
            context.assets.open("labels.txt").bufferedReader().useLines { lines ->
                lines.forEach { labels.add(it) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            labels.add("Unknown") // fallback jika file tidak ditemukan
        }
        return labels
    }

    fun classifyImage(image: Bitmap) {
        try {
            android.util.Log.d("PlanktonDebug", "=== CLASSIFICATION START ===")
            android.util.Log.d("PlanktonDebug", "Selected model: ${selectedModel.name}")

            // Creates inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

            // Choose preprocessing based on model type
            val byteBuffer = when (selectedModel) {
                ModelType.MOBILENET_V3_SMALL -> preprocessImageForMobileNetV3BuildIn(image) // Use raw pixel values
                ModelType.RESNET50_V2 -> preprocessImageForResNetV2(image) // Use [-1,1] normalization
                ModelType.EFFICIENTNET_V2_B0 -> preprocessImageForEfficientNetV2BuildIn(image) // Use raw pixel values
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
                    ModelType.RESNET50_V2 -> {
                        try {
                            // Try different possible model names
                            val modelClass = try {
                                Class.forName("com.example.planktondetectionapps.ml.ResNet50V2")
                            } catch (e: ClassNotFoundException) {
                                try {
                                    Class.forName("com.example.planktondetectionapps.ml.ResNet50V2with300Data")
                                } catch (e2: ClassNotFoundException) {
                                    Class.forName("com.example.planktondetectionapps.ml.Resnet50v2")
                                }
                            }

                            // Use reflection to create and run the model
                            val modelInstance = modelClass.getMethod("newInstance", android.content.Context::class.java)
                                .invoke(null, applicationContext)
                            val processMethod = modelClass.getMethod("process", TensorBuffer::class.java)
                            val outputs = processMethod.invoke(modelInstance, inputFeature0)
                            val outputMethod = outputs::class.java.getMethod("getOutputFeature0AsTensorBuffer")
                            val tensorBuffer = outputMethod.invoke(outputs) as TensorBuffer
                            val result = tensorBuffer.floatArray

                            // Close model
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
                            // Try different possible model names
                            val modelClass = try {
                                Class.forName("com.example.planktondetectionapps.ml.EfficientNetV2B0")
                            } catch (e: ClassNotFoundException) {
                                try {
                                    Class.forName("com.example.planktondetectionapps.ml.EfficientNetV2B0with300Data")
                                } catch (e2: ClassNotFoundException) {
                                    Class.forName("com.example.planktondetectionapps.ml.Efficientnetv2b0")
                                }
                            }

                            // Use reflection to create and run the model
                            val modelInstance = modelClass.getMethod("newInstance", android.content.Context::class.java)
                                .invoke(null, applicationContext)
                            val processMethod = modelClass.getMethod("process", TensorBuffer::class.java)
                            val outputs = processMethod.invoke(modelInstance, inputFeature0)
                            val outputMethod = outputs::class.java.getMethod("getOutputFeature0AsTensorBuffer")
                            val tensorBuffer = outputMethod.invoke(outputs) as TensorBuffer
                            val result = tensorBuffer.floatArray

                            // Close model
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

            // Comprehensive debugging
            android.util.Log.d("PlanktonDebug", "=== MODEL OUTPUT DEBUG ===")
            android.util.Log.d("PlanktonDebug", "Total classes: ${confidences.size}")
            android.util.Log.d("PlanktonDebug", "Raw output values (first 5):")
            for (i in 0 until minOf(5, confidences.size)) {
                android.util.Log.d("PlanktonDebug", "Class $i: ${confidences[i]}")
            }

            // Check if output is already probabilities or needs softmax
            val sumConfidences = confidences.sum()
            android.util.Log.d("PlanktonDebug", "Sum of all confidences: $sumConfidences")

            val finalConfidences = if (sumConfidences > 0.99 && sumConfidences < 1.01) {
                // Output sudah dalam bentuk probabilitas (sum ≈ 1.0)
                android.util.Log.d("PlanktonDebug", "Using raw output (already probabilities)")
                confidences
            } else {
                // Output masih berupa logits, perlu softmax
                android.util.Log.d("PlanktonDebug", "Applying softmax to raw logits")
                applySoftmax(confidences)
            }

            // Find max confidence
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
                android.util.Log.d("PlanktonDebug", "Predicted class name: ${classes[maxPos]}")
            }


            if (maxPos < classes.size) {
                // Store classification results for saving
                currentClassificationResult = classes[maxPos]
                currentConfidence = maxConfidence

                result!!.text = classes[maxPos]

                // Create sorted confidence pairs
                val classConfidencePairs = mutableListOf<Pair<Int, Float>>()
                for (i in finalConfidences.indices) {
                    classConfidencePairs.add(Pair(i, finalConfidences[i]))
                }

                // Sort by confidence descending
                classConfidencePairs.sortByDescending { it.second }

                // Show top 3 predictions with model name
                val top3 = classConfidencePairs.take(3)
                var s = "Model: ${selectedModel.name}\nTop 3 Predictions:\n"
                for ((index, conf) in top3) {
                    if (index < classes.size) {
                        s += String.format(Locale.getDefault(), "%s: %.1f%%\n", classes[index], conf * 100)
                    }
                }

                confidence!!.text = s

                // Enable save button after successful classification
                saveButton?.isEnabled = true
            } else {
                showError("Error: Invalid classification result")
            }

        } catch (e: Exception) {
            android.util.Log.e("PlanktonDebug", "Error in classifyImage", e)
            showError("Error classifying image with ${selectedModel.name}: ${e.message}")
        }
    }

    /**
     * Apply softmax function to convert logits to probabilities
     */
    private fun applySoftmax(logits: FloatArray): FloatArray {
        val result = FloatArray(logits.size)

        // Find max for numerical stability
        val maxLogit = logits.maxOrNull() ?: 0f

        // Calculate exp(x - max) for all elements
        var sumExp = 0f
        for (i in logits.indices) {
            result[i] = kotlin.math.exp(logits[i] - maxLogit)
            sumExp += result[i]
        }

        // Normalize
        for (i in result.indices) {
            result[i] = result[i] / sumExp
        }

        return result
    }

    /**
     * MobileNetV3Small preprocessing: Scales image to [-1, 1] range
     * Uses tf.keras.applications.mobilenet_v3.preprocess_input equivalent
     */
    private fun preprocessImageForMobileNetV3(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d("PlanktonDebug", "Using MobileNetV3 preprocessing: [-1, 1] normalization")

        var pixel = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val value = intValues[pixel++]

                // Extract RGB values
                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                // MobileNetV3 preprocessing: normalize to [-1, 1]
                // Formula: (pixel_value / 127.5) - 1.0
                byteBuffer.putFloat((red / 127.5f) - 1.0f)
                byteBuffer.putFloat((green / 127.5f) - 1.0f)
                byteBuffer.putFloat((blue / 127.5f) - 1.0f)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * MobileNetV3Small preprocessing with built-in preprocessing: Uses raw pixel values [0-255]
     * For models that have built-in preprocessing layers and expect unnormalized input
     */
    private fun preprocessImageForMobileNetV3BuildIn(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Create properly scaled bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)

        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d("PlanktonDebug", "Processing image for MobileNetV3 with built-in preprocessing")
        android.util.Log.d("PlanktonDebug", "Image size: ${imageSize}x${imageSize}")

        var pixel = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val value = intValues[pixel++]

                // Extract RGB values (Android uses ARGB format)
                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                // MobileNetV3 dengan built-in preprocessing expects raw pixel values [0-255]
                // TIDAK melakukan normalisasi karena model akan melakukannya secara internal
                byteBuffer.putFloat(red.toFloat())
                byteBuffer.putFloat(green.toFloat())
                byteBuffer.putFloat(blue.toFloat())
            }
        }

        android.util.Log.d("PlanktonDebug", "ByteBuffer filled with raw pixel values [0-255]")

        // Reset position for reading
        byteBuffer.rewind()

        return byteBuffer
    }

    /**
     * ResNetV2 preprocessing following tf.keras.applications.resnet_v2.preprocess_input
     * Preprocesses a batch of images by scaling pixel values to [-1, 1] sample-wise.
     *
     * The inputs pixel values are scaled between -1 and 1, sample-wise using:
     * normalized_pixel = (pixel / 127.5) - 1.0
     *
     * This is the official preprocessing for ResNetV2 models as documented in:
     * https://www.tensorflow.org/api_docs/python/tf/keras/applications/resnet_v2/preprocess_input
     */
    private fun preprocessImageForResNetV2(image: Bitmap): ByteBuffer {
        val imageSize = 224
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d("PlanktonDebug", "Using ResNetV2 preprocessing: tf.keras.applications.resnet_v2.preprocess_input - scaling to [-1, 1]")

        var pixel = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val value = intValues[pixel++]

                // Extract RGB values (0-255 range)
                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                // ResNetV2 preprocessing: scale from [0, 255] to [-1, 1]
                // Formula: (pixel / 127.5) - 1.0
                // This ensures: 0 -> -1.0, 127.5 -> 0.0, 255 -> 1.0
                byteBuffer.putFloat((red / 127.5f) - 1.0f)
                byteBuffer.putFloat((green / 127.5f) - 1.0f)
                byteBuffer.putFloat((blue / 127.5f) - 1.0f)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * EfficientNetV2 preprocessing: Rescales from [0, 255] to [0, 1]
     * Uses tf.keras.applications.efficientnet_v2.preprocess_input equivalent
     * Includes scaling without per-channel mean subtraction
     */
    private fun preprocessImageForEfficientNetV2(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d("PlanktonDebug", "Using EfficientNetV2 preprocessing: [0, 1] normalization")

        var pixel = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val value = intValues[pixel++]

                // Extract RGB values
                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                // EfficientNetV2 preprocessing: rescale to [0, 1]
                // Formula: pixel_value / 255.0
                byteBuffer.putFloat(red / 255.0f)
                byteBuffer.putFloat(green / 255.0f)
                byteBuffer.putFloat(blue / 255.0f)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * EfficientNetV2 preprocessing with built-in preprocessing: Uses raw pixel values [0-255]
     * For models that have built-in preprocessing layers and expect unnormalized input
     */
    private fun preprocessImageForEfficientNetV2BuildIn(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Create properly scaled bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)

        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d("PlanktonDebug", "Processing image for EfficientNetV2 with built-in preprocessing")
        android.util.Log.d("PlanktonDebug", "Image size: ${imageSize}x${imageSize}")

        var pixel = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val value = intValues[pixel++]

                // Extract RGB values (Android uses ARGB format)
                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                // EfficientNetV2 dengan built-in preprocessing expects raw pixel values [0-255]
                // TIDAK melakukan normalisasi karena model akan melakukannya secara internal
                byteBuffer.putFloat(red.toFloat())
                byteBuffer.putFloat(green.toFloat())
                byteBuffer.putFloat(blue.toFloat())
            }
        }

        android.util.Log.d("PlanktonDebug", "ByteBuffer filled with raw pixel values [0-255]")

        // Reset position for reading
        byteBuffer.rewind()

        return byteBuffer
    }

    /**
     * Fixed preprocessing that tries to match the exact preprocessing used during training
     * @deprecated Use specific preprocessing functions for each model instead
     */
    private fun preprocessImageFixed(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Create properly scaled bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)

        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d("PlanktonDebug", "Processing image size: ${imageSize}x${imageSize}")

        var pixel = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val value = intValues[pixel++]

                // Extract RGB values (Android uses ARGB format)
                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                // Try standard [0,1] normalization first
                byteBuffer.putFloat(red / 255.0f)
                byteBuffer.putFloat(green / 255.0f)
                byteBuffer.putFloat(blue / 255.0f)
            }
        }

        android.util.Log.d("PlanktonDebug", "ByteBuffer size: ${byteBuffer.capacity()}, position after fill: ${byteBuffer.position()}")

        // Reset position for reading
        byteBuffer.rewind()

        return byteBuffer
    }

    private fun showCameraSelectionDialog() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val availableCameras = getAvailableCameras(cameraManager)

            if (availableCameras.isEmpty()) {
                // Jika tidak ada kamera yang terdeteksi, gunakan kamera default
                launchDefaultCamera()
                return
            }

            // Filter hanya kamera default dan USB eksternal
            val filteredCameras = availableCameras.filter { camera ->
                camera.second == "Kamera Default" || camera.second == "Kamera USB Eksternal"
            }

            if (filteredCameras.size == 1) {
                // Jika hanya ada satu pilihan, langsung buka
                launchCameraWithId(filteredCameras[0].first)
                return
            }

            if (filteredCameras.isEmpty()) {
                // Jika tidak ada kamera yang sesuai filter, gunakan default
                launchDefaultCamera()
                return
            }

            val cameraNames = filteredCameras.map { it.second }.toTypedArray()

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Pilih Kamera")
                .setIcon(R.drawable.ic_camera_capture)
                .setItems(cameraNames) { dialog, which ->
                    val selectedCameraId = filteredCameras[which].first
                    launchCameraWithId(selectedCameraId)
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } catch (e: Exception) {
            // Jika terjadi error, gunakan kamera default
            launchDefaultCamera()
        }
    }

    private fun getAvailableCameras(cameraManager: CameraManager): List<Pair<String, String>> {
        val cameras = mutableListOf<Pair<String, String>>()

        try {
            val cameraIdList = cameraManager.cameraIdList

            for (cameraId in cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                // Hanya tambahkan kamera yang sesuai kriteria
                when (facing) {
                    CameraCharacteristics.LENS_FACING_BACK -> {
                        // Gunakan kamera belakang sebagai kamera default
                        cameras.add(Pair(cameraId, "Kamera Default"))
                    }
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> {
                        // Tambahkan kamera USB eksternal
                        cameras.add(Pair(cameraId, "Kamera USB Eksternal"))
                    }
                    // Abaikan kamera depan dan lainnya
                }
            }
        } catch (e: Exception) {
            // Fallback untuk perangkat yang tidak mendukung Camera2 API
            cameras.add(Pair("0", "Kamera Default"))
        }

        return cameras
    }

    private fun launchDefaultCamera() {
        try {
            // Create temporary file for full resolution capture
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

    private fun launchCameraWithId(cameraId: String) {
        try {
            // Create temporary file for full resolution capture
            val photoFile = createImageFile()
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            cameraLauncher.launch(cameraIntent)

            val cameraType = if (cameraId.contains("external") || (cameraId.toIntOrNull() ?: 0) > 1) {
                "kamera USB eksternal"
            } else {
                "kamera default"
            }

            Toast.makeText(this, "Membuka $cameraType untuk foto resolusi penuh...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            showError("Error launching camera: ${e.message}")
        }
    }

    /**
     * Create temporary file for full resolution camera capture
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    /**
     * Clean up temporary file after processing
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

    private fun saveImageToGallery() {
        if (currentBitmap == null || currentClassificationResult == null) {
            showError("Tidak ada gambar atau hasil klasifikasi untuk disimpan.")
            return
        }

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${currentClassificationResult}_${timeStamp}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 and above - use MediaStore API
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

                    // Clear the IS_PENDING flag
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)

                    showSuccessDialog(
                        "Berhasil Disimpan",
                        "Gambar berhasil disimpan ke galeri dengan nama:\n$fileName\n\nKlasifikasi: $currentClassificationResult\nTingkat kepercayaan: ${String.format(Locale.getDefault(), "%.1f%%", currentConfidence * 100)}"
                    )
                } else {
                    showError("Gagal menyimpan gambar ke galeri.")
                }
            } else {
                // Android 9 and below - use traditional file storage
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
                    "Berhasil Disimpan",
                    "Gambar berhasil disimpan ke galeri dengan nama:\n$fileName\n\nKlasifikasi: $currentClassificationResult\nTingkat kepercayaan: ${String.format(Locale.getDefault(), "%.1f%%", currentConfidence * 100)}"
                )
            }

        } catch (e: Exception) {
            android.util.Log.e("PlanktonDebug", "Error saving image to gallery", e)
            showError("Gagal menyimpan gambar: ${e.message}")
        }
    }

    /**
     * Load high-quality image from URI without quality loss
     */
    private fun loadHighQualityImageFromUri(uri: Uri): Bitmap {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Use newer ImageDecoder for better quality
                val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                // Fallback for older versions with better options
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inSampleSize = 1 // Don't downsample
                    inMutable = true
                }
                val inputStream = contentResolver.openInputStream(uri)
                android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                    ?: throw Exception("Failed to decode image from URI")
            }
        } catch (e: Exception) {
            android.util.Log.e("PlanktonDebug", "Error loading image from URI", e)
            // Fallback to the deprecated method if others fail
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    /**
     * Create high-quality square image without losing quality
     * Using advanced filtering and processing for maximum sharpness
     */
    private fun createHighQualitySquareImage(originalBitmap: Bitmap, targetSize: Int): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height

        // Ensure we start with ARGB_8888 for best quality
        val sourceBitmap = if (originalBitmap.config != Bitmap.Config.ARGB_8888) {
            originalBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            originalBitmap
        }

        // Calculate the size of the square (use the smaller dimension)
        val squareSize = min(width, height)

        // Calculate the starting coordinates for cropping (center crop)
        val xOffset = (width - squareSize) / 2
        val yOffset = (height - squareSize) / 2

        // Create square bitmap by cropping from center with highest quality
        val squareBitmap = Bitmap.createBitmap(
            sourceBitmap,
            xOffset,
            yOffset,
            squareSize,
            squareSize
        )

        // If target size is same as square size, return as-is to avoid unnecessary scaling
        if (squareSize == targetSize) {
            return squareBitmap
        }

        // Use Paint with anti-aliasing and filtering for best quality scaling
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        // Create target bitmap with best config
        val scaledBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(scaledBitmap)

        // Create rect for source and destination
        val srcRect = android.graphics.Rect(0, 0, squareSize, squareSize)
        val dstRect = android.graphics.Rect(0, 0, targetSize, targetSize)

        // Draw with high-quality scaling
        canvas.drawBitmap(squareBitmap, srcRect, dstRect, paint)

        return scaledBitmap
    }
}
