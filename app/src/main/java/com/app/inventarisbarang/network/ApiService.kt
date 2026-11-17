package com.app.inventarisbarang.network

import com.app.inventarisbarang.model.BaseResponse
import com.app.inventarisbarang.model.Barang
import com.app.inventarisbarang.model.DashboardData
import com.app.inventarisbarang.model.Kategori
import com.app.inventarisbarang.model.LoginData
import com.app.inventarisbarang.model.Peminjaman
import com.app.inventarisbarang.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Auth & Profile
    @POST("register")
    suspend fun register(
        @Body body: Map<String, String>
    ): Response<BaseResponse<User>>

    @POST("login")
    suspend fun login(
        @Body body: Map<String, String>
    ): Response<BaseResponse<LoginData>>

    @GET("profile")
    suspend fun getProfile(
        @Query("user_id") userId: Int? = null
    ): Response<BaseResponse<User>>

    @POST("change-password")
    suspend fun changePassword(
        @Body body: Map<String, String>
    ): Response<BaseResponse<Any>>

    @POST("logout")
    suspend fun logout(): Response<BaseResponse<Any>>

    // Dashboard
    @GET("dashboard")
    suspend fun getDashboard(): Response<BaseResponse<DashboardData>>

    // Kategori
    @GET("kategori")
    suspend fun getKategori(
        @Query("search") search: String? = null
    ): Response<BaseResponse<List<Kategori>>>

    @POST("kategori")
    suspend fun tambahKategori(
        @Body body: Map<String, String>
    ): Response<BaseResponse<Kategori>>

    @GET("kategori/{id}")
    suspend fun getDetailKategori(
        @Path("id") id: Int
    ): Response<BaseResponse<Kategori>>

    @PUT("kategori/{id}")
    suspend fun updateKategori(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<BaseResponse<Kategori>>

    @DELETE("kategori/{id}")
    suspend fun deleteKategori(
        @Path("id") id: Int
    ): Response<BaseResponse<Any>>

    // Barang
    @GET("barang")
    suspend fun getBarang(
        @Query("search") search: String? = null,
        @Query("kategori_id") kategoriId: Int? = null
    ): Response<BaseResponse<List<Barang>>>

    @GET("barang/{id}")
    suspend fun getDetailBarang(
        @Path("id") id: Int
    ): Response<BaseResponse<Barang>>

    @POST("barang")
    suspend fun tambahBarang(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<BaseResponse<Barang>>

    @retrofit2.http.Multipart
    @POST("barang")
    suspend fun tambahBarangMultipart(
        @retrofit2.http.Part("kode_barang") kodeBarang: okhttp3.RequestBody,
        @retrofit2.http.Part("nama_barang") namaBarang: okhttp3.RequestBody,
        @retrofit2.http.Part("kategori_id") kategoriId: okhttp3.RequestBody,
        @retrofit2.http.Part("stok") stok: okhttp3.RequestBody,
        @retrofit2.http.Part("kondisi") kondisi: okhttp3.RequestBody,
        @retrofit2.http.Part("deskripsi") deskripsi: okhttp3.RequestBody?,
        @retrofit2.http.Part("lokasi_rak") lokasiRak: okhttp3.RequestBody?,
        @retrofit2.http.Part gambar: okhttp3.MultipartBody.Part?
    ): Response<BaseResponse<Barang>>

    @PUT("barang/{id}")
    suspend fun updateBarang(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<BaseResponse<Barang>>

    @retrofit2.http.Multipart
    @POST("barang/{id}")
    suspend fun updateBarangMultipart(
        @Path("id") id: Int,
        @retrofit2.http.Part("kode_barang") kodeBarang: okhttp3.RequestBody,
        @retrofit2.http.Part("nama_barang") namaBarang: okhttp3.RequestBody,
        @retrofit2.http.Part("kategori_id") kategoriId: okhttp3.RequestBody,
        @retrofit2.http.Part("stok") stok: okhttp3.RequestBody,
        @retrofit2.http.Part("kondisi") kondisi: okhttp3.RequestBody,
        @retrofit2.http.Part("deskripsi") deskripsi: okhttp3.RequestBody?,
        @retrofit2.http.Part("lokasi_rak") lokasiRak: okhttp3.RequestBody?,
        @retrofit2.http.Part("lokasi") lokasi: okhttp3.RequestBody?,
        @retrofit2.http.Part gambar: okhttp3.MultipartBody.Part?
    ): Response<BaseResponse<Barang>>

    @DELETE("barang/{id}")
    suspend fun deleteBarang(
        @Path("id") id: Int
    ): Response<BaseResponse<Any>>

    // Peminjaman
    @GET("peminjaman")
    suspend fun getPeminjaman(
        @Query("search") search: String? = null,
        @Query("user_id") userId: Int? = null,
        @Query("status") status: String? = null
    ): Response<BaseResponse<List<Peminjaman>>>

    @GET("peminjaman/{id}")
    suspend fun getDetailPeminjaman(
        @Path("id") id: Int
    ): Response<BaseResponse<Peminjaman>>

    @POST("peminjaman")
    suspend fun tambahPeminjaman(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<BaseResponse<Peminjaman>>

    @PUT("peminjaman/{id}")
    suspend fun updatePeminjaman(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<BaseResponse<Peminjaman>>

    @DELETE("peminjaman/{id}")
    suspend fun deletePeminjaman(
        @Path("id") id: Int
    ): Response<BaseResponse<Any>>

    // User Management
    @GET("users")
    suspend fun getUsers(
        @Query("search") search: String? = null,
        @Query("role") role: String? = null
    ): Response<BaseResponse<List<User>>>

    @POST("users")
    suspend fun tambahUser(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<BaseResponse<User>>

    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<BaseResponse<User>>

    @DELETE("users/{id}")
    suspend fun deleteUser(
        @Path("id") id: Int
    ): Response<BaseResponse<Any>>

    // Notifikasi Sistem
    @GET("notifikasi")
    suspend fun getNotifikasi(): Response<BaseResponse<List<com.app.inventarisbarang.model.Notifikasi>>>
}
