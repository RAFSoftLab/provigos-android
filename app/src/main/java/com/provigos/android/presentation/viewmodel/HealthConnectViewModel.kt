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

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Context.USAGE_STATS_SERVICE
import android.os.RemoteException
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provigos.android.data.HealthConnectManager
import com.provigos.android.data.remote.DatabaseConnection
import kotlinx.coroutines.launch
import okio.IOException
import timber.log.Timber
import java.lang.IllegalStateException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.UUID

class HealthConnectViewModel(private val healthConnectManager: HealthConnectManager): ViewModel() {

    val healthConnectData:  MutableMap<String, MutableMap<String, String>> = HashMap()
    var healthConnectData1: MutableMap<String, String> =  HashMap()

    private var zdt = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)

    private var pureZdt = pureDate(zdt)

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
    var weightList: MutableState<List<WeightRecord>> = mutableStateOf(listOf())
        private set
    var weightFor30Days: MutableMap<String, String> = HashMap()
        private set
    var lastLeanBodyMass: MutableState<Long> = mutableLongStateOf(0)
        private set
    var bodyFatList: MutableState<List<BodyFatRecord>> = mutableStateOf(listOf())
        private set
    var bodyFatFor30Days: MutableMap<String, String> = HashMap()
        private set
    var heartRateList: MutableState<List<HeartRateRecord>> = mutableStateOf(listOf())
        private set
    var heartRateFor30Days: MutableMap<String, String> = HashMap()
        private set
    var bloodPressureList: MutableState<List<BloodPressureRecord>> = mutableStateOf(listOf())
        private set
    var bloodPressureFor30Days: MutableMap<String, String> = HashMap()
        private set
    var bodyTemperatureList: MutableState<List<BodyTemperatureRecord>> = mutableStateOf(listOf())
        private set
    var bodyTemperatureFor30Days: MutableMap<String, String> = HashMap()
        private set
    var heightList: MutableState<List<HeightRecord>> = mutableStateOf(listOf())
        private set
    var heightFor30Days: MutableMap<String, String> = HashMap()
        private set
    var bloodGlucoseList: MutableState<List<BloodGlucoseRecord>> = mutableStateOf(listOf())
        private set
    var bloodGlucoseFor30Days: MutableMap<String, String> = HashMap()
        private set
    var oxygenSaturationList: MutableState<List<OxygenSaturationRecord>> = mutableStateOf(listOf())
        private set
    var oxygenSaturationFor30Days: MutableMap<String, String> = HashMap()
        private set
    var respiratoryRateList: MutableState<List<RespiratoryRateRecord>> = mutableStateOf(listOf())
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
                healthConnectData1 = HashMap()
                readHealthConnectData()
                DatabaseConnection().postData(context, healthConnectData)
                //healthConnectData.forEach { item -> Timber.e("${item.key}, ${item.value}")}
                //healthConnectData1.forEach { item -> Timber.e("${item.key}, ${item.value}")}
            //}
        }
    }

    private suspend fun readHealthConnectData() {
        readStepsFor30Days()
        readWeightFor30Days()
        readHeightFor30Days()
        readHeartRateFor30Days()
        readBodyFatFor30Days()
        readBloodPressureFor30Days()
        readBodyTemperatureFor30Days()
        readBloodGlucoseFor30Days()
        readOxygenSaturationFor30Days()
        readRespiratoryRateFor30Days()
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
                healthConnectData["steps"] = stepsFor30Days
                healthConnectData1["steps"] = healthConnectManager.aggregateStepsForToday(zdt.toInstant())!!.toString()
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readWeightFor30Days(): Boolean {
        try {
            weightList.value = healthConnectManager.readWeightForLast30Days(zdt.toInstant())
            if(weightList.value.isNotEmpty()) {
                weightList.value.forEach { weightRecord: WeightRecord ->
                    val zdt = ZonedDateTime.ofInstant(weightRecord.time, ZoneId.systemDefault())
                   weightFor30Days[pureDate(zdt)] = weightRecord.weight.inKilograms.toString()
                }
                healthConnectData["weight"] = weightFor30Days
                healthConnectData1["weight"] = weightList.value.last().weight.inKilograms.toString()
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readHeightFor30Days(): Boolean {
        try {
            heightList.value = healthConnectManager.readHeightForLast30Days(zdt.toInstant())
            if(heightList.value.isNotEmpty()) {
                heightList.value.forEach { heightRecord: HeightRecord ->
                    val zdt = ZonedDateTime.ofInstant(heightRecord.time, ZoneId.systemDefault())
                    heightFor30Days[pureDate(zdt)] = (heightRecord.height.inMeters * 100).toLong().toString()
                }
                healthConnectData["height"] = heightFor30Days
                healthConnectData1["height"] = (heightList.value.last().height.inMeters * 100).toLong().toString()
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readHeartRateFor30Days(): Boolean {
        try {
            heartRateList.value = healthConnectManager.readHeartRateForLast30Days(zdt.toInstant())
            if(heartRateList.value.isNotEmpty()) {
                heartRateList.value.forEach { heartRateRecord: HeartRateRecord ->
                    val zdt = ZonedDateTime.ofInstant(heartRateRecord.startTime, ZoneId.systemDefault())
                    heartRateFor30Days[pureDate(zdt)] = heartRateRecord.samples.last().beatsPerMinute.toString()
                }
                healthConnectData["heartRate"] = heartRateFor30Days
                healthConnectData1["heart_rate"] = heartRateList.value.last().samples.last().beatsPerMinute.toString()
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readBodyFatFor30Days(): Boolean {
        try {
            bodyFatList.value = healthConnectManager.readBodyFatForLast30Days(zdt.toInstant())
            if(bodyFatList.value.isNotEmpty()) {
                bodyFatList.value.forEach { bodyFatRecord: BodyFatRecord ->
                    val zdt = ZonedDateTime.ofInstant(bodyFatRecord.time, ZoneId.systemDefault())
                    bodyFatFor30Days[pureDate(zdt)] = bodyFatRecord.percentage.value.toString()
                }
                healthConnectData["bodyFat"] = bodyFatFor30Days
                healthConnectData1["body_fat"] = bodyFatList.value.last().percentage.value.toString()
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    /*
    private fun readLeanBodyMassForToday(): Boolean {
        try {
            if(lastBodyFat.value != 0.0 && lastWeight.value != 0.0) {
                val percentage = (100 - lastBodyFat.value.toLong()) / 100.00
                lastLeanBodyMass.value = (percentage * lastWeight.value).toLong()
                healthConnectData1["lean_body_mass"] = lastLeanBodyMass.value.toDouble().toString()
                healthConnectData["leanBodyMass"] = mapOf(pureZdt to lastLeanBodyMass.value.toString()) as MutableMap<String, String>
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }
     */

    private suspend fun readBloodPressureFor30Days(): Boolean {
        try {
           bloodPressureList.value = healthConnectManager.readBloodPressureForLast30Days(zdt.toInstant())
            if(bloodPressureList.value.isNotEmpty()) {
                bloodPressureList.value.forEach { bloodPressureRecord: BloodPressureRecord ->
                    val zdt = ZonedDateTime.ofInstant(bloodPressureRecord.time, ZoneId.systemDefault())
                    bloodPressureFor30Days[pureDate(zdt)] = "${bloodPressureRecord.systolic.inMillimetersOfMercury.toLong()}/${bloodPressureRecord.diastolic.inMillimetersOfMercury.toLong()}"
                }
                healthConnectData["bloodPressure"] = bloodPressureFor30Days
                healthConnectData1["blood_pressure"] = "${bloodPressureList.value.last().systolic.inMillimetersOfMercury.toLong()}/${bloodPressureList.value.last().diastolic.inMillimetersOfMercury.toLong()}"
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readBodyTemperatureFor30Days(): Boolean {
        try {
           bodyTemperatureList.value = healthConnectManager.readBodyTemperatureForLast30Days(zdt.toInstant())
            if(bodyTemperatureList.value.isNotEmpty()) {
                bodyTemperatureList.value.forEach { bodyTemperatureRecord: BodyTemperatureRecord ->
                    val zdt = ZonedDateTime.ofInstant(bodyTemperatureRecord.time, ZoneId.systemDefault())
                    bodyTemperatureFor30Days[pureDate(zdt)] = bodyTemperatureRecord.temperature.inCelsius.toString()
                }
                healthConnectData["bodyTemperature"] = bodyTemperatureFor30Days
                healthConnectData1["body_temperature"] = bodyTemperatureList.value.last().temperature.inCelsius.toString()
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readBloodGlucoseFor30Days(): Boolean {
        try {
            bloodGlucoseList.value = healthConnectManager.readBloodGlucoseForLast30Days(zdt.toInstant())
            if(bloodGlucoseList.value.isNotEmpty()) {
                bloodGlucoseList.value.forEach { bloodGlucoseRecord: BloodGlucoseRecord ->
                    val zdt = ZonedDateTime.ofInstant(bloodGlucoseRecord.time, ZoneId.systemDefault())
                    bloodGlucoseFor30Days[pureDate(zdt)] = bloodGlucoseRecord.level.inMillimolesPerLiter.toString()
                }
                healthConnectData["bloodGlucose"] = bloodGlucoseFor30Days
                healthConnectData1["blood_glucose"] = bloodGlucoseList.value.last().level.inMillimolesPerLiter.toString()
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readOxygenSaturationFor30Days(): Boolean {
        try {
            oxygenSaturationList.value = healthConnectManager.readOxygenSaturationForLast30Days(zdt.toInstant())
            if(oxygenSaturationList.value.isNotEmpty()) {
                oxygenSaturationList.value.forEach { oxygenSaturationRecord: OxygenSaturationRecord ->
                    val zdt = ZonedDateTime.ofInstant(oxygenSaturationRecord.time, ZoneId.systemDefault())
                    oxygenSaturationFor30Days[pureDate(zdt)] = oxygenSaturationRecord.percentage.value.toString()
                }
                healthConnectData["oxygenSaturation"] = oxygenSaturationFor30Days
                healthConnectData1["oxygen_saturation"] = oxygenSaturationList.value.last().percentage.value.toString()
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readRespiratoryRateFor30Days(): Boolean {
        try {
            respiratoryRateList.value = healthConnectManager.readRespiratoryRateForLast30Days(zdt.toInstant())
            if(respiratoryRateList.value.isNotEmpty()) {
                respiratoryRateList.value.forEach { respiratoryRateRecord: RespiratoryRateRecord ->
                    val zdt = ZonedDateTime.ofInstant(respiratoryRateRecord.time, ZoneId.systemDefault())
                    respiratoryRateFor30Days[pureDate(zdt)] = respiratoryRateRecord.rate.toString()
                }
                healthConnectData["respiratoryRate"] = respiratoryRateFor30Days
                healthConnectData1["respiratory_rate"] = respiratoryRateList.value.last().rate.toString()
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

    private fun pureDate(zdt: ZonedDateTime): String { return "" + zdt.year + "-" + zdt.month.value + "-" + zdt.dayOfMonth }

    sealed class UiState {
        data object Uninitialized: UiState()
        data object Done: UiState()
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }
}