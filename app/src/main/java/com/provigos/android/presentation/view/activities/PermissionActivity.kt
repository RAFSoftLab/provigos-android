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

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.checkbox.MaterialCheckBox
import com.provigos.android.R
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.databinding.ActivityPermissionBinding

class PermissionActivity: AppCompatActivity(R.layout.activity_permission) {


    private val sharedPrefs = SharedPreferenceManager.get()

    companion object {
        private const val GITHUB = "github"
        private const val SPOTIFY = "spotify"
    }

    private val integrationSettings = mapOf(
        GITHUB to listOf(
            Triple(sharedPrefs::isAllowGithubTotalCommits, sharedPrefs::setAllowGithubTotalCommits, "Track Total Github Commits"),
            Triple(sharedPrefs::isAllowGithubDailyCommits, sharedPrefs::setAllowGithubDailyCommits, "Track Daily Github Commits")
        ),
        SPOTIFY to listOf(
            Triple(sharedPrefs::isAllowSpotifyArtistGenres, sharedPrefs::setAllowSpotifyArtistGenres, "Track Spotify Artists' Genres"),
            Triple(sharedPrefs::isAllowSpotifyArtistPopularity, sharedPrefs::setAllowSpotifyArtistPopularity, "Track Spotify Artists' Popularity")
        )
    )

    private lateinit var binding: ActivityPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.extras?.getString("type")

        when(type) {
            GITHUB -> binding.permissionSetting.text = getString(R.string.tracked_items_through_github_integration)
            SPOTIFY -> binding.permissionSetting.text = getString(R.string.tracked_items_through_spotify_integration)
        }

        integrationSettings[type]?.forEach { (a, b, c) ->
            createCheckbox(a, b, c)
        }

        binding.continueButton.setOnClickListener {
            Toast.makeText(this, "Tracked items remembered!", Toast.LENGTH_SHORT).show()
            finish()
        }

    }

    private fun createCheckbox(readIsAllow: () -> Boolean, readAllow: (Boolean) -> Unit, trackItem: String) {
        val checkBox = MaterialCheckBox(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = trackItem
            textSize = 18f
            isChecked = readIsAllow()
            buttonTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PermissionActivity, R.color.teal1))
            setPadding(16, 16, 16 ,16)
            setOnCheckedChangeListener { _, isChecked -> readAllow(isChecked) }
        }
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
            )
            setBackgroundColor(ContextCompat.getColor(this@PermissionActivity, R.color.divider))
        }
            binding.permissionLinearLayout.addView(checkBox)
    }
}