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
package com.provigos.android.data.local

import android.content.SharedPreferences

class SharedPreferenceDataSource(private val encryptedSharedPreferences: SharedPreferences) {

    companion object {
        private const val PRIVATE_POLICY_KEY = "privacy_policy"
        private const val GOOGLE_TOKEN_KEY = "google_token"
        private const val GITHUB_ACCESS_TOKEN_KEY = "github_access_token"
        private const val SPOTIFY_ACCESS_TOKEN_KEY = "spotify_access_token"
        private const val SPOTIFY_REFRESH_TOKEN_KEY = "spotify_refresh_token"
        private const val SPOTIFY_TOKEN_EXPIRATION = "spotify_token_expiration"
        private const val STATE = "state"
        private const val AUTH_DESTINATION = "auth_destination"
        private const val PKCE_CODE_VERIFIER = "code_verifier"
        private const val IS_FIRST_TIME_USER = "first_time_user"
        private const val IS_HEALTH_USER = "health_user"
        private const val IS_GITHUB_USER = "github_user"
        private const val IS_SPOTIFY_USER = "spotify_user"
        private const val ALLOW_GITHUB_TOTAL_COMMITS = "allow_github_total_commits"
        private const val ALLOW_GITHUB_DAILY_COMMITS = "allow_github_daily_commits"
        private const val ALLOW_SPOTIFY_ARTIST_GENRES = "allow_spotify_artist_genres"
        private const val ALLOW_SPOTIFY_ARTIST_POPULARITY = "allow_spotify_artist_popularity"
    }

    fun setPrivacyPolicy(bool: Boolean) { encryptedSharedPreferences.edit().putBoolean(PRIVATE_POLICY_KEY, bool).apply() }

    fun isPrivacyPolicy(): Boolean { return encryptedSharedPreferences.getBoolean(PRIVATE_POLICY_KEY, false) }

    fun setState(token: String) { encryptedSharedPreferences.edit().putString(STATE, token).apply() }

    fun getState(): String? { return encryptedSharedPreferences.getString(STATE, null) }

    fun setGoogleToken(token: String) { encryptedSharedPreferences.edit().putString(GOOGLE_TOKEN_KEY, token).apply() }

    fun getGoogleToken(): String? { return encryptedSharedPreferences.getString(GOOGLE_TOKEN_KEY, null) }

    fun setGithubAccessToken(token: String) { encryptedSharedPreferences.edit().putString(
        GITHUB_ACCESS_TOKEN_KEY, token).apply() }

    fun getGithubAccessToken(): String? { return encryptedSharedPreferences.getString(
        GITHUB_ACCESS_TOKEN_KEY, null) }

    fun setSpotifyAccessToken(token: String) { encryptedSharedPreferences.edit().putString(
        SPOTIFY_ACCESS_TOKEN_KEY, token).apply() }

    fun getSpotifyAccessToken(): String? { return encryptedSharedPreferences.getString(
        SPOTIFY_ACCESS_TOKEN_KEY, null) }

    fun setSpotifyRefreshToken(token: String) { encryptedSharedPreferences.edit().putString(
        SPOTIFY_REFRESH_TOKEN_KEY, token).apply() }

    fun getSpotifyRefreshToken(): String? { return encryptedSharedPreferences.getString(
        SPOTIFY_REFRESH_TOKEN_KEY, null) }

    fun setAuthDestination(token: String) { encryptedSharedPreferences.edit().putString(AUTH_DESTINATION, token).apply() }

    fun getAuthDestination(): String? { return encryptedSharedPreferences.getString(AUTH_DESTINATION, null) }

    fun setCodeVerifier(token: String) { encryptedSharedPreferences.edit().putString(
        PKCE_CODE_VERIFIER, token).apply() }

    fun getCodeVerifier(): String? { return encryptedSharedPreferences.getString(PKCE_CODE_VERIFIER, null) }

    fun setHealthUser(bool: Boolean) { encryptedSharedPreferences.edit().putBoolean(IS_HEALTH_USER, bool).apply() }

    fun isFirstTimeUser(): Boolean { return encryptedSharedPreferences.getBoolean(IS_FIRST_TIME_USER, true) }

    fun setFirstTimeUser(bool: Boolean) { encryptedSharedPreferences.edit().putBoolean(IS_FIRST_TIME_USER, bool).apply() }

    fun isHealthUser(): Boolean { return encryptedSharedPreferences.getBoolean(IS_HEALTH_USER, false) }

    fun setGithubUser(bool: Boolean) { encryptedSharedPreferences.edit().putBoolean(IS_GITHUB_USER, bool).apply() }

    fun isGithubUser(): Boolean { return encryptedSharedPreferences.getBoolean(IS_GITHUB_USER, false) }

    fun setSpotifyUser(bool: Boolean) { encryptedSharedPreferences.edit().putBoolean(IS_SPOTIFY_USER, bool).apply() }

    fun isSpotifyUser(): Boolean { return encryptedSharedPreferences.getBoolean(IS_SPOTIFY_USER, false) }

    fun setAllowGithubTotalCommits(bool: Boolean) { encryptedSharedPreferences.edit().putBoolean(
        ALLOW_GITHUB_TOTAL_COMMITS, bool).apply() }

    fun isAllowGithubTotalCommits(): Boolean { return encryptedSharedPreferences.getBoolean(
        ALLOW_GITHUB_TOTAL_COMMITS, false) }

    fun setAllowGithubDailyCommits(bool: Boolean) { encryptedSharedPreferences.edit().putBoolean(
        ALLOW_GITHUB_DAILY_COMMITS, bool).apply() }

    fun isAllowGithubDailyCommits(): Boolean { return encryptedSharedPreferences.getBoolean(
        ALLOW_GITHUB_DAILY_COMMITS, false) }

    fun setAllowSpotifyArtistGenres(bool: Boolean) { encryptedSharedPreferences.edit().putBoolean(
        ALLOW_SPOTIFY_ARTIST_GENRES, bool).apply() }

    fun isAllowSpotifyArtistGenres(): Boolean { return encryptedSharedPreferences.getBoolean(
        ALLOW_SPOTIFY_ARTIST_GENRES, false) }

    fun setAllowSpotifyArtistPopularity(bool: Boolean) { encryptedSharedPreferences.edit().putBoolean(
        ALLOW_SPOTIFY_ARTIST_POPULARITY, bool).apply() }

    fun isAllowSpotifyArtistPopularity(): Boolean { return encryptedSharedPreferences.getBoolean(
        ALLOW_SPOTIFY_ARTIST_POPULARITY, false) }

    fun setSpotifyTokenExpiration(expiration: Long) { return encryptedSharedPreferences.edit().putLong(
        SPOTIFY_TOKEN_EXPIRATION, expiration).apply() }

    fun getSpotifyTokenExpiration(): Long { return encryptedSharedPreferences.getLong(
        SPOTIFY_TOKEN_EXPIRATION, 0) }
}