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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.provigos.android.R
import com.provigos.android.data.model.DashboardViewItemModel

class DashboardRecyclerViewAdapter(private var data: List<DashboardViewItemModel>):
    RecyclerView.Adapter<DashboardRecyclerViewAdapter.DashboardRecyclerViewHolder>() {

    var onItemClicked: ((String) -> Unit)? = null
    var isClickable: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardRecyclerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.layout_item_card, parent, false)
        return DashboardRecyclerViewHolder(view)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: DashboardRecyclerViewHolder, position: Int) {
        val item = data[position]

        holder.measurementType.text = item.label
        holder.measurementNumber.text = item.value
        holder.itemView.setOnClickListener {
            if(isClickable) {
                onItemClicked?.invoke(item.type)
            }
        }
    }

    fun updateData(newData: List<DashboardViewItemModel>) {
        val diffCallback = DashboardDiffCallback(data, newData)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        data = newData
        diffResult.dispatchUpdatesTo(this)
    }

    inner class DashboardRecyclerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)  {
        val measurementNumber: TextView = itemView.findViewById(R.id.measurement_number)
        val measurementType: TextView = itemView.findViewById(R.id.measurement_type)
    }

    inner class DashboardDiffCallback(
        private val oldData: List<DashboardViewItemModel>,
        private val newData: List<DashboardViewItemModel>
    ): DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldData.size

        override fun getNewListSize(): Int = newData.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldData[oldItemPosition].type == newData[newItemPosition].type
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldData[oldItemPosition] == newData[newItemPosition]
        }

    }
}
