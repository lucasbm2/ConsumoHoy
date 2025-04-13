package com.example.consumohoy.conexion

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//Creo un objeto RetrofitClient con una instancia de Retrofit para poder acceder a la API
//Mediante la libreria de Retrofit
object RetrofitClient {
    private const val BASE_URL = "https://apidatos.ree.es/es/datos/"

    //Creo un medio para coger los datos de la API con OkHttp
    //La API de Red Eléctrica Española necesita que le envíen este token para funcionar bien
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request: Request = chain.request().newBuilder()
                .addHeader("User-Agent", "MyAndroidApp")
                .build()
            chain.proceed(request)
        }
        .build()

    //Creo instancia de Retrofit para poder acceder a la API
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    //Creo instancia de ApiService para poder acceder a los métodos de la API
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
