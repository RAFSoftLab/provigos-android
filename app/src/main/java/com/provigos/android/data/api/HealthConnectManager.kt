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

import android.os.Build
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

class HealthConnectManager(private val context: Context) {

    private val mHealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    var availability = mutableStateOf(HealthConnectAvailability.NOT_SUPPORTED)
        private set

    suspend fun hasHealthConnectPermission(permission: String): Boolean {
        return mHealthConnectClient
            .permissionController
            .getGrantedPermissions()
            .contains(permission)
    }

    fun checkAvailability() {
        availability.value = when {
            HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
            Build.VERSION.SDK_INT  >= MIN_SUPPORTED_SDK -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    // STEPS
    suspend fun readStepsForLast30Days(date: Instant): Map<String, String> {
        return try {
            val request = AggregateGroupByDurationRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(date.minus(30, ChronoUnit.DAYS), date.plus(1, ChronoUnit.DAYS)),
                timeRangeSlicer = Duration.ofDays(1)
            )
            val response = mHealthConnectClient.aggregateGroupByDuration(request)
            response.associate { result ->
                val startTime = result.startTime.atZone(ZoneId.systemDefault()).toLocalDate().toString()
                val steps = result.result[StepsRecord.COUNT_TOTAL].toString()
                (startTime to steps)
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun writeSteps(start: ZonedDateTime, count: Long) {
        mHealthConnectClient.insertRecords(listOf(
            StepsRecord(
                startTime = start.toInstant(),
                startZoneOffset = start.offset,
                endTime = start.toInstant().plus(5, ChronoUnit.MINUTES),
                endZoneOffset = start.offset,
                count = count
            )
        ))
    }

    // WEIGHT
    suspend fun readWeightForLast30Days(date: Instant): List<WeightRecord> {
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date.minus(30, ChronoUnit.DAYS), date)
        )
        val response = mHealthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun writeWeight(date: ZonedDateTime, weight: Double) {
        mHealthConnectClient.insertRecords(listOf(
            WeightRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                weight = Mass.kilograms(weight)
            )
        ))
    }

    // BODY FAT
    suspend fun readBodyFatForLast30Days(date: Instant): List<BodyFatRecord> {
        val request = ReadRecordsRequest(
            recordType = BodyFatRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date.minus(30, ChronoUnit.DAYS), date)
        )
        val response = mHealthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun writeBodyFat(date: ZonedDateTime, percentage: Double) {
        mHealthConnectClient.insertRecords(listOf(
            BodyFatRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                percentage = Percentage(percentage)
            )
        ))
    }

    // HEART RATE
    suspend fun readHeartRateForLast30Days(date: Instant): List<HeartRateRecord> {
        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date.minus(30, ChronoUnit.DAYS), date)
        )
        val response = mHealthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun writeHeartRate(start: ZonedDateTime, end: ZonedDateTime, count: Long) {
        mHealthConnectClient.insertRecords(listOf(
            HeartRateRecord(
                startTime = start.toInstant(),
                startZoneOffset = start.offset,
                endTime = end.toInstant(),
                endZoneOffset = end.offset,
                samples = listOf(HeartRateRecord.Sample(
                    time = ZonedDateTime.now().toInstant(),
                    beatsPerMinute = count
                ))
        )))
    }

    // BLOOD PRESSURE
    suspend fun readBloodPressureForLast30Days(date: Instant): List<BloodPressureRecord> {
        val request = ReadRecordsRequest(
            recordType = BloodPressureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date.minus(30, ChronoUnit.DAYS), date)
        )
        val response = mHealthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun writeBloodPressure(date: ZonedDateTime, upper: Long, lower: Long) {
        mHealthConnectClient.insertRecords(listOf(
            BloodPressureRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                systolic = Pressure.millimetersOfMercury(upper.toDouble()),
                diastolic = Pressure.millimetersOfMercury(lower.toDouble()),
                bodyPosition = BloodPressureRecord.BODY_POSITION_UNKNOWN,
                measurementLocation = BloodPressureRecord.MEASUREMENT_LOCATION_UNKNOWN,
            )
        ))
    }

    // HEIGHT
    suspend fun readHeightForLast30Days(date: Instant): List<HeightRecord> {
        val request = ReadRecordsRequest(
            recordType = HeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date.minus(30, ChronoUnit.DAYS), date)
        )
        val response = mHealthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun writeHeight(date: ZonedDateTime, height: Long) {
        mHealthConnectClient.insertRecords(listOf(
            HeightRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                height = Length.meters(height.toDouble()/100.00),
            )
        ))
    }

    // BLOOD GLUCOSE
    suspend fun readBloodGlucoseForLast30Days(date: Instant): List<BloodGlucoseRecord> {
        val request = ReadRecordsRequest(
            recordType = BloodGlucoseRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date.minus(30, ChronoUnit.DAYS), date)
        )
        val response = mHealthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun writeBloodGlucose(date: ZonedDateTime, level: Double) {
        mHealthConnectClient.insertRecords(listOf(
            BloodGlucoseRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                level = BloodGlucose.millimolesPerLiter(level),
                specimenSource = BloodGlucoseRecord.SPECIMEN_SOURCE_SERUM,
                mealType = MealType.MEAL_TYPE_UNKNOWN,
                relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL,
            )
        ))
    }

    // OXYGEN SATURATION
    suspend fun readOxygenSaturationForLast30Days(date: Instant): List<OxygenSaturationRecord> {
        val request = ReadRecordsRequest(
            recordType = OxygenSaturationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date.minus(30, ChronoUnit.DAYS), date)
        )
        val response = mHealthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun writeOxygenSaturation(date: ZonedDateTime, percentage: Double) {
        mHealthConnectClient.insertRecords(listOf(
            OxygenSaturationRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                percentage = Percentage(percentage),
            )
        ))
    }

    // BODY TEMPERATURE
    suspend fun readBodyTemperatureForLast30Days(date: Instant): List<BodyTemperatureRecord> {
        val request = ReadRecordsRequest(
            recordType = BodyTemperatureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date.minus(30, ChronoUnit.DAYS), date)
        )
        val response = mHealthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun writeBodyTemperature(date: ZonedDateTime, temperature: Double) {
        mHealthConnectClient.insertRecords(listOf(
            BodyTemperatureRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                temperature = Temperature.celsius(temperature),
                measurementLocation = BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_UNKNOWN,
            )
        ))
    }

    // RESPIRATORY RATE
    suspend fun readRespiratoryRateForLast30Days(date: Instant): List<RespiratoryRateRecord> {
        val request = ReadRecordsRequest(
            recordType = RespiratoryRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date.minus(30, ChronoUnit.DAYS), date)
        )
        val response = mHealthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun writeRespiratoryRate(date: ZonedDateTime, rate: Double) {
        mHealthConnectClient.insertRecords(listOf(
            RespiratoryRateRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                rate = rate,
            )
        ))
    }

    enum class HealthConnectAvailability {
        INSTALLED,
        NOT_INSTALLED,
        NOT_SUPPORTED
    }
}