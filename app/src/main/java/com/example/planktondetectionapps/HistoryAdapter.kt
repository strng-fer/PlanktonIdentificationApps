package com.example.planktondetectionapps

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter untuk menampilkan daftar riwayat klasifikasi
 */
class HistoryAdapter(
    private val context: Context,
    private var historyList: MutableList<HistoryEntry>,
    private val onFeedbackClick: (HistoryEntry) -> Unit,
    private val onDeleteClick: (HistoryEntry) -> Unit,
    private val onItemClick: (HistoryEntry) -> Unit = {} // Add click handler for detailed view
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.historyImageView)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val classificationText: TextView = view.findViewById(R.id.classificationText)
        val confidenceText: TextView = view.findViewById(R.id.confidenceText)
        val modelText: TextView = view.findViewById(R.id.modelText)
        val feedbackText: TextView = view.findViewById(R.id.feedbackText)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
        val feedbackContainer: LinearLayout = view.findViewById(R.id.feedbackContainer)
        val statusIcon: ImageView = view.findViewById(R.id.statusIcon)
        val feedbackResultContainer: LinearLayout = view.findViewById(R.id.feedbackResultContainer)
        val feedbackResultText: TextView = view.findViewById(R.id.feedbackResultText)
        val actualClassificationContainer: LinearLayout = view.findViewById(R.id.actualClassificationContainer)
        val actualClassificationText: TextView = view.findViewById(R.id.actualClassificationText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        Log.d("HistoryAdapter", "=== onCreateViewHolder() called ===")
        val view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false)
        Log.d("HistoryAdapter", "ViewHolder created successfully")
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        Log.d("HistoryAdapter", "=== onBindViewHolder() called for position: $position ===")

        if (position >= historyList.size) {
            Log.e("HistoryAdapter", "Position $position is out of bounds for list size ${historyList.size}")
            return
        }

        val entry = historyList[position]
        Log.d("HistoryAdapter", "Binding entry: ID=${entry.id}, Result=${entry.classificationResult}")

        // Load image
        try {
            val imageFile = File(entry.imagePath)
            if (imageFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                holder.imageView.setImageBitmap(bitmap)
            } else {
                holder.imageView.setImageResource(R.drawable.ic_image_placeholder)
            }
        } catch (e: Exception) {
            holder.imageView.setImageResource(R.drawable.ic_image_placeholder)
        }

        // Set timestamp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.timestampText.text = dateFormat.format(entry.timestamp)

        // Set classification result
        holder.classificationText.text = entry.classificationResult

        // Set confidence with color coding
        val confidencePercent = (entry.confidence * 100).toInt()
        holder.confidenceText.text = "${confidencePercent}%"

        // Color code confidence
        when {
            entry.confidence >= 0.8f -> holder.confidenceText.setTextColor(context.getColor(android.R.color.holo_green_dark))
            entry.confidence >= 0.6f -> holder.confidenceText.setTextColor(context.getColor(android.R.color.holo_orange_dark))
            else -> holder.confidenceText.setTextColor(context.getColor(android.R.color.holo_red_dark))
        }

        // Set model name
        holder.modelText.text = entry.modelUsed

        // Handle feedback display
        if (entry.userFeedback.isNotEmpty()) {
            Log.d("HistoryAdapter", "Entry has feedback - showing feedback UI")
            holder.feedbackContainer.visibility = View.VISIBLE
            holder.feedbackText.text = entry.userFeedback

            // Display feedback result
            holder.feedbackResultContainer.visibility = View.VISIBLE

            // Debug logging
            Log.d("HistoryAdapter", "Entry ${entry.id}: isCorrect=${entry.isCorrect}, correctClass='${entry.correctClass}'")

            when (entry.isCorrect) {
                true -> {
                    Log.d("HistoryAdapter", "Showing correct prediction feedback")
                    holder.feedbackResultText.text = "Prediksi Benar"
                    holder.feedbackResultText.setTextColor(context.getColor(android.R.color.holo_green_dark))
                    holder.statusIcon.setImageResource(R.drawable.ic_check_circle)
                    holder.statusIcon.setColorFilter(context.getColor(android.R.color.holo_green_dark))
                    holder.actualClassificationContainer.visibility = View.GONE
                    Log.d("HistoryAdapter", "Hiding actual classification container for correct prediction")
                }
                false -> {
                    Log.d("HistoryAdapter", "Showing incorrect prediction feedback")
                    holder.feedbackResultText.text = "Prediksi Salah"
                    holder.feedbackResultText.setTextColor(context.getColor(android.R.color.holo_red_dark))
                    holder.statusIcon.setImageResource(R.drawable.ic_error_circle)
                    holder.statusIcon.setColorFilter(context.getColor(android.R.color.holo_red_dark))

                    // Show actual classification if available
                    if (entry.correctClass.isNotEmpty()) {
                        Log.d("HistoryAdapter", "Showing actual classification: '${entry.correctClass}'")
                        Log.d("HistoryAdapter", "Setting actualClassificationContainer visibility to VISIBLE")
                        holder.actualClassificationContainer.visibility = View.VISIBLE
                        holder.actualClassificationText.text = entry.correctClass
                        Log.d("HistoryAdapter", "Set actualClassificationText to: '${entry.correctClass}'")
                    } else {
                        Log.d("HistoryAdapter", "No correct class provided - hiding actual classification")
                        holder.actualClassificationContainer.visibility = View.GONE
                    }
                }
                null -> {
                    Log.d("HistoryAdapter", "Showing neutral feedback")
                    holder.feedbackResultText.text = "Menunggu Verifikasi"
                    holder.feedbackResultText.setTextColor(context.getColor(android.R.color.darker_gray))
                    holder.statusIcon.setImageResource(R.drawable.ic_help_circle)
                    holder.statusIcon.setColorFilter(context.getColor(android.R.color.darker_gray))
                    holder.actualClassificationContainer.visibility = View.GONE
                    Log.d("HistoryAdapter", "Neutral feedback - hiding actual classification")
                }
            }
            holder.statusIcon.visibility = View.VISIBLE
            Log.d("HistoryAdapter", "Feedback UI setup completed for entry ${entry.id}")
        } else {
            holder.feedbackContainer.visibility = View.GONE
            holder.feedbackResultContainer.visibility = View.GONE
            holder.actualClassificationContainer.visibility = View.GONE
            holder.statusIcon.visibility = View.GONE
            Log.d("HistoryAdapter", "No feedback - hiding all feedback UI")
        }

        // Set click listeners
        holder.deleteButton.setOnClickListener {
            onDeleteClick(entry)
        }

        // Item click listener for detailed view
        holder.itemView.setOnClickListener {
            onItemClick(entry)
        }
    }

    override fun getItemCount(): Int = historyList.size

    /**
     * Update data list
     */
    fun updateData(newList: List<HistoryEntry>) {
        Log.d("HistoryAdapter", "=== updateData() called ===")
        Log.d("HistoryAdapter", "Current list size: ${historyList.size}")
        Log.d("HistoryAdapter", "New list size: ${newList.size}")

        historyList.clear()
        historyList.addAll(newList)

        Log.d("HistoryAdapter", "Updated list size: ${historyList.size}")
        Log.d("HistoryAdapter", "Calling notifyDataSetChanged()")

        notifyDataSetChanged()

        Log.d("HistoryAdapter", "=== updateData() finished ===")
    }

    /**
     * Remove item from list
     */
    fun removeItem(entry: HistoryEntry) {
        val position = historyList.indexOf(entry)
        if (position != -1) {
            historyList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * Update specific item
     */
    fun updateItem(entry: HistoryEntry) {
        val position = historyList.indexOfFirst { it.id == entry.id }
        if (position != -1) {
            historyList[position] = entry
            notifyItemChanged(position)
        }
    }
}
