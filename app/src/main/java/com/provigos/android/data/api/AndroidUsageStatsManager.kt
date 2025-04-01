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

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class AndroidUsageStatsManager(private val context: Context) {

    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(UsageStatsManager::class.java)
    }

    fun getScreenTime(days: Int = 1): Long {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            add(Calendar.HOUR_OF_DAY, 0)
            add(Calendar.MINUTE, 0)
            add(Calendar.SECOND, 0)
        }

        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )?.filter { it.totalTimeInForeground > 0 }
            ?.groupBy { it.packageName }
            ?.mapValues { (_, stats) ->
                stats.sumOf { it.totalTimeInForeground }
            }?.values?.sum() ?: 0
    }

    fun getNotificationsNumber(days: Int = 1): Long {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            add(Calendar.HOUR_OF_DAY, 0)
            add(Calendar.MINUTE, 0)
            add(Calendar.SECOND, 0)
            add(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        var notificationCount = 0
        val usageEvents = usageStatsManager.queryEventsForSelf(startTime, endTime)
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if(event.eventType == 50)
                notificationCount++

        }
        return notificationCount.toLong()
    }

    companion object {
        fun formatDuration(millis: Long): String {
            return String.format(
                Locale.US,
                "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % 60,
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            )
        }
    }
}