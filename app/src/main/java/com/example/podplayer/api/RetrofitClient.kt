package com.example.podplayer.api

import android.content.Context
import com.example.podplayer.util.Auth
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val DEV_SERVER = "https://itunes.apple.com"

    private fun getLogginInterceptor(): HttpLoggingInterceptor{
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return interceptor
    }

    private fun getAuthInterceptor(ctx: Context): Interceptor{
        return Interceptor { chain ->
            val headers = chain.request().headers.newBuilder().apply {
                add("Content-Type", "application/json")
                add("Accept", "application/json")
                if(Auth.isLoggedIn(ctx)){
                    add("Authorization", "Bearer ${Auth.getToken(ctx)}")
                }
            }
                .build()
            val request = chain.request().newBuilder().headers(headers).build()
            chain.proceed(request)
        }
    }

    private fun getOkHttpClient(ctx:Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(getLogginInterceptor())
            .addInterceptor(getAuthInterceptor(ctx))
            .connectTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .build()
    }
    fun getClient(ctx: Context):RetrofitInterface{
        return Retrofit.Builder()
            .baseUrl(DEV_SERVER)
            .client(getOkHttpClient(ctx))
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
            .create(RetrofitInterface::class.java)
    }
}