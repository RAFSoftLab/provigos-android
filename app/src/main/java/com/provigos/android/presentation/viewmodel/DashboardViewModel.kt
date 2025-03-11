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
package com.provigos.android.presentation.viewmodel

import android.content.Context
import android.os.RemoteException
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provigos.android.data.HealthConnectManager
import com.provigos.android.data.HttpManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okio.IOException
import timber.log.Timber
import java.lang.IllegalStateException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DashboardViewModel(private val healthConnectManager: HealthConnectManager, private val httpManager: HttpManager): ViewModel() {

    private val _dataToView = MutableStateFlow<Map<String, String>>(emptyMap())
    val dataToView: StateFlow<Map<String, String>> get() = _dataToView

    private val _dataToSend = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val dataToSend: StateFlow<Map<String, Map<String, String>>> get() = _dataToSend

    private var zdt = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
    private var pureZdt = pureDate(zdt)

    private fun pureDate(zdt: ZonedDateTime): String { return "" + zdt.year + "-" + zdt.month.value + "-" + zdt.dayOfMonth }

    private val _uiState = MutableStateFlow<UiState>(UiState.Uninitialized)
    val uiState: StateFlow<UiState> get() = _uiState

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    )

    var permissionsGranted = mutableStateOf(false)
        private set

    var stepsList: MutableState<List<StepsRecord>> = mutableStateOf(listOf())
        private set
    var stepsFor30Days: MutableMap<String, String> = HashMap()
        private set

    private suspend fun tryWithPermissionCheck(block: suspend () -> Unit) {
        permissionsGranted.value = healthConnectManager.hasAllPermissions(permissions)

        _uiState.value = UiState.Loading

        try {
            if(permissionsGranted.value) {
                block()
                _uiState.value = UiState.Done
            } else {
                _uiState.value = UiState.Error(Exception("Permissions not granted"))
            }
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e)
        }
    }

    fun init(context: Context) {
        viewModelScope.launch {
            tryWithPermissionCheck {
                _uiState.value = UiState.Loading

                _dataToView.value = emptyMap()
                _dataToSend.value = emptyMap()

                coroutineScope {
                    val healthDataJob = async { readHealthConnectData() }
                    val githubDataJob = async { readGithubData(context) }

                    healthDataJob.await()
                    githubDataJob.await()
                }

                sendData(context, _dataToSend.value)

                _uiState.value = UiState.Done
            }
        }
    }

    private suspend fun readHealthConnectData() {
        readStepsFor30Days()

        readHealthData(
            healthConnectManager::readWeightForLast30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.weight.inKilograms.toString() },
            "weight"
        )

        readHealthData(
            healthConnectManager::readHeightForLast30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to (record.height.inMeters * 100).toLong().toString() },
            "height"
        )

        readHealthData(
            healthConnectManager::readHeartRateForLast30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.startTime, ZoneId.systemDefault())) to record.samples.last().beatsPerMinute.toString() },
            "heartRate"
        )

        readHealthData(
            healthConnectManager::readBodyFatForLast30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.percentage.value.toString() },
            "bodyFat"
        )

        readHealthData(
            healthConnectManager::readBloodPressureForLast30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to
                    "${record.systolic.inMillimetersOfMercury.toLong()}/${record.diastolic.inMillimetersOfMercury.toLong()}" },
            "bloodPressure"
        )

        readHealthData(
            healthConnectManager::readBodyTemperatureForLast30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.temperature.inCelsius.toString() },
            "bodyTemperature"
        )

        readHealthData(
            healthConnectManager::readBloodGlucoseForLast30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.level.inMillimolesPerLiter.toString() },
            "bloodGlucose"
        )

        readHealthData(
            healthConnectManager::readOxygenSaturationForLast30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.percentage.value.toString() },
            "oxygenSaturation"
        )

        readHealthData(
            healthConnectManager::readRespiratoryRateForLast30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.rate.toString() },
            "respiratoryRate"
        )
    }

    private suspend fun <T> readHealthData(readFunction: suspend (java.time.Instant) -> List<T>,
                                           extractor: (T) -> Pair<String, String>,
                                           key: String) {
        val map = mutableMapOf<String, String>()
        try {
            val dataList = readFunction(zdt.toInstant())
            if(dataList.isNotEmpty()) {
                dataList.forEach { record ->
                    val (date, value) = extractor(record)
                    map[date] = value
                }
                _dataToSend.value += (key to map)
                _dataToView.value += (key to map.values.last())
            }
        } catch (_: Exception) {
            Timber.tag("DashboardViewModel").d("Error reading health data for $key")
        }
    }

    private suspend fun readStepsFor30Days() {
        try {
            stepsList.value = healthConnectManager.readStepsForLast30Days(zdt.toInstant())
            val stepsMap = mutableMapOf<String, String>()
            if(stepsList.value.isNotEmpty()) {
                stepsList.value.forEach { stepsRecord: StepsRecord ->
                    val date = pureDate(ZonedDateTime.ofInstant(stepsRecord.startTime, ZoneId.systemDefault()))
                    stepsMap[date] = stepsRecord.count.toString()
                    }
                }
                stepsMap.forEach { (date, count) ->
                    stepsFor30Days[date] = (stepsFor30Days[date]?.toLong()?.plus(count.toLong()) ?: count.toLong()).toString()
                }
            _dataToSend.value += ("steps" to stepsFor30Days)
            _dataToView.value += ("steps" to stepsFor30Days.values.last())
        } catch (_: Exception) {
            Timber.tag("DashboardViewModel").d("Error reading steps data")
        }
    }

    suspend fun writeSteps(start: ZonedDateTime, count: Long) = healthConnectManager.writeSteps(start, count)
    suspend fun writeWeight(date: ZonedDateTime, weight: Double) = healthConnectManager.writeWeight(date, weight)
    suspend fun writeHeight(date: ZonedDateTime, height: Long) = healthConnectManager.writeHeight(date, height)
    suspend fun writeHeartRate(start: ZonedDateTime, end: ZonedDateTime, count: Long) = healthConnectManager.writeHeartRate(start, end, count)
    suspend fun writeBodyFat(date: ZonedDateTime, percentage: Double) = healthConnectManager.writeBodyFat(date, percentage)
    suspend fun writeBloodPressure(date: ZonedDateTime, upper: Long, lower: Long) = healthConnectManager.writeBloodPressure(date, upper, lower)
    suspend fun writeBodyTemperature(date: ZonedDateTime, temperature: Double) = healthConnectManager.writeBodyTemperature(date, temperature)
    suspend fun writeBloodGlucose(date: ZonedDateTime, level: Double) = healthConnectManager.writeBloodGlucose(date, level)
    suspend fun writeOxygenSaturation(date: ZonedDateTime, percentage: Double) = healthConnectManager.writeOxygenSaturation(date, percentage)
    suspend fun writeRespiratoryRate(date: ZonedDateTime, rate: Double) = healthConnectManager.writeRespiratoryRate(date, rate)

    private suspend fun readGithubData(context: Context) {
        val githubCommits = httpManager.getGithubCommits(context)
        val commitMap = mutableMapOf<String, String>()
        var count: Long = 0
        for (entry in githubCommits.entries) {
            count += entry.value.toLong()
            commitMap[entry.key] = entry.value
        }
        //_dataToSend.value += ("github" to commitMap)
        _dataToView.value += ("totalGithubCommits" to count.toString())
        val dailyCommits = commitMap[pureZdt] ?: "0"
        _dataToView.value += ("dailyGithubCommits" to dailyCommits)
    }

    private fun sendData(context: Context, dataToSend: Map<String, Map<String, String>>) {
        viewModelScope.launch {
            val result = httpManager.postData(context, dataToSend)
            result.onSuccess {
                Timber.tag("DashboardViewModel").d("Data sent: $it")
            }.onFailure { exception ->
                Timber.e("Error sending data: ${exception.message}")
            }
        }
    }

    fun refreshData(context: Context) {
        viewModelScope.launch {
            _uiState.value = UiState.Refreshing

            _dataToView.value = emptyMap()
            _dataToSend.value = emptyMap()

            coroutineScope {
                val healthDataJob = async { readHealthConnectData() }
                val githubDataJob = async { readGithubData(context) }

                healthDataJob.await()
                githubDataJob.await()
            }

            sendData(context, _dataToSend.value)

            _uiState.value = UiState.Done
        }
    }

    sealed class UiState {
        data object Uninitialized: UiState()
        data object Done: UiState()
        data object Loading: UiState()
        data object Refreshing: UiState()
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }
}