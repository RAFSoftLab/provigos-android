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
package com.provigos.android.presentation.view.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.provigos.android.R
import kotlin.math.roundToLong

class DashboardRecyclerViewAdapter(private val hashMap: MutableMap<String, String>):
    RecyclerView.Adapter<DashboardRecyclerViewAdapter.DashboardRecyclerViewHolder>() {

    var onItemClicked: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardRecyclerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.layout_item_recycler, parent, false)
        return DashboardRecyclerViewHolder(view)
    }

    override fun getItemCount() = hashMap.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DashboardRecyclerViewHolder, position: Int) {
        var k = 0
        var measurementType: String = ""
        var measurementValue: String = ""
        for(item in hashMap) {
            if(k == position) {
                measurementType = item.key
                measurementValue = item.value
                break
            }
            k++
        }
        when (measurementType) {
            "steps" -> {
                holder.measurementType.setText(R.string.steps_today)
                holder.measurementNumber.text = holder.context.getString(R.string.num_of_steps, measurementValue.toDouble().roundToLong().toString())
                holder.itemView.setOnClickListener { onItemClicked?.invoke("steps") }
            }
            "weight" -> {
                holder.measurementType.setText(R.string.recent_weight)
                holder.measurementNumber.text = holder.context.getString(R.string.num_of_weight, measurementValue.toDouble().roundToLong().toString(), "kg")
                holder.itemView.setOnClickListener { onItemClicked?.invoke("weight") }
            }
            "heart_rate" -> {
                holder.measurementType.setText(R.string.heart_rate)
                holder.measurementNumber.text = holder.context.getString(R.string.heart_bpm, measurementValue.toDouble().roundToLong().toString())
                holder.itemView.setOnClickListener { onItemClicked?.invoke("heart_rate") }
            }
            "body_fat" -> {
                holder.measurementType.setText(R.string.body_fat)
                holder.measurementNumber.text = holder.context.getString(R.string.body_fat_val, measurementValue, "%")
                holder.itemView.setOnClickListener { onItemClicked?.invoke("body_fat") }
            }
            "lean_body_mass" -> {
                holder.measurementType.setText(R.string.lean_body_mass)
                holder.measurementNumber.text = holder.context.getString(R.string.num_of_weight, measurementValue.toDouble().roundToLong().toString(), "kg")
            }
            "blood_pressure" -> {
                holder.measurementType.text = "Blood pressure"
                holder.measurementNumber.text = "$measurementValue mmHg"
                holder.itemView.setOnClickListener { onItemClicked?.invoke("blood_pressure") }
            }
            "height" -> {
                holder.measurementType.text = "Height"
                holder.measurementNumber.text = "$measurementValue cm"
                holder.itemView.setOnClickListener { onItemClicked?.invoke("height") }
            }
            "body_temperature" -> {
                holder.measurementType.text = "Body temperature"
                holder.measurementNumber.text = "$measurementValue ℃"
                holder.itemView.setOnClickListener { onItemClicked?.invoke("body_temperature") }
            }
            "oxygen_saturation" -> {
                holder.measurementType.text = "Oxygen saturation"
                holder.measurementNumber.text = "$measurementValue %"
                holder.itemView.setOnClickListener { onItemClicked?.invoke("oxygen_saturation") }
            }
            "blood_glucose" -> {
                holder.measurementType.text = "Blood glucose"
                holder.measurementNumber.text = "$measurementValue mmol/L"
                holder.itemView.setOnClickListener { onItemClicked?.invoke("blood_glucose") }
            }
            "respiratory_rate" -> {
                holder.measurementType.text = "Respiratory rate"
                holder.measurementNumber.text = "$measurementValue rpm"
                holder.itemView.setOnClickListener { onItemClicked?.invoke("respiratory_rate") }
            }
        }
    }

    inner class DashboardRecyclerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)  {
        val measurementNumber: TextView = itemView.findViewById(R.id.measurement_number)
        val measurementType: TextView = itemView.findViewById(R.id.measurement_type)
        val context: Context = itemView.context
    }

}
