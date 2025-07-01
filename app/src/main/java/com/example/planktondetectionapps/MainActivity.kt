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
                        showError("❌ Gagal mengambil gambar dari kamera.")
                    }
                } catch (e: Exception) {
                    showError("❌ Error processing image: ${e.message}")
                }
            } else {
                showError("❌ Pengambilan gambar dibatalkan.")
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
                        showError("❌ Gagal mengambil gambar dari galeri.")
                    }
                } catch (e: Exception) {
                    showError("❌ Error processing gallery image: ${e.message}")
                }
            } else {
                showError("❌ Pemilihan gambar dibatalkan.")
            }
        }

        // Permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraLauncher.launch(cameraIntent)
            } else {
                showError("❌ Izin kamera diperlukan untuk mengambil foto.")
            }
        }
    }

    private fun showError(message: String) {
        result?.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
            byteBuffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)
            var pixel = 0
            for (i in 0..<imageSize) {
                for (j in 0..<imageSize) {
                    val `val` = intValues[pixel++] // RGB
                    byteBuffer.putFloat(((`val` shr 16) and 0xFF) * (1f / 255f))
                    byteBuffer.putFloat(((`val` shr 8) and 0xFF) * (1f / 255f))
                    byteBuffer.putFloat((`val` and 0xFF) * (1f / 255f))
                }
            }

            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result.
            val outputs: ModelUnquant.Outputs = model.process(inputFeature0)
            val outputFeature0: TensorBuffer = outputs.getOutputFeature0AsTensorBuffer()

            val confidences = outputFeature0.floatArray
            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidences.indices) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i]
                    maxPos = i
                }
            }

            val classes = loadLabels(this)

            if (maxPos < classes.size) {
                result!!.text = classes[maxPos]

                // Buat list pasangan (index, confidence) dan urutkan berdasarkan confidence tertinggi
                val classConfidencePairs = mutableListOf<Pair<Int, Float>>()
                for (i in confidences.indices) {
                    classConfidencePairs.add(Pair(i, confidences[i]))
                }

                // Urutkan berdasarkan confidence dari tertinggi ke terendah
                classConfidencePairs.sortByDescending { it.second }

                // Ambil hanya 3 tertinggi
                val top3 = classConfidencePairs.take(3)

                // Format string untuk menampilkan top 3
                var s = "Top 3 Predictions:\n"
                for ((index, conf) in top3) {
                    if (index < classes.size) {
                        s += String.format(Locale.getDefault(), "%s: %.1f%%\n", classes[index], conf * 100)
                    }
                }

                confidence!!.text = s
            } else {
                showError("❌ Error: Invalid classification result")
            }

        } catch (e: Exception) {
            showError("❌ Error classifying image: ${e.message}")
        } finally {
            // Releases model resources if no longer used.
            model?.close()
        }
    }

    private fun showCameraSelectionDialog() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val availableCameras = getAvailableCameras(cameraManager)

            if (availableCameras.isEmpty()) {
                showError("❌ Tidak ada kamera yang tersedia")
                return
            }

            val cameraNames = availableCameras.map { it.second }.toTypedArray()

            val builder = AlertDialog.Builder(this)
            builder.setTitle("📷 Pilih Kamera")
                .setItems(cameraNames) { dialog, which ->
                    val selectedCameraId = availableCameras[which].first
                    launchCameraWithId(selectedCameraId)
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } catch (e: Exception) {
            showError("❌ Error accessing camera: ${e.message}")
        }
    }

    private fun getAvailableCameras(cameraManager: CameraManager): List<Pair<String, String>> {
        val cameras = mutableListOf<Pair<String, String>>()

        try {
            val cameraIdList = cameraManager.cameraIdList

            for (cameraId in cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val cameraName = when (facing) {
                    CameraCharacteristics.LENS_FACING_BACK -> "📷 Kamera Belakang"
                    CameraCharacteristics.LENS_FACING_FRONT -> "🤳 Kamera Depan"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "🔌 Kamera USB Eksternal"
                    else -> "📹 Kamera Lainnya"
                }
                cameras.add(Pair(cameraId, cameraName))
            }
        } catch (e: Exception) {
            // Fallback untuk perangkat yang tidak mendukung Camera2 API
            cameras.add(Pair("0", "📷 Kamera Default"))
        }

        return cameras
    }

    private fun launchCameraWithId(cameraId: String) {
        try {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            // Untuk kamera tertentu, kita bisa menggunakan extra intent
            when (cameraId) {
                "0" -> {
                    // Kamera belakang (default)
                    cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 0)
                }
                "1" -> {
                    // Kamera depan
                    cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 1)
                }
                else -> {
                    // Kamera lain atau eksternal
                    cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 0)
                }
            }

            cameraLauncher.launch(cameraIntent)
            Toast.makeText(this, "📸 Membuka kamera...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            showError("❌ Error launching camera: ${e.message}")
        }
    }
}
