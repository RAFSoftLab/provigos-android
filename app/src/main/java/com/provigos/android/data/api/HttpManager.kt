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
package com.provigos.android.data.api

import com.provigos.android.data.api.interfaces.GithubAPI
import com.provigos.android.data.api.interfaces.ProvigosAPI
import com.provigos.android.data.api.interfaces.SpotifyAPI
import com.provigos.android.data.local.SharedPreferenceManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber

class HttpManager(private val provigosAPI: ProvigosAPI,
                  private val githubAPI: GithubAPI,
                  private val spotifyAPI: SpotifyAPI,
                  private val moshi: Moshi) {

    private val sharedPreferenceDataSource = SharedPreferenceManager.get()

    suspend fun postData(userData:  Map<String, Map<String, String>>): Result<String> {
        return try {

            val googleToken = sharedPreferenceDataSource.getGoogleToken()
            if(googleToken.isNullOrBlank()) {
                Timber.tag("HttpManager").e("Google token is missing")
                return Result.failure(IllegalArgumentException("Google token is null or empty"))
            }

            val jsonAdapter: JsonAdapter<Map<String, Map<String, String>>> = moshi.adapter(
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Types.newParameterizedType(
                        Map::class.java,
                        String::class.java,
                        String::class.java)
                    )
                )

            val json = jsonAdapter.toJsonValue(userData)

            val response = provigosAPI.postProvigosData(googleToken, json)

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

    suspend fun getGithubCommits(): HashMap<String, String> {

        val githubCommits: HashMap<String, String> = hashMapOf()

        if(!sharedPreferenceDataSource.isAllowGithubTotalCommits() &&
            !sharedPreferenceDataSource.isAllowGithubDailyCommits()) {
            return githubCommits
        }

        val githubToken = sharedPreferenceDataSource.getGithubAccessToken()
            ?: throw IllegalArgumentException("GitHub token doesn't exist. Re-authorize")

        try {
            val user = githubAPI.getGithubUserData("Bearer $githubToken")
            val userLogin = user.login
            val userName = user.name

            val repos = githubAPI.getGithubUserRepos("Bearer $githubToken")

            for(repo in repos) {
                val repoName = repo.name

                val commits = githubAPI.getGithubUserCommits(
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


            val orgs = githubAPI.getGithubUserOrgs("Bearer $githubToken", userLogin)
            for(org in orgs) {
                Timber.tag("HttpManager").d("$org")
                val orgRepos = githubAPI.getGithubUserOrgsRepos("Bearer $githubToken", org.login)

                for(repo in orgRepos) {
                    val repoName = repo.name

                    val commits = githubAPI.getGithubUserCommits(
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
        } catch (e: Exception) {
            Timber.e("Error fetching GitHub commits: ${e.message}")
        }

        return githubCommits
    }

    suspend fun getSpotifyArtists(): HashMap<String, String> {

        val spotifyData: HashMap<String, String> = hashMapOf()

        if(!sharedPreferenceDataSource.isAllowSpotifyArtistPopularity() &&
            !sharedPreferenceDataSource.isAllowSpotifyArtistGenres()) {
            return spotifyData
        }

        if(sharedPreferenceDataSource.getSpotifyTokenExpiration() < System.currentTimeMillis()) {
            return spotifyData
        }

        val spotifyToken = sharedPreferenceDataSource.getSpotifyAccessToken()
            ?: throw IllegalArgumentException("Spotify token doesn't exist. Re-authorize")

        try {
            val userArtists = spotifyAPI.getTopArtists("Bearer $spotifyToken")

            val popularity = mutableListOf<Int>()
            val genres = mutableMapOf<String, Int>()

            for (item in userArtists.items) {
                Timber.tag("HttpManager").d("${item.popularity}")
                popularity.add(item.popularity)
                for (genre in item.genres) {
                Timber.tag("HttpManager").d(genre)
                    genres[genre] = genres.getOrDefault(genre, 0) + 1
                }
            }

            if (sharedPreferenceDataSource.isAllowSpotifyArtistPopularity()) {
                val averagePopularity = if (popularity.isNotEmpty()) {
                    popularity.average()
                } else {
                    0.0
                }
                spotifyData += ("spotifyPopularity" to averagePopularity.toString())
            }

            if (sharedPreferenceDataSource.isAllowSpotifyArtistGenres()) {
                var mostPopularGenre = genres.maxByOrNull { it.value }?.key
                if (mostPopularGenre == null) {
                    mostPopularGenre = "None"
                }
                spotifyData += ("spotifyGenre" to mostPopularGenre)
            }
        } catch (e: Exception) {
            Timber.tag("HttpManager").d("Error fetching Spotify data ${e.message}")
        }

        return spotifyData
    }

    suspend fun getMe() {

        val response = spotifyAPI.getMe("Bearer ${sharedPreferenceDataSource.getSpotifyAccessToken()}")
        Timber.e("${response.body()}")
    }
}