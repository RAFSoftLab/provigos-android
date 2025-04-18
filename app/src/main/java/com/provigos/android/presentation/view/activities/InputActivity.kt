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
package com.provigos.android.presentation.view.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.provigos.android.R
import com.provigos.android.databinding.ActivityInputBinding
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

class InputActivity: AppCompatActivity(R.layout.activity_input) {

    private lateinit var binding: ActivityInputBinding
    private lateinit var datePicker: TextView
    private lateinit var timePicker: TextView
    private var date: LocalDate? = null
    private var time: LocalTime? = null
    private val viewModel by viewModel<DashboardViewModel>()

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInputBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val type = intent.extras?.getString("key") ?: "custom"

        val description = binding.inputDescription
        val measurement = binding.inputMeasurement
        datePicker = binding.inputDate
        timePicker = binding.inputTime
        val numberPicker = binding.inputNumberPicker

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

        if(localTimeNow.hour < 10 && localTimeNow.minute < 10) {
            timePicker.text = "0${localTimeNow.hour}:0${localTimeNow.minute}"
        }
        else if(localTimeNow.minute < 10) {
            timePicker.text = "${localTimeNow.hour}:0${localTimeNow.minute}"
        }
        else if(localTimeNow.hour < 10) {
            timePicker.text = "0${localTimeNow.hour}:${localTimeNow.minute}"
        }
        else {
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
                        if(!viewModel.writeHeartRate(
                            zonedDateTime,
                            zonedDateTime,
                            numberPicker.value.toLong()
                        )) {
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
                        if(!viewModel.writeBodyFat(zonedDateTime, numberPicker.value.toDouble())) {
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
                        if(!viewModel.writeOxygenSaturation(
                            zonedDateTime,
                            numberPicker.value.toDouble()
                        )) {
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
                        if(!viewModel.writeBloodGlucose(
                            zonedDateTime,
                            numberPicker.value.toDouble()
                        )) {
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
                        if(!viewModel.writeSteps(zonedDateTime, numberPicker.value.toLong())) {
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
                        if(!viewModel.writeHeight(zonedDateTime, numberPicker.value.toLong())) {
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
                        if(!viewModel.writeRespiratoryRate(
                            zonedDateTime,
                            numberPicker.value.toDouble()
                        )) {
                            toast()
                        }
                        finish()
                    }
                }
            }
            "custom" -> {
                datePicker.visibility = View.GONE
                timePicker.visibility = View.GONE
                binding.datetime.visibility = View.GONE
                description.text = intent.extras?.getString("label")
                measurement.text = intent.extras?.getString("units")
                numberPicker.minValue = 0
                numberPicker.maxValue = 100
                binding.inputSave.setOnClickListener {
                    GlobalScope.launch {
                        if(!viewModel.updateCustomData(
                                intent.extras?.getString("name")!!,
                                numberPicker.value.toString())) {
                        }
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private val datePickerDialogListener: DatePickerDialog.OnDateSetListener =
        DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            date = LocalDate.of(year, month + 1, dayOfMonth)
            datePicker.text = date!!.dayOfWeek.getDisplayName(
                TextStyle.SHORT,
                Locale.US
            ) + ", " + date!!.month.getDisplayName(
                TextStyle.SHORT, Locale.US
            ) + " " + dayOfMonth
        }

    @SuppressLint("SetTextI18n")
    private val timePickerDialogListener: TimePickerDialog.OnTimeSetListener =
        TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            time = LocalTime.of(hour, minute)
            if(hour < 10 && minute < 10) {
                timePicker.text = "0${hour}:0${minute}"
            }
            else if(minute < 10) {
                timePicker.text = "${hour}:0${minute}"
            }
            else if(hour < 10) {
                timePicker.text = "0${hour}:${minute}"
            }
            else {
                timePicker.text = "${hour}:${minute}"
            }
        }

    private fun toast() {
        Toast.makeText(this@InputActivity, "No permission", Toast.LENGTH_LONG).show()
    }
}