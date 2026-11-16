package com.app.inventarisbarang.utils

import com.app.inventarisbarang.model.BaseResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorUtils {

    fun parseError(response: Response<*>): String {
        val errorBody = response.errorBody()?.string()
        if (errorBody.isNullOrBlank()) {
            return when (response.code()) {
                401 -> "Sesi telah berakhir atau kredensial salah. Silakan login kembali."
                403 -> "Anda tidak memiliki hak akses untuk tindakan ini."
                404 -> "Data tidak ditemukan (404)."
                422 -> "Validasi data gagal."
                500 -> "Terjadi kesalahan internal server (500)."
                else -> "Terjadi kesalahan (Kode: ${response.code()})."
            }
        }

        try {
            val gson = Gson()
            val type = object : TypeToken<BaseResponse<Any>>() {}.type
            val baseResponse: BaseResponse<Any>? = gson.fromJson(errorBody, type)

            // If there are field errors (422)
            if (!baseResponse?.errors.isNullOrEmpty()) {
                val errorList = mutableListOf<String>()
                baseResponse?.errors?.forEach { (_, messages) ->
                    errorList.addAll(messages)
                }
                if (errorList.isNotEmpty()) {
                    return errorList.joinToString("\n")
                }
            }

            if (!baseResponse?.message.isNullOrBlank()) {
                return baseResponse.message
            }

            // Fallback: parse direct { "message": "..." } or { "error": "..." }
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val rawMap: Map<String, Any>? = gson.fromJson(errorBody, mapType)
            val msg = rawMap?.get("message") as? String ?: rawMap?.get("error") as? String
            if (!msg.isNullOrBlank()) {
                return msg
            }
        } catch (_: Exception) {
            // Error body might not be valid JSON
        }

        return when (response.code()) {
            401 -> "Kredensial tidak valid atau tidak memiliki akses (401)."
            403 -> "Akses ditolak (403)."
            404 -> "Data tidak ditemukan di server (404)."
            422 -> "Data yang dimasukkan tidak valid (422)."
            500 -> "Server mengalami kendala internal (500)."
            else -> "Terjadi kesalahan pada server (Kode: ${response.code()})."
        }
    }

    fun parseThrowable(throwable: Throwable): String {
        return when (throwable) {
            is ConnectException -> "Gagal terhubung ke server. Pastikan backend Laravel berjalan di port 8000."
            is SocketTimeoutException -> "Koneksi ke server timeout. Silakan coba kembali."
            is UnknownHostException -> "Alamat server tidak dapat ditemukan. Periksa konfigurasi IP/URL."
            is IOException -> "Terjadi gangguan jaringan: ${throwable.localizedMessage ?: "Koneksi terputus."}"
            else -> throwable.localizedMessage ?: "Terjadi kesalahan tidak terduga."
        }
    }
}
