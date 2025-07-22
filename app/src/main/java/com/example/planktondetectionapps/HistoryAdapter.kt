package com.example.planktondetectionapps

import android.content.Context
import android.graphics.BitmapFactory
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
    private val onDeleteClick: (HistoryEntry) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.historyImageView)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val classificationText: TextView = view.findViewById(R.id.classificationText)
        val confidenceText: TextView = view.findViewById(R.id.confidenceText)
        val modelText: TextView = view.findViewById(R.id.modelText)
        val feedbackText: TextView = view.findViewById(R.id.feedbackText)
        val feedbackButton: Button = view.findViewById(R.id.feedbackButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
        val feedbackContainer: LinearLayout = view.findViewById(R.id.feedbackContainer)
        val statusIcon: ImageView = view.findViewById(R.id.statusIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val entry = historyList[position]

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
            holder.feedbackContainer.visibility = View.VISIBLE
            holder.feedbackText.text = entry.userFeedback
            holder.feedbackButton.text = "Edit Feedback"

            // Set status icon based on feedback
            when (entry.isCorrect) {
                true -> {
                    holder.statusIcon.setImageResource(R.drawable.ic_check_circle)
                    holder.statusIcon.setColorFilter(context.getColor(android.R.color.holo_green_dark))
                }
                false -> {
                    holder.statusIcon.setImageResource(R.drawable.ic_error_circle)
                    holder.statusIcon.setColorFilter(context.getColor(android.R.color.holo_red_dark))
                    if (entry.correctClass.isNotEmpty()) {
                        holder.feedbackText.text = "${entry.userFeedback}\nCorrect class: ${entry.correctClass}"
                    }
                }
                null -> {
                    holder.statusIcon.setImageResource(R.drawable.ic_help_circle)
                    holder.statusIcon.setColorFilter(context.getColor(android.R.color.darker_gray))
                }
            }
            holder.statusIcon.visibility = View.VISIBLE
        } else {
            holder.feedbackContainer.visibility = View.GONE
            holder.feedbackButton.text = "Add Feedback"
            holder.statusIcon.visibility = View.GONE
        }

        // Set click listeners
        holder.feedbackButton.setOnClickListener {
            onFeedbackClick(entry)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(entry)
        }
    }

    override fun getItemCount(): Int = historyList.size

    /**
     * Update data list
     */
    fun updateData(newList: List<HistoryEntry>) {
        historyList.clear()
        historyList.addAll(newList)
        notifyDataSetChanged()
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
