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
import kotlinx.coroutines.flow.SharedFlow
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
                         val mHttpManager: HttpManager): ViewModel() {

    private val refreshMutex = Mutex()
    private val cacheMutex = Mutex()

    private var currentRefreshJob: Job? = null

    private val sharedPrefs = SharedPreferenceManager.get()
    private var zdt = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
    private var pureZdt = pureDate(zdt)

    private fun pureDate(zdt: ZonedDateTime): String {
        return String.format(Locale.getDefault(),"%d-%02d-%02d", zdt.year, zdt.month.value, zdt.dayOfMonth)
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Uninitialized)
    val uiState: StateFlow<UiState> get() = _uiState

    private val _dataToView = MutableStateFlow<Map<String, String>>(emptyMap())
    val dataToView: StateFlow<Map<String, String>> get() = _dataToView
    private val _dataToSend = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())


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

    private val _navigateIntegrationScreen = MutableStateFlow<String?>(null)
    val navigateIntegrationScreen: SharedFlow<String?> get() = _navigateIntegrationScreen

    fun setIntegrationSettings(destination: String) {
        _navigateIntegrationScreen.tryEmit(destination)
    }

    fun resetIntegration() {
        _navigateIntegrationScreen.tryEmit(null)
    }

    private val _preferencesUpdated = MutableStateFlow(false)
    val preferencesUpdated: StateFlow<Boolean> get() = _preferencesUpdated

    fun notifyPreferencesChanged(destination: String) {
        invalidateCache(destination)
        _preferencesUpdated.tryEmit(true)
    }

    fun resetPreferencesChanged() {
        _preferencesUpdated.value = false
    }

    private val invalidatedCaches = mutableSetOf<String>()

    private val _cachedHealthDataToView = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _cachedHealthDataToSend = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())

    private val _cachedGithubDataToView = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _cachedGithubDataToSend = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())

    private val _cachedSpotifyDataToView = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _cachedSpotifyDataToSend = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())

    fun invalidateCache(destination: String) {
        viewModelScope.launch {
            cacheMutex.withLock {
                invalidatedCaches.add(destination)
                when(destination) {
                    "health_connect" -> {
                        _cachedHealthDataToView.value = emptyMap()
                        _cachedHealthDataToSend.value = emptyMap()
                    }
                    "github" -> {
                        _cachedGithubDataToView.value = emptyMap()
                        _cachedGithubDataToSend.value = emptyMap()
                    }
                    "spotify" -> {
                        _cachedSpotifyDataToView.value = emptyMap()
                        _cachedSpotifyDataToSend.value = emptyMap()
                    }
                    "custom" -> {

                    }
                }
            }
        }
    }

    private fun combineCaches() {

        val healthEnabled = sharedPrefs.isHealthUser()
        val githubEnabled = sharedPrefs.isGithubUser()
        val spotifyEnabled = sharedPrefs.isSpotifyUser()

        _dataToView.update { current -> current +
            (if (healthEnabled) _cachedHealthDataToView.value else emptyMap()) +
                    (if (githubEnabled) _cachedGithubDataToView.value else emptyMap()) +
                    (if (spotifyEnabled) _cachedSpotifyDataToView.value else emptyMap())

        }
        _dataToSend.update { current -> current +
            (if (healthEnabled) _cachedHealthDataToSend.value else emptyMap()) +
                    (if (githubEnabled) _cachedGithubDataToSend.value else emptyMap()) +
                    (if (spotifyEnabled) _cachedSpotifyDataToSend.value else emptyMap())
        }

        _uiState.value = UiState.Done
    }

    fun fetchAllData() {

        val healthConnect = sharedPrefs.isHealthUser()
        val github = sharedPrefs.isGithubUser()
        val spotify = sharedPrefs.isSpotifyUser()

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                coroutineScope {
                    val healthDataJob =
                        if (healthConnect) async { readHealthConnectData() } else null
                    val githubDataJob = if (github) async { readGithubData() } else null
                    val spotifyDataJob = if (spotify) async { readSpotifyData() } else null

                    healthDataJob?.await()
                    githubDataJob?.await()
                    spotifyDataJob?.await()
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e)
            }

            combineCaches()
            //sendData(_dataToSend.value)
        }
    }

    private fun sendData(dataToSend: Map<String, Map<String, String>>) {
        viewModelScope.launch {
            val result = mHttpManager.postProvigosData(dataToSend)
            result.onSuccess {
                Timber.tag("DashboardViewModel").d("Data sent: $it")
            }.onFailure { exception ->
                Timber.e("Error sending data: ${exception.message}")
            }
        }
    }

    fun refreshData() {

        currentRefreshJob?.cancel()

        currentRefreshJob = viewModelScope.launch {

            refreshMutex.withLock {

                _uiState.value = UiState.Loading

                try {
                    coroutineScope {
                        val healthDataJob = async { refreshHealthData() }
                        val githubDataJob = async { refreshGithubData() }
                        val spotifyDataJob = async { refreshSpotifyData() }

                        healthDataJob.await()
                        githubDataJob.await()
                        spotifyDataJob.await()
                    }
                } catch (e: Exception) {
                    UiState.Error(e)
                }

                combineCaches()
                //_sendData(_dataToSend.value)
            }
        }
    }

    private suspend fun refreshHealthData() {
        if(!sharedPrefs.isHealthUser()) return

        val (isInvalid, viewEmpty, sendEmpty) = cacheMutex.withLock {
            Triple("health_connect" in invalidatedCaches, _cachedHealthDataToView.value.isEmpty(), _cachedHealthDataToSend.value.isEmpty())
        }

        if (isInvalid || viewEmpty || sendEmpty) {
            readHealthConnectData()
            cacheMutex.withLock { invalidatedCaches.remove("health_connect") }
        }
    }

    private suspend fun refreshGithubData() {
        if(!sharedPrefs.isGithubUser()) return

        val (isInvalid, viewEmpty, sendEmpty) = cacheMutex.withLock {
            Triple("github" in invalidatedCaches, _cachedGithubDataToView.value.isEmpty(), _cachedGithubDataToSend.value.isEmpty())
        }

        if (isInvalid || viewEmpty || sendEmpty) {
            readGithubData()
            cacheMutex.withLock { invalidatedCaches.remove("github") }
        }
    }

    private suspend fun refreshSpotifyData() {
        if(!sharedPrefs.isSpotifyUser()) return

        val (isInvalid, viewEmpty, sendEmpty) = cacheMutex.withLock {
            Triple("spotify" in invalidatedCaches, _cachedSpotifyDataToView.value.isEmpty(), _cachedSpotifyDataToSend.value.isEmpty())
        }

        if(isInvalid || viewEmpty || sendEmpty) {
            readSpotifyData()
            cacheMutex.withLock { invalidatedCaches.remove("spotify") }
        }
    }

    private suspend fun readHealthConnectData() {

        try {
            withContext(Dispatchers.IO) {

                if (mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getReadPermission(StepsRecord::class))) {
                    val stepsMap = mHealthConnectManager.readStepsForLast30Days(zdt.toInstant())
                    val lastStep = stepsMap.values.lastOrNull()
                    if(lastStep != null) {
                        _cachedHealthDataToView.update { current -> current + ("steps" to lastStep) }
                        _cachedHealthDataToSend.update { current -> current + ("steps" to stepsMap) }
                    } else {
                        Timber.tag("DashboardViewModel").d("Error reading lastValue for steps")
                    }
                } else {
                    Timber.tag("DashboardViewModel").d("No steps permission")
                }

                if (mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getReadPermission(WeightRecord::class))) {
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
                } else {
                    Timber.tag("DashboardViewModel").d("No weight permission")
                }

                if (mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getReadPermission(HeightRecord::class))) {
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
                } else {
                    Timber.tag("DashboardViewModel").d("No height permission")
                }

                if (mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getReadPermission(HeartRateRecord::class))) {
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
                } else {
                    Timber.tag("DashboardViewModel").d("No heart rate permission")
                }

                if (mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getReadPermission(BodyFatRecord::class))) {
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
                } else {
                    Timber.tag("DashboardViewModel").d("No body fat permission")
                }

                if (mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getReadPermission(BloodPressureRecord::class))) {
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
                } else {
                    Timber.tag("DashboardViewModel").d("No blood pressure permission")
                }

                if (mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getReadPermission(BodyTemperatureRecord::class))) {
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
                } else {
                    Timber.tag("DashboardViewModel").d("No body temperature permission")
                }

                 if(mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getReadPermission(BloodGlucoseRecord::class))) {
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
                } else {
                     Timber.tag("DashboardViewModel").d("No blood glucose permission")
                 }

                if (mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getReadPermission(OxygenSaturationRecord::class))) {
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
                } else {
                    Timber.tag("DashboardViewModel").d("No oxygen saturation permission")
                }

                if (mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getReadPermission(RespiratoryRateRecord::class))) {
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
                } else {
                    Timber.tag("DashboardViewModel").d("No respiratory rate permission")
                }
            }
        } catch (e: Exception) {
            Timber.tag("DashboardViewModel").d("Health Connect data fetch failed: ${e.message}")
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
                _cachedHealthDataToSend.update {  currentMap -> currentMap + (key to map) }
                val lastValue = map.values.lastOrNull()
                if(lastValue != null) {
                    _cachedHealthDataToView.update {  current -> current + (key to lastValue) }
                } else {
                    Timber.tag("DashboardViewModel").d("Error reading lastValue for $key")
                }
            } else {
                Timber.tag("DashboardViewModel").d("Empty data list for $key")
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
        try {
            withContext(Dispatchers.IO) {
                val githubCommits = mHttpManager.getGithubData()
                if (sharedPrefs.isAllowGithubTotalCommits()) {
                    val totalCommits = githubCommits.values.sumOf { it.toLong() }
                    _cachedGithubDataToView.update { current -> current + ("githubTotal" to totalCommits.toString()) }
                    //_cachedGithubDataToSend.update { current -> current + ("githubTotal" to mapOf(pureZdt to totalCommits.toString())) }
                }
                if (sharedPrefs.isAllowGithubDailyCommits()) {
                    val dailyCommits = githubCommits[pureZdt] ?: "0"
                    _cachedGithubDataToView.update { current -> current + ("githubDaily" to dailyCommits) }
                    //_cachedGithubDataToSend.update { current -> current + ("githubDaily" to mapOf(pureZdt to dailyCommits)) }
                }
            }
        } catch (e: Exception) {
            Timber.tag("DashboardViewModel").d("GitHub data fetch failed")
        }
    }

    private suspend fun readSpotifyData() {
        try {
            withContext(Dispatchers.IO) {
                val spotifyData = mHttpManager.getSpotifyData()
                if (sharedPrefs.isAllowSpotifyArtistGenres()) {
                    val popularGenre: String? = spotifyData["spotifyGenre"]
                    if (popularGenre != null) {
                        _cachedSpotifyDataToView.update { current -> current + ("spotifyGenre" to popularGenre) }
                        //_cachedSpotifyDataToSend.update { current -> current + ("spotifyGenre" to mapOf(pureZdt to popularGenre)) }
                    }
                }
                if (sharedPrefs.isAllowSpotifyArtistPopularity()) {
                    val popularity = spotifyData["spotifyPopularity"]
                    if (popularity != null) {
                        _cachedSpotifyDataToView.update { current -> current + ("spotifyPopularity" to popularity) }
                        //_cachedSpotifyDataToSend.update { current -> current + ("spotifyPopularity" to mapOf(pureZdt to popularity)) }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("DashboardViewModel").d("Spotify data fetch failed")
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