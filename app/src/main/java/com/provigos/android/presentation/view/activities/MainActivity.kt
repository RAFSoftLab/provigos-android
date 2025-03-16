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

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import com.provigos.android.R
import com.provigos.android.databinding.ActivityMainBinding
import com.provigos.android.presentation.view.adapters.MainPagerAdapter
import com.provigos.android.presentation.view.fragments.DashboardFragment
import com.provigos.android.presentation.view.fragments.SettingsFragment
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: AppCompatActivity(R.layout.activity_main) {

    private val mDashboardViewModel: DashboardViewModel by viewModel<DashboardViewModel>()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableStrictMode()

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        handleIntentExtras(intent)

        val mainPagerAdapter = MainPagerAdapter(this)
        mainPagerAdapter.addFragment(DashboardFragment(), "Dashboard", "dashboard_fragment")
        mainPagerAdapter.addFragment(SettingsFragment(), "Settings", "settings_fragment")

        binding.viewPager.adapter = mainPagerAdapter
        binding.viewPager.currentItem = 0

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = mainPagerAdapter.getTabTitle(position)
            tab.tag = mainPagerAdapter.getTabTag(position)
        }.attach()

        observePreferenceChanges()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentExtras(intent)
    }

    private fun handleIntentExtras(intent: Intent?) {
        intent?.let {
            val destination = it.getStringExtra("open_settings")
            val tabIndex = it.getIntExtra("select_tab", 0)
            if(!destination.isNullOrEmpty()) {
                mDashboardViewModel.setIntegrationSettings(destination)
            }
            binding.viewPager.currentItem = tabIndex
        }
    }

    private fun observePreferenceChanges() {
        lifecycleScope.launch {
            mDashboardViewModel.preferencesUpdated.collect { isUpdated ->
                if(isUpdated) {
                    refreshDashboard()
                    mDashboardViewModel.resetPreferencesChanged()
                }
            }
        }
    }

    private fun refreshDashboard() {
       val pagerAdapter = binding.viewPager.adapter as MainPagerAdapter
        val dashboardFragment = pagerAdapter.getFragmentAtPosition(0) as? DashboardFragment
        dashboardFragment?.refreshData()
    }

    private fun enableStrictMode(flag: Boolean = true) {
        if(!flag) return
        else {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
    }
}