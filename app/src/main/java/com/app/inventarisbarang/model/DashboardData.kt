package com.app.inventarisbarang.model

import com.google.gson.annotations.SerializedName

data class DashboardData(
    @SerializedName("total_jenis_barang")
    val total_jenis_barang: Int = 0,

    @SerializedName("total_stok_barang")
    val total_stok_barang: Int = 0,

    @SerializedName("total_kategori")
    val total_kategori: Int = 0,

    @SerializedName("peminjaman_menunggu")
    val peminjaman_menunggu: Int = 0,

    @SerializedName("peminjaman_aktif")
    val peminjaman_aktif: Int = 0,

    @SerializedName("peminjaman_terbaru")
    val peminjaman_terbaru: List<Peminjaman>? = emptyList()
)
