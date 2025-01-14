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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.provigos.android.R
import com.provigos.android.data.SharedPreferenceDataSource
import com.provigos.android.databinding.ActivityLoginBinding
import com.provigos.android.databinding.FragmentDashboardBinding
import com.provigos.android.presentation.view.activities.LoginActivity
import com.provigos.android.presentation.view.activities.MainActivity
import com.provigos.android.presentation.view.adapters.DashboardRecyclerViewAdapter
import com.provigos.android.presentation.viewmodel.HealthConnectViewModel
import kotlinx.coroutines.runBlocking
import org.koin.androidx.scope.fragmentScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class DashboardFragment: Fragment(R.layout.fragment_dashboard) {

    private val viewModel by viewModel<HealthConnectViewModel>()
    private lateinit var adapter: DashboardRecyclerViewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {


        val root: View = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val recyclerView: RecyclerView = root.findViewById(R.id.dashboard_recycler_view)
        recyclerView.postDelayed({
            recyclerView.layoutManager = LinearLayoutManager(context)
        }, 500)
        adapter = DashboardRecyclerViewAdapter(viewModel.healthConnectData1)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.init()
    }
}