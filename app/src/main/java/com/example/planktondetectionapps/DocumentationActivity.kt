package com.example.planktondetectionapps

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Activity untuk menampilkan galeri fitoplankton dengan optimasi performa
 */
class DocumentationActivity : AppCompatActivity() {

    // UI Components
    private lateinit var backButton: ImageButton
    private lateinit var planktonRecyclerView: RecyclerView
    private lateinit var optimizedAdapter: OptimizedPlanktonAdapter

    /**
     * Inisialisasi activity dan setup UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentation)

        initializeViews()
        setupListeners()
        setupOptimizedPlanktonGallery()
    }

    /**
     * Inisialisasi semua view components
     */
    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        planktonRecyclerView = findViewById(R.id.planktonRecyclerView)
    }

    /**
     * Setup listener untuk UI interactions
     */
    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }
    }

    /**
     * Setup galeri plankton dengan optimasi performa maksimal
     * Menggunakan thumbnail loading dan lazy loading untuk mencegah lag
     */
    private fun setupOptimizedPlanktonGallery() {
        val planktonList = getPlanktonData()

        // Gunakan OptimizedPlanktonAdapter untuk performa terbaik
        optimizedAdapter = OptimizedPlanktonAdapter(this, planktonList)

        planktonRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DocumentationActivity)
            adapter = optimizedAdapter

            // Optimasi RecyclerView untuk performa
            setHasFixedSize(true)
            setItemViewCacheSize(20) // Cache lebih banyak view
            setDrawingCacheEnabled(true)
            setDrawingCacheQuality(android.view.View.DRAWING_CACHE_QUALITY_LOW)

            // Enable recycler view pool
            recycledViewPool.setMaxRecycledViews(0, 15)
        }

        android.util.Log.d("DocumentationActivity", "Optimized adapter setup complete with ${planktonList.size} items")
    }

    /**
     * Clean up resources when activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        if (::optimizedAdapter.isInitialized) {
            optimizedAdapter.cleanup()
        }
    }

    /**
     * Data plankton dengan nama dan deskripsi - hanya gambar yang tersedia
     */
    private fun getPlanktonData(): List<PlanktonInfo> {
        return listOf(
            PlanktonInfo(
                "Achnanthes sp",
                "Diatom bentik yang berbentuk oval dengan struktur raphe yang khas. Sering ditemukan menempel pada substrat di lingkungan air tawar dan laut. Memiliki frustule simetris dengan ornamen yang halus.",
                "Diatom",
                R.drawable.achnanthes_sp_1,
                listOf(R.drawable.achnanthes_sp_1, R.drawable.achnanthes_sp_2, R.drawable.achnanthes_sp_1)
            ),
            PlanktonInfo(
                "Bacteriastrum delicatulum",
                "Diatom planktonik yang berbentuk rantai dengan seta panjang dan halus. Hidup di perairan laut dan membentuk koloni berbentuk spiral atau zigzag. Memiliki kloroplas yang efisien untuk fotosintesis.",
                "Diatom",
                R.drawable.bacteriastrum_delicatulum_1,
                listOf(R.drawable.bacteriastrum_delicatulum_1, R.drawable.bacteriastrum_delicatulum_1, R.drawable.bacteriastrum_delicatulum_1)
            ),
            PlanktonInfo(
                "Bleakeleya notata",
                "Diatom dengan bentuk karakteristik yang memiliki notch atau lekukan pada bagian tengah. Ditemukan di perairan laut dengan salinitas tinggi. Memiliki ornamen yang kompleks pada dinding sel.",
                "Diatom",
                R.drawable.bleakeleya_notata_1,
                listOf(R.drawable.bleakeleya_notata_1, R.drawable.bleakeleya_notata_2, R.drawable.bleakeleya_notata_3)
            ),
            PlanktonInfo(
                "Chaetoceros affinis",
                "Diatom planktonik yang membentuk rantai dengan seta yang panjang dan bercabang. Sangat umum di perairan laut dan merupakan produsen primer penting. Dapat membentuk blooming di kondisi nutrisi yang tepat.",
                "Diatom",
                R.drawable.chaetoceros_affinis_1,
                listOf(R.drawable.chaetoceros_affinis_1, R.drawable.chaetoceros_affinis_1, R.drawable.chaetoceros_affinis_1)
            ),
            PlanktonInfo(
                "Chaetoceros diversus",
                "Spesies Chaetoceros dengan variasi morfologi yang beragam. Memiliki seta yang lebih pendek dibanding C. affinis. Adaptif terhadap berbagai kondisi lingkungan perairan laut.",
                "Diatom",
                R.drawable.chaetoceros_diversus_1,
                listOf(R.drawable.chaetoceros_diversus_1, R.drawable.chaetoceros_diversus_2, R.drawable.chaetoceros_diversus_3)
            ),
            PlanktonInfo(
                "Chaetoceros peruvianus",
                "Diatom yang pertama kali ditemukan di perairan Peru. Memiliki seta yang khas dan struktur rantai yang rapat. Indikator kondisi upwelling dan perairan yang kaya nutrisi.",
                "Diatom",
                R.drawable.chaetoceros_peruvianus_1,
                listOf(R.drawable.chaetoceros_peruvianus_1, R.drawable.chaetoceros_peruvianus_2, R.drawable.chaetoceros_peruvianus_1)
            ),
            PlanktonInfo(
                "Coscinodiscus oculus-iridis",
                "Diatom sentrik berukuran besar dengan pola ornamen yang menyerupai mata. Memiliki areola yang tersusun radial dan merupakan salah satu diatom paling indah. Hidup sebagai plankton di perairan laut.",
                "Diatom",
                R.drawable.coscinodiscus_oculus_iridis_1,
                listOf(R.drawable.coscinodiscus_oculus_iridis_1, R.drawable.coscinodiscus_oculus_iridis_2, R.drawable.coscinodiscus_oculus_iridis_3)
            ),
            PlanktonInfo(
                "Diatom",
                "Kelompok alga mikroskopis dengan dinding sel dari silika. Merupakan produsen primer utama di ekosistem akuatik. Memiliki bentuk yang sangat beragam dan ornamen yang kompleks.",
                "Diatom",
                R.drawable.diatom_1,
                listOf(R.drawable.diatom_1, R.drawable.diatom_2, R.drawable.diatom_3)
            ),
            PlanktonInfo(
                "Guinardia flaccida",
                "Diatom berbentuk silinder panjang yang hidup soliter atau dalam rantai pendek. Memiliki kloroplas besar dan efisien. Umum ditemukan di perairan laut tropis dan subtropis.",
                "Diatom",
                R.drawable.guinardia_flaccida_1,
                listOf(R.drawable.guinardia_flaccida_1, R.drawable.guinardia_flaccida_2, R.drawable.guinardia_flaccida_3)
            ),
            PlanktonInfo(
                "Hemiaulus hauckii",
                "Diatom dengan bentuk yang unik memiliki elevasi di bagian tengah. Hidup sebagai plankton di perairan laut. Memiliki struktur yang membantu dalam flotasi dan distribusi nutrisi.",
                "Diatom",
                R.drawable.hemiaulus_hauckii_1,
                listOf(R.drawable.hemiaulus_hauckii_1, R.drawable.hemiaulus_hauckii_2, R.drawable.hemiaulus_hauckii_3)
            ),
            PlanktonInfo(
                "Hemiaulus membranaceus",
                "Spesies Hemiaulus dengan membran tipis yang karakteristik. Adaptasi khusus untuk hidup mengapung di kolom air. Memiliki mekanisme yang efisien untuk menangkap cahaya.",
                "Diatom",
                R.drawable.hemiaulus_membranaceus_1,
                listOf(R.drawable.hemiaulus_membranaceus_1, R.drawable.hemiaulus_membranaceus_2, R.drawable.hemiaulus_membranaceus_3)
            ),
            PlanktonInfo(
                "Mastogloia sp",
                "Diatom dengan chamber tambahan (partecta) di sepanjang margin sel. Struktur unik ini diduga berfungsi untuk regulasi osmotik. Umumnya hidup di lingkungan bentik atau epifitik.",
                "Diatom",
                R.drawable.mastogloia_sp_1,
                listOf(R.drawable.mastogloia_sp_1, R.drawable.mastogloia_sp_2, R.drawable.mastogloia_sp_3)
            ),
            PlanktonInfo(
                "Nitzschia",
                "Genus diatom dengan raphe yang ekssentrik dan keel yang khas. Sangat beragam dan adaptif, ditemukan di berbagai habitat akuatik. Beberapa spesies dapat menghasilkan toksin.",
                "Diatom",
                R.drawable.nitzschia_1,
                listOf(R.drawable.nitzschia_1, R.drawable.nitzschia_2, R.drawable.nitzschia_1)
            ),
            PlanktonInfo(
                "Nitzschia longissima",
                "Spesies Nitzschia yang sangat panjang dan ramping. Memiliki rasio panjang-lebar yang ekstrem. Hidup sebagai plankton dan dapat membentuk populasi yang padat di kondisi tertentu.",
                "Diatom",
                R.drawable.nitzschia_longissima_1,
                listOf(R.drawable.nitzschia_longissima_1, R.drawable.nitzschia_longissima_2, R.drawable.nitzschia_longissima_3)
            ),
            PlanktonInfo(
                "Plagiotropis lepidoptera",
                "Diatom dengan bentuk yang menyerupai sayap kupu-kupu. Memiliki ornamen yang halus dan simetris. Hidup di lingkungan bentik dan epipelik di perairan tawar dan payau.",
                "Diatom",
                R.drawable.plagiotropis_lepidoptera_1,
                listOf(R.drawable.plagiotropis_lepidoptera_1, R.drawable.plagiotropis_lepidoptera_2, R.drawable.plagiotropis_lepidoptera_3)
            ),
            PlanktonInfo(
                "Pleurosigma",
                "Diatom dengan bentuk sigmoid (S) yang karakteristik. Memiliki ornamen striae yang sangat halus dan teratur. Sering digunakan sebagai objek tes untuk mikroskop beresolusi tinggi.",
                "Diatom",
                R.drawable.pleurosigma_1,
                listOf(R.drawable.pleurosigma_1, R.drawable.pleurosigma_2, R.drawable.pleurosigma_3)
            ),
            PlanktonInfo(
                "Proboscia alata",
                "Diatom dengan ekstensi tubular panjang yang menyerupai proboscis. Adaptasi untuk hidup mengapung dan meningkatkan efisiensi nutrisi. Indikator perairan oligotrofik.",
                "Diatom",
                R.drawable.proboscia_alata_1,
                listOf(R.drawable.proboscia_alata_1, R.drawable.proboscia_alata_2, R.drawable.proboscia_alata_3)
            ),
            PlanktonInfo(
                "Proboscia indica",
                "Spesies Proboscia yang ditemukan di perairan Indo-Pasifik. Memiliki morfologi yang sama dengan P. alata namun dengan variasi ukuran. Penting dalam rantai makanan planktonik.",
                "Diatom",
                R.drawable.proboscia_indica_1,
                listOf(R.drawable.proboscia_indica_1, R.drawable.proboscia_indica_2, R.drawable.proboscia_indica_3)
            ),
            PlanktonInfo(
                "Pseudo-nitzschia spp",
                "Kompleks spesies diatom yang sulit dibedakan secara morfologi. Beberapa spesies dapat menghasilkan asam domoic yang beracun. Penting dalam monitoring kualitas air laut.",
                "Diatom",
                R.drawable.pseudo_nitzschia_spp_1,
                listOf(R.drawable.pseudo_nitzschia_spp_1, R.drawable.pseudo_nitzschia_spp_2, R.drawable.pseudo_nitzschia_spp_3)
            ),
            PlanktonInfo(
                "Pseudosolenia calcar-avis",
                "Diatom dengan bentuk yang menyerupai taji ayam. Memiliki struktur silika yang kuat dengan proyeksi tajam. Adaptasi untuk hidup planktonik dengan efisiensi flotasi tinggi.",
                "Diatom",
                R.drawable.pseudosolenia_calcar_avis_1,
                listOf(R.drawable.pseudosolenia_calcar_avis_1, R.drawable.pseudosolenia_calcar_avis_1, R.drawable.pseudosolenia_calcar_avis_2)
            ),
            PlanktonInfo(
                "Rhizosolenia calcar-avis",
                "Diatom berbentuk silinder dengan ekstensi seperti tanduk. Nama mengacu pada bentuknya yang menyerupai taji ayam. Hidup sebagai plankton di perairan laut terbuka.",
                "Diatom",
                R.drawable.rhizosolenia_calcar_avis_1,
                listOf(R.drawable.rhizosolenia_calcar_avis_1, R.drawable.rhizosolenia_calcar_avis_2, R.drawable.rhizosolenia_calcar_avis_3)
            ),
            PlanktonInfo(
                "Rhizosolenia cochlea",
                "Spesies Rhizosolenia dengan bentuk melingkar seperti siput. Memiliki struktur spiral yang unik. Adaptasi khusus untuk kondisi perairan dengan turbulensi rendah.",
                "Diatom",
                R.drawable.rhizosolenia_cochlea_1,
                listOf(R.drawable.rhizosolenia_cochlea_1, R.drawable.rhizosolenia_cochlea_2, R.drawable.rhizosolenia_cochlea_3)
            ),
            PlanktonInfo(
                "Rhizosolenia imbricata",
                "Diatom dengan sel-sel yang saling tumpang tindih dalam rantai. Struktur ini memberikan kekuatan mekanik dan efisiensi hidrodinamik. Umum di perairan laut produktif.",
                "Diatom",
                R.drawable.rhizosolenia_imbricata_1,
                listOf(R.drawable.rhizosolenia_imbricata_1, R.drawable.rhizosolenia_imbricata_2, R.drawable.rhizosolenia_imbricata_3)
            ),
            PlanktonInfo(
                "Tetramphora decussata",
                "Diatom dengan empat area yang tersusun dalam pola silang. Struktur yang kompleks dengan ornamen yang detail. Hidup di lingkungan bentik dengan kondisi stabil.",
                "Diatom",
                R.drawable.tetramphora_decussata_1,
                listOf(R.drawable.tetramphora_decussata_1, R.drawable.tetramphora_decussata_2, R.drawable.tetramphora_decussata_3)
            ),
            PlanktonInfo(
                "Thalassionema nitzschioides",
                "Diatom berbentuk jarum yang hidup dalam koloni berbentuk bintang atau radial. Sangat umum di perairan laut dan merupakan komponen penting plankton. Indikator perairan yang sehat.",
                "Diatom",
                R.drawable.thalassionema_nitzschioides_1,
                listOf(R.drawable.thalassionema_nitzschioides_1, R.drawable.thalassionema_nitzschioides_2, R.drawable.thalassionema_nitzschioides_3)
            ),
            PlanktonInfo(
                "Toxarium undulatum",
                "Diatom dengan permukaan yang bergelombang karakteristik. Memiliki ornamen yang kompleks dengan pola berulang. Adaptasi untuk kondisi lingkungan yang spesifik di perairan laut.",
                "Diatom",
                R.drawable.toxarium_undulatum_1,
                listOf(R.drawable.toxarium_undulatum_1, R.drawable.toxarium_undulatum_2, R.drawable.toxarium_undulatum_3)
            )
        )
    }
}
