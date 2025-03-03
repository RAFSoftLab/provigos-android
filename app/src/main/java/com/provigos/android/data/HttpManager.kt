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
package com.provigos.android.data

import android.content.Context
import com.provigos.android.util.RetrofitAPI
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class HttpManager {

    private val retrofitAPI: RetrofitAPI
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    init {
        val mHttpLoggingInterceptor = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)

        val mOkHttpClient = OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(mHttpLoggingInterceptor)
            .build()

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(RetrofitAPI.URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(mOkHttpClient)
            .build()

        retrofitAPI = retrofit.create(RetrofitAPI::class.java)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun postData(context: Context, userData:  MutableMap<String, MutableMap<String, String>>) {
        try {
            val jsonAdapter: JsonAdapter<MutableMap<String, MutableMap<String,String>>> = moshi.adapter()
            val json = jsonAdapter.toJsonValue(userData)

            Timber.tag("HttpManager").d("Sending JSON: $json")

            val googleToken = SharedPreferenceDataSource(context).getGoogleToken()

            if(googleToken.isNullOrBlank()) {
                Timber.tag("HttpManager").e("Google token is missing")
                throw IllegalArgumentException("Google Token is null or empty")
            } else {
                retrofitAPI.postData(SharedPreferenceDataSource(context).getGoogleToken()!!, json)
                    .enqueue(object : Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            if (response.isSuccessful) {
                                Timber.tag("HttpManager").e("POST Success: ${response.body()}")
                            } else {
                                Timber.tag("HttpManager").e("POST Error: ${response.code()} - ${response.errorBody()?.string()}")
                            }
                        }

                        override fun onFailure(call: Call<String>, t: Throwable) {
                            when (t) {
                                is SocketTimeoutException -> Timber.tag("HttpManager").e("Socket Timeout: ${t.message}")
                                is IOException -> Timber.tag("HttpManager").e("Network Error: ${t.message}")
                                else -> Timber.tag("HttpManager").e("Unknown Error: ${t.message}")
                            }
                            t.printStackTrace()
                        }
                    })
            }
        } catch (e: Exception) {
            Timber.tag("HttpManager").e("Exception in postData: ${e.message}")
        }
    }
}