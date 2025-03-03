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
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.provigos.android.R
import com.provigos.android.data.SharedPreferenceDataSource
import com.provigos.android.databinding.ActivityLoginBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.MessageDigest

class LoginActivity: AppCompatActivity(R.layout.activity_login) {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var request: GetCredentialRequest

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        request = GetCredentialRequest.Builder()
            .addCredentialOption(getSignInWithGoogleOption())
            .build()

        val credentialManager = CredentialManager.create(this@LoginActivity)

        /*if(SharedPreferenceDataSource(this).isRememberMe()) {
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
        *///else {
            lifecycleScope.launch {
                try {
                    val result = credentialManager.getCredential(
                        context = this@LoginActivity,
                        request = request
                    )
                    handleSignIn(result)
                } catch (e: GetCredentialException) {
                    handleFailure(e)
                }
            }
        //}

    }

    private fun getSignInWithGoogleOption(): GetSignInWithGoogleOption {
        val rawNonce = "burka"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(rawNonce.toByteArray())
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }
        return GetSignInWithGoogleOption.Builder(getString(R.string.SERVER_CLIENT_ID2))
            .setNonce(hashedNonce)
            .build()
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        when(val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        val googleIdToken = googleIdTokenCredential.idToken
                        SharedPreferenceDataSource(this@LoginActivity).setGoogleToken(googleIdToken)
                        SharedPreferenceDataSource(this@LoginActivity).setRememberMe(true)
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } catch (e: GoogleIdTokenParsingException) {
                        Timber.tag("GITPE").e(e, "Received an invalid google id token response")
                    }
                }
                else {
                    Timber.tag("badcred").e("Unexpected type of credential")
                }
            }
            else -> {
                Timber.tag("badcred").e("Unexpected type of credential")
            }
        }
    }

    private fun handleFailure(e: GetCredentialException) {
        Timber.e(e.toString())
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun signout() {
        GlobalScope.launch {
            CredentialManager.create(this@LoginActivity).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}