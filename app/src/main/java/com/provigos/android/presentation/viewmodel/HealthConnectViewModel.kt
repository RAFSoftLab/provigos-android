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
    var aggregateStepsForToday: MutableState<String> = mutableStateOf("")
        private set
    var stepsToday: MutableMap<String, String> = HashMap()
        private set
    var weightList: MutableState<List<WeightRecord>> = mutableStateOf(listOf())
        private set
    var weightToday: MutableMap<String, String> = HashMap()
        private set
    var lastWeight: MutableState<Double> = mutableDoubleStateOf(0.0)
        private set
    var lastLeanBodyMass: MutableState<Long> = mutableLongStateOf(0)
        private set
    var bodyFatList: MutableState<List<BodyFatRecord>> = mutableStateOf(listOf())
        private set
    var lastBodyFat: MutableState<Double> = mutableDoubleStateOf(0.0)
        private set
    var bodyFatToday: MutableMap<String, String> = HashMap()
        private set
    var heartRateList: MutableState<List<HeartRateRecord>> = mutableStateOf(listOf())
        private set
    var heartRateToday: MutableMap<String, String> = HashMap()
        private set
    var lastHeartRate: MutableState<Long> = mutableLongStateOf(0)
        private set
    var activeCaloriesBurnedList: MutableState<List<ActiveCaloriesBurnedRecord>> = mutableStateOf(listOf())
        private set
    var lastActiveCaloriesBurned: MutableState<Double> = mutableDoubleStateOf(0.0)
        private set
    var sleepSessionList: MutableState<List<SleepSessionRecord>> = mutableStateOf(listOf())
        private set
    var lastSleep: MutableState<Double> = mutableDoubleStateOf(0.0)
        private set
    var bloodPressureList: MutableState<List<BloodPressureRecord>> = mutableStateOf(listOf())
        private set
    var lastBloodPressure: MutableState<String> = mutableStateOf("")
        private set
    var bloodPressureToday: MutableMap<String, String> = HashMap()
        private set
    var bodyTemperatureList: MutableState<List<BodyTemperatureRecord>> = mutableStateOf(listOf())
        private set
    var lastBodyTemperature: MutableState<String> = mutableStateOf("")
        private set
    var bodyTemperatureToday: MutableMap<String, String> = HashMap()
        private set
    var heightList: MutableState<List<HeightRecord>> = mutableStateOf(listOf())
        private set
    var lastHeight: MutableState<String> = mutableStateOf("")
        private set
    var heightToday: MutableMap<String, String> = HashMap()
        private set
    var bloodGlucoseList: MutableState<List<BloodGlucoseRecord>> = mutableStateOf(listOf())
        private set
    var lastBloodGlucose: MutableState<String> = mutableStateOf("")
        private set
    var bloodGlucoseToday: MutableMap<String, String> = HashMap()
        private set
    var oxygenSaturationList: MutableState<List<OxygenSaturationRecord>> = mutableStateOf(listOf())
        private set
    var lastOxygenSaturation: MutableState<String> = mutableStateOf("")
        private set
    var oxygenSaturationToday: MutableMap<String, String> = HashMap()
        private set
    var respiratoryRateList: MutableState<List<RespiratoryRateRecord>> = mutableStateOf(listOf())
        private set
    var lastRespiratoryRate: MutableState<String> = mutableStateOf("")
        private set
    var respiratoryRateToday: MutableMap<String, String> = HashMap()
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
                aggregateStepsForToday()
                readWeightForToday()
                readBodyFatForToday()
                readHeartRateForToday()
                readLeanBodyMassForToday()
                readBloodPressureForToday()
                readHeightForToday()
                readBodyTemperatureForToday()
                readBloodGlucoseForToday()
                readOxygenSaturationForToday()
                readRespiratoryRateForToday()
                healthConnectData1.forEach { item -> Timber.e("${item.key}, ${item.value}")}
                DatabaseConnection().postHealthConnectData(context, healthConnectData)
            //}
        }
    }

    private suspend fun readSteps() {
        val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val now = Instant.now()
        stepsList.value = healthConnectManager.readSteps(startOfDay.toInstant(), now)
    }

    private suspend fun aggregateStepsForToday(): Boolean {
        try {
            aggregateStepsForToday.value = healthConnectManager.aggregateStepsForToday(zdt.toInstant())!!.toString()
            if(!aggregateStepsForToday.value.equals(null)) {
                stepsToday[pureZdt] = aggregateStepsForToday.value
                healthConnectData1["steps"] = aggregateStepsForToday.value.toDouble().toString()
                healthConnectData["steps"] = stepsToday
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readWeightForToday(): Boolean {
        try {
            weightList.value = healthConnectManager.readWeightForToday(zdt.toInstant())
            if(weightList.value.isNotEmpty()) {
                lastWeight = mutableDoubleStateOf(weightList.value.last().weight.inKilograms)
                weightToday[pureZdt] = lastWeight.value.toLong().toString()
                healthConnectData1["weight"] = lastWeight.value.toString()
                healthConnectData["weight"] = weightToday
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readHeightForToday(): Boolean {
        try {
            heightList.value = healthConnectManager.readHeightForToday(zdt.toInstant())
            if(heightList.value.isNotEmpty()) {
                lastHeight.value = (heightList.value.last().height.inMeters * 100).toLong().toString()
                heightToday[pureZdt] = lastHeight.value
                healthConnectData1["height"] = lastHeight.value
                healthConnectData["height"] = heightToday
                return true
            }
            else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }
    private suspend fun readHeartRateForToday(): Boolean {
        try {
            heartRateList.value = healthConnectManager.readHeartRateFatForToday(zdt.toInstant())
            if(heartRateList.value.isNotEmpty()) {
                lastHeartRate.value = heartRateList.value.last().samples.last().beatsPerMinute
                heartRateToday[pureZdt] = lastHeartRate.value.toString()
                healthConnectData1["heart_rate"] = lastHeartRate.value.toDouble().toString()
                healthConnectData["heartRate"] = heartRateToday
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readBodyFatForToday(): Boolean {
        try {
            bodyFatList.value = healthConnectManager.readBodyFatForToday(zdt.toInstant())
            if(bodyFatList.value.isNotEmpty()) {
                lastBodyFat.value = bodyFatList.value.last().percentage.value
                bodyFatToday[pureZdt] = lastBodyFat.value.toLong().toString()
                healthConnectData1["body_fat"] = lastBodyFat.value.toString()
                healthConnectData["bodyFat"] = bodyFatToday
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

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

    private suspend fun readBloodPressureForToday(): Boolean {
        try {
            bloodPressureList.value = healthConnectManager.readBloodPressureForToday(zdt.toInstant())
            if(bloodPressureList.value.isNotEmpty()) {
                lastBloodPressure.value = "${bloodPressureList.value.last().systolic.inMillimetersOfMercury.toLong()}/${bloodPressureList.value.last().diastolic.inMillimetersOfMercury.toLong()}"
                bloodPressureToday[pureZdt] = lastBloodPressure.value
                healthConnectData1["blood_pressure"] = lastBloodPressure.value
                healthConnectData["bloodPressure"] = bloodPressureToday
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readBodyTemperatureForToday(): Boolean {
        try {
            bodyTemperatureList.value = healthConnectManager.readBodyTemperatureForToday(zdt.toInstant())
            if(bodyTemperatureList.value.isNotEmpty()) {
                lastBodyTemperature.value = bodyTemperatureList.value.last().temperature.inCelsius.toString()
                bodyTemperatureToday[pureZdt] = lastBodyTemperature.value
                healthConnectData1["body_temperature"] = lastBodyTemperature.value
                healthConnectData["bodyTemperature"] = bodyTemperatureToday
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readBloodGlucoseForToday(): Boolean {
        try {
            bloodGlucoseList.value = healthConnectManager.readBloodGlucoseForToday(zdt.toInstant())
            if(bloodGlucoseList.value.isNotEmpty()) {
                lastBloodGlucose.value = bloodGlucoseList.value.last().level.inMillimolesPerLiter.toString()
                bloodGlucoseToday[pureZdt] = lastBloodGlucose.value
                healthConnectData1["blood_glucose"] = lastBloodGlucose.value
                healthConnectData["bloodGlucose"] = bloodGlucoseToday
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readOxygenSaturationForToday(): Boolean {
        try {
            oxygenSaturationList.value = healthConnectManager.readOxygenSaturationForToday(zdt.toInstant())
            if(oxygenSaturationList.value.isNotEmpty()) {
                lastOxygenSaturation.value = oxygenSaturationList.value.last().percentage.value.toString()
                oxygenSaturationToday[pureZdt] = lastOxygenSaturation.value
                healthConnectData1["oxygen_saturation"] = lastOxygenSaturation.value
                healthConnectData["oxygenSaturation"] = oxygenSaturationToday
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private suspend fun readRespiratoryRateForToday(): Boolean {
        try {
            respiratoryRateList.value = healthConnectManager.readRespiratoryRateForToday(zdt.toInstant())
            if(respiratoryRateList.value.isNotEmpty()) {
                lastRespiratoryRate.value = respiratoryRateList.value.last().rate.toString()
                respiratoryRateToday[pureZdt] = lastRespiratoryRate.value
                healthConnectData1["respiratory_rate"] = lastRespiratoryRate.value
                healthConnectData["respiratoryRate"] = respiratoryRateToday
                return true
            } else {
                return false
            }
        } catch (_: NullPointerException) {}
        return false
    }

    private fun getScreenTime(context: Context) {
        val localDate = LocalDate.now()
        val c = Calendar.getInstance()
        val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        c.time = date
        val dateEnd = System.currentTimeMillis()
        val usageStatsManager: UsageStatsManager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(c.timeInMillis, dateEnd)
    }

    suspend fun writeWeight(date: ZonedDateTime, weight: Double) = healthConnectManager.writeWeight(date, weight)
    suspend fun writeBodyFat(date: ZonedDateTime, percentage: Double) = healthConnectManager.writeBodyFat(date, percentage)
    suspend fun writeSteps(start: ZonedDateTime, end: ZonedDateTime, count: Long) = healthConnectManager.writeSteps(start, end, count)
    suspend fun writeHeartRate(start: ZonedDateTime, end: ZonedDateTime, count: Long) = healthConnectManager.writeHeartRate(start, end, count)
    suspend fun writeBloodPressure(date: ZonedDateTime, upper: Long, lower: Long) = healthConnectManager.writeBloodPressure(date, upper, lower)
    suspend fun writeHeight(date: ZonedDateTime, height: Long) = healthConnectManager.writeHeight(date, height)
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