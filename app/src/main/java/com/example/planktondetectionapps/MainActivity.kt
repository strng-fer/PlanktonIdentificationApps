package com.example.planktondetectionapps

import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import com.example.planktondetectionapps.ml.ModelUnquant
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    var result: TextView? = null
    var confidence: TextView? = null
    var imageView: ImageView? = null
    var picture: Button? = null
    var galleryButton: Button? = null
    var imageSize: Int = 224

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        result = findViewById<TextView>(R.id.result)
        confidence = findViewById<TextView>(R.id.confidence)
        imageView = findViewById<ImageView>(R.id.imageView)
        picture = findViewById<Button>(R.id.button)
        galleryButton = findViewById<Button>(R.id.galleryButton)

        // Show welcome dialog when app starts
        showWelcomeDialog()

        // Initialize ActivityResultLaunchers
        initializeLaunchers()

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
    }

    private fun showWelcomeDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Selamat Datang di Plankton Detection")
        dialogBuilder.setIcon(R.drawable.ic_microscope)
        dialogBuilder.setMessage(
            "Aplikasi ini menggunakan teknologi AI untuk mendeteksi dan mengklasifikasi jenis plankton.\n\n" +
            "• Ambil foto menggunakan kamera\n" +
            "• Pilih foto dari galeri\n" +
            "• Dapatkan hasil klasifikasi dengan tingkat kepercayaan\n\n" +
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
        // Camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val extras = result.data?.extras
                    @Suppress("DEPRECATION")
                    val photo = extras?.get("data") as? Bitmap

                    if (photo != null) {
                        val dimension = min(photo.width, photo.height)
                        var image = ThumbnailUtils.extractThumbnail(photo, dimension, dimension)
                        imageView?.setImageBitmap(image)

                        image = image.scale(imageSize, imageSize)
                        classifyImage(image)
                    } else {
                        showError("Gagal mengambil gambar dari kamera.")
                    }
                } catch (e: Exception) {
                    showError("Error processing image: ${e.message}")
                }
            } else {
                showError("Pengambilan gambar dibatalkan.")
            }
        }

        // Gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                try {
                    val imageUri = result.data?.data
                    if (imageUri != null) {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)

                        // Process and display the image
                        val dimension = min(bitmap.width, bitmap.height)
                        var image = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension)
                        imageView?.setImageBitmap(image)

                        image = image.scale(imageSize, imageSize)
                        classifyImage(image)
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
    }

    private fun showError(message: String) {
        showErrorDialog("Gagal", message)
        result?.text = "Terjadi kesalahan"
        confidence?.text = "Silakan coba lagi"
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
        var model: ModelUnquant? = null
        try {
            model = ModelUnquant.newInstance(applicationContext)

            // Creates inputs for reference.
            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

            // MobileNetV3 dengan preprocessing built-in - gunakan raw pixel values [0-255]
            val byteBuffer = preprocessImageForMobileNetV3BuildIn(image)
            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result.
            val outputs: ModelUnquant.Outputs = model.process(inputFeature0)
            val outputFeature0: TensorBuffer = outputs.getOutputFeature0AsTensorBuffer()

            val confidences = outputFeature0.floatArray

            // Comprehensive debugging
            android.util.Log.d("PlanktonDebug", "=== MODEL OUTPUT DEBUG ===")
            android.util.Log.d("PlanktonDebug", "Total classes: ${confidences.size}")
            android.util.Log.d("PlanktonDebug", "Raw output values (first 5):")
            for (i in 0 until minOf(5, confidences.size)) {
                android.util.Log.d("PlanktonDebug", "Class $i: ${confidences[i]}")
            }

            // Model MobileNetV3 dengan softmax sudah built-in, jadi tidak perlu apply softmax lagi
            // Tapi kita cek dulu apakah output sudah dalam bentuk probabilitas
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

            // Check for potential issues
            val avgConfidence = finalConfidences.average()
            val minConfidence = finalConfidences.minOrNull() ?: 0f
            val maxConfidenceValue = finalConfidences.maxOrNull() ?: 0f
            val confidenceRange = maxConfidenceValue - minConfidence

            android.util.Log.d("PlanktonDebug", "Confidence stats - Min: $minConfidence, Max: $maxConfidenceValue, Avg: $avgConfidence, Range: $confidenceRange")

            if (maxPos < classes.size) {
                result!!.text = classes[maxPos]

                // Create sorted confidence pairs
                val classConfidencePairs = mutableListOf<Pair<Int, Float>>()
                for (i in finalConfidences.indices) {
                    classConfidencePairs.add(Pair(i, finalConfidences[i]))
                }

                // Sort by confidence descending
                classConfidencePairs.sortByDescending { it.second }

                // Show top 3 predictions
                val top3 = classConfidencePairs.take(3)
                var s = "Top 3 Predictions:\n"
                for ((index, conf) in top3) {
                    if (index < classes.size) {
                        s += String.format(Locale.getDefault(), "%s: %.1f%%\n", classes[index], conf * 100)
                    }
                }

                confidence!!.text = s
            } else {
                showError("Error: Invalid classification result")
            }

        } catch (e: Exception) {
            android.util.Log.e("PlanktonDebug", "Error in classifyImage", e)
            showError("Error classifying image: ${e.message}")
        } finally {
            model?.close()
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
     * Fixed preprocessing that tries to match the exact preprocessing used during training
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

    /**
     * Alternative preprocessing methods for testing different normalization approaches
     */
    private fun preprocessImageForMobileNetV3(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        var pixel = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val value = intValues[pixel++]

                // Extract RGB values
                val red = (value shr 16) and 0xFF
                val green = (value shr 8) and 0xFF
                val blue = value and 0xFF

                // MobileNet_V3 preprocessing: normalize to [-1, 1]
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
     * Preprocessing untuk MobileNetV3 dengan built-in preprocessing
     * Model sudah melakukan normalisasi internal, jadi kita hanya perlu memberikan raw pixel values [0-255]
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
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
            Toast.makeText(this, "Membuka kamera default...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            showError("Error launching default camera: ${e.message}")
        }
    }

    private fun launchCameraWithId(cameraId: String) {
        try {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)

            val cameraType = if (cameraId.contains("external") || cameraId.toIntOrNull() ?: 0 > 1) {
                "kamera USB eksternal"
            } else {
                "kamera default"
            }

            Toast.makeText(this, "Membuka $cameraType...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            showError("Error launching camera: ${e.message}")
        }
    }
}
