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
package com.provigos.android.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.provigos.android.data.api.HttpManager
import org.koin.java.KoinJavaComponent.inject
import java.io.IOException

class DailyWorker(context: Context, workerParameters: WorkerParameters): CoroutineWorker(context, workerParameters) {

    //WM-WorkerWrapper com.provigos.android  I  Worker result SUCCESS for Work [ id=ca8d7639-e270-4716-a0f3-83c3ced5a7f6,
    // tags={ com.provigos.android.util.DailyWorker } ]
    private val mHttpManager: HttpManager by inject(HttpManager::class.java)

    override suspend fun doWork(): Result {
        return try {
            val jsonString = inputData.getString("map") ?: return Result.failure()
            val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
            val data: Map<String, Map<String, String>> = Gson().fromJson(jsonString, type)
            val response = mHttpManager.postProvigosData(data)
            if(response.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}