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

import com.provigos.android.data.model.custom.CustomItemModelWrapper
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ProvigosAPI {

    companion object {
        const val PROVIGOS_API = "https://provigos-prod-api.azurewebsites.net/api/"
    }

    @Headers("Accept: */*", "Content-Type: application/json")
    @POST("healthConnectIntegration")
    suspend fun postProvigosData(@Header("Authorization") token: String, @Body json: Any?): Response<String>

    @Headers("Accept: */*", "Content-Type: application/json")
    @GET("customFieldsKeys")
    suspend fun getCustomFieldsKeys(@Header("Authorization") token: String): CustomItemModelWrapper

    @Headers("Accept: */*", "Content-Type: application/json")
    @POST("customFieldsKeys")
    suspend fun postCustomFieldsKeys(@Header("Authorization") token: String, @Body json: Any?): Response<String>

    @Headers("Accept: */*", "Content-Type: application/json")
    @POST("customFieldsData")
    suspend fun postCustomFieldsData(@Header("Authorization") token: String, @Body json: Any?): Response<String>

    @Headers("Accept: */*", "Content-Type: application/json")
    @GET("customFieldsData")
    suspend fun getCustomFieldsData(@Header("Authorization") token: String): Map<String, Map<String, String>>
}