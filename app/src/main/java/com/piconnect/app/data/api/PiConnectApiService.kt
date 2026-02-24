package com.piconnect.app.data.api

import com.piconnect.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface PiConnectApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/v1/devices")
    suspend fun getDevices(@Header("Authorization") token: String): Response<DevicesResponse>

    @POST("api/v1/devices/{deviceId}/tunnel")
    suspend fun createTunnel(
        @Header("Authorization") token: String,
        @Path("deviceId") deviceId: String,
        @Body request: TunnelRequest = TunnelRequest()
    ): Response<TunnelResponse>

    companion object {
        private const val BASE_URL = "https://connect.raspberrypi.com/"

        fun create(): PiConnectApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PiConnectApiService::class.java)
        }
    }
}
