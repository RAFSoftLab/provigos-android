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
package com.provigos.android.presentation.viewmodel

import android.os.RemoteException
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provigos.android.data.DatabaseConnection
import com.provigos.android.data.HealthConnectManager
import kotlinx.coroutines.launch
import okio.IOException
import java.lang.IllegalStateException
import java.lang.StringBuilder
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.roundToLong

class HealthConnectViewModel(private val healthConnectManager: HealthConnectManager): ViewModel() {

    val healthConnectData:  MutableMap<String, MutableMap<String, Long>> = HashMap()

    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    var permissionsGranted = mutableStateOf(false)
        private set

    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    var stepsList: MutableState<List<StepsRecord>> = mutableStateOf(listOf())
        private set

    var aggregateStepsForToday: MutableState<Long> = mutableLongStateOf(0)
        private set

    var weightList: MutableState<List<WeightRecord>> = mutableStateOf(listOf())

    var lastWeight: Long = 0

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

    val stepsToday: MutableMap<String, Long> = HashMap()

    val weightToday: MutableMap<String, Long> = HashMap()


    private suspend fun tryWithPermissionCheck(block: suspend () -> Unit) {
        permissionsGranted.value = healthConnectManager.hasAllPermissions(PERMISSIONS)
        uiState = try {
            if(permissionsGranted.value) {
                block()
            }
            UiState.Done
        } catch (remoteException: RemoteException) {
            UiState.Error(remoteException)
        } catch (securityException: SecurityException) {
            UiState.Error(securityException)
        } catch (ioException: IOException) {
            UiState.Error(ioException)
        } catch (illegalStateException: IllegalStateException) {
            UiState.Error(illegalStateException)
        }
    }

    fun init() {
        viewModelScope.launch {
            tryWithPermissionCheck {
                readWeightForToday()
                aggregateStepsForToday()
                readSteps()
                DatabaseConnection().postData(healthConnectData)
            }
        }
    }

    private suspend fun readSteps() {
        val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val now = Instant.now()
        stepsList.value = healthConnectManager.readSteps(startOfDay.toInstant(), now)
    }

    private suspend fun aggregateStepsForToday() {
        val zdt = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        aggregateStepsForToday.value = healthConnectManager.aggregateStepsForToday(zdt.toInstant())!!
        stepsToday[pureDate(zdt)] = aggregateStepsForToday.value
        healthConnectData["steps"] = stepsToday
    }

    private suspend fun readWeightForToday() {
        val zdt = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        weightList.value = healthConnectManager.readWeightForToday(zdt.toInstant())
        lastWeight = weightList.value.last().weight.inKilograms.roundToLong()
        weightToday[pureDate(zdt)] = lastWeight
        healthConnectData["weight"] = weightToday
    }


    private fun pureDate(zdt: ZonedDateTime): String {
        return "" + zdt.year + "-" + zdt.month.value + "-" + zdt.dayOfMonth
    }

    sealed class UiState {
        data object Uninitialized: UiState()
        data object Done: UiState()
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }

}