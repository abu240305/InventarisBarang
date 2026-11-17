package com.app.inventarisbarang.model

import com.google.gson.annotations.SerializedName

data class Peminjaman(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("kode_peminjaman")
    val kode_peminjaman: String? = null,

    @SerializedName("user_id")
    val user_id: Int = 0,

    @SerializedName("barang_id")
    val barang_id: Int = 0,

    @SerializedName("jumlah")
    val jumlah: Int = 0,

    @SerializedName("tanggal_pinjam")
    val tanggal_pinjam: String = "",

    @SerializedName("tanggal_kembali")
    val tanggal_kembali: String? = null,

    @SerializedName("status")
    val status: String = "Menunggu",

    @SerializedName("sisa_hari")
    val sisa_hari: Int? = null,

    @SerializedName("status_tempo")
    val status_tempo: String? = null,

    @SerializedName("qr_code_url")
    val qr_code_url: String? = null,

    @SerializedName("keterangan")
    val keterangan: String? = null,

    @SerializedName("user")
    val user: User? = null,

    @SerializedName("barang")
    val barang: Barang? = null
) {
    val displayKode: String
        get() = if (!kode_peminjaman.isNullOrBlank()) kode_peminjaman else "TRX-$id"
}
