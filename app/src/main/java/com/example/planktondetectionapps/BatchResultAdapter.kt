package com.example.planktondetectionapps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter untuk menampilkan hasil batch processing dalam RecyclerView
 */
class BatchResultAdapter(
    private val results: List<BatchProcessingActivity.BatchResult>,
    private val onItemClick: (BatchProcessingActivity.BatchResult) -> Unit
) : RecyclerView.Adapter<BatchResultAdapter.BatchResultViewHolder>() {

    class BatchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.batchItemImage)
        val predictionText: TextView = itemView.findViewById(R.id.batchItemPrediction)
        val confidenceText: TextView = itemView.findViewById(R.id.batchItemConfidence)
        val fileNameText: TextView = itemView.findViewById(R.id.batchItemFileName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_batch_result, parent, false)
        return BatchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: BatchResultViewHolder, position: Int) {
        val result = results[position]

        holder.imageView.setImageBitmap(result.bitmap)
        holder.predictionText.text = result.prediction
        holder.confidenceText.text = "${(result.confidence * 100).toInt()}%"
        holder.fileNameText.text = "Gambar ${position + 1}"

        holder.itemView.setOnClickListener {
            onItemClick(result)
        }
    }

    override fun getItemCount(): Int = results.size
}
