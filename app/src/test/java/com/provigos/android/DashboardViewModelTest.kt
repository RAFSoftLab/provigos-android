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
package com.provigos.android

import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import com.provigos.android.data.api.AndroidUsageStatsManager
import com.provigos.android.data.api.HealthConnectManager
import com.provigos.android.data.api.HttpManager
import com.provigos.android.data.local.SharedPreferenceDataSource
import com.provigos.android.data.model.custom.CustomItemModel
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class DashboardViewModelTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    class MainDispatcherRule(
        private val testDispatcher: TestDispatcher = StandardTestDispatcher()): TestWatcher() {
        override fun starting(description: Description?) {
            Dispatchers.setMain(testDispatcher)
        }

        override fun finished(description: Description?) {
            Dispatchers.resetMain()
        }
        }

    @Mock
    private lateinit var mockHealthConnectManager: HealthConnectManager

    @Mock
    private lateinit var mockHttpManager: HttpManager

    @Mock
    private lateinit var mockAndroidUsageStatsManager: AndroidUsageStatsManager

    @Mock
    private lateinit var mockSharedPrefs: SharedPreferenceDataSource

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
//        Mockito.`when`(mockSharedPrefs.isHealthUser()).thenReturn(true)
//        Mockito.`when`(mockSharedPrefs.isGithubUser()).thenReturn(true)
//        Mockito.`when`(mockSharedPrefs.isSpotifyUser()).thenReturn(true)
//        Mockito.`when`(mockSharedPrefs.isCustomUser()).thenReturn(true)
//        Mockito.`when`(mockSharedPrefs.isAndroidUser()).thenReturn(true)
//        Mockito.`when`(mockSharedPrefs.isAllowGithubDailyCommits()).thenReturn(true)
//        Mockito.`when`(mockSharedPrefs.isAllowGithubTotalCommits()).thenReturn(true)
//        Mockito.`when`(mockSharedPrefs.isAllowSpotifyArtistGenres()).thenReturn(true)
//        Mockito.`when`(mockSharedPrefs.isAllowSpotifyArtistPopularity()).thenReturn(true)
//        Mockito.`when`(mockSharedPrefs.isAllowAndroidScreenTime()).thenReturn(true)
//        Mockito.`when`(mockSharedPrefs.isAllowAndroidBiometrics()).thenReturn(true)

        viewModel = DashboardViewModel(mockHealthConnectManager,
            mockHttpManager,
            mockAndroidUsageStatsManager,
            mockSharedPrefs
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun mockSuccessfulHealthData() {
        Mockito.`when`(mockHealthConnectManager.hasHealthConnectPermission(any())).thenReturn(true)
        Mockito.`when`(mockHealthConnectManager.readStepsForLast30Days(any())).thenReturn(mapOf("2001-01-01" to "1000"))

        val weightRecordTest = listOf(mockWeightRecord())
        Mockito.`when`(mockHealthConnectManager.readWeightForLast30Days(any())).thenReturn(weightRecordTest)

        val heartRateRecordTest = listOf(mockHeartRateRecord())
        Mockito.`when`(mockHealthConnectManager.readHeartRateForLast30Days(any())).thenReturn(heartRateRecordTest)

        val bodyFatRecordTest = listOf(mockBodyFatRecord())
        Mockito.`when`(mockHealthConnectManager.readBodyFatForLast30Days(any())).thenReturn(bodyFatRecordTest)

        val bloodPressureRecordTest = listOf(mockBloodPressureRecord())
        Mockito.`when`(mockHealthConnectManager.readBloodPressureForLast30Days(any())).thenReturn(bloodPressureRecordTest)

        val heightRecordTest = listOf(mockHeightRecord())
        Mockito.`when`(mockHealthConnectManager.readHeightForLast30Days(any())).thenReturn(heightRecordTest)

        val bodyTemperatureRecordTest = listOf(mockBodyTemperatureRecord())
        Mockito.`when`(mockHealthConnectManager.readBodyTemperatureForLast30Days(any())).thenReturn(bodyTemperatureRecordTest)

        val oxygenSaturationRecordTest = listOf(mockOxygenSaturationRecord())
        Mockito.`when`(mockHealthConnectManager.readOxygenSaturationForLast30Days(any())).thenReturn(oxygenSaturationRecordTest)

        val bloodGlucoseRecordTest = listOf(mockBloodGlucoseRecord())
        Mockito.`when`(mockHealthConnectManager.readBloodGlucoseForLast30Days(any())).thenReturn(bloodGlucoseRecordTest)

        val respiratoryRateRecordTest = listOf(mockRespiratoryRateRecord())
        Mockito.`when`(mockHealthConnectManager.readRespiratoryRateForLast30Days(any())).thenReturn(respiratoryRateRecordTest)
    }

    private suspend fun mockSuccessfulGithubData() {
        Mockito.`when`(mockHttpManager.getGithubData()).thenReturn(
            hashMapOf("2001-01-01" to "1", "2002-02-02" to "2")
        )
    }

    private suspend fun mockSuccessfulSpotifyData() {
        Mockito.`when`(mockHttpManager.getSpotifyData()).thenReturn(
            hashMapOf("spotifyGenre" to "Rock", "spotifyPopularity" to "75.5")
        )
    }

    private suspend fun mockSuccessfulCustomData() {
        Mockito.`when`(mockHttpManager.getProvigosCustomKeys()).thenReturn(
            listOf(CustomItemModel("customMetric", "Custom Metric", "units"))
        )
        Mockito.`when`(mockHttpManager.getProvigosCustomData()).thenReturn(
            mapOf("customMetric" to mapOf("2001-01-01" to "30"))
        )
        Mockito.`when`(mockSharedPrefs.isAllowCustomItem("customMetric")).thenReturn(true)
    }

    private fun mockSuccessfulAndroidData() {
        Mockito.`when`(mockAndroidUsageStatsManager.getScreenTime()).thenReturn(100000L)
        Mockito.`when`(mockSharedPrefs.unlockAttemptsCount()).thenReturn(5)
    }

    private fun mockWeightRecord(weight: Double = 75.0): WeightRecord {
        return WeightRecord(
            time = Instant.now(),
            weight = Mass.kilograms(weight),
            zoneOffset = ZoneOffset.UTC
        )
    }

    private fun mockHeartRateRecord(count: Long = 80): HeartRateRecord {
        return HeartRateRecord(
            startTime = Instant.now(),
            startZoneOffset = ZoneOffset.UTC,
            endTime = Instant.now(),
            endZoneOffset = ZoneOffset.UTC,
            samples = listOf(HeartRateRecord.Sample(
                time = ZonedDateTime.now().toInstant(),
                beatsPerMinute = count
            ))
        )
    }

    private fun mockBodyFatRecord(percentage: Double = 15.0): BodyFatRecord {
        return BodyFatRecord(
            time = Instant.now(),
            zoneOffset = ZoneOffset.UTC,
            percentage = Percentage(percentage)
        )
    }

    private fun mockBloodPressureRecord(sys: Double = 120.0, dia: Double = 80.0): BloodPressureRecord {
        return BloodPressureRecord(
            time = Instant.now(),
            zoneOffset = ZoneOffset.UTC,
            systolic = Pressure.millimetersOfMercury(sys),
            diastolic = Pressure.millimetersOfMercury(dia))
    }

    private fun mockHeightRecord(height: Double = 1.80): HeightRecord {
        return HeightRecord(
            time = Instant.now(),
            zoneOffset = ZoneOffset.UTC,
            height = Length.meters(height)
        )
    }

    private fun mockBodyTemperatureRecord(temperature: Double = 37.5): BodyTemperatureRecord {
        return BodyTemperatureRecord(
            time = Instant.now(),
            zoneOffset = ZoneOffset.UTC,
            temperature = Temperature.celsius(temperature)
        )
    }

    private fun mockOxygenSaturationRecord(percentage: Double = 99.0): OxygenSaturationRecord {
        return OxygenSaturationRecord(
            time = Instant.now(),
            zoneOffset = ZoneOffset.UTC,
            percentage = Percentage(percentage)
        )
    }

    private fun mockBloodGlucoseRecord(bloodGlucose: Double = 4.5): BloodGlucoseRecord {
        return BloodGlucoseRecord(
            time = Instant.now(),
            zoneOffset = ZoneOffset.UTC,
            level = BloodGlucose.millimolesPerLiter(bloodGlucose)
        )
    }

    private fun mockRespiratoryRateRecord(rate: Double = 12.0): RespiratoryRateRecord {
        return RespiratoryRateRecord(
            time = Instant.now(),
            zoneOffset = ZoneOffset.UTC,
            rate = rate
        )
    }

    @Test
    fun `initial state should be Uninitialized`() = testScope.runTest {
        val state = viewModel.uiState.value
        assertTrue(state is DashboardViewModel.UiState.Uninitialized)
    }

//    @Test
//    fun `fetchAllData state transitioning`() = testScope.runTest {
//
//        mockSuccessfulHealthData()
//        mockSuccessfulGithubData()
//        mockSuccessfulSpotifyData()
//        mockSuccessfulCustomData()
//        mockSuccessfulAndroidData()
//
//        val states = mutableListOf<DashboardViewModel.UiState>()
//        val job = launch {
//            viewModel.uiState.toList(states)
//        }
//
//        viewModel.fetchAllData()
//        advanceUntilIdle()
//
//        job.cancel()
//
//        assertEquals(3, states.size)
//        assertTrue(states[0] is DashboardViewModel.UiState.Uninitialized)
//        assertTrue(states[1] is DashboardViewModel.UiState.Loading)
//        assertTrue(states[2] is DashboardViewModel.UiState.Done)
//    }
//
//    @Test
//    fun `fetchAllData should combine data sources`() = testScope.runTest {
//        mockSuccessfulHealthData()
//        mockSuccessfulGithubData()
//        mockSuccessfulSpotifyData()
//        mockSuccessfulCustomData()
//        mockSuccessfulAndroidData()
//
//        viewModel.fetchAllData()
//        advanceUntilIdle()
//
//        val dashboardItems = viewModel.mDashboardViewList.value
//        assertEquals(14, dashboardItems.size)
//    }
}