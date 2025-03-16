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
import androidx.appcompat.app.AlertDialog
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.provigos.android.R
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SpotifyPreferenceFragmentCompat: PreferenceFragmentCompat() {

    private val sharedPrefs = SharedPreferenceManager.get()
    private val mDashboardViewModel: DashboardViewModel by viewModel<DashboardViewModel>( ownerProducer = { requireActivity() })

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.spotify_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trackGenres = findPreference<CheckBoxPreference>(getString(R.string.spotify_genre))
        trackGenres?.isChecked = sharedPrefs.isAllowSpotifyArtistGenres()
        trackGenres?.setOnPreferenceChangeListener { _, newValue ->
            sharedPrefs.setAllowSpotifyArtistGenres(newValue as Boolean)
            mDashboardViewModel.notifyPreferencesChanged("spotify")
            true
        }

        val trackPopularity = findPreference<CheckBoxPreference>(getString(R.string.spotify_popularity))
        trackPopularity?.isChecked = sharedPrefs.isAllowSpotifyArtistPopularity()
        trackPopularity?.setOnPreferenceChangeListener { _, newValue ->
            sharedPrefs.setAllowSpotifyArtistPopularity(newValue as Boolean)
            mDashboardViewModel.notifyPreferencesChanged("spotify")
            true
        }

        val spotifyCache = findPreference<Preference>("invalidate_spotify_cache")
        spotifyCache?.setOnPreferenceClickListener {
            mDashboardViewModel.invalidateCache("spotify")
            true
        }

        val disableSpotifyIntegration = findPreference<Preference>("disable_spotify_integration")
        disableSpotifyIntegration?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Disable Spotify integration")
                .setTitle("Are you sure you want to disable Spotify integration?")
                .setPositiveButton("Disable") { _, _ ->
                    sharedPrefs.setSpotifyUser(false)
                    sharedPrefs.setAllowSpotifyArtistGenres(false)
                    sharedPrefs.setAllowSpotifyArtistPopularity(false)
                    sharedPrefs.setSpotifyAccessToken("")
                    sharedPrefs.setSpotifyRefreshToken("")
                    mDashboardViewModel.notifyPreferencesChanged("spotify")
                    parentFragmentManager.popBackStack()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }
}