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
package com.provigos.android.data

import android.os.Build
import android.content.Context
import android.annotation.SuppressLint
import android.health.connect.datatypes.BloodGlucoseRecord.RelationToMealType
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import timber.log.Timber
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    var availability = mutableStateOf(HealthConnectAvailability.NOT_SUPPORTED)
        private set

    init {
        checkAvailability()
    }

    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient
            .permissionController
            .getGrantedPermissions()
            .containsAll(permissions)
    }

    suspend fun revokePermissions() {
        healthConnectClient
            .permissionController
            .revokeAllPermissions()
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    @SuppressLint("ObsoleteSdkInt")
    fun checkAvailability() {
        availability.value = when {
            HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
            Build.VERSION.SDK_INT  >= MIN_SUPPORTED_SDK -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    // STEPS
    suspend fun readSteps(start: Instant, end: Instant): List<StepsRecord> {
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter =  TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { record -> Timber.e(record.toString()) }
        return response.records
    }

    suspend fun aggregateStepsForToday(date: Instant): Long? {
        val request = AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
            )
        val response = healthConnectClient.aggregate(request)
        response.longValues.forEach { (s, l) -> Timber.e("%s, %d", s, l) }
        return response.longValues["Steps_count_total"]
    }

    suspend fun aggregateStepsForLast30Days(date: Instant): Long? {
        val request = AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(date, date.minus(30, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.aggregate(request)
        response.longValues.forEach { (s, l) -> Timber.e("%s, %d", s, l) }
        return response.longValues["Steps_count_total"]
    }

    // WEIGHT
    suspend fun readWeightForToday(date: Instant): List<WeightRecord> {
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    suspend fun readWeightForLast30Days(date: Instant): List<WeightRecord> {
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.minus(30, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    // BODY FAT
    suspend fun readBodyFatForToday(date: Instant): List<BodyFatRecord> {
        val request = ReadRecordsRequest(
            recordType = BodyFatRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    suspend fun readBodyFatForLast30Days(date: Instant): List<BodyFatRecord> {
        val request = ReadRecordsRequest(
            recordType = BodyFatRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.minus(30, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    // HEART RATE
    suspend fun readHeartRateFatForToday(date: Instant): List<HeartRateRecord> {
        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    suspend fun readHeartRateForLast30Days(date: Instant): List<HeartRateRecord> {
        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.minus(30, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    // LEAN BODY MASS
    suspend fun readLeanBodyMassFatForToday(date: Instant): List<LeanBodyMassRecord> {
        val request = ReadRecordsRequest(
            recordType = LeanBodyMassRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readLeanBodyMassForLast30Days(date: Instant): List<LeanBodyMassRecord> {
        val request = ReadRecordsRequest(
            recordType = LeanBodyMassRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.minus(30, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    // ACTIVE CALORIES BURNED
    suspend fun readActiveCaloriesBurnedForToday(date: Instant): List<ActiveCaloriesBurnedRecord> {
        val request = ReadRecordsRequest(
            recordType = ActiveCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    suspend fun readActiveCaloriesBurnedForLast30Days(date: Instant): List<ActiveCaloriesBurnedRecord> {
        val request = ReadRecordsRequest(
            recordType = ActiveCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.minus(30, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    // SLEEP SESSION
    suspend fun readSleepSessionForToday(date: Instant): List<SleepSessionRecord> {
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    suspend fun readSleepSessionForLast30Days(date: Instant): List<SleepSessionRecord> {
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.minus(30, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readBloodPressureForToday(date: Instant): List<BloodPressureRecord> {
        val request = ReadRecordsRequest(
            recordType = BloodPressureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    suspend fun readHeightForToday(date: Instant): List<HeightRecord> {
        val request = ReadRecordsRequest(
            recordType = HeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    suspend fun writeHeight(date: ZonedDateTime, height: Long) {
        healthConnectClient.insertRecords(listOf(
            HeightRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                height = Length.meters(height.toDouble()/100.00),
            )
        ))
    }

    suspend fun readBloodGlucoseForToday(date: Instant): List<BloodGlucoseRecord> {
        val request = ReadRecordsRequest(
            recordType = BloodGlucoseRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        Timber.e("AAAAAA")
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    suspend fun writeBloodGlucose(date: ZonedDateTime, level: Double) {
        healthConnectClient.insertRecords(listOf(
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

    suspend fun readOxygenSaturationForToday(date: Instant): List<OxygenSaturationRecord> {
        val request = ReadRecordsRequest(
            recordType = OxygenSaturationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    suspend fun writeOxygenSaturation(date: ZonedDateTime, percentage: Double) {
        healthConnectClient.insertRecords(listOf(
            OxygenSaturationRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                percentage = Percentage(percentage),
            )
        ))
    }

    suspend fun readBodyTemperatureForToday(date: Instant): List<BodyTemperatureRecord> {
        val request = ReadRecordsRequest(
            recordType = BodyTemperatureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    suspend fun writeBodyTemperature(date: ZonedDateTime, temperature: Double) {
        healthConnectClient.insertRecords(listOf(
            BodyTemperatureRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                temperature = Temperature.celsius(temperature),
                measurementLocation = BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_UNKNOWN,
            )
        ))
    }

    suspend fun writeBloodPressure(date: ZonedDateTime, upper: Long, lower: Long) {
        healthConnectClient.insertRecords(listOf(
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

    suspend fun readRespiratoryRateForToday(date: Instant): List<RespiratoryRateRecord> {
        val request = ReadRecordsRequest(
            recordType = RespiratoryRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(date, date.plus(1, ChronoUnit.DAYS))
        )
        val response = healthConnectClient.readRecords(request)
        response.records.forEach { w -> Timber.e(w.toString()) }
        return response.records
    }

    suspend fun writeRespiratoryRate(date: ZonedDateTime, rate: Double) {
        healthConnectClient.insertRecords(listOf(
            RespiratoryRateRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                rate = rate,
            )
        ))
    }

    suspend fun writeSteps(start: ZonedDateTime, end: ZonedDateTime, count: Long) {
        healthConnectClient.insertRecords(listOf(
            StepsRecord(
                startTime = start.toInstant(),
                startZoneOffset = start.offset,
                endTime = end.toInstant(),
                endZoneOffset = end.offset,
                count = count
            )
        ))
    }

    suspend fun writeHeartRate(start: ZonedDateTime, end: ZonedDateTime, count: Long) {
        healthConnectClient.insertRecords(listOf(
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

    suspend fun writeWeight(date: ZonedDateTime, weight: Double) {
        healthConnectClient.insertRecords(listOf(
            WeightRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                weight = Mass.kilograms(weight)
            )
        ))
    }

    suspend fun writeBodyFat(date: ZonedDateTime, percentage: Double) {
        healthConnectClient.insertRecords(listOf(
            BodyFatRecord(
                time = date.toInstant(),
                zoneOffset = date.offset,
                percentage = Percentage(percentage)
            )
        ))
    }

    enum class HealthConnectAvailability {
        INSTALLED,
        NOT_INSTALLED,
        NOT_SUPPORTED
    }
}