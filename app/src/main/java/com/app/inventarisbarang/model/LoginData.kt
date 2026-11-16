package com.app.inventarisbarang.model

import com.google.gson.annotations.SerializedName

data class LoginData(
    @SerializedName("user")
    val user: User? = null,

    @SerializedName("token", alternate = ["access_token", "plainTextToken"])
    val token: String? = null
)
