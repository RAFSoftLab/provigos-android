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
import retrofit2.await
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class HttpManager {

    private lateinit var retrofitAPI: RetrofitAPI
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private var retrofitBuilder: Retrofit.Builder

    init {
        val mHttpLoggingInterceptor = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)

        val mOkHttpClient = OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            //.addInterceptor(mHttpLoggingInterceptor)
            .build()

        retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(mOkHttpClient)
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun postData(context: Context, userData:  Map<String, Map<String, String>>): Result<String> {
        return try {

            val sharedPreferenceDataSource = SharedPreferenceDataSource(context)

            val googleToken = sharedPreferenceDataSource.getGoogleToken()
            if(googleToken.isNullOrBlank()) {
                Timber.tag("HttpManager").e("Google token is missing")
                return Result.failure(IllegalArgumentException("Google token is null or empty"))
            }
            val jsonAdapter: JsonAdapter<Map<String, Map<String, String>>> = moshi.adapter()
            val json = jsonAdapter.toJsonValue(userData)

            retrofitAPI = retrofitBuilder.baseUrl(RetrofitAPI.PROVIGOS_API)
                .build()
                .create(RetrofitAPI::class.java)

            val response = retrofitAPI.postProvigosData(googleToken, json)

            return if(response.isSuccessful) {
                Timber.tag("HttpManager").d("POST Success: ${response.body()}")
                Result.success("Data posted successfully")
            } else {
                Timber.tag("HttpManager").e("POST Error: ${response.code()} - ${response.errorBody()?.string()}")
                Result.failure(Exception("POST failed: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Timber.tag("HttpManager").e("Exception in postData: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getGithubCommits(context: Context): HashMap<String, String> {

        val githubCommits: HashMap<String, String> = HashMap()

        retrofitBuilder.baseUrl(RetrofitAPI.GITHUB_API)
        retrofitAPI = retrofitBuilder.build().create(RetrofitAPI::class.java)

        val githubToken = SharedPreferenceDataSource(context).getGithubAccessToken()
            ?: throw IllegalArgumentException("GitHub token doesn't exist. Re-authorize")

        try {
            val user = retrofitAPI.getGithubUserData("Bearer $githubToken")
            val userLogin = user.login
            val userName = user.name

            val repos = retrofitAPI.getGithubUserRepos("Bearer $githubToken")

            for(repo in repos) {
                val repoName = repo.name

                val commits = retrofitAPI.getGithubUserCommits(
                    "Bearer $githubToken",
                    userLogin,
                    repoName,
                    userLogin
                )

                for (commit in commits) {
                    val date = commit.commit.author.date.substring(0, 10)
                    githubCommits[date] = (githubCommits[date]?.toLong()?.plus(1) ?: 1).toString()
                }
            }

            /*
            val orgs = retrofitAPI.getGithubUserOrgs("Bearer $githubToken", userLogin)
            for(org in orgs) {
                Timber.tag("HttpManager").d("$org")
                repos = retrofitAPI.getGithubUserOrgsRepos("Bearer $githubToken", org.login)

                for(repo in repos) {
                    val repoName = repo.name

                    val commits = retrofitAPI.getGithubUserCommits(
                        "Bearer $githubToken",
                        org.login,
                        repoName,
                        userLogin
                    )

                    for(commit in commits) {
                        if(userName == commit.commit.author.name) {
                            val date = commit.commit.author.date.substring(0, 10)
                            githubCommits[date] = (githubCommits[date]?.toLong()?.plus(1) ?: 1).toString()
                        }
                    }
                }
            }
            */


        } catch (e: Exception) {
            Timber.e("Error fetching GitHub commits: ${e.message}")
        }

        return githubCommits
    }
}