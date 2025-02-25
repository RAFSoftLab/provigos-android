/* MIT License
 *
 * Copyright 2024 Provigos

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
        private const val GITHUB_REFRESH_TOKEN_KEY = "github_refresh_token"
        private const val SPOTIFY_ACCESS_TOKEN_KEY = "spotify_access_token"
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

    fun setRememberMe(boolean: Boolean) { editor.putBoolean(REMEMBER_ME, boolean).apply() }

    fun isRememberMe(): Boolean { return sharedPreferences.getBoolean(REMEMBER_ME, false) }

    fun setGoogleToken(token: String) { encryptedEditor.putString(GOOGLE_TOKEN_KEY, token).apply() }

    fun getGoogleToken(): String { return encryptedSharedPreferences.getString(GOOGLE_TOKEN_KEY, "empty")!! }
}