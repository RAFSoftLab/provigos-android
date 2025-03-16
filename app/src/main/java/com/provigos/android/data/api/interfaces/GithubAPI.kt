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
package com.provigos.android.data.api.interfaces

import com.provigos.android.data.model.github.GithubRepoModel
import com.provigos.android.data.model.github.GithubRepoCommitModel
import com.provigos.android.data.model.github.GithubUserModel
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface GithubAPI {

    companion object {
        const val GITHUB_API = "https://api.github.com/"
    }

    @Headers("Accept: */*", "Content-Type: application/vnd.github+json")
    @GET("user")
    suspend fun getGithubUserData(@Header("Authorization") token: String): GithubUserModel

    @GET("user/repos")
    suspend fun getGithubUserRepos
                (@Header("Authorization") token: String, ): List<GithubRepoModel>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getGithubUserCommits(@Header("Authorization") token: String,
                       @Path("owner") owner: String,
                       @Path("repo") repo: String,
                       @Query("author") author: String): List<GithubRepoCommitModel>

    @Headers("Accept: */*", "Content-Type: application/vnd.github+json")
    @GET("users/{username}/orgs")
    suspend fun getGithubUserOrgs(@Header("Authorization") token: String,
                                  @Path("username") username: String): List<GithubUserModel>

    @Headers("Accept: */*", "Content-Type: application/vnd.github+json")
    @GET("orgs/{owner}/repos")
    suspend fun getGithubUserOrgsRepos(@Header("Authorization") token: String,
                        @Path("owner") owner: String): List<GithubRepoModel>

}