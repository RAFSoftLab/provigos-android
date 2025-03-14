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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.auth0.jwt.JWT
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.provigos.android.BuildConfig
import com.provigos.android.R
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.MessageDigest
import java.util.Date

class LoginActivity: AppCompatActivity(R.layout.activity_login) {

    companion object {
        private const val GOOGLE_CLIENT_ID = BuildConfig.GOOGLE_CLIENT_ID
    }

    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialManager: CredentialManager
    private lateinit var request: GetCredentialRequest
    private val sharedPrefs = SharedPreferenceManager.get()

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        credentialManager = CredentialManager.create(this)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        request = GetCredentialRequest.Builder()
            .addCredentialOption(getSignInWithGoogleOption())
            .build()

        if (hasValidStoredToken()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            requestSignIn()
        }
    }

    private fun hasValidStoredToken(): Boolean {
        val googleToken = sharedPrefs.getGoogleToken()
        if(googleToken.isNullOrBlank()) {
            return false
        } else {
            try {
                val decodedJWT = JWT.decode(googleToken)
                val expiration = decodedJWT.expiresAt
                val currentTime = Date()
                return expiration.after(currentTime)
            } catch (e: Exception) {
                Timber.tag("LoginActivity").e("Failed to decode token")
                sharedPrefs.setGoogleToken("")
                return false
            }
        }
    }

    private fun requestSignIn() {
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Timber.tag("LoginActivity").e("Failed to sign-in ${e.message}")
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        when(val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val googleIdToken = googleIdTokenCredential.idToken
                        sharedPrefs.setGoogleToken(googleIdToken)
                        if(sharedPrefs.isFirstTimeUser()) {
                            startActivity(Intent(this@LoginActivity, IntroActivity::class.java))
                        } else {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        }
                        finish()
                    } catch (e: GoogleIdTokenParsingException) {
                        Timber.tag("LoginActivity").e(e, "Received an invalid google id token response")
                        showToast("Sign-in failed: Invalid token")
                    }
                }
                else {
                    Timber.tag("LoginActivity").e("Unexpected type of credential")
                    showToast("Sign-in failed: unexpected credential type")
                }
            }
            else -> {
                Timber.tag("LoginActivity").e("Unexpected type of credential")
                showToast("Sign-in failed: unexpected credential type")
            }
        }
    }

    private fun getSignInWithGoogleOption(): GetSignInWithGoogleOption {
        val rawNonce = "burka"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(rawNonce.toByteArray())
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }
        return GetSignInWithGoogleOption.Builder(GOOGLE_CLIENT_ID)
            .setNonce(hashedNonce)
            .build()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}