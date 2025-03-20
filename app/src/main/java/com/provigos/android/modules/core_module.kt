/* MIT License
 *
 * Copyright 2025 Provigos

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.provigos.android.modules

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.provigos.android.application.ProvigosApplication
import com.provigos.android.data.api.HttpManager
import com.provigos.android.data.local.SharedPreferenceDataSource
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import com.provigos.android.data.api.interfaces.GithubAPI
import com.provigos.android.data.api.interfaces.ProvigosAPI
import com.provigos.android.data.api.interfaces.SpotifyAPI
import com.provigos.android.data.local.SharedPreferenceManager
import com.squareup.moshi.Moshi
import org.koin.android.ext.koin.androidApplication
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit

    inline fun <reified T> createRetrofit(okHttpClient: OkHttpClient, moshi: Moshi, baseUrl: String): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(T::class.java)
    }

    val coreModule = module {

        single<SharedPreferences> {
            val masterKey = MasterKey.Builder(androidApplication())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                androidApplication(),
                "encrypted_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        single {
            SharedPreferenceDataSource(
                encryptedSharedPreferences = get()
            )
        }

        single {
            SharedPreferenceManager.init(
                sharedPreferenceDataSource = get()
            )
        }

        single {
            Moshi.Builder().apply {
                add(Date::class.java, Rfc3339DateJsonAdapter())
                addLast(KotlinJsonAdapterFactory())
            }.build()
        }

        single {
            OkHttpClient.Builder().apply {
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            }.build()
        }

        single<ProvigosAPI> {
            createRetrofit<ProvigosAPI>(
                okHttpClient = get(),
                moshi = get(),
                baseUrl = ProvigosAPI.PROVIGOS_API
            )
        }

        single<GithubAPI> {
            createRetrofit<GithubAPI>(
                okHttpClient = get(),
                moshi = get(),
                baseUrl = GithubAPI.GITHUB_API
            )
        }

        single<SpotifyAPI> {
            createRetrofit<SpotifyAPI>(
                okHttpClient = get(),
                moshi = get(),
                baseUrl = SpotifyAPI.SPOTIFY_API
            )
        }


        single {
            HttpManager(
                provigosAPI = get(),
                githubAPI = get(),
                spotifyAPI = get(),
                moshi = get()
            )
        }

        viewModel {
            DashboardViewModel(
                mHealthConnectManager = (androidApplication() as ProvigosApplication).healthConnectManager,
                mHttpManager = get()
            )
        }
    }

