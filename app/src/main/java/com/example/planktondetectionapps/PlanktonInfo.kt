package com.example.planktondetectionapps

/**
 * Data class untuk menyimpan informasi plankton
 */
data class PlanktonInfo(
    val name: String,
    val description: String,
    val classification: String, // Klasifikasi plankton (Diatom, Dinoflagellata, dll)
    val mainImage: Int = R.drawable.ic_microscope, // Gambar utama
    val sampleImages: List<Int> = listOf(
        R.drawable.ic_microscope,
        R.drawable.ic_microscope,
        R.drawable.ic_microscope
    ) // 3 gambar contoh
)
