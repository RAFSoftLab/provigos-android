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
package com.provigos.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SharedPreferenceDataSource(context: Context) {

    companion object {
        private const val PREFS_NAME = "encrypted_prefs"
        private const val REMEMBER_ME = "remember_me"
        private const val PRIVATE_POLICY_KEY = "privacy_policy"
        private const val GOOGLE_TOKEN_KEY = "google_token"
        private const val GITHUB_ACCESS_TOKEN_KEY = "github_access_token"
        private const val SPOTIFY_ACCESS_TOKEN_KEY = "spotify_access_token"
        private const val SPOTIFY_REFRESH_TOKEN_KEY = "spotify_refresh_token"
        private const val STATE = "state"
        private const val AUTH_DESTINATION = "auth_destination"
        private const val PKCE_CODE_VERIFIER = "code_verifier"
    }

    private var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var editor: SharedPreferences.Editor = sharedPreferences.edit()

    private var masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    private var encryptedSharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private var encryptedEditor: SharedPreferences.Editor = encryptedSharedPreferences.edit()


    fun setPrivacyPolicy(boolean: Boolean) { editor.putBoolean(PRIVATE_POLICY_KEY, boolean).apply() }

    fun isPrivacyPolicy(): Boolean { return sharedPreferences.getBoolean(PRIVATE_POLICY_KEY, false) }

    fun setState(token: String) { encryptedEditor.putString(STATE, token).apply() }

    fun getState(): String? { return encryptedSharedPreferences.getString(STATE, null) }

    fun setGoogleToken(token: String) { encryptedEditor.putString(GOOGLE_TOKEN_KEY, token).apply() }

    fun getGoogleToken(): String? { return encryptedSharedPreferences.getString(GOOGLE_TOKEN_KEY, null) }

    fun setGithubAccessToken(token: String) { encryptedEditor.putString(GITHUB_ACCESS_TOKEN_KEY, token).apply() }

    fun getGithubAccessToken(): String? { return encryptedSharedPreferences.getString(GITHUB_ACCESS_TOKEN_KEY, null) }

    fun setSpotifyAccessToken(token: String) { encryptedEditor.putString(SPOTIFY_ACCESS_TOKEN_KEY, token).apply() }

    fun getSpotifyAccessToken(): String? { return encryptedSharedPreferences.getString(SPOTIFY_ACCESS_TOKEN_KEY, null) }

    fun setSpotifyRefreshToken(token: String) { encryptedEditor.putString(SPOTIFY_REFRESH_TOKEN_KEY, token).apply() }

    fun getSpotifyRefreshToken(): String? { return encryptedSharedPreferences.getString(SPOTIFY_REFRESH_TOKEN_KEY, null) }

    fun setAuthDestination(token: String) { editor.putString(AUTH_DESTINATION, token).apply() }

    fun getAuthDestination(): String? { return sharedPreferences.getString(AUTH_DESTINATION, null) }

    fun setCodeVerifier(token: String) { encryptedEditor.putString(PKCE_CODE_VERIFIER, token).apply() }

    fun getCodeVerifier(): String? { return encryptedSharedPreferences.getString(PKCE_CODE_VERIFIER, null) }
}