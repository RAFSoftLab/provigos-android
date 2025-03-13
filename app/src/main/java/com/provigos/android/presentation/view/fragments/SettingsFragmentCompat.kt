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

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.provigos.android.R
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.presentation.view.activities.OAuthActivity
import com.provigos.android.presentation.view.activities.HealthConnectPrivacyPolicyActivity
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragmentCompat: PreferenceFragmentCompat() {

    private val sharedPrefs = SharedPreferenceManager.get()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val healthConnectSwitch = findPreference<Preference>(getString(R.string.switch_pref1_key))
        val isHealthUser = sharedPrefs.isHealthUser()
        if(isHealthUser) {
            healthConnectSwitch?.summary = "Manage your Health Connect permissions"
        } else {
            healthConnectSwitch?.summary = "Allow Provigos to access your Health Connect data"
        }
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
                openGithubPreferenceScreen()
            } else {
                githubIntegration()
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
                openSpotifyPreferenceScreen()
            } else {
                spotifyIntegration()
            }
            true
        }

        findPreference<Preference>(getString(R.string.switch_pref4_key))?.setOnPreferenceClickListener {
            true
        }

        findPreference<Preference>(getString(R.string.switch_pref5_key))?.setOnPreferenceClickListener {
            showSignOutDialog()
            true
        }
    }

    private fun healthConnectIntegration() {
        startActivity(Intent(activity, HealthConnectPrivacyPolicyActivity::class.java))
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
}