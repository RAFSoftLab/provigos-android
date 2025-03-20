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
package com.provigos.android.presentation.view.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.provigos.android.R
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.databinding.FragmentDashboardBinding
import com.provigos.android.presentation.view.activities.InputActivity
import com.provigos.android.presentation.view.activities.Input2Activity
import com.provigos.android.presentation.view.adapters.DashboardRecyclerViewAdapter
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class DashboardFragment: Fragment(R.layout.fragment_dashboard) {

    companion object {
        private val sharedPrefs = SharedPreferenceManager.get()
        private val INPUT_ACTIVITY = InputActivity::class.java
        private val INPUT2_ACTIVITY = Input2Activity::class.java
        private val userMap = listOf(
            sharedPrefs.isHealthUser(),
            sharedPrefs.isGithubUser(),
            sharedPrefs.isSpotifyUser()
        )
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            mDashboardViewModel.notifyPreferencesChanged("custom")
        }
    }

    private val mDashboardViewModel by viewModel<DashboardViewModel>(ownerProducer = { requireActivity() } )

    private lateinit var binding: FragmentDashboardBinding
    private lateinit var adapter: DashboardRecyclerViewAdapter

    private var loadingState = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentDashboardBinding.inflate(inflater, container, false)

        adapter = DashboardRecyclerViewAdapter(emptyList())
        binding.dashboardRecyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.dashboardRecyclerView.adapter = adapter

        if(!userMap.contains(true)) {
            binding.emptyDash.visibility = View.VISIBLE
        }

        setupItemClickListener()
        setupSwipeToRefresh()

        observeData()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mDashboardViewModel.fetchAllData()
    }

    private fun setupItemClickListener() {
        val activity = mapOf(
            "weight" to INPUT2_ACTIVITY,
            "bloodPressure" to INPUT2_ACTIVITY,
            "bodyTemperature" to INPUT2_ACTIVITY,
            "steps" to INPUT_ACTIVITY,
            "heartRate" to INPUT_ACTIVITY,
            "bodyFat" to INPUT_ACTIVITY,
            "height" to INPUT_ACTIVITY,
            "bloodGlucose" to INPUT_ACTIVITY,
            "oxygenSaturation" to INPUT_ACTIVITY,
            "respiratoryRate" to INPUT_ACTIVITY,
        )

        adapter.onItemClicked = { type ->
            if (type != "githubTotal" && type != "githubDaily" && type != "spotifyGenre" && type != "spotifyPopularity") {
                if(activity.containsKey(type)) {
                    val intent = Intent(context, activity[type]).putExtra("key", type)
                    startActivity(intent)
                } else {
                    val item = mDashboardViewModel.customKeys.value.find { it.name == type }
                    val intent = Intent(context, InputActivity::class.java)
                        .putExtra("name", item?.name)
                        .putExtra("units", item?.units)
                        .putExtra("label", item?.label)
                    activityResultLauncher.launch(intent)
                }
            }
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mDashboardViewModel.uiState
                    .combine(mDashboardViewModel.mDashboardViewList) { uiState, data -> Pair(uiState, data)  }
                    .collect { (uiState, data) ->
                    when (uiState) {
                        is DashboardViewModel.UiState.Uninitialized,
                        is DashboardViewModel.UiState.Loading,
                        is DashboardViewModel.UiState.Refreshing -> {
                            showLoading(true, "Refreshing")
                        }
                        is DashboardViewModel.UiState.Done -> {
                            showLoading(false)
                            adapter.updateData(data)
                        }
                        is DashboardViewModel.UiState.Error -> {
                            showLoading(false)
                            Timber.tag("DashboardFragment").d("${uiState.exception}")
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isVisible: Boolean, text: String = "Loading") {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        val recycler = if (isVisible) View.GONE else View.VISIBLE

        binding.loadingOverlay.visibility = visibility
        binding.loadingMessage.visibility = visibility

        binding.dashboardRecyclerView.visibility = recycler
        if(recycler == View.VISIBLE && binding.dashboardRecyclerView.layoutManager == null) {
            binding.dashboardRecyclerView.layoutManager = GridLayoutManager(context, 2)
        }

        binding.swipeRefresh.isEnabled = !isVisible
        adapter.isClickable = !isVisible

        if(isVisible) animateLoadingText(text)
    }

    private fun animateLoadingText(text: String) {
        lifecycleScope.launch {
            var loadingText = text
            while(binding.loadingMessage.visibility == View.VISIBLE) {
                binding.loadingMessage.text = loadingText
                loadingText = when (loadingText) {
                    "${text}." -> "${text}.."
                    "${text}.." -> "${text}..."
                    else -> "${text}."
                }
                delay(200)
            }
        }
    }

    private fun setupSwipeToRefresh() {
        val swipeRefresh = binding.swipeRefresh
        swipeRefresh.setOnRefreshListener {
            refreshData()
            swipeRefresh.isRefreshing = false
        }
    }

    fun refreshData() {
        binding.emptyDash.visibility = View.GONE
        mDashboardViewModel.refreshData()
    }
}