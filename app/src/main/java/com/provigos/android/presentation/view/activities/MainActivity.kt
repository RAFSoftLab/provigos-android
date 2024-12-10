package com.provigos.android.presentation.view.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract

import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import com.google.android.material.tabs.TabLayoutMediator
import com.provigos.android.R
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

        val mainPagerAdapter = MainPagerAdapter(this)
        mainPagerAdapter.addFragment(DashboardFragment(), "Dashboard")
        mainPagerAdapter.addFragment(SettingsFragment(), "Settings")

        binding.viewPager.adapter = mainPagerAdapter
        binding.viewPager.currentItem = 0

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = mainPagerAdapter.getTabTitle(position)
        }.attach()
    }
}