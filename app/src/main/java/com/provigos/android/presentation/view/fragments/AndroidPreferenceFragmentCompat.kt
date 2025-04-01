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

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.provigos.android.R
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class AndroidPreferenceFragmentCompat: PreferenceFragmentCompat() {

    private val sharedPrefs = SharedPreferenceManager.get()
    private val mDashboardViewModel: DashboardViewModel by viewModel<DashboardViewModel>( ownerProducer = { requireActivity() } )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.android_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trackScreenTime = findPreference<CheckBoxPreference>("screen_time")
        trackScreenTime?.isChecked = sharedPrefs.isAllowAndroidScreenTime()
        trackScreenTime?.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                sharedPrefs.setAllowAndroidScreenTime(newValue as Boolean)
                mDashboardViewModel.notifyPreferencesChanged("android")
            }
            true
        }

        val trackNotifications = findPreference<CheckBoxPreference>("notification_count")
        trackNotifications?.isChecked = sharedPrefs.isAllowAndroidNotificationCount()
        trackNotifications?.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                sharedPrefs.setAllowAndroidNotificationCount(newValue as Boolean)
                mDashboardViewModel.notifyPreferencesChanged("android")
            }
            true
        }

        val androidCache = findPreference<Preference>("invalidate_android_cache")
        androidCache?.setOnPreferenceClickListener {
            lifecycleScope.launch {
                mDashboardViewModel.invalidateCache("android")
            }
            true
        }

        val trackCustomUser = findPreference<CheckBoxPreference>("is_android_user")
        trackCustomUser?.isChecked = sharedPrefs.isAndroidUser()
        trackCustomUser?.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                sharedPrefs.setAndroidUser(newValue as Boolean)
                mDashboardViewModel.notifyPreferencesChanged("android")
            }
            true
        }
    }
}