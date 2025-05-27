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
import com.provigos.android.data.api.AndroidUsageStatsManager
import com.provigos.android.data.api.HealthConnectManager
import com.provigos.android.data.api.HttpManager
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.data.model.DashboardViewItemModel
import com.provigos.android.data.model.custom.CustomItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.Thread.State
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

class DashboardViewModel(private val mHealthConnectManager: HealthConnectManager,
    private val mHttpManager: HttpManager,
    private val mAndroidUsageStatsManager: AndroidUsageStatsManager): ViewModel() {

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
    private val _dataToSend = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val dataToSend: StateFlow<Map<String, Map<String, String>>> get() = _dataToSend

    private val _mDashboardViewList = MutableStateFlow<List<DashboardViewItemModel>>(emptyList())
    val mDashboardViewList: StateFlow<List<DashboardViewItemModel>> get() = _mDashboardViewList

    private val _customData = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val customData: StateFlow<Map<String, Map<String, String>>> get() = _customData
    private val _customKeys = MutableStateFlow<List<CustomItemModel>>(emptyList())
    val customKeys: StateFlow<List<CustomItemModel>> get() = _customKeys

    private val _navigateIntegrationScreen = MutableStateFlow<String?>(null)
    val navigateIntegrationScreen: SharedFlow<String?> get() = _navigateIntegrationScreen

    fun setIntegrationSettings(destination: String) = _navigateIntegrationScreen.tryEmit(destination)

    fun resetIntegration() = _navigateIntegrationScreen.tryEmit(null)

    fun notifyPreferencesChanged(destination: String) {
        Timber.d("notifyPreferencesChanged $destination")
        viewModelScope.launch {
            invalidateCache(destination)
            refreshData()
        }
    }

    private val invalidatedCaches = mutableSetOf<String>()
    private val _cachedHealthDataToView = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _cachedHealthDataToSend = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    private val _cachedGithubDataToView = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _cachedGithubDataToSend = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    private val _cachedSpotifyDataToView = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _cachedSpotifyDataToSend = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    private val _cachedCustomDataToView = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _cachedAndroidDataToView = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _cachedAndroidDataToSend = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())

    suspend fun invalidateCache(destination: String) {
        cacheMutex.withLock {
            invalidatedCaches.add(destination)
            when (destination) {
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
                    _cachedCustomDataToView.value = emptyMap()

                }
                "android" -> {
                    _cachedAndroidDataToView.value = emptyMap()
                    _cachedAndroidDataToSend.value = emptyMap()
                }
            }
        }
    }

    private fun combineCaches() {
        val healthEnabled = sharedPrefs.isHealthUser()
        val githubEnabled = sharedPrefs.isGithubUser()
        val spotifyEnabled = sharedPrefs.isSpotifyUser()
        val customEnabled = sharedPrefs.isCustomUser()
        val androidEnabled = sharedPrefs.isAndroidUser()

        val newDataToView = buildMap {
            if (healthEnabled) putAll(_cachedHealthDataToView.value)
            if (githubEnabled) putAll(_cachedGithubDataToView.value)
            if (spotifyEnabled) putAll(_cachedSpotifyDataToView.value)
            if (customEnabled) putAll(_cachedCustomDataToView.value)
            if (androidEnabled) putAll(_cachedAndroidDataToView.value)
        }

        val newDataToSend = buildMap {
            if (healthEnabled) putAll(_cachedHealthDataToSend.value)
            if (githubEnabled) putAll(_cachedGithubDataToSend.value)
            if (spotifyEnabled) putAll(_cachedSpotifyDataToSend.value)
            if (androidEnabled) putAll(_cachedAndroidDataToSend.value)
        }

        Timber.d("combineCaches new data $newDataToView")
        _dataToView.value = newDataToView
        _dataToSend.value = newDataToSend
        _mDashboardViewList.value = transformData(_dataToSend.value)
    }

    private fun transformData(map: Map<String, Map<String, String>>): List<DashboardViewItemModel> {
        return map.mapNotNull { (k, values) ->
            val recentDate = values.keys.maxOrNull()
            recentDate?.let { date ->
                val v = values[date]
                when (k) {
                    "steps" -> DashboardViewItemModel("steps", "Steps", v!!, date)
                    "weight" -> DashboardViewItemModel("weight", "Weight", "${v!!.toDouble().roundToLong()} kg", date)
                    "heartRate" -> DashboardViewItemModel("heartRate", "Heart rate", "${v!!.toDouble().roundToLong()} bpm", date)
                    "bodyFat" -> DashboardViewItemModel("bodyFat", "Body fat", "$v %", date)
                    "bloodPressure" -> DashboardViewItemModel("bloodPressure", "Blood pressure", "$v mmHg", date)
                    "height" -> DashboardViewItemModel("height", "Height", "$v cm", date)
                    "bodyTemperature" -> DashboardViewItemModel("bodyTemperature", "Body temperature", String.format(Locale.US, "%.1f ℃", v!!.toDouble()), date)
                    "oxygenSaturation" -> DashboardViewItemModel("oxygenSaturation", "Oxygen saturation", "$v %", date)
                    "bloodGlucose" -> DashboardViewItemModel("bloodGlucose", "Blood glucose", "$v mmol/L", date)
                    "respiratoryRate" -> DashboardViewItemModel("respiratoryRate", "Respiratory rate", "$v rpm", date)
                    "githubTotal" -> DashboardViewItemModel("githubTotal", "Total commits", v!!, date)
                    "githubDaily" -> DashboardViewItemModel("githubDaily", "Commits today", v!!, date)
                    "spotifyGenre" -> DashboardViewItemModel("spotifyGenre", "Most listened to genre", v!!, date)
                    "spotifyPopularity" -> DashboardViewItemModel("spotifyPopularity", "Average artist popularity", v!!, date)
                    "screenTime" -> DashboardViewItemModel("screenTime", "Daily screen time", v!!, date)
                    "unlockAttempts" -> DashboardViewItemModel("unlockAttempts", "Unlock attempts", v!!, date)
                    else -> transformCustomData(k, v!!, date)
                }
            }
        }
    }

    private fun transformCustomData(key: String, value: String, date: String): DashboardViewItemModel? {
        if(!sharedPrefs.isCustomUser()) return null
        val item = _customKeys.value.find { it.name == key }
        return if(item != null && sharedPrefs.isAllowCustomItem(item.name)) {
            val name = item.name
            val unit = item.units
            val label = item.label
            DashboardViewItemModel(name, label, "$value $unit", date)
        } else {
            null
        }
    }

    fun fetchAllData() {

        val healthConnect = sharedPrefs.isHealthUser()
        val github = sharedPrefs.isGithubUser()
        val spotify = sharedPrefs.isSpotifyUser()
        val custom = sharedPrefs.isCustomUser()
        val android = sharedPrefs.isAndroidUser()

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                coroutineScope {
                    val healthDataJob = if (healthConnect) async { readHealthConnectData() } else null
                    val githubDataJob = if (github) async { readGithubData() } else null
                    val spotifyDataJob = if (spotify) async { readSpotifyData() } else null
                    val customDataJob = if (custom) async { readCustomData() } else null
                    val androidDataJob = if (android) async { readAndroidData() } else null

                    healthDataJob?.await()
                    githubDataJob?.await()
                    spotifyDataJob?.await()
                    customDataJob?.await()
                    androidDataJob?.await()
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e)
            }

            combineCaches()
            //sendData(_dataToSend.value)
            _uiState.value = UiState.Done
        }
    }

    private fun sendData(dataToSend: Map<String, Map<String, String>> = _dataToSend.value) {
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
        Timber.d("refreshData refreshing")
        currentRefreshJob?.cancel()

        currentRefreshJob = viewModelScope.launch {

            refreshMutex.withLock {

                _uiState.value = UiState.Refreshing

                try {
                    coroutineScope {
                        val healthDataJob = async { refreshHealthData() }
                        val githubDataJob = async { refreshGithubData() }
                        val spotifyDataJob = async { refreshSpotifyData() }
                        val customDataJob = async { refreshCustomData() }
                        val androidDataJob = async { refreshAndroidData() }

                        healthDataJob.await()
                        githubDataJob.await()
                        spotifyDataJob.await()
                        customDataJob.await()
                        androidDataJob.await()
                    }
                } catch (e: Exception) {
                    UiState.Error(e)
                }
                cacheMutex.withLock {
                    combineCaches()
                }
                //sendData(_dataToSend.value)
                _uiState.value = UiState.Done
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

    private suspend fun refreshCustomData() {
        if(!sharedPrefs.isCustomUser()) return

        val (isInvalid, viewEmpty) = cacheMutex.withLock {
            Pair("spotify" in invalidatedCaches, _cachedCustomDataToView.value.isEmpty())
        }
        if(isInvalid || viewEmpty) {
            readCustomData()
            cacheMutex.withLock { invalidatedCaches.remove("spotify") }
        }
    }

    private suspend fun refreshAndroidData() {
        if(!sharedPrefs.isAndroidUser()) return

        val (isInvalid, viewEmpty, sendEmpty) = cacheMutex.withLock {
            Triple("android" in invalidatedCaches, _cachedAndroidDataToView.value.isEmpty(), _cachedAndroidDataToSend.value.isEmpty())
        }
        if(isInvalid || viewEmpty || sendEmpty) {
            readAndroidData()
            cacheMutex.withLock { invalidatedCaches.remove("android") }
        }
    }

    private suspend fun readHealthConnectData() {

        val maxRetries = 3
        var retryCount = 0
        val baseDelay = 1000L

        while (retryCount <= maxRetries) {
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
            return
        } catch (e: Exception) {
            if (retryCount == maxRetries) {
                Timber.tag("DashboardViewModel").d("Health Connect data fetch failed: ${e.message}")
                break
            }
        }
            val delayMs = baseDelay * (retryCount + 1)
            Timber.tag("DashboardViewModel").d("Health Connect data fetch failed (retry ${retryCount + 1}/$maxRetries). Retrying in $delayMs ms")
            delay(delayMs)
            retryCount++
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

    suspend fun writeSteps(start: ZonedDateTime, count: Long): Boolean {
        if(mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getWritePermission(StepsRecord::class))) {
            mHealthConnectManager.writeSteps(start, count)
            return true
        } else {
            return false
        }
    }
    suspend fun writeWeight(date: ZonedDateTime, weight: Double): Boolean {
       if(mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getWritePermission(WeightRecord::class))) {
            mHealthConnectManager.writeWeight(date, weight)
           return true
       } else {
           return false
       }
    }
    suspend fun writeHeight(date: ZonedDateTime, height: Long): Boolean {
        if(mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getWritePermission(HeightRecord::class))) {
            mHealthConnectManager.writeHeight(date, height)
            return true
        } else {
            return false
        }
    }
    suspend fun writeHeartRate(start: ZonedDateTime, end: ZonedDateTime, count: Long): Boolean {
        if(mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getWritePermission(HeartRateRecord::class))) {
            mHealthConnectManager.writeHeartRate(start, end, count)
            return true
        } else {
            return false
        }
    }
    suspend fun writeBodyFat(date: ZonedDateTime, percentage: Double): Boolean {
        if(mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getWritePermission(BodyFatRecord::class))) {
            mHealthConnectManager.writeBodyFat(date, percentage)
            return true
        } else {
            return false
        }
    }
    suspend fun writeBloodPressure(date: ZonedDateTime, upper: Long, lower: Long): Boolean {
        if(mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getWritePermission(BloodPressureRecord::class))) {
            mHealthConnectManager.writeBloodPressure(date, upper, lower)
            return true
        } else {
            return false
        }
    }
    suspend fun writeBodyTemperature(date: ZonedDateTime, temperature: Double): Boolean {
        if(mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getWritePermission(BodyTemperatureRecord::class))) {
            mHealthConnectManager.writeBodyTemperature(date, temperature)
            return true
        } else {
            return false
        }
    }
    suspend fun writeBloodGlucose(date: ZonedDateTime, level: Double): Boolean {
        if(mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getWritePermission(BloodGlucoseRecord::class))) {
            mHealthConnectManager.writeBloodGlucose(date, level)
            return true
        } else {
            return false
        }
    }
    suspend fun writeOxygenSaturation(date: ZonedDateTime, percentage: Double): Boolean {
        if(mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getWritePermission(OxygenSaturationRecord::class))) {
            mHealthConnectManager.writeOxygenSaturation(date, percentage)
            return true
        } else {
            return false
        }
    }
    suspend fun writeRespiratoryRate(date: ZonedDateTime, rate: Double): Boolean {
        if(mHealthConnectManager.hasHealthConnectPermission(HealthPermission.getWritePermission(RespiratoryRateRecord::class))) {
            mHealthConnectManager.writeRespiratoryRate(date, rate)
            return true
        } else {
            return false
        }
    }

    private suspend fun readCustomData() {
        try {
            withContext(Dispatchers.IO) {
                _customKeys.value = mHttpManager.getProvigosCustomKeys()
                _customData.value = mHttpManager.getProvigosCustomData()
                val newDataToView = buildMap { putAll(_customData.value.mapValues { (_, innerMap) -> innerMap[pureZdt] ?: "0" }) }
                _cachedCustomDataToView.value = newDataToView
            }
        } catch (e: Exception) {
            Timber.tag("DashboardViewModel").d("Custom data fetch failed: ${e.message}")
        }
    }

    suspend fun writeCustomData(item: CustomItemModel, value: String): Boolean {
        val map = mapOf(pureZdt to value)
        val postKeys = mHttpManager.postProvigosCustomKeys(listOf(item))
        val postData = mHttpManager.postProvigosCustomData(mapOf(item.name to map))
        return postKeys && postData
    }

    suspend fun updateCustomData(name: String, value: String): Boolean {
        val map = mapOf(pureZdt to value)
        return mHttpManager.postProvigosCustomData(mapOf(name to map))
    }

    suspend fun updateCustomKeys(item: CustomItemModel): Boolean {
        return mHttpManager.postProvigosCustomKeys(listOf(item))
    }

    private suspend fun readGithubData() {
        try {
            withContext(Dispatchers.IO) {
                val githubCommits = mHttpManager.getGithubData()
                if (sharedPrefs.isAllowGithubTotalCommits()) {
                    val totalCommits = githubCommits.values.sumOf { it.toLong() }
                    _cachedGithubDataToView.update { current -> current + ("githubTotal" to totalCommits.toString()) }
                    _cachedGithubDataToSend.update { current -> current + ("githubTotal" to mapOf(pureZdt to totalCommits.toString())) }
                }
                if (sharedPrefs.isAllowGithubDailyCommits()) {
                    val dailyCommits = githubCommits[pureZdt] ?: "0"
                    _cachedGithubDataToView.update { current -> current + ("githubDaily" to dailyCommits) }
                    _cachedGithubDataToSend.update { current -> current + ("githubDaily" to mapOf(pureZdt to dailyCommits)) }
                }
            }
        } catch (e: Exception) {
            Timber.tag("DashboardViewModel").d("GitHub data fetch failed: ${e.message}")
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
                        _cachedSpotifyDataToSend.update { current -> current + ("spotifyGenre" to mapOf(pureZdt to popularGenre)) }
                    }
                }
                if (sharedPrefs.isAllowSpotifyArtistPopularity()) {
                    val popularity = spotifyData["spotifyPopularity"]
                    if (popularity != null) {
                        _cachedSpotifyDataToView.update { current -> current + ("spotifyPopularity" to popularity) }
                        _cachedSpotifyDataToSend.update { current -> current + ("spotifyPopularity" to mapOf(pureZdt to popularity)) }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("DashboardViewModel").d("Spotify data fetch failed: ${e.message}")
        }
    }

    private suspend fun readAndroidData() {
        try {
            withContext(Dispatchers.IO) {
                if (sharedPrefs.isAllowAndroidScreenTime()) {
                    val screenTime = mAndroidUsageStatsManager.getScreenTime()
                    val formattedScreenTime = AndroidUsageStatsManager.formatDuration(screenTime)
                    _cachedAndroidDataToView.update { current -> current + ("screenTime" to formattedScreenTime) }
                    _cachedAndroidDataToSend.update { current -> current + ("screenTime" to mapOf(pureZdt to formattedScreenTime)) }
                }
                Timber.d("is allow biometrics ${sharedPrefs.isAllowAndroidBiometrics()}")
                if (sharedPrefs.isAllowAndroidBiometrics()) {
                    val unlockAttempts = sharedPrefs.unlockAttemptsCount()
                    Timber.d("$unlockAttempts")
                    _cachedAndroidDataToView.update { current -> current + ("unlockAttempts" to unlockAttempts.toString()) }
                    _cachedAndroidDataToSend.update { current -> current + ("unlockAttempts" to mapOf(pureZdt to unlockAttempts.toString())) }
                }
            }
        } catch (e: Exception) {
            Timber.tag("DashboardViewModel").d("Android data fetch failed: ${e.message}")
        }
    }

    fun getChartData(destination: String): Map<String, String> {
        return _dataToSend.value.get(destination)!!
    }

    sealed class UiState {
        data object Uninitialized: UiState()
        data object Done: UiState()
        data object Loading: UiState()
        data object Refreshing: UiState()
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }
}