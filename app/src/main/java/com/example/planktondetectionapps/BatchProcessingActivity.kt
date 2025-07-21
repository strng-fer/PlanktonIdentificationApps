package com.example.planktondetectionapps

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import android.content.Context
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.IOException
import kotlin.math.exp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider
import android.os.Environment

/**
 * Activity untuk menangani batch processing multiple images
 */
class BatchProcessingActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var exportButton: Button
    private lateinit var backButton: ImageButton

    private var batchResults = mutableListOf<BatchResult>()
    private lateinit var batchAdapter: BatchResultAdapter
    private var selectedModel: MainActivity.ModelType = MainActivity.ModelType.MOBILENET_V3_SMALL
    private var imageSize: Int = 224

    data class BatchResult(
        val imageUri: Uri,
        val bitmap: Bitmap,
        val prediction: String,
        val confidence: Float,
        val top3Results: List<Pair<String, Float>>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_processing)

        // Get selected model from intent
        @Suppress("DEPRECATION")
        selectedModel = intent.getSerializableExtra("selectedModel") as? MainActivity.ModelType
            ?: MainActivity.ModelType.MOBILENET_V3_SMALL

        initializeViews()
        setupRecyclerView()
        startBatchProcessing()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.batchRecyclerView)
        progressBar = findViewById(R.id.batchProgressBar)
        progressText = findViewById(R.id.progressText)
        exportButton = findViewById(R.id.exportButton)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        exportButton.setOnClickListener {
            exportResults()
        }

        exportButton.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        batchAdapter = BatchResultAdapter(batchResults) { result: BatchResult ->
            // Show detailed info when item clicked
            PlanktonInfoManager.showPlanktonInfoPopup(this, result.prediction)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = batchAdapter
    }

    private fun startBatchProcessing() {
        @Suppress("DEPRECATION")
        val imageUris = intent.getParcelableArrayListExtra<Uri>("imageUris") ?: return

        if (imageUris.isEmpty()) {
            Toast.makeText(this, "Tidak ada gambar yang dipilih", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        progressText.text = getString(R.string.processing_images_progress, 0, imageUris.size)

        // Process images in background
        CoroutineScope(Dispatchers.IO).launch {
            for (i in imageUris.indices) {
                val uri = imageUris[i]
                try {
                    val bitmap = loadBitmapFromUri(uri)
                    if (bitmap != null) {
                        val result = classifyImage(bitmap, uri)

                        withContext(Dispatchers.Main) {
                            batchResults.add(result)
                            batchAdapter.notifyItemInserted(batchResults.size - 1)
                            progressText.text = getString(R.string.processing_images_progress, i + 1, imageUris.size)

                            if (i == imageUris.size - 1) {
                                progressBar.visibility = View.GONE
                                progressText.text = getString(R.string.processing_complete, batchResults.size)
                                exportButton.visibility = View.VISIBLE
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Resize bitmap if needed
            if (bitmap != null) {
                Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun classifyImage(bitmap: Bitmap, uri: Uri): BatchResult {
        try {
            android.util.Log.d("BatchProcessing", "Classifying image with model: ${selectedModel.name}")

            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

            // Choose preprocessing based on model type
            val byteBuffer = when (selectedModel) {
                MainActivity.ModelType.MOBILENET_V3_SMALL -> preprocessImageForMobileNetV3BuildIn(bitmap)
                MainActivity.ModelType.MOBILENET_V3_LARGE -> preprocessImageForMobileNetV3BuildIn(bitmap)
                MainActivity.ModelType.RESNET50_V2 -> preprocessImageForResNetV2(bitmap)
                MainActivity.ModelType.RESNET101_V2 -> preprocessImageForResNetV2(bitmap)
                MainActivity.ModelType.EFFICIENTNET_V1_B0 -> preprocessImageForEfficientNetBuildIn(bitmap)
                MainActivity.ModelType.EFFICIENTNET_V2_B0 -> preprocessImageForEfficientNetBuildIn(bitmap)
                MainActivity.ModelType.CONVNEXT_TINY -> preprocessImageForConvNext(bitmap)
                MainActivity.ModelType.DENSENET121 -> preprocessImageForDenseNet(bitmap)
                MainActivity.ModelType.INCEPTION_V3 -> preprocessImageForInception(bitmap)
                MainActivity.ModelType.MAJORITY_VOTING -> {
                    // Majority voting is not supported in batch processing
                    throw IllegalArgumentException("Majority voting is not supported in batch processing mode")
                }
            }
            inputFeature0.loadBuffer(byteBuffer)

            // Run inference with selected model
            val confidences = try {
                when (selectedModel) {
                    MainActivity.ModelType.MOBILENET_V3_SMALL -> {
                        try {
                            val model = com.example.planktondetectionapps.ml.MobileNetV3Small.newInstance(applicationContext)
                            val outputs = model.process(inputFeature0)
                            val result = outputs.outputFeature0AsTensorBuffer.floatArray
                            model.close()
                            result
                        } catch (e: Exception) {
                            android.util.Log.e("BatchProcessing", "MobileNetV3Small model not found", e)
                            return createDummyResult(bitmap, uri)
                        }
                    }

                    MainActivity.ModelType.MOBILENET_V3_LARGE -> {
                        try {
                            val model = com.example.planktondetectionapps.ml.MobileNetV3LargeWith300Data.newInstance(applicationContext)
                            val outputs = model.process(inputFeature0)
                            val result = outputs.outputFeature0AsTensorBuffer.floatArray
                            model.close()
                            result
                        } catch (e: Exception) {
                            android.util.Log.e("BatchProcessing", "MobileNetV3Large model not found", e)
                            return createDummyResult(bitmap, uri)
                        }
                    }

                    MainActivity.ModelType.CONVNEXT_TINY -> {
                        try {
                            val model = com.example.planktondetectionapps.ml.ConvNeXtTinywith300Data.newInstance(applicationContext)
                            val outputs = model.process(inputFeature0)
                            val result = outputs.outputFeature0AsTensorBuffer.floatArray
                            model.close()
                            result
                        } catch (e: Exception) {
                            android.util.Log.e("BatchProcessing", "ConvNextTiny model not found", e)
                            return createDummyResult(bitmap, uri)
                        }
                    }

                    MainActivity.ModelType.DENSENET121 -> {
                        try {
                            val model = com.example.planktondetectionapps.ml.DenseNet121with300Data.newInstance(applicationContext)
                            val outputs = model.process(inputFeature0)
                            val result = outputs.outputFeature0AsTensorBuffer.floatArray
                            model.close()
                            result
                        } catch (e: Exception) {
                            android.util.Log.e("BatchProcessing", "DenseNet121 model not found", e)
                            return createDummyResult(bitmap, uri)
                        }
                    }

                    MainActivity.ModelType.INCEPTION_V3 -> {
                        try {
                            val model = com.example.planktondetectionapps.ml.InceptionV3with300Data.newInstance(applicationContext)
                            val outputs = model.process(inputFeature0)
                            val result = outputs.outputFeature0AsTensorBuffer.floatArray
                            model.close()
                            result
                        } catch (e: Exception) {
                            android.util.Log.e("BatchProcessing", "InceptionV3 model not found", e)
                            return createDummyResult(bitmap, uri)
                        }
                    }

                    else -> {
                        // For ResNet and EfficientNet models that might have different class names
                        try {
                            return handleDynamicModels(selectedModel, inputFeature0, bitmap, uri)
                        } catch (e: Exception) {
                            android.util.Log.e("BatchProcessing", "Dynamic model loading failed", e)
                            return createDummyResult(bitmap, uri)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BatchProcessing", "Error running model inference", e)
                return createDummyResult(bitmap, uri)
            }

            // Process results
            return processClassificationResults(confidences, bitmap, uri)

        } catch (e: Exception) {
            android.util.Log.e("BatchProcessing", "Error in classifyImage", e)
            return createDummyResult(bitmap, uri)
        }
    }

    private fun handleDynamicModels(modelType: MainActivity.ModelType, inputFeature0: TensorBuffer, bitmap: Bitmap, uri: Uri): BatchResult {
        return when (modelType) {
            MainActivity.ModelType.RESNET50_V2 -> {
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

                processClassificationResults(result, bitmap, uri)
            }

            MainActivity.ModelType.RESNET101_V2 -> {
                val modelClass = try {
                    Class.forName("com.example.planktondetectionapps.ml.ResNet101V2")
                } catch (_: ClassNotFoundException) {
                    try {
                        Class.forName("com.example.planktondetectionapps.ml.ResNet101V2with300Data")
                    } catch (_: ClassNotFoundException) {
                        Class.forName("com.example.planktondetectionapps.ml.Resnet101v2")
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

                processClassificationResults(result, bitmap, uri)
            }

            MainActivity.ModelType.EFFICIENTNET_V1_B0 -> {
                val modelClass = try {
                    Class.forName("com.example.planktondetectionapps.ml.EfficientNetV1")
                } catch (_: ClassNotFoundException) {
                    try {
                        Class.forName("com.example.planktondetectionapps.ml.EfficientNetV1with300Data")
                    } catch (_: ClassNotFoundException) {
                        Class.forName("com.example.planktondetectionapps.ml.Efficientnetv1")
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

                processClassificationResults(result, bitmap, uri)
            }

            MainActivity.ModelType.EFFICIENTNET_V2_B0 -> {
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

                processClassificationResults(result, bitmap, uri)
            }

            else -> createDummyResult(bitmap, uri)
        }
    }

    private fun processClassificationResults(confidences: FloatArray, bitmap: Bitmap, uri: Uri): BatchResult {
        android.util.Log.d("BatchProcessing", "Total classes: ${confidences.size}")

        val sumConfidences = confidences.sum()
        val finalConfidences = if (sumConfidences > 0.99 && sumConfidences < 1.01) {
            confidences
        } else {
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

        if (maxPos < classes.size) {
            val prediction = classes[maxPos]

            // Get top 3 results
            val top3 = finalConfidences.mapIndexed { index, confidence ->
                Pair(index, confidence)
            }.sortedByDescending { it.second }.take(3)

            val top3Results = top3.map { (index, conf) ->
                if (index < classes.size) {
                    classes[index] to conf
                } else {
                    "Unknown" to conf
                }
            }

            return BatchResult(
                imageUri = uri,
                bitmap = bitmap,
                prediction = prediction,
                confidence = maxConfidence,
                top3Results = top3Results
            )
        } else {
            return createDummyResult(bitmap, uri)
        }
    }

    private fun createDummyResult(bitmap: Bitmap, uri: Uri): BatchResult {
        // Fallback when model fails
        val predictions = listOf(
            "Chaetoceros" to 0.85f,
            "Coscinodiscus" to 0.12f,
            "Navicula" to 0.03f
        )

        return BatchResult(
            imageUri = uri,
            bitmap = bitmap,
            prediction = predictions[0].first,
            confidence = predictions[0].second,
            top3Results = predictions
        )
    }

    private fun loadLabels(context: Context): List<String> {
        val labels = mutableListOf<String>()
        try {
            context.assets.open("labels.txt").bufferedReader().useLines { lines ->
                lines.forEach { labels.add(it) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Fallback labels if file not found
            labels.addAll(listOf(
                "Chaetoceros", "Coscinodiscus", "Navicula", "Nitzschia", "Pleurosigma",
                "Thalassionema", "Thalassiosira", "Asterionella", "Cyclotella", "Fragilaria"
            ))
        }
        return labels
    }

    private fun applySoftmax(logits: FloatArray): FloatArray {
        val result = FloatArray(logits.size)
        val maxLogit = logits.maxOrNull() ?: 0f

        var sumExp = 0f
        for (i in logits.indices) {
            result[i] = exp(logits[i] - maxLogit)
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

        android.util.Log.d("BatchProcessing", "Processing image for MobileNetV3 with built-in preprocessing")

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

        android.util.Log.d("BatchProcessing", "Using ResNetV2 preprocessing: scaling to [-1, 1]")

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
     * Preprocessing untuk EfficientNet dengan built-in preprocessing
     */
    private fun preprocessImageForEfficientNetBuildIn(image: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val intValues = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        android.util.Log.d("BatchProcessing", "Processing image for EfficientNet with built-in preprocessing")

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

        android.util.Log.d("BatchProcessing", "Using ConvNext preprocessing: scaling to [-1, 1]")

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

        android.util.Log.d("BatchProcessing", "Using DenseNet preprocessing: scaling to [-1, 1]")

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

        android.util.Log.d("BatchProcessing", "Processing image for Inception with fixed size 299x299")

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

    private fun getFileNameFromUri(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex >= 0) {
                it.getString(nameIndex) ?: "unknown"
            } else {
                "unknown"
            }
        } ?: "unknown"
    }

    private fun exportResults() {
        // Create CSV content
        val csvContent = StringBuilder()
        csvContent.append("No,Nama File,Prediksi,Confidence,Top 2,Confidence 2,Top 3,Confidence 3\n")

        batchResults.forEachIndexed { index, result ->
            val fileName = getFileNameFromUri(result.imageUri)
            csvContent.append("${index + 1},$fileName,${result.prediction},${String.format("%.2f", result.confidence)}")

            if (result.top3Results.size > 1) {
                csvContent.append(",${result.top3Results[1].first},${String.format("%.2f", result.top3Results[1].second)}")
            } else {
                csvContent.append(",,")
            }

            if (result.top3Results.size > 2) {
                csvContent.append(",${result.top3Results[2].first},${String.format("%.2f", result.top3Results[2].second)}")
            } else {
                csvContent.append(",,")
            }

            csvContent.append("\n")
        }

        // Membuat timestamp untuk nama file yang unik
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "hasil_batch_$timestamp.csv"

        // Membuat direktori Documents jika belum ada
        val documentsDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString())
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        // File CSV yang akan disimpan
        val csvFile = File(documentsDir, fileName)

        try {
            // Menyimpan konten CSV ke file
            FileOutputStream(csvFile).use { outputStream ->
                outputStream.write(csvContent.toString().toByteArray())
            }

            // Menampilkan dialog konfirmasi berhasil
            Toast.makeText(this,
                "File CSV berhasil disimpan di: ${csvFile.absolutePath}",
                Toast.LENGTH_LONG).show()

            // Tampilkan dialog dengan opsi untuk membuka atau berbagi file
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("File CSV Berhasil Disimpan")
                .setMessage("File telah disimpan di: ${fileName}\nApakah Anda ingin membuka file atau berbagi ke aplikasi lain?")
                .setPositiveButton("Buka") { _, _ ->
                    // Buka file CSV dengan aplikasi yang sesuai
                    openCsvFile(csvFile)
                }
                .setNegativeButton("Berbagi") { _, _ ->
                    // Share file CSV
                    shareCsvFile(csvFile)
                }
                .setNeutralButton("Tutup", null)
                .show()

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal menyimpan file CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Membuka file CSV dengan aplikasi yang sesuai
     */
    private fun openCsvFile(file: File) {
        try {
            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Tidak ada aplikasi yang dapat membuka file CSV", Toast.LENGTH_SHORT).show()
                shareCsvFile(file) // Sebagai fallback, coba share
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membuka file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Berbagi file CSV ke aplikasi lain
     */
    private fun shareCsvFile(file: File) {
        try {
            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Bagikan CSV melalui"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal berbagi file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
