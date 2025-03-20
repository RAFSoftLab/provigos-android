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
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.provigos.android.R
import com.provigos.android.databinding.ActivityInput2Binding
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
import org.koin.androidx.viewmodel.ext.android.viewModel

class Input2Activity: AppCompatActivity(R.layout.activity_input2) {

    private lateinit var binding: ActivityInput2Binding
    private lateinit var datePicker: TextView
    private lateinit var timePicker: TextView
    private var date: LocalDate? = null
    private var time: LocalTime? = null
    private val viewModel by viewModel<DashboardViewModel>()

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInput2Binding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val type = intent.extras?.getString("key")

        val description = binding.desc
        val measurement = binding.`val`
        val separator = binding.extraSep
        datePicker = binding.input2Date
        timePicker = binding.input2Time
        val numberPicker = binding.input2NumberPicker
        val numberPicker2 = binding.input2NumberPicker2

        numberPicker.wrapSelectorWheel = true
        numberPicker2.wrapSelectorWheel = true

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

        when(type) {
            "weight" -> {
                description.text = "Weight"
                measurement.text = "kg"
                separator.text = "."
                numberPicker.minValue = 1
                numberPicker.maxValue = 300
                numberPicker2.minValue = 0
                numberPicker2.maxValue = 9
                binding.saveText.setOnClickListener {
                    GlobalScope.launch {
                        if(!viewModel.writeWeight(
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
                description.text = "Blood pressure"
                measurement.text = "mmHg"
                separator.text = "/"
                numberPicker.minValue = 50
                numberPicker.maxValue = 200
                numberPicker2.minValue = 50
                numberPicker2.maxValue = 200
                binding.saveText.setOnClickListener {
                    GlobalScope.launch {
                        if(!viewModel.writeBloodPressure(zonedDateTime, numberPicker.value.toLong(), numberPicker2.value.toLong())) {
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
                numberPicker.minValue = 33
                numberPicker.maxValue = 43
                numberPicker2.minValue = 0
                numberPicker2.maxValue = 9
                binding.saveText.setOnClickListener {
                    GlobalScope.launch {
                        if(!viewModel.writeBodyTemperature(
                            zonedDateTime,
                            "${numberPicker.value}.${numberPicker2.value}".toDouble()
                        )) {
                            toast()
                        }
                        finish()
                    }
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
        Toast.makeText(this@Input2Activity, "no permission", Toast.LENGTH_LONG).show()
    }

}