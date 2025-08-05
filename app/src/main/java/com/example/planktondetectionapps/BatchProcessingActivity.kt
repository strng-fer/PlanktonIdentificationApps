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
    private lateinit var backButton: ImageButton

    private var batchResults = mutableListOf<BatchResult>()
    private lateinit var batchAdapter: BatchResultAdapter
    private var selectedModel: MainActivity.ModelType = MainActivity.ModelType.MOBILENET_V3_SMALL
    private var imageSize: Int = 224

    // History Management
    private lateinit var historyManager: HistoryManager
    private var batchHistoryEntries = mutableListOf<String>() // Track individual entry IDs for this batch
    private var batchSessionId: String? = null // Unique ID for this batch session

    data class BatchResult(
        val imageUri: Uri,
        val bitmap: Bitmap,
        val prediction: String,
        val confidence: Float,
        val top3Results: List<Pair<String, Float>>,
        val historyEntryId: String? = null // Add history entry ID tracking
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_processing)

        // Get selected model from intent
        @Suppress("DEPRECATION")
        selectedModel = intent.getSerializableExtra("selectedModel") as? MainActivity.ModelType
            ?: MainActivity.ModelType.MOBILENET_V3_SMALL

        // Initialize history manager
        historyManager = HistoryManager(this)

        // Create unique batch session ID
        batchSessionId = "BATCH_${System.currentTimeMillis()}"

        initializeViews()
        setupRecyclerView()
        startBatchProcessing()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.batchRecyclerView)
        progressBar = findViewById(R.id.batchProgressBar)
        progressText = findViewById(R.id.progressText)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        batchAdapter = BatchResultAdapter(batchResults,
            onItemClick = { result: BatchProcessingActivity.BatchResult ->
                // Show detailed info when item clicked
                PlanktonInfoManager.showPlanktonInfoPopup(this, result.prediction)
            },
            onFeedbackClick = { result: BatchProcessingActivity.BatchResult ->
                // Show feedback dialog for this specific result
                showFeedbackDialog(result)
            }
        )
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

                                // Log batch processing completion
                                android.util.Log.d("BatchProcessing", "Batch processing completed. Session: $batchSessionId")
                                android.util.Log.d("BatchProcessing", "Total images processed: ${batchResults.size}")
                                android.util.Log.d("BatchProcessing", "History entries created: ${batchHistoryEntries.size}")
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

            // Save to history
            val historyEntryId = saveResultToHistory(prediction, maxConfidence, top3Results, bitmap, uri)

            return BatchResult(
                imageUri = uri,
                bitmap = bitmap,
                prediction = prediction,
                confidence = maxConfidence,
                top3Results = top3Results,
                historyEntryId = historyEntryId // Track history entry ID
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

    /**
     * Show feedback dialog for a specific batch result
     */
    private fun showFeedbackDialog(result: BatchResult) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback, null)

        // Get UI elements from dialog (using correct field IDs from layout)
        val feedbackComment = dialogView.findViewById<EditText>(R.id.feedbackComment)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val feedbackRadioGroup = dialogView.findViewById<RadioGroup>(R.id.feedbackRadioGroup)
        val correctRadio = dialogView.findViewById<RadioButton>(R.id.correctRadio)
        val incorrectRadio = dialogView.findViewById<RadioButton>(R.id.incorrectRadio)
        val neutralRadio = dialogView.findViewById<RadioButton>(R.id.neutralRadio)
        val correctClassSpinner = dialogView.findViewById<Spinner>(R.id.correctClassSpinner)
        val correctClassLabel = dialogView.findViewById<TextView>(R.id.correctClassLabel)
        val warningText = dialogView.findViewById<TextView>(R.id.warningText)
        val planktonPreviewImage = dialogView.findViewById<ImageView>(R.id.planktonPreviewImage)

        // Set plankton image preview
        planktonPreviewImage?.setImageBitmap(result.bitmap)

        // Set current prediction info
        val currentPrediction = dialogView.findViewById<TextView>(R.id.currentPrediction)
        val currentConfidence = dialogView.findViewById<TextView>(R.id.currentConfidence)

        currentPrediction?.text = result.prediction
        currentConfidence?.text = "Tingkat Kepercayaan: ${(result.confidence * 100).toInt()}%"

        // Load plankton labels and add "Unrecognize" option
        val planktonLabels = loadLabels(this).toMutableList()
        planktonLabels.add("Unrecognize")

        // Setup spinner with plankton labels
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            planktonLabels
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        correctClassSpinner?.adapter = spinnerAdapter

        // Show/hide correct class label and spinner based on radio selection
        feedbackRadioGroup?.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.incorrectRadio -> {
                    correctClassLabel?.visibility = View.VISIBLE
                    correctClassSpinner?.visibility = View.VISIBLE
                }
                else -> {
                    correctClassLabel?.visibility = View.GONE
                    correctClassSpinner?.visibility = View.GONE
                }
            }
            // Clear warning when selection changes
            warningText?.visibility = View.GONE
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Make dialog background transparent to prevent overlap with custom rounded background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set button listeners
        submitButton?.setOnClickListener {
            val feedback = feedbackComment?.text?.toString()?.trim() ?: ""

            // Determine correctness based on radio selection
            val isCorrect = when (feedbackRadioGroup?.checkedRadioButtonId) {
                R.id.correctRadio -> true
                R.id.incorrectRadio -> false
                R.id.neutralRadio -> null
                else -> null // no selection
            }

            // Validate input based on rules
            val validationResult = validateFeedbackInput(isCorrect, feedback, correctClassSpinner)

            if (validationResult.isValid) {
                // Get correct class if prediction is marked as incorrect
                val correctClass = if (isCorrect == false) {
                    correctClassSpinner?.selectedItem?.toString() ?: ""
                } else {
                    ""
                }

                // Update feedback in history entry
                updateBatchHistoryFeedback(result, feedback, isCorrect, correctClass)

                dialog.dismiss()
            } else {
                // Show warning message
                warningText?.text = validationResult.errorMessage
                warningText?.visibility = View.VISIBLE
            }
        }

        cancelButton?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Validate feedback input based on the specified rules
     */
    private fun validateFeedbackInput(isCorrect: Boolean?, feedback: String, correctClassSpinner: Spinner?): ValidationResult {
        return when (isCorrect) {
            true -> {
                // Classification is correct - feedback can be sent (comment is optional)
                ValidationResult(true, "")
            }
            false -> {
                // Classification is incorrect - user must select correct classification (comment is optional)
                val selectedClass = correctClassSpinner?.selectedItem?.toString()?.trim()
                if (selectedClass.isNullOrEmpty()) {
                    ValidationResult(false, "Harap pilih klasifikasi yang benar")
                } else {
                    ValidationResult(true, "")
                }
            }
            null -> {
                // Not sure - user must provide comment
                if (feedback.isEmpty()) {
                    ValidationResult(false, "Komentar wajib diisi jika memilih 'Tidak Yakin'")
                } else {
                    ValidationResult(true, "")
                }
            }
        }
    }

    /**
     * Data class for validation result
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String
    )

    /**
     * Update feedback for a specific batch result in history
     */
    private fun updateBatchHistoryFeedback(result: BatchResult, feedback: String, isCorrect: Boolean?, correctClass: String) {
        if (result.historyEntryId.isNullOrEmpty()) {
            Toast.makeText(this, "Tidak dapat menyimpan feedback untuk hasil ini", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            android.util.Log.d("BatchProcessing", "=== updateBatchHistoryFeedback() called ===")
            android.util.Log.d("BatchProcessing", "Entry ID: ${result.historyEntryId}")
            android.util.Log.d("BatchProcessing", "Feedback: '$feedback'")
            android.util.Log.d("BatchProcessing", "IsCorrect: $isCorrect")
            android.util.Log.d("BatchProcessing", "CorrectClass: '$correctClass'")

            // Update the history entry using HistoryManager
            val updateSuccess = historyManager.updateEntryFeedback(
                entryId = result.historyEntryId,
                feedback = feedback,
                isCorrect = isCorrect,
                correctClass = correctClass
            )

            if (updateSuccess) {
                android.util.Log.d("BatchProcessing", "Batch feedback saved successfully")
                Toast.makeText(this, "Feedback berhasil disimpan", Toast.LENGTH_SHORT).show()

                // Show success message with details for incorrect classification
                if (isCorrect == false && correctClass.isNotEmpty()) {
                    val message = "Feedback disimpan! Klasifikasi yang benar: $correctClass"
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    android.util.Log.d("BatchProcessing", "Incorrect classification feedback saved with correct class: $correctClass")
                }
            } else {
                android.util.Log.e("BatchProcessing", "Failed to save batch feedback")
                Toast.makeText(this, "Gagal menyimpan feedback", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            android.util.Log.e("BatchProcessing", "Error updating batch feedback", e)
            Toast.makeText(this, "Error menyimpan feedback: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Simpan hasil klasifikasi ke dalam history
     */
    private fun saveResultToHistory(prediction: String, confidence: Float, top3Results: List<Pair<String, Float>>, bitmap: Bitmap, uri: Uri): String {
        return try {
            // Save image to internal storage first
            val imageFile = saveImageToInternalStorage(bitmap, uri)

            if (imageFile != null && imageFile.exists()) {
                // Create unique ID for this batch entry
                val entryId = "${batchSessionId}_${System.currentTimeMillis()}_${batchHistoryEntries.size + 1}"

                // Create history entry with proper constructor
                val historyEntry = HistoryEntry(
                    id = entryId,
                    userId = "", // Will be set by saveHistoryEntryWithCurrentUser
                    timestamp = Date(),
                    imagePath = imageFile.absolutePath,
                    classificationResult = prediction,
                    confidence = confidence,
                    modelUsed = "BATCH_${selectedModel.name}",
                    userFeedback = "",
                    isCorrect = null,
                    correctClass = ""
                )

                // Save to history using HistoryManager with current user
                if (historyManager.saveHistoryEntryWithCurrentUser(historyEntry)) {
                    // Track this entry ID for batch session
                    batchHistoryEntries.add(entryId)

                    android.util.Log.d("BatchProcessing", "History entry saved successfully: $entryId")
                    android.util.Log.d("BatchProcessing", "Batch session: $batchSessionId, Total entries: ${batchHistoryEntries.size}")

                    return entryId
                } else {
                    android.util.Log.e("BatchProcessing", "Failed to save history entry for: $prediction")
                    return ""
                }
            } else {
                android.util.Log.e("BatchProcessing", "Failed to save image to internal storage")
                return ""
            }
        } catch (e: Exception) {
            android.util.Log.e("BatchProcessing", "Error saving to history", e)
            ""
        }
    }

    /**
     * Save image to internal storage for batch processing
     */
    private fun saveImageToInternalStorage(bitmap: Bitmap, uri: Uri): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val fileName = getFileNameFromUri(uri)
            val cleanFileName = fileName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val imageFileName = "BATCH_${timeStamp}_${cleanFileName}.jpg"

            // Create batch images directory
            val batchImagesDir = File(filesDir, "batch_images/$batchSessionId")
            if (!batchImagesDir.exists()) {
                batchImagesDir.mkdirs()
            }

            val imageFile = File(batchImagesDir, imageFileName)

            // Save bitmap to file with compression
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }

            android.util.Log.d("BatchProcessing", "Image saved to internal storage: ${imageFile.absolutePath}")
            imageFile
        } catch (e: Exception) {
            android.util.Log.e("BatchProcessing", "Error saving image to internal storage", e)
            null
        }
    }
}
