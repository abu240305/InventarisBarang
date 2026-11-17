package com.app.inventarisbarang.model

import com.google.gson.annotations.SerializedName

data class Barang(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("kategori_id")
    val kategori_id: Int = 0,

    @SerializedName("kode_barang")
    val kode_barang: String = "",

    @SerializedName("nama_barang")
    val nama_barang: String = "",

    @SerializedName("stok")
    val stok: Int = 0,

    @SerializedName("kondisi")
    val kondisi: String = "Baik",

    @SerializedName("gambar")
    val gambar: String? = null,

    @SerializedName("status_ketersediaan")
    val status_ketersediaan: String? = "Tersedia",

    @SerializedName("lokasi_rak")
    val lokasi_rak: String? = null,

    @SerializedName("lokasi")
    val lokasi: String? = null,

    @SerializedName("qr_code_url")
    val qr_code_url: String? = null,

    @SerializedName("deskripsi")
    val deskripsi: String? = null,

    @SerializedName("kategori")
    val kategori: Kategori? = null
) {
    val displayLokasi: String
        get() = when {
            !lokasi.isNullOrBlank() -> lokasi
            !lokasi_rak.isNullOrBlank() -> lokasi_rak
            else -> "Rak Gudang Logistik"
        }

    override fun toString(): String {
        return "$nama_barang (Stok: $stok)"
    }
}
