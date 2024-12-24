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

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


interface RetrofitAPI {

    companion object {
        const val userId = "mobiletest1234"
        const val code = "d6TaJkgiVWPzFXVyw4t3iAbEZbtngYY5P5RTswy0wEFpAzFuG6Xmzg%3D%3D"
        const val URL = "https://provigos-prod-api.azurewebsites.net/api/"
    }
    @Headers("Accept: */*", "Content-Type: application/json")
    @POST("healthConnectIngetration?code=d6TaJkgiVWPzFXVyw4t3iAbEZbtngYY5P5RTswy0wEFpAzFuG6Xmzg%3D%3D&userId=mobiletest1234")
    fun postRawJSON(@Body json: Any): Call<String>
}