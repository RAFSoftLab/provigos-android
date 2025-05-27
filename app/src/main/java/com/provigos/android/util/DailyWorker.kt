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