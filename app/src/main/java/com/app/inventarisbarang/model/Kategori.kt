package com.app.inventarisbarang.model

import com.google.gson.annotations.SerializedName

data class Kategori(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("kode_kategori")
    val kode_kategori: String? = null,

    @SerializedName("nama_kategori")
    val nama_kategori: String = "",

    @SerializedName("deskripsi")
    val deskripsi: String? = null,

    @SerializedName("barangs_count")
    val barangs_count: Int? = 0
) {
    override fun toString(): String {
        return nama_kategori
    }
}
