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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.provigos.android.R
import timber.log.Timber

class DashboardRecyclerViewAdapter(private val hashMap: Map<String, Double>): RecyclerView.Adapter<DashboardRecyclerViewAdapter.DashboardRecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardRecyclerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view =layoutInflater.inflate(R.layout.layout_item_recycler, parent, false)
        return DashboardRecyclerViewHolder(view)
    }

    override fun getItemCount() = hashMap.size

    override fun onBindViewHolder(holder: DashboardRecyclerViewHolder, position: Int) {
        var k = 0
        var measurementType: String = ""
        var measurementValue: Double = 0.0
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
                holder.measurementNumber.text = holder.context.getString(R.string.num_of_steps, measurementValue.toLong().toString())
            }
            "weight" -> {
                holder.measurementType.setText(R.string.recent_weight)
                holder.measurementNumber.text = holder.context.getString(R.string.num_of_weight, measurementValue.toLong().toString(), "kg")
            }
            "heart_rate" -> {
                holder.measurementType.setText(R.string.heart_rate)
                holder.measurementNumber.text = holder.context.getString(R.string.heart_bpm, measurementValue.toLong().toString())
            }
            "calories_burned" -> {
                holder.measurementType.setText(R.string.active_calories)
                holder.measurementNumber.text = holder.context.getString(R.string.calories_val, measurementValue.toString())
            }
            "body_fat" -> {
                holder.measurementType.setText(R.string.body_fat)
                holder.measurementNumber.text = holder.context.getString(R.string.body_fat_val, measurementValue.toString(), "%")
            }
        }
    }

    class DashboardRecyclerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)  {
        val measurementNumber: TextView = itemView.findViewById(R.id.measurement_number)
        val measurementType: TextView = itemView.findViewById(R.id.measurement_type)
        val context: Context = itemView.context
    }
}
