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
package com.provigos.android.presentation.view.activities

import android.os.Bundle
import android.os.StrictMode
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.provigos.android.R
import com.provigos.android.application.ProvigosApplication
import com.provigos.android.data.HealthConnectManager
import com.provigos.android.databinding.ActivityMainBinding
import com.provigos.android.presentation.view.adapters.MainPagerAdapter
import com.provigos.android.presentation.view.fragments.DashboardFragment
import com.provigos.android.presentation.view.fragments.SettingsFragment

class MainActivity: AppCompatActivity(R.layout.activity_main) {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

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


        val mainPagerAdapter = MainPagerAdapter(this)
        mainPagerAdapter.addFragment(DashboardFragment(), "Dashboard")
        mainPagerAdapter.addFragment(SettingsFragment(), "Settings")

        binding.viewPager.adapter = mainPagerAdapter
        binding.viewPager.currentItem = 0

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = mainPagerAdapter.getTabTitle(position)
            tab.tag = tab.text
        }.attach()
    }
}