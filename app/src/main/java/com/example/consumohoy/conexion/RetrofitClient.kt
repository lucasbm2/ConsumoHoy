package com.example.consumohoy.conexion

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
private const val BASE_URL = "https://apidatos.ree.es/es/datos/"

private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
        val request: Request = chain.request().newBuilder()
        .addHeader("User-Agent", "MyAndroidApp")
        .build()
    chain.proceed(request)
}
        .build()

val retrofit: Retrofit by lazy {
    Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}

val apiService: ApiService by lazy {
    retrofit.create(ApiService::class.java)
}
}
