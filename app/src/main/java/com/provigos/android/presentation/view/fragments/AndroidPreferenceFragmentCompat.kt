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
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.provigos.android.R
import com.provigos.android.util.AndroidAdminReceiverManager
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class AndroidPreferenceFragmentCompat: PreferenceFragmentCompat() {

    private val sharedPrefs = SharedPreferenceManager.get()
    private val mDashboardViewModel: DashboardViewModel by viewModel<DashboardViewModel>( ownerProducer = { requireActivity() } )

    private lateinit var adminLauncher: ActivityResultLauncher<Intent>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.android_preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Toast.makeText(requireContext(), "Admin enabled!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Admin required!", Toast.LENGTH_SHORT).show()
                }
        }
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

        val trackUnlockAttempts = findPreference<CheckBoxPreference>("unlock_attempts")
        trackUnlockAttempts?.isEnabled = isDeviceAdminEnabled()
        trackUnlockAttempts?.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                sharedPrefs.setAllowAndroidBiometrics(newValue as Boolean)
                mDashboardViewModel.notifyPreferencesChanged("android")
            }
            true
        }

        /*fun Context.openUsageAccessSettings() {
    try {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    } catch (e: ActivityNotFoundException) {
        // Fallback: open general settings if this specific screen isn’t available
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}   */

        val adminRights = findPreference<Preference>("admin_user")
        adminRights?.setOnPreferenceClickListener {
            if (isDeviceAdminEnabled()) {
                revokeAdminRights()
            } else {
                askForGrantAdminRights()
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

    private fun isDeviceAdminEnabled(): Boolean {
        val devicePolicyManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(requireContext(), AndroidAdminReceiverManager::class.java)
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    private fun askForGrantAdminRights() {
        val adminComponent = ComponentName(requireContext(), AndroidAdminReceiverManager::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
        }
        adminLauncher.launch(intent)
    }

    private fun revokeAdminRights() {
        val devicePolicyManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(requireContext(), AndroidAdminReceiverManager::class.java)
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            AlertDialog.Builder(requireContext())
                .setTitle("Revoke Admin Rights")
                .setMessage("Are you sure? This will stop unlock attempt tracking.")
                .setPositiveButton("Confirm") { _, _ ->
                    devicePolicyManager.removeActiveAdmin(adminComponent)
                    Toast.makeText(requireContext(), "Admin rights revoked", Toast.LENGTH_SHORT).show()
                }
        }
    }
}