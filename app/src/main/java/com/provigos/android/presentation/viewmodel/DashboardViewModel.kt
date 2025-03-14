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
import com.provigos.android.data.api.HealthConnectManager
import com.provigos.android.data.api.HttpManager
import com.provigos.android.data.local.SharedPreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

class DashboardViewModel(val mHealthConnectManager: HealthConnectManager,
                         private val mHttpManager: HttpManager): ViewModel() {

    private val refreshMutex = Mutex()
    private var currentRefreshJob: Job? = null

    private val sharedPrefs = SharedPreferenceManager.get()
    private var zdt = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
    private var pureZdt = pureDate(zdt)

    private fun pureDate(zdt: ZonedDateTime): String {
        return String.format(Locale.getDefault(),"%d-%02d-%02d", zdt.year, zdt.month.value, zdt.dayOfMonth)
    }

    private val _dataToView = MutableStateFlow<Map<String, String>>(emptyMap())
    val dataToView: StateFlow<Map<String, String>> get() = _dataToView

    private val _dataToSend = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val dataToSend: StateFlow<Map<String, Map<String, String>>> get() = _dataToSend

    private val _uiState = MutableStateFlow<UiState>(UiState.Uninitialized)
    val uiState: StateFlow<UiState> get() = _uiState

    val writePermissionMap: Map<String, String> = mapOf(
        "steps" to HealthPermission.getWritePermission(StepsRecord::class),
        "weight" to HealthPermission.getWritePermission(WeightRecord::class),
        "height" to HealthPermission.getWritePermission(HeightRecord::class),
        "heartRate" to HealthPermission.getWritePermission(HeartRateRecord::class),
        "bodyFat" to HealthPermission.getWritePermission(BodyFatRecord::class),
        "bloodPressure" to HealthPermission.getWritePermission(BloodPressureRecord::class),
        "bodyTemperature" to HealthPermission.getWritePermission(BodyTemperatureRecord::class),
        "bloodGlucose" to HealthPermission.getWritePermission(BloodGlucoseRecord::class),
        "oxygenSaturation" to HealthPermission.getWritePermission(OxygenSaturationRecord::class),
        "respiratoryRate" to HealthPermission.getWritePermission(RespiratoryRateRecord::class)
    )

    fun fetchAllData() {

        val healthConnect = sharedPrefs.isHealthUser()
        val github = sharedPrefs.isGithubUser()
        val spotify = sharedPrefs.isSpotifyUser()

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            _dataToView.value = emptyMap()
            _dataToSend.value = emptyMap()


            coroutineScope {
                val healthDataJob = if (healthConnect) async { readHealthConnectData() } else null
                val githubDataJob = if (github) async { readGithubData() } else null
                val spotifyDataJob = if (spotify) async { readSpotifyData() } else null

                healthDataJob?.await()
                githubDataJob?.await()
                spotifyDataJob?.await()
            }

            //sendData(_dataToSend.value)

            _uiState.value = UiState.Done
        }
    }

    private fun sendData(dataToSend: Map<String, Map<String, String>>) {
        viewModelScope.launch {
            val result = mHttpManager.postData(dataToSend)
            result.onSuccess {
                Timber.tag("DashboardViewModel").d("Data sent: $it")
            }.onFailure { exception ->
                Timber.e("Error sending data: ${exception.message}")
            }
        }
    }

    fun refreshData() {

        val healthConnect = sharedPrefs.isHealthUser()
        val github = sharedPrefs.isGithubUser()
        val spotify = sharedPrefs.isSpotifyUser()

        currentRefreshJob?.cancel()

        currentRefreshJob = viewModelScope.launch {

            refreshMutex.withLock {
                _uiState.value = UiState.Loading

                _dataToView.value = emptyMap()
                _dataToSend.value = emptyMap()

                coroutineScope {
                    val healthDataJob = if (healthConnect) async { readHealthConnectData() } else null
                    val githubDataJob = if (github) async { readGithubData() } else null
                    val spotifyDataJob = if (spotify) async { readSpotifyData() } else null

                    healthDataJob?.await()
                    githubDataJob?.await()
                    spotifyDataJob?.await()
                }

                _uiState.value = UiState.Done
            }
        }
    }


    private suspend fun readHealthConnectData() {

        withContext(Dispatchers.IO) {
            if (mHealthConnectManager.hasHealthConnectPermission(
                    HealthPermission.getReadPermission(
                        StepsRecord::class
                    )
                )
            ) {
                val stepsMap = mHealthConnectManager.readStepsForLast30Days(zdt.toInstant())
                _dataToView.update { current -> current + ("steps" to stepsMap.values.last()) }
                _dataToSend.update { current -> current + ("steps" to stepsMap) }
            }

            if (mHealthConnectManager.hasHealthConnectPermission(
                    HealthPermission.getReadPermission(
                        WeightRecord::class
                    )
                )
            ) {
                readHealthData(
                    mHealthConnectManager::readWeightForLast30Days,
                    { record ->
                        pureDate(
                            ZonedDateTime.ofInstant(
                                record.time,
                                ZoneId.systemDefault()
                            )
                        ) to record.weight.inKilograms.toString()
                    },
                    "weight"
                )
            }

            if (mHealthConnectManager.hasHealthConnectPermission(
                    HealthPermission.getReadPermission(
                        HeightRecord::class
                    )
                )
            ) {
                readHealthData(
                    mHealthConnectManager::readHeightForLast30Days,
                    { record ->
                        pureDate(
                            ZonedDateTime.ofInstant(
                                record.time,
                                ZoneId.systemDefault()
                            )
                        ) to (record.height.inMeters * 100).toLong().toString()
                    },
                    "height"
                )
            }

            if (mHealthConnectManager.hasHealthConnectPermission(
                    HealthPermission.getReadPermission(
                        HeartRateRecord::class
                    )
                )
            ) {
                readHealthData(
                    mHealthConnectManager::readHeartRateForLast30Days,
                    { record ->
                        pureDate(
                            ZonedDateTime.ofInstant(
                                record.startTime,
                                ZoneId.systemDefault()
                            )
                        ) to record.samples.last().beatsPerMinute.toString()
                    },
                    "heartRate"
                )
            }

            if (mHealthConnectManager.hasHealthConnectPermission(
                    HealthPermission.getReadPermission(
                        BodyFatRecord::class
                    )
                )
            ) {
                readHealthData(
                    mHealthConnectManager::readBodyFatForLast30Days,
                    { record ->
                        pureDate(
                            ZonedDateTime.ofInstant(
                                record.time,
                                ZoneId.systemDefault()
                            )
                        ) to record.percentage.value.toString()
                    },
                    "bodyFat"
                )
            }

            if (mHealthConnectManager.hasHealthConnectPermission(
                    HealthPermission.getReadPermission(
                        BloodPressureRecord::class
                    )
                )
            ) {
                readHealthData(
                    mHealthConnectManager::readBloodPressureForLast30Days,
                    { record ->
                        pureDate(
                            ZonedDateTime.ofInstant(
                                record.time,
                                ZoneId.systemDefault()
                            )
                        ) to
                                "${record.systolic.inMillimetersOfMercury.toLong()}/${record.diastolic.inMillimetersOfMercury.toLong()}"
                    },
                    "bloodPressure"
                )
            }

            if (mHealthConnectManager.hasHealthConnectPermission(
                    HealthPermission.getReadPermission(
                        BodyTemperatureRecord::class
                    )
                )
            ) {
                readHealthData(
                    mHealthConnectManager::readBodyTemperatureForLast30Days,
                    { record ->
                        pureDate(
                            ZonedDateTime.ofInstant(
                                record.time,
                                ZoneId.systemDefault()
                            )
                        ) to record.temperature.inCelsius.toString()
                    },
                    "bodyTemperature"
                )
            }

            if (mHealthConnectManager.hasHealthConnectPermission(
                    HealthPermission.getReadPermission(
                        BloodGlucoseRecord::class
                    )
                )
            ) {
                readHealthData(
                    mHealthConnectManager::readBloodGlucoseForLast30Days,
                    { record ->
                        pureDate(
                            ZonedDateTime.ofInstant(
                                record.time,
                                ZoneId.systemDefault()
                            )
                        ) to record.level.inMillimolesPerLiter.toString()
                    },
                    "bloodGlucose"
                )
            }

            if (mHealthConnectManager.hasHealthConnectPermission(
                    HealthPermission.getReadPermission(
                        OxygenSaturationRecord::class
                    )
                )
            ) {
                readHealthData(
                    mHealthConnectManager::readOxygenSaturationForLast30Days,
                    { record ->
                        pureDate(
                            ZonedDateTime.ofInstant(
                                record.time,
                                ZoneId.systemDefault()
                            )
                        ) to record.percentage.value.toString()
                    },
                    "oxygenSaturation"
                )
            }

            if (mHealthConnectManager.hasHealthConnectPermission(
                    HealthPermission.getReadPermission(
                        RespiratoryRateRecord::class
                    )
                )
            ) {
                readHealthData(
                    mHealthConnectManager::readRespiratoryRateForLast30Days,
                    { record ->
                        pureDate(
                            ZonedDateTime.ofInstant(
                                record.time,
                                ZoneId.systemDefault()
                            )
                        ) to record.rate.toString()
                    },
                    "respiratoryRate"
                )
            }
        }
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
                _dataToSend.update {  currentMap -> currentMap + (key to map) }
                _dataToView.update {  current -> current + (key to map.values.last()) }
            }
        } catch (_: Exception) {
            Timber.tag("DashboardViewModel").d("Error reading health data for $key")
        }
    }

    suspend fun writeSteps(start: ZonedDateTime, count: Long) = mHealthConnectManager.writeSteps(start, count)
    suspend fun writeWeight(date: ZonedDateTime, weight: Double) = mHealthConnectManager.writeWeight(date, weight)
    suspend fun writeHeight(date: ZonedDateTime, height: Long) = mHealthConnectManager.writeHeight(date, height)
    suspend fun writeHeartRate(start: ZonedDateTime, end: ZonedDateTime, count: Long) = mHealthConnectManager.writeHeartRate(start, end, count)
    suspend fun writeBodyFat(date: ZonedDateTime, percentage: Double) = mHealthConnectManager.writeBodyFat(date, percentage)
    suspend fun writeBloodPressure(date: ZonedDateTime, upper: Long, lower: Long) = mHealthConnectManager.writeBloodPressure(date, upper, lower)
    suspend fun writeBodyTemperature(date: ZonedDateTime, temperature: Double) = mHealthConnectManager.writeBodyTemperature(date, temperature)
    suspend fun writeBloodGlucose(date: ZonedDateTime, level: Double) = mHealthConnectManager.writeBloodGlucose(date, level)
    suspend fun writeOxygenSaturation(date: ZonedDateTime, percentage: Double) = mHealthConnectManager.writeOxygenSaturation(date, percentage)
    suspend fun writeRespiratoryRate(date: ZonedDateTime, rate: Double) = mHealthConnectManager.writeRespiratoryRate(date, rate)

    private suspend fun readGithubData() {
        withContext(Dispatchers.IO) {
            val githubCommits = mHttpManager.getGithubCommits()
            if (sharedPrefs.isAllowGithubTotalCommits()) {
                val totalCommits = githubCommits.values.sumOf { it.toLong() }
                _dataToView.update { current -> current + ("githubTotal" to totalCommits.toString()) }
                //_dataToSend.update { current -> current + ("githubTotal" to mapOf(pureZdt to totalCommits.toString())) }
            }
            if (sharedPrefs.isAllowGithubDailyCommits()) {
                val dailyCommits = githubCommits[pureZdt] ?: "0"
                _dataToView.update { current -> current + ("githubDaily" to dailyCommits) }
                //_dataToSend.update { current -> current + ("githubDaily" to mapOf(pureZdt to dailyCommits)) }
            }
        }
    }

    private suspend fun readSpotifyData() {
        withContext(Dispatchers.IO) {
            val spotifyData = mHttpManager.getSpotifyArtists()
            if (sharedPrefs.isAllowSpotifyArtistGenres()) {
                val popularGenre: String? = spotifyData["spotifyGenre"]
                if (popularGenre != null) {
                    _dataToView.update { current -> current + ("spotifyGenre" to popularGenre) }
                    //_dataToSend.update { current -> current + ("spotifyGenre" to mapOf(pureZdt to popularGenre)) }
                }
            }
            if (sharedPrefs.isAllowSpotifyArtistPopularity()) {
                val popularity = spotifyData["spotifyPopularity"]
                if (popularity != null) {
                    _dataToView.update { current -> current + ("spotifyPopularity" to popularity) }
                    //_dataToSend.update { current -> current + ("spotifyPopularity" to mapOf(pureZdt to popularity)) }
                }
            }
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