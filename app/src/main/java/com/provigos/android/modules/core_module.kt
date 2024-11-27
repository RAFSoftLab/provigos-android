package com.provigos.android.modules

import android.content.Context
import android.content.SharedPreferences
import com.facebook.stetho.BuildConfig
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.squareup.moshi.Moshi
import org.koin.android.ext.koin.androidApplication
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit

val coreModule = module {

    single<SharedPreferences> {
        androidApplication().getSharedPreferences(androidApplication().packageName, Context.MODE_PRIVATE)
    }

//    single { Room.databaseBuilder(androidContext(), Database::class.java, "db")
//        .fallbackToDestructiveMigration()
//        .build()
//    }

    fun createMoshi() : Moshi {
        return Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter())
            .build()
    }

    fun createRetrofit(moshi: Moshi, httpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://provigos-prod-api.azurewebsites.net/api/helloWorldEndpoint?code=Q49EQQVcMm9G8cPqSdJ2XSdPfRPXmLuqiEkZdsxkS_5HAzFu9Lw5jg%3D%3D")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .client(httpClient)
            .build()
    }

    fun createOkHttpClient() : OkHttpClient {

        val httpClient = OkHttpClient.Builder()
        httpClient.readTimeout(60, TimeUnit.SECONDS)
        httpClient.connectTimeout(60, TimeUnit.SECONDS)
        httpClient.writeTimeout(60, TimeUnit.SECONDS)

        if(BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            httpClient.addInterceptor(logging)
            httpClient.addNetworkInterceptor(StethoInterceptor())
        }

        return httpClient.build()
    }

    single { createRetrofit(moshi = get(), httpClient = get()) }
    single { createMoshi() }
    single { createOkHttpClient() }

}
