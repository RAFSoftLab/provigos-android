/* MIT License
 *
 * Copyright 2024 Provigos

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
package com.provigos.android.data.remote

import android.content.Context
import com.provigos.android.data.SharedPreferenceDataSource
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

class DatabaseConnection {

    private val retrofitAPI: RetrofitAPI
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    init {
        val mHttpLoggingInterceptor = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)

        val mOkHttpClient = OkHttpClient
            .Builder()
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
    fun postHealthConnectData(context: Context, healthConnectData:  MutableMap<String, MutableMap<String, String>>) {
        val jsonAdapter: JsonAdapter<MutableMap<String, MutableMap<String,String>>> = moshi.adapter<MutableMap<String, MutableMap<String, String>>>()
        val json = jsonAdapter.toJsonValue(healthConnectData)!!
        Timber.e(json.toString())
        retrofitAPI.postHealthConnectData(SharedPreferenceDataSource(context).getUserToken(), json).enqueue(object: Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                Timber.e("POST Success")
            }
            override fun onFailure(call: Call<String>, t: Throwable) = t.printStackTrace()
        })
    }
    
    @OptIn(ExperimentalStdlibApi::class)
    fun postLogin(user: User, context: Context) {
        val jsonAdapter: JsonAdapter<User> = moshi.adapter<User>()
        val json = jsonAdapter.toJsonValue(user)!!
        retrofitAPI.postLogin(json).enqueue(object: Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                SharedPreferenceDataSource(context).setUserToken(response.body().toString())
            }
            override fun onFailure(call: Call<String>, t: Throwable) = t.printStackTrace()
        })
    }

    fun postLogin2(token: String) {
        retrofitAPI.postLogin2(token).enqueue(object: Callback<String> {
            override fun onResponse(p0: Call<String>, p1: Response<String>) {
                TODO("Not yet implemented")
            }
            override fun onFailure(call: Call<String>, t: Throwable) = t.printStackTrace()
        })
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun postUser(user: User) {
        val jsonAdapter: JsonAdapter<User> = moshi.adapter<User>()
        val json = jsonAdapter.toJsonValue(user)!!
        retrofitAPI.postUser(json).enqueue(object: Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                TODO("Not yet implemented")
            }
            override fun onFailure(call: Call<String>, t: Throwable) = t.printStackTrace()
        })
    }
}