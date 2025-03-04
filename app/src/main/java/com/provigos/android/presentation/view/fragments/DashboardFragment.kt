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
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DashboardFragment: Fragment(R.layout.fragment_dashboard) {


    private val viewModel by viewModel<DashboardViewModel>()
    private lateinit var adapter: DashboardRecyclerViewAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.init(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val root: View = inflater.inflate(R.layout.fragment_dashboard, container, false)

        recyclerView = root.findViewById(R.id.dashboard_recycler_view)
        recyclerView.postDelayed({
            recyclerView.layoutManager = LinearLayoutManager(context)
        }, 1200)
        adapter = DashboardRecyclerViewAdapter(viewModel.dataToView)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)

        setupItemClickListener()
        setupSwipeToRefresh(root)

        return root
    }

    private fun setupItemClickListener() {
        val activity = mapOf(
            "weight" to Input2Activity::class.java,
            "bloodPressure" to Input2Activity::class.java,
            "bodyTemperature" to Input2Activity::class.java,
            "steps" to InputActivity::class.java,
            "heartRate" to InputActivity::class.java,
            "bodyFat" to InputActivity::class.java,
            "height" to InputActivity::class.java,
            "bloodGlucose" to InputActivity::class.java,
            "oxygenSaturation" to InputActivity::class.java,
            "respiratoryRate" to InputActivity::class.java
        )

        adapter.onItemClicked = { type ->
            activity[type].let {
                target -> val intent = Intent(context, target).apply {
                    putExtra("key", type)
            }
                startActivity(intent)
            }
        }
    }

    private fun setupSwipeToRefresh(root: View) {
        val swipeRefresh = root.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isRefreshing = false
            refresh()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            adapter.notifyDataSetChanged()
            recyclerView.scrollBy(0, 0)
        }, 0)
    }

}