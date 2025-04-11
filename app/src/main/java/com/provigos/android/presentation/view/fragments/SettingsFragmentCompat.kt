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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.provigos.android.R
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.presentation.view.activities.OAuthActivity
import com.provigos.android.presentation.view.activities.HealthConnectPrivacyPolicyActivity
import com.provigos.android.presentation.view.activities.LicenseActivity
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragmentCompat: PreferenceFragmentCompat() {

    private val mDashboardViewModel: DashboardViewModel by viewModel<DashboardViewModel>( ownerProducer = { requireActivity() })
    private val sharedPrefs = SharedPreferenceManager.get()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mDashboardViewModel.navigateIntegrationScreen.collect { destination ->
                    destination?.let {
                        openIntegrationSettings(it)
                        mDashboardViewModel.resetIntegration()
                    }
                }
            }
        }

        val healthConnectSwitch = findPreference<Preference>(getString(R.string.switch_pref1_key))
        healthConnectSwitch?.summary = "Manage your Health Connect permissions"
        healthConnectSwitch?.setOnPreferenceClickListener {
            healthConnectIntegration()
            true
        }

        val githubSwitch = findPreference<Preference>(getString(R.string.switch_pref2_key))
        val isGithubUser = sharedPrefs.isGithubUser()
        if(isGithubUser) {
            githubSwitch?.summary = "Manage your GitHub integration"
        } else {
            githubSwitch?.summary = "Allow Provigos to access your GitHub data"
        }

        githubSwitch?.setOnPreferenceClickListener {
            if(isGithubUser) {
                openIntegrationSettings("github")
            } else {
                githubIntegration()
                parentFragmentManager.popBackStack()
            }
            true
        }

        val spotifySwitch = findPreference<Preference>(getString(R.string.switch_pref3_key))
        val isSpotifyUser = sharedPrefs.isSpotifyUser()
        if(isSpotifyUser) {
            spotifySwitch?.summary = "Manage your Spotify integration"
        } else {
            spotifySwitch?.summary = "Allow Provigos to access your Spotify data"
        }
        spotifySwitch?.setOnPreferenceClickListener {
            if(isSpotifyUser) {
                openIntegrationSettings("spotify")
            } else {
                spotifyIntegration()
                parentFragmentManager.popBackStack()
            }
            true
        }

        val androidSwitch = findPreference<Preference>("android_integration")
        val isAndroidUser = sharedPrefs.isAndroidUser()
        if(isAndroidUser) {
            androidSwitch?.summary = "Manage your Android integration"
        } else {
            androidSwitch?.summary = "Allow Provigos to access your Android data"
        }
        androidSwitch?.setOnPreferenceClickListener {
            openIntegrationSettings("android")
            true
        }

        findPreference<Preference>(getString(R.string.switch_pref4_key))?.setOnPreferenceClickListener {
            openIntegrationSettings("custom")
            true
        }

        findPreference<Preference>(getString(R.string.switch_pref5_key))?.setOnPreferenceClickListener {
            showSignOutDialog()
            true
        }

        findPreference<Preference>("licence")?.setOnPreferenceClickListener {
            startActivity(Intent(requireActivity(), LicenseActivity::class.java))
            true
        }

        findPreference<Preference>("web_link")?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://provigos.com"))
            startActivity(intent)
            true
        }
    }

    private fun healthConnectIntegration() {
        startActivity(Intent(activity, HealthConnectPrivacyPolicyActivity::class.java))
        lifecycleScope.launch {
            mDashboardViewModel.invalidateCache("health_connect")
        }
    }

    private fun githubIntegration() {
        startActivity(Intent(activity, OAuthActivity::class.java)
            .putExtra("oauth", "github")
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    private fun spotifyIntegration() {
        startActivity(Intent(activity, OAuthActivity::class.java)
            .putExtra("oauth", "spotify")
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sign Out")
            .setTitle("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                sharedPrefs.setGoogleToken("")
                lifecycleScope.launch {
                    CredentialManager.create(requireContext()).clearCredentialState(ClearCredentialStateRequest())
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openGithubPreferenceScreen() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.view_settings, GithubPreferenceFragmentCompat())
            .addToBackStack(null)
            .commit()
    }

    private fun openSpotifyPreferenceScreen() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.view_settings, SpotifyPreferenceFragmentCompat())
            .addToBackStack(null)
            .commit()
    }

    private fun openCustomPreferenceScreen() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.view_settings, CustomPreferenceFragmentCompat())
            .addToBackStack(null)
            .commit()
    }

    private fun openAndroidPreferenceScreen() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.view_settings, AndroidPreferenceFragmentCompat())
            .addToBackStack(null)
            .commit()
    }

    private fun openIntegrationSettings(destination: String) {
        when (destination) {
            "github" -> openGithubPreferenceScreen()
            "spotify" -> openSpotifyPreferenceScreen()
            "android" -> openAndroidPreferenceScreen()
            "custom" -> openCustomPreferenceScreen()
        }
    }
}