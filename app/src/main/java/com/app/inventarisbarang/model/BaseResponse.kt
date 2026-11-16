package com.app.inventarisbarang.model

import com.google.gson.annotations.SerializedName

data class BaseResponse<T>(
    @SerializedName("status")
    val status: Boolean = false,

    @SerializedName("message")
    val message: String = "",

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("meta")
    val meta: Map<String, Any>? = null,

    @SerializedName("errors")
    val errors: Map<String, List<String>>? = null
)
