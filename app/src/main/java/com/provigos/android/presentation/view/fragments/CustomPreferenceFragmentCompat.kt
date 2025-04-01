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
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.provigos.android.R
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.presentation.view.activities.EditActivity
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomPreferenceFragmentCompat: PreferenceFragmentCompat() {

    private val mDashboardViewModel: DashboardViewModel by viewModel<DashboardViewModel>(ownerProducer = { requireActivity() } )
    private val sharedPrefs = SharedPreferenceManager.get()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
       setPreferencesFromResource(R.xml.custom_preferences, rootKey)
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            mDashboardViewModel.notifyPreferencesChanged("custom")
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addCustom = findPreference<Preference>("add_custom_item")
        addCustom?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), EditActivity::class.java).putExtra("value", true)
            activityResultLauncher.launch(intent)
            true
        }

        val editCustom = findPreference<Preference>("edit_custom_item")
        editCustom?.setOnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.view_settings, CustomListPreferenceFragmentCompat())
                .addToBackStack(null)
                .commit()
            true
        }

        val trackCustom = findPreference<Preference>("custom_item_tracking")
        trackCustom?.setOnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.view_settings, CustomTrackingPreferenceFragmentCompat())
                .addToBackStack(null)
                .commit()
            true
        }

        val trackCustomUser = findPreference<CheckBoxPreference>("is_custom_user")
        trackCustomUser?.isChecked = sharedPrefs.isCustomUser()
        trackCustomUser?.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                sharedPrefs.setCustomUser(newValue as Boolean)
                mDashboardViewModel.notifyPreferencesChanged("custom")
            }
            true
        }
    }
}