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
import kotlinx.coroutines.launch
import okio.IOException
import timber.log.Timber
import java.lang.IllegalStateException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DashboardViewModel(private val healthConnectManager: HealthConnectManager): ViewModel() {

    var dataToSend:  MutableMap<String, MutableMap<String, String>> = HashMap()
    var dataToView: MutableMap<String, String> =  HashMap()

    private var zdt = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)

    private var pureZdt = pureDate(zdt)

    private fun pureDate(zdt: ZonedDateTime): String { return "" + zdt.year + "-" + zdt.month.value + "-" + zdt.dayOfMonth }

    val PERMISSIONS = setOf(
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

    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    var permissionsGranted = mutableStateOf(false)
        private set

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

    var stepsList: MutableState<List<StepsRecord>> = mutableStateOf(listOf())
        private set
    var stepsFor30Days: MutableMap<String, String> = HashMap()
        private set
    var weightFor30Days: MutableMap<String, String> = HashMap()
        private set
    var bodyFatFor30Days: MutableMap<String, String> = HashMap()
        private set
    var heartRateFor30Days: MutableMap<String, String> = HashMap()
        private set
    var bloodPressureFor30Days: MutableMap<String, String> = HashMap()
        private set
    var bodyTemperatureFor30Days: MutableMap<String, String> = HashMap()
        private set
    var heightFor30Days: MutableMap<String, String> = HashMap()
        private set
    var bloodGlucoseFor30Days: MutableMap<String, String> = HashMap()
        private set
    var oxygenSaturationFor30Days: MutableMap<String, String> = HashMap()
        private set
    var respiratoryRateFor30Days: MutableMap<String, String> = HashMap()
        private set

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

    fun init(context: Context) {
        viewModelScope.launch {
            //tryWithPermissionCheck {
                dataToView = HashMap()
                readHealthConnectData()
                //HttpManager().postData(context, dataToSend)
                //healthConnectData.forEach { item -> Timber.e("${item.key}, ${item.value}")}
                //dataToView.forEach { item -> Timber.e("${item.key}, ${item.value}") }
            //}
        }
    }

    private suspend fun readHealthConnectData() {
        readStepsFor30Days()

        readHealthData(
            healthConnectManager::readWeightForLast30Days,
            weightFor30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.weight.inKilograms.toString() },
            "weight"
        )

        readHealthData(
            healthConnectManager::readHeightForLast30Days,
            heightFor30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to (record.height.inMeters * 100).toLong().toString() },
            "height"
        )

        readHealthData(
            healthConnectManager::readHeartRateForLast30Days,
            heartRateFor30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.startTime, ZoneId.systemDefault())) to record.samples.last().beatsPerMinute.toString() },
            "heartRate"
        )

        readHealthData(
            healthConnectManager::readBodyFatForLast30Days,
            bodyFatFor30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.percentage.value.toString() },
            "bodyFat"
        )

        readHealthData(
            healthConnectManager::readBloodPressureForLast30Days,
            bloodPressureFor30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to
                    "${record.systolic.inMillimetersOfMercury.toLong()}/${record.diastolic.inMillimetersOfMercury.toLong()}" },
            "bloodPressure"
        )

        readHealthData(
            healthConnectManager::readBodyTemperatureForLast30Days,
            bodyTemperatureFor30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.temperature.inCelsius.toString() },
            "bodyTemperature"
        )

        readHealthData(
            healthConnectManager::readBloodGlucoseForLast30Days,
            bloodGlucoseFor30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.level.inMillimolesPerLiter.toString() },
            "bloodGlucose"
        )

        readHealthData(
            healthConnectManager::readOxygenSaturationForLast30Days,
            oxygenSaturationFor30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.percentage.value.toString() },
            "oxygenSaturation"
        )

        readHealthData(
            healthConnectManager::readRespiratoryRateForLast30Days,
            respiratoryRateFor30Days,
            { record -> pureDate(ZonedDateTime.ofInstant(record.time, ZoneId.systemDefault())) to record.rate.toString() },
            "respiratoryRate"
        )
    }

    private suspend fun <T> readHealthData(readFunction: suspend (java.time.Instant) -> List<T>,
                                           map: MutableMap<String, String>,
                                           extractor: (T) -> Pair<String, String>,
                                           key: String): Boolean {
        return try {
            val dataList = readFunction(zdt.toInstant())
            if(dataList.isNotEmpty()) {
                dataList.forEach { record ->
                    val (date, value) = extractor(record)
                    map[date] = value
                }
                dataToSend[key] = map
                dataToView[key] = map.values.last()
                true
            } else {
                false
            }
        } catch (_: NullPointerException) {
            false
        }
    }

    private suspend fun readStepsFor30Days(): Boolean {
        try {
            stepsList.value = healthConnectManager.readStepsForLast30Days(zdt.toInstant())
            if(stepsList.value.isNotEmpty()) {
                stepsList.value.forEach { stepsRecord: StepsRecord ->
                    val zdt = ZonedDateTime.ofInstant(stepsRecord.startTime, ZoneId.systemDefault())
                    if(stepsFor30Days.containsKey(pureDate(zdt))) {
                        stepsFor30Days[pureDate(zdt)] = (stepsFor30Days[pureDate(zdt)]!!.toLong() + stepsRecord.count).toString()
                    } else {
                        stepsFor30Days[pureDate(zdt)] = stepsRecord.count.toString()
                    }
                }
                dataToSend["steps"] = stepsFor30Days
                dataToView["steps"] = healthConnectManager.aggregateStepsForToday(zdt.toInstant())!!.toString()
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
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

    sealed class UiState {
        data object Uninitialized: UiState()
        data object Done: UiState()
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }
}