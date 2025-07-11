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
 * Adapter sederhana untuk testing - memastikan semua item dapat ditampilkan
 */
class SimplePlanktonAdapter(private val planktonList: List<PlanktonInfo>) :
    RecyclerView.Adapter<SimplePlanktonAdapter.PlanktonViewHolder>() {

    class PlanktonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val planktonImage: ImageView = itemView.findViewById(R.id.planktonImage)
        val planktonName: TextView = itemView.findViewById(R.id.planktonName)
        val planktonDescription: TextView = itemView.findViewById(R.id.planktonDescription)
        val sampleImage1: ImageView = itemView.findViewById(R.id.sampleImage1)
        val sampleImage2: ImageView = itemView.findViewById(R.id.sampleImage2)
        val sampleImage3: ImageView = itemView.findViewById(R.id.sampleImage3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanktonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plankton, parent, false)
        return PlanktonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanktonViewHolder, position: Int) {
        val plankton = planktonList[position]

        // Log setiap item yang di-bind
        android.util.Log.d("SimplePlanktonAdapter", "Binding position $position: ${plankton.name}")

        // Set text terlebih dahulu
        holder.planktonName.text = plankton.name
        holder.planktonDescription.text = plankton.description

        // Load gambar dengan error handling yang ketat
        try {
            holder.planktonImage.setImageResource(plankton.imageResource)
        } catch (e: Exception) {
            android.util.Log.e("SimplePlanktonAdapter", "Error loading main image: ${e.message}")
            holder.planktonImage.setImageResource(R.drawable.ic_microscope)
        }

        // Load sample images
        try {
            if (plankton.sampleImages.isNotEmpty()) {
                holder.sampleImage1.setImageResource(plankton.sampleImages.getOrElse(0) { R.drawable.ic_microscope })
                holder.sampleImage2.setImageResource(plankton.sampleImages.getOrElse(1) { R.drawable.ic_microscope })
                holder.sampleImage3.setImageResource(plankton.sampleImages.getOrElse(2) { R.drawable.ic_microscope })
            } else {
                holder.sampleImage1.setImageResource(R.drawable.ic_microscope)
                holder.sampleImage2.setImageResource(R.drawable.ic_microscope)
                holder.sampleImage3.setImageResource(R.drawable.ic_microscope)
            }
        } catch (e: Exception) {
            android.util.Log.e("SimplePlanktonAdapter", "Error loading sample images: ${e.message}")
            holder.sampleImage1.setImageResource(R.drawable.ic_microscope)
            holder.sampleImage2.setImageResource(R.drawable.ic_microscope)
            holder.sampleImage3.setImageResource(R.drawable.ic_microscope)
        }

        // Simple click listeners
        holder.planktonImage.setOnClickListener {
            showSimplePopup(holder.itemView, plankton.imageResource, plankton.name)
        }
    }

    override fun getItemCount(): Int {
        val count = planktonList.size
        android.util.Log.d("SimplePlanktonAdapter", "getItemCount: $count")
        return count
    }

    private fun showSimplePopup(view: View, imageResource: Int, planktonName: String) {
        val context = view.context
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_image_popup)

        val fullSizeImage = dialog.findViewById<ImageView>(R.id.fullSizeImage)
        val imageTitle = dialog.findViewById<TextView>(R.id.imageTitle)
        val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)

        try {
            fullSizeImage.setImageResource(imageResource)
        } catch (e: Exception) {
            fullSizeImage.setImageResource(R.drawable.ic_microscope)
        }

        imageTitle.text = planktonName

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
