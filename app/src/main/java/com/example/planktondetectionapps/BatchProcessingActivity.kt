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
        // This is a simplified classification - you'll need to implement the actual TensorFlow Lite model inference
        // For now, using dummy data
        val predictions = arrayOf(
            "Chaetoceros" to 0.85f,
            "Coscinodiscus" to 0.12f,
            "Navicula" to 0.03f
        )

        val topPrediction = predictions[0]
        val top3Results = predictions.toList()

        return BatchResult(
            imageUri = uri,
            bitmap = bitmap,
            prediction = topPrediction.first,
            confidence = topPrediction.second,
            top3Results = top3Results
        )
    }

    private fun exportResults() {
        // Create CSV content
        val csvContent = StringBuilder()
        csvContent.append("No,Nama File,Prediksi,Confidence,Top 2,Confidence 2,Top 3,Confidence 3\n")

        batchResults.forEachIndexed { index, result ->
            val fileName = getFileNameFromUri(result.imageUri)
            csvContent.append("${index + 1},$fileName,${result.prediction},${result.confidence}")

            if (result.top3Results.size > 1) {
                csvContent.append(",${result.top3Results[1].first},${result.top3Results[1].second}")
            } else {
                csvContent.append(",,")
            }

            if (result.top3Results.size > 2) {
                csvContent.append(",${result.top3Results[2].first},${result.top3Results[2].second}")
            } else {
                csvContent.append(",,")
            }

            csvContent.append("\n")
        }

        // Share or save CSV
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, csvContent.toString())
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Hasil Batch Processing Plankton")
        startActivity(Intent.createChooser(shareIntent, "Export Hasil"))
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
}
