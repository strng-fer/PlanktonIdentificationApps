package com.example.planktondetectionapps

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Simple adapter untuk menampilkan daftar plankton dengan struktur data yang baru
 */
class SimplePlanktonAdapter(private val planktonList: List<PlanktonInfo>) :
    RecyclerView.Adapter<SimplePlanktonAdapter.PlanktonViewHolder>() {

    /**
     * ViewHolder untuk item plankton
     */
    class PlanktonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val planktonThumbnail: ImageView = itemView.findViewById(R.id.planktonThumbnail)
        val planktonName: TextView = itemView.findViewById(R.id.planktonName)
        val planktonType: TextView = itemView.findViewById(R.id.planktonType)
        val planktonDescription: TextView = itemView.findViewById(R.id.planktonDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanktonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plankton, parent, false)
        return PlanktonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanktonViewHolder, position: Int) {
        val plankton = planktonList[position]

        // Debug logging
        android.util.Log.d("SimplePlanktonAdapter", "Binding position $position: ${plankton.name}")

        // Set text data
        holder.planktonName.text = plankton.name
        holder.planktonType.text = plankton.type
        holder.planktonDescription.text = plankton.description

        // Load main image
        try {
            holder.planktonThumbnail.setImageResource(plankton.mainImageResId)
        } catch (e: Exception) {
            android.util.Log.e("SimplePlanktonAdapter", "Error loading main image: ${e.message}")
            holder.planktonThumbnail.setImageResource(R.drawable.ic_plankton_logo)
        }
    }

    override fun getItemCount(): Int {
        val count = planktonList.size
        android.util.Log.d("SimplePlanktonAdapter", "getItemCount: $count")
        return count
    }
}
