package com.provigos.android.presentation.view.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.marginStart
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.provigos.android.R
import com.provigos.android.databinding.ActivityDetailsBinding
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DetailsActivity: AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private val mDashboardViewModel by viewModel<DashboardViewModel>()
    private lateinit var datePicker: TextView
    private lateinit var timePicker: TextView
    private var date: LocalDate? = null
    private var time: LocalTime? = null

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailsBinding.inflate(layoutInflater)

        val bundle = intent.getBundleExtra("data")
        val type = intent.getStringExtra("key") ?: "custom"

        val map = bundle?.keySet()?.associateWith { key -> bundle.getString(key) }

        val dates = map!!.keys
            .sortedBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it) }
        val formattedDates = dates.map { dateStr ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            val formatter = SimpleDateFormat("MMM, dd", Locale.getDefault())
            formatter.format(date)
        }

        val entries = mutableListOf<BarEntry>()
        val entries2 = mutableListOf<BarEntry>()

        if (type != "bloodPressure") {
            for (i in dates.indices) {
                val value = map[dates[i]]?.toFloatOrNull() ?: 0f
                entries.add(BarEntry(i.toFloat(), value))
            }
        } else {
            for(i in dates.indices) {
                val parts = map[dates[i]]?.split("/")
                Timber.d("$parts")
                val data1 = parts?.get(0)?.toFloatOrNull() ?: 0f
                val data2 = parts?.get(1)?.toFloatOrNull() ?: 0f
                entries.add(BarEntry(i.toFloat(), data1))
                entries2.add(BarEntry(i.toFloat(), data2))
            }
        }

        val dataSet = BarDataSet(entries, null).apply {
            color = ContextCompat.getColor(this@DetailsActivity, R.color.teal1)
            valueTextColor = ContextCompat.getColor(this@DetailsActivity, R.color.teal1)
            valueTextSize = 10f
            if(type == "bloodPressure") {
                setDrawValues(true)
            } else {
                setDrawValues(false)
            }
        }

        val dataSet2 = BarDataSet(entries2, null).apply {
            color = ContextCompat.getColor(this@DetailsActivity, R.color.teal1)
            valueTextColor = ContextCompat.getColor(this@DetailsActivity, R.color.teal1)
            valueTextSize = 10f
            setDrawValues(true)
        }

        val barChart: BarChart = binding.barChart
        barChart.apply {

            if(entries2.isNotEmpty()) {
                data = BarData(dataSet, dataSet2).apply {
                    barWidth = 0.4f
                }
                barChart.groupBars(0f, 0.1f, 0.02f)
            } else {
                data = BarData(dataSet).apply {
                    barWidth = 0.4f
                }
            }
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            axisLeft.apply {
                textColor = ContextCompat.getColor(this@DetailsActivity, R.color.teal1)
                setDrawGridLines(false)
                axisMinimum = 0.1f
                granularity = 3f
            }
            xAxis.apply {
                textColor = ContextCompat.getColor(this@DetailsActivity, R.color.teal1)
                textSize = 10f
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                isGranularityEnabled = true
                setLabelCount(5, true)
                valueFormatter = object : ValueFormatter() {
                    @SuppressLint("ConstantLocale")
                    override fun getFormattedValue(value: Float): String {
                        val startIndex = lowestVisibleX.toInt().coerceAtLeast(0)
                        val endIndex = highestVisibleX.toInt().coerceAtMost(dates.lastIndex)
                        val currentIndex = value.toInt()
                        return if (currentIndex in startIndex..endIndex) {
                            formattedDates[currentIndex] ?: ""
                        } else {
                            ""
                        }
                    }
                }
            }
            axisRight.isEnabled = false
            setScaleEnabled(true)
            setVisibleXRangeMaximum(10f)
            setVisibleXRangeMinimum(3f)
            animateY(1000)
            isAutoScaleMinMaxEnabled = true
            fitScreen()
            setFitBars(true)
            invalidate()
        }

        val description = binding.inputDescription
        val measurement = binding.inputMeasurement
        val separator = binding.extraSep
        datePicker = binding.inputDate
        timePicker = binding.inputTime
        val numberPicker = binding.inputNumberPicker
        val numberPicker2 = binding.inputNumberPicker2

        numberPicker.wrapSelectorWheel = true

        val calendarDate = Calendar.getInstance()

        datePicker.setOnClickListener {
            val datePicker: DatePickerDialog = DatePickerDialog(
                this,
                datePickerDialogListener,
                calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH),
                calendarDate.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        val localTimeNow = LocalTime.now()

        if (localTimeNow.hour < 10 && localTimeNow.minute < 10) {
            timePicker.text = "0${localTimeNow.hour}:0${localTimeNow.minute}"
        } else if (localTimeNow.minute < 10) {
            timePicker.text = "${localTimeNow.hour}:0${localTimeNow.minute}"
        } else if (localTimeNow.hour < 10) {
            timePicker.text = "0${localTimeNow.hour}:${localTimeNow.minute}"
        } else {
            timePicker.text = "${localTimeNow.hour}:${localTimeNow.minute}"
        }

        timePicker.setOnClickListener {
            val timePicker: TimePickerDialog = TimePickerDialog(
                this, timePickerDialogListener,
                LocalTime.now().hour, LocalTime.now().minute, true
            )
            timePicker.show()
        }

        if (date == null) date = LocalDate.now()
        if (time == null) time = LocalTime.now()

        val zonedDateTime = ZonedDateTime.of(date, time, ZoneId.systemDefault())

        when (type) {
            "heartRate" -> {
                description.text = "Heart Rate"
                measurement.text = "BPM"
                numberPicker.minValue = 1
                numberPicker.maxValue = 200
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        if (!mDashboardViewModel.writeHeartRate(
                                zonedDateTime,
                                zonedDateTime,
                                numberPicker.value.toLong()
                            )
                        ) {
                            toast()
                        }
                        finish()
                    }
                }
            }

            "bodyFat" -> {
                description.text = "Body Fat"
                measurement.text = "%"
                numberPicker.minValue = 1
                numberPicker.maxValue = 100
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        if (!mDashboardViewModel.writeBodyFat(zonedDateTime, numberPicker.value.toDouble())) {
                            toast()
                        }
                        finish()
                    }
                }
            }

            "oxygenSaturation" -> {
                description.text = "Oxygen Saturation"
                measurement.text = "%"
                numberPicker.minValue = 1
                numberPicker.maxValue = 100
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        if (!mDashboardViewModel.writeOxygenSaturation(
                                zonedDateTime,
                                numberPicker.value.toDouble()
                            )
                        ) {
                            toast()
                        }
                        finish()
                    }
                }
            }

            "bloodGlucose" -> {
                description.text = "Blood Glucose"
                measurement.text = "mmol/L"
                numberPicker.minValue = 1
                numberPicker.maxValue = 1500
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        if (!mDashboardViewModel.writeBloodGlucose(
                                zonedDateTime,
                                numberPicker.value.toDouble()
                            )
                        ) {
                            toast()
                        }
                        finish()
                    }
                }
            }

            "steps" -> {
                description.text = "Steps"
                measurement.text = ""
                numberPicker.minValue = 1000
                numberPicker.maxValue = 15000
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        if (!mDashboardViewModel.writeSteps(zonedDateTime, numberPicker.value.toLong())) {
                            toast()
                        }
                        finish()
                    }
                }
            }

            "height" -> {
                description.text = "Height"
                measurement.text = "cm"
                numberPicker.minValue = 0
                numberPicker.maxValue = 250
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        if (!mDashboardViewModel.writeHeight(zonedDateTime, numberPicker.value.toLong())) {
                            toast()
                        }
                        finish()
                    }
                }
            }

            "respiratoryRate" -> {
                description.text = "Respiratory Rate"
                measurement.text = "rpm"
                numberPicker.minValue = 20
                numberPicker.maxValue = 100
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        if (!mDashboardViewModel.writeRespiratoryRate(
                                zonedDateTime,
                                numberPicker.value.toDouble()
                            )
                        ) {
                            toast()
                        }
                        finish()
                    }
                }
            }

            "weight" -> {
                description.text = "Weight"
                measurement.text = "kg"
                separator.text = "."
                separator.visibility = View.VISIBLE
                numberPicker2.visibility = View.VISIBLE
                setMargin(numberPicker)
                numberPicker.minValue = 1
                numberPicker.maxValue = 300
                numberPicker2.minValue = 0
                numberPicker2.maxValue = 9
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        if(!mDashboardViewModel.writeWeight(
                                zonedDateTime,
                                "${numberPicker.value}.${numberPicker2.value}".toDouble()
                            )) {
                            toast()
                        }
                        finish()
                    }
                }
            }
            "bloodPressure" -> {
                separator.visibility = View.VISIBLE
                numberPicker2.visibility = View.VISIBLE
                setMargin(numberPicker)
                description.text = "Blood pressure"
                measurement.text = "mmHg"
                separator.text = "/"
                numberPicker.minValue = 50
                numberPicker.maxValue = 200
                numberPicker2.minValue = 50
                numberPicker2.maxValue = 200
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        if(!mDashboardViewModel.writeBloodPressure(zonedDateTime, numberPicker.value.toLong(), numberPicker2.value.toLong())) {
                            toast()
                        }
                        finish()
                    }
                }
            }
            "bodyTemperature" -> {
                description.text = "Body Temperature"
                measurement.text = "℃"
                separator.text = "."
                separator.visibility = View.VISIBLE
                numberPicker2.visibility = View.VISIBLE
                setMargin(numberPicker)
                numberPicker.minValue = 33
                numberPicker.maxValue = 43
                numberPicker2.minValue = 0
                numberPicker2.maxValue = 9
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        if(!mDashboardViewModel.writeBodyTemperature(
                                zonedDateTime,
                                "${numberPicker.value}.${numberPicker2.value}".toDouble()
                            )) {
                            toast()
                        }
                        finish()
                    }
                }
            }
            "custom" ->  {
                datePicker.visibility = View.GONE
                timePicker.visibility = View.GONE
                binding.datetime.visibility = View.GONE
                description.text = intent.extras?.getString("label")
                measurement.text = intent.extras?.getString("units")
                numberPicker.minValue = 0
                numberPicker.maxValue = 100
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        mDashboardViewModel.updateCustomData(intent.extras?.getString("name")!!, numberPicker.value.toString())
                    }
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }

        setContentView(binding.root)
    }

    @SuppressLint("SetTextI18n")
    private val datePickerDialogListener: DatePickerDialog.OnDateSetListener =
        DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            date = LocalDate.of(year, month + 1, dayOfMonth)
            datePicker.text = date!!.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US) +
                    ", " +
                    date!!.month.getDisplayName(TextStyle.SHORT, Locale.US) +
                    " " + dayOfMonth
        }

    @SuppressLint("SetTextI18n")
    private val timePickerDialogListener: TimePickerDialog.OnTimeSetListener =
        TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            time = LocalTime.of(hour, minute)
            if (hour < 10 && minute < 10) {
                timePicker.text = "0${hour}:0${minute}"
            } else if (minute < 10) {
                timePicker.text = "${hour}:0${minute}"
            } else if (hour < 10) {
                timePicker.text = "0${hour}:${minute}"
            } else {
                timePicker.text = "${hour}:${minute}"
            }
        }

    private fun toast() {
        Toast.makeText(this@DetailsActivity, "No permission", Toast.LENGTH_LONG).show()
    }

    private fun setMargin(numberPicker: NumberPicker, context: Context = this, marginDp: Int = 65) {
        val marginPx = marginDp.dpToPx(context)
        val params = numberPicker.layoutParams as? ViewGroup.MarginLayoutParams
        if(params != null) {
            params.marginStart = marginPx
            numberPicker.layoutParams = params
        }
        numberPicker.requestLayout()
    }
    private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
}
