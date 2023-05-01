package com.example.arthreedmodelrotation

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface GltfApi {

    @Streaming
    @GET
    suspend fun downloadFile(@Url FileUrl: String): Response<ResponseBody>

//    companion object {
//        operator fun invoke(networkConnectionInterceptor: NetworkConnectionInterceptor): GltfApi {
//            val okHttpClient = OkHttpClient.Builder()
//                .addInterceptor(networkConnectionInterceptor)
//                .connectTimeout(3, TimeUnit.MINUTES)
//                .readTimeout(3, TimeUnit.MINUTES)
//                .build()
//
//            return Retrofit.Builder()
//                .client(okHttpClient)
//                .baseUrl("https://firebasestorage.googleapis.con/")
//                .addConverterFactory(GsonConverterFactory.create())
//                .build().create(GltfApi::class.java)
//        }
//    }
}
