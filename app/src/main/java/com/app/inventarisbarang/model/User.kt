package com.app.inventarisbarang.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("username")
    val username: String = "",

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("role")
    val role: String = "user",

    @SerializedName("divisi")
    val divisi: String? = null,

    @SerializedName("avatar")
    val avatar: String? = null,

    @SerializedName("status_aktif")
    val statusAktif: Boolean? = true,

    @SerializedName("total_approval")
    val totalApproval: Int? = 0,

    @SerializedName("total_peminjaman")
    val totalPeminjaman: Int? = 0
)
