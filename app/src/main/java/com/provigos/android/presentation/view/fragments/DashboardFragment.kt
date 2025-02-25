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
package com.provigos.android.presentation.view.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.provigos.android.R
import com.provigos.android.presentation.view.activities.InputActivity
import com.provigos.android.presentation.view.activities.Input2Activity
import com.provigos.android.presentation.view.adapters.DashboardRecyclerViewAdapter
import com.provigos.android.presentation.viewmodel.HealthConnectViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DashboardFragment: Fragment(R.layout.fragment_dashboard) {


    private val viewModel by viewModel<HealthConnectViewModel>()
    private lateinit var adapter: DashboardRecyclerViewAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        val root: View = inflater.inflate(R.layout.fragment_dashboard, container, false)

        recyclerView = root.findViewById(R.id.dashboard_recycler_view)
        recyclerView.postDelayed({
            recyclerView.layoutManager = LinearLayoutManager(context)
        }, 800)
        adapter = DashboardRecyclerViewAdapter(viewModel.healthConnectData1)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)

        adapter.onItemClicked = { type ->
                val intent = Intent(context, InputActivity::class.java)
                val intent2 = Intent(context, Input2Activity::class.java)
            when (type) {
                "weight" -> {
                    intent2.putExtra("key", "weight")
                    startActivity(intent2)
                }
                "blood_pressure" -> {
                    intent2.putExtra("key", "blood_pressure")
                    startActivity(intent2)
                }
                "body_temperature" -> {
                    intent2.putExtra("key", "body_temperature")
                    startActivity(intent2)
                }
                "steps" -> {
                    intent.putExtra("key", "steps")
                    startActivity(intent)
                }
                "heart_rate" -> {
                    intent.putExtra("key", "heart_rate")
                    startActivity(intent)
                }
                "body_fat" -> {
                    intent.putExtra("key", "body_fat")
                    startActivity(intent)
                }
                "height" -> {
                    intent.putExtra("key", "height")
                    startActivity(intent)
                }
                "blood_glucose" -> {
                    intent.putExtra("key", "blood_glucose")
                    startActivity(intent)
                }
                "oxygen_saturation" -> {
                    intent.putExtra("key", "oxygen_saturation")
                    startActivity(intent)
                }
                "respiratory_rate" -> {
                    intent.putExtra("key", "respiratory_rate")
                    startActivity(intent)
                }
            }
        }

        val swipeRefresh = root.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isRefreshing = false
            refresh()
        }


        return root
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            recyclerView.swapAdapter(DashboardRecyclerViewAdapter(viewModel.healthConnectData1), false)
            recyclerView.scrollBy(0, 0)
        }, 0)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.init(context)
    }

    override fun onResume() {
        super.onResume()
        //refresh()
    }

}