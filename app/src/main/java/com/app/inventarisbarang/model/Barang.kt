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
    val lokasi_rak: String? = "Rak A-01",

    @SerializedName("deskripsi")
    val deskripsi: String? = null,

    @SerializedName("kategori")
    val kategori: Kategori? = null
) {
    override fun toString(): String {
        return "$nama_barang (Stok: $stok)"
    }
}
