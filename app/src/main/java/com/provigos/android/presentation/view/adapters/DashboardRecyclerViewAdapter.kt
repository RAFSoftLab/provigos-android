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
package com.provigos.android.presentation.view.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.provigos.android.R
import java.util.Locale
import kotlin.math.roundToLong

class DashboardRecyclerViewAdapter(private var hashMap: Map<String, String>):
    RecyclerView.Adapter<DashboardRecyclerViewAdapter.DashboardRecyclerViewHolder>() {

    var onItemClicked: ((String) -> Unit)? = null
    var isClickable: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardRecyclerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.layout_item_card, parent, false)
        return DashboardRecyclerViewHolder(view)
    }

    override fun getItemCount() = hashMap.size

    override fun onBindViewHolder(holder: DashboardRecyclerViewHolder, position: Int) {

        val (measurementType, measurementValue) = hashMap.entries.elementAt(position)

        val context = holder.context

        val (label, value) = when (measurementType) {
            "steps" -> R.string.steps_today to measurementValue
            "weight" -> R.string.recent_weight to context.getString(R.string.num_of_weight, measurementValue.toDouble().roundToLong().toString(), "kg")
            "heartRate" -> R.string.heart_rate to context.getString(R.string.heart_bpm, measurementValue.toDouble().roundToLong().toString())
            "bodyFat" -> R.string.body_fat to context.getString(R.string.body_fat_val, measurementValue, "%")
            "leanBodyMass" -> R.string.lean_body_mass to context.getString(R.string.num_of_weight, measurementValue.toDouble().roundToLong().toString(), "kg")
            "bloodPressure" -> R.string.blood_pressure to "$measurementValue mmHg"
            "height" -> R.string.height to "$measurementValue cm"
            "bodyTemperature" -> R.string.body_temperature to String.format(Locale.US, "%.1f ℃", measurementValue.toDouble())
            "oxygenSaturation" -> R.string.oxygen_saturation to "$measurementValue %"
            "bloodGlucose" -> R.string.blood_glucose to "$measurementValue mmol/L"
            "respiratoryRate" -> R.string.respiratory_rate to "${measurementValue.toDouble().roundToLong()} rpm"
            "githubTotal" -> R.string.total_github_commits to measurementValue
            "githubDaily" -> R.string.daily_github_commits to measurementValue
            "spotifyGenre" -> R.string.spotify_genre to measurementValue
            "spotifyPopularity" -> R.string.spotify_popularity to measurementValue
            else -> null to null
        }

        label?.let { holder.measurementType.setText(it) }
        value?.let { holder.measurementNumber.text = it }

        holder.itemView.setOnClickListener {
            if(isClickable) {
                onItemClicked?.invoke(measurementType)
            }
        }
    }

    fun updateData(newData: Map<String, String>) {
        val diffCallback = DashboardDiffCallback(hashMap, newData)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        hashMap = newData
        diffResult.dispatchUpdatesTo(this)
    }

    inner class DashboardRecyclerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)  {
        val measurementNumber: TextView = itemView.findViewById(R.id.measurement_number)
        val measurementType: TextView = itemView.findViewById(R.id.measurement_type)
        val context: Context = itemView.context
    }

    inner class DashboardDiffCallback(
        private val oldData: Map<String, String>,
        private val newData: Map<String, String>
    ): DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldData.size

        override fun getNewListSize(): Int = newData.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldData.keys.elementAt(oldItemPosition) == newData.keys.elementAt(newItemPosition)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldData.values.elementAt(oldItemPosition) == newData.values.elementAt(newItemPosition)
        }

    }
}
