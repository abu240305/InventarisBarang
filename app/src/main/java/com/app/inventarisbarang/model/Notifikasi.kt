package com.app.inventarisbarang.model

import com.google.gson.annotations.SerializedName

data class Notifikasi(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("tipe")
    val tipe: String = "",

    @SerializedName("judul")
    val judul: String = "",

    @SerializedName("pesan")
    val pesan: String = "",

    @SerializedName("kode_transaksi")
    val kode_transaksi: String? = null,

    @SerializedName("peminjaman_id")
    val peminjaman_id: Int? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("waktu")
    val waktu: String = "",

    @SerializedName("created_at")
    val created_at: String? = null,

    @SerializedName("is_read")
    var is_read: Boolean = false
)
