package com.example.planktondetectionapps

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Activity untuk menampilkan galeri fitoplankton
 * Menampilkan berbagai jenis plankton dengan nama, deskripsi, dan contoh gambar
 */
class DocumentationActivity : AppCompatActivity() {

    // UI Components
    private lateinit var backButton: ImageButton
    private lateinit var planktonRecyclerView: RecyclerView
    private lateinit var planktonAdapter: PlanktonAdapter

    /**
     * Inisialisasi activity dan setup UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentation)

        initializeViews()
        setupListeners()
        setupPlanktonGallery()
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
     * Setup galeri plankton dengan RecyclerView
     */
    private fun setupPlanktonGallery() {
        val planktonList = getPlanktonData()

        planktonAdapter = PlanktonAdapter(planktonList)
        planktonRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DocumentationActivity)
            adapter = planktonAdapter
        }
    }

    /**
     * Data plankton dengan nama dan deskripsi
     */
    private fun getPlanktonData(): List<PlanktonInfo> {
        return listOf(
            PlanktonInfo(
                "Achnanthes sp",
                "Diatom bentik yang berbentuk oval dengan struktur raphe yang khas. Sering ditemukan menempel pada substrat di lingkungan air tawar dan laut. Memiliki frustule simetris dengan ornamen yang halus."
            ),
            PlanktonInfo(
                "Bacteriastrum delicatulum",
                "Diatom planktonik yang berbentuk rantai dengan seta panjang dan halus. Hidup di perairan laut dan membentuk koloni berbentuk spiral atau zigzag. Memiliki kloroplas yang efisien untuk fotosintesis."
            ),
            PlanktonInfo(
                "Bleakeleya notata",
                "Diatom dengan bentuk karakteristik yang memiliki notch atau lekukan pada bagian tengah. Ditemukan di perairan laut dengan salinitas tinggi. Memiliki ornamen yang kompleks pada dinding sel."
            ),
            PlanktonInfo(
                "Chaetoceros affinis",
                "Diatom planktonik yang membentuk rantai dengan seta yang panjang dan bercabang. Sangat umum di perairan laut dan merupakan produsen primer penting. Dapat membentuk blooming di kondisi nutrisi yang tepat."
            ),
            PlanktonInfo(
                "Chaetoceros diversus",
                "Spesies Chaetoceros dengan variasi morfologi yang beragam. Memiliki seta yang lebih pendek dibanding C. affinis. Adaptif terhadap berbagai kondisi lingkungan perairan laut."
            ),
            PlanktonInfo(
                "Chaetoceros peruvianus",
                "Diatom yang pertama kali ditemukan di perairan Peru. Memiliki seta yang khas dan struktur rantai yang rapat. Indikator kondisi upwelling dan perairan yang kaya nutrisi."
            ),
            PlanktonInfo(
                "Coscinodiscus oculus-iridis",
                "Diatom sentrik berukuran besar dengan pola ornamen yang menyerupai mata. Memiliki areola yang tersusun radial dan merupakan salah satu diatom paling indah. Hidup sebagai plankton di perairan laut."
            ),
            PlanktonInfo(
                "Diatom",
                "Kelompok alga mikroskopis dengan dinding sel dari silika. Merupakan produsen primer utama di ekosistem akuatik. Memiliki bentuk yang sangat beragam dan ornamen yang kompleks."
            ),
            PlanktonInfo(
                "Guinardia flaccida",
                "Diatom berbentuk silinder panjang yang hidup soliter atau dalam rantai pendek. Memiliki kloroplas besar dan efisien. Umum ditemukan di perairan laut tropis dan subtropis."
            ),
            PlanktonInfo(
                "Hemiaulus hauckii",
                "Diatom dengan bentuk yang unik memiliki elevasi di bagian tengah. Hidup sebagai plankton di perairan laut. Memiliki struktur yang membantu dalam flotasi dan distribusi nutrisi."
            ),
            PlanktonInfo(
                "Hemiaulus membranaceus",
                "Spesies Hemiaulus dengan membran tipis yang karakteristik. Adaptasi khusus untuk hidup mengapung di kolom air. Memiliki mekanisme yang efisien untuk menangkap cahaya."
            ),
            PlanktonInfo(
                "Mastogloia sp",
                "Diatom dengan chamber tambahan (partecta) di sepanjang margin sel. Struktur unik ini diduga berfungsi untuk regulasi osmotik. Umumnya hidup di lingkungan bentik atau epifitik."
            ),
            PlanktonInfo(
                "Nitzschia",
                "Genus diatom dengan raphe yang ekssentrik dan keel yang khas. Sangat beragam dan adaptif, ditemukan di berbagai habitat akuatik. Beberapa spesies dapat menghasilkan toksin."
            ),
            PlanktonInfo(
                "Nitzschia longissima",
                "Spesies Nitzschia yang sangat panjang dan ramping. Memiliki rasio panjang-lebar yang ekstrem. Hidup sebagai plankton dan dapat membentuk populasi yang padat di kondisi tertentu."
            ),
            PlanktonInfo(
                "Plagiotropis lepidoptera",
                "Diatom dengan bentuk yang menyerupai sayap kupu-kupu. Memiliki ornamen yang halus dan simetris. Hidup di lingkungan bentik dan epipelik di perairan tawar dan payau."
            ),
            PlanktonInfo(
                "Pleurosigma",
                "Diatom dengan bentuk sigmoid (S) yang karakteristik. Memiliki ornamen striae yang sangat halus dan teratur. Sering digunakan sebagai objek tes untuk mikroskop beresolusi tinggi."
            ),
            PlanktonInfo(
                "Proboscia alata",
                "Diatom dengan ekstensi tubular panjang yang menyerupai proboscis. Adaptasi untuk hidup mengapung dan meningkatkan efisiensi nutrisi. Indikator perairan oligotrofik."
            ),
            PlanktonInfo(
                "Proboscia indica",
                "Spesies Proboscia yang ditemukan di perairan Indo-Pasifik. Memiliki morfologi yang sama dengan P. alata namun dengan variasi ukuran. Penting dalam rantai makanan planktonik."
            ),
            PlanktonInfo(
                "Pseudo-nitzschia spp",
                "Kompleks spesies diatom yang sulit dibedakan secara morfologi. Beberapa spesies dapat menghasilkan asam domoic yang beracun. Penting dalam monitoring kualitas air laut."
            ),
            PlanktonInfo(
                "Pseudosolenia calcar-avis",
                "Diatom dengan bentuk yang menyerupai taji ayam. Memiliki struktur silika yang kuat dengan proyeksi tajam. Adaptasi untuk hidup planktonik dengan efisiensi flotasi tinggi."
            ),
            PlanktonInfo(
                "Rhizosolenia calcar-avis",
                "Diatom berbentuk silinder dengan ekstensi seperti tanduk. Nama mengacu pada bentuknya yang menyerupai taji ayam. Hidup sebagai plankton di perairan laut terbuka."
            ),
            PlanktonInfo(
                "Rhizosolenia cochlea",
                "Spesies Rhizosolenia dengan bentuk melingkar seperti siput. Memiliki struktur spiral yang unik. Adaptasi khusus untuk kondisi perairan dengan turbulensi rendah."
            ),
            PlanktonInfo(
                "Rhizosolenia imbricata",
                "Diatom dengan sel-sel yang saling tumpang tindih dalam rantai. Struktur ini memberikan kekuatan mekanik dan efisiensi hidrodinamik. Umum di perairan laut produktif."
            ),
            PlanktonInfo(
                "Tetramphora decussata",
                "Diatom dengan empat area yang tersusun dalam pola silang. Struktur yang kompleks dengan ornamen yang detail. Hidup di lingkungan bentik dengan kondisi stabil."
            ),
            PlanktonInfo(
                "Thalassionema nitzschioides",
                "Diatom berbentuk jarum yang hidup dalam koloni berbentuk bintang atau radial. Sangat umum di perairan laut dan merupakan komponen penting plankton. Indikator perairan yang sehat."
            ),
            PlanktonInfo(
                "Toxarium undulatum",
                "Diatom dengan permukaan yang bergelombang karakteristik. Memiliki ornamen yang kompleks dengan pola berulang. Adaptasi untuk kondisi lingkungan yang spesifik di perairan laut."
            )
        )
    }
}
