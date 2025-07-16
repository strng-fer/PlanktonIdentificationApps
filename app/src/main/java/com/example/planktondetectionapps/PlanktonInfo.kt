package com.example.planktondetectionapps

/**
 * Data class untuk menyimpan informasi plankton
 */
data class PlanktonInfo(
    val name: String,
    val description: String,
    val type: String,
    val mainImageResId: Int,
    val additionalImages: List<Int> = emptyList()
)
