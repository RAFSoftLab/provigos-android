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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.provigos.android.data.SharedPreferenceDataSource
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import timber.log.Timber

class OAuthActivity: AppCompatActivity() {

    companion object {
        private const val GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize"
        private const val GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token"
        private const val GITHUB_CLIENT_ID = "TODO"
        private const val GITHUB_SCOPES = "repo"

        private const val SPOTIFY_AUTH_URL = "https://accounts.spotify.com/authorize"
        private const val SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token"
        private const val SPOTIFY_CLIENT_ID = "TODO"
        private const val SPOTIFY_SCOPES = "TODO"

        private const val CALLBACK = "com.provigos.android"
    }

    private var authDestination: String? = null

    private lateinit var authService: AuthorizationService
    private lateinit var authIntent: Intent

    private val authResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ActivityResultCallback { result ->

            val data: Intent? = result.data
            val response = AuthorizationResponse.fromIntent(data!!)
            val exception = AuthorizationException.fromIntent(data)

            if(response != null) {
                exchangeCodeForToken(response)
            } else {
                Timber.tag("OAuth").e("Authorization failed: $exception")
            }

        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authService = AuthorizationService(this@OAuthActivity)

        authDestination = intent.extras?.getString("oauth")

        try {
            when (authDestination) {
                "github" -> {
                    val authRequest = buildAuthIntent(GITHUB_AUTH_URL, GITHUB_TOKEN_URL, GITHUB_CLIENT_ID, GITHUB_SCOPES)
                    authIntent = authService.getAuthorizationRequestIntent(authRequest)
                }
                "spotify" -> {
                    val authRequest = buildAuthIntent(SPOTIFY_AUTH_URL, SPOTIFY_TOKEN_URL, SPOTIFY_CLIENT_ID, SPOTIFY_SCOPES)
                    authIntent = authService.getAuthorizationRequestIntent(authRequest)
                }
                null -> {
                    Timber.tag("AuthDestination").e("OAuth authorization process begun with null target service")
                }
            }

            authResultLauncher.launch(authIntent)

        } catch (e: UninitializedPropertyAccessException) {
            Timber.e("AuthRequest is not initialized")
        }
    }

    private fun exchangeCodeForToken(resp: AuthorizationResponse) {

        val tokenRequest = resp.createTokenExchangeRequest()

        authService.performTokenRequest(tokenRequest) { response, exception ->

            if(response != null) {
                val accessToken = response.accessToken
                val refreshToken = response.refreshToken?: ""

                val preferences = SharedPreferenceDataSource(this@OAuthActivity)
                when(authDestination) {
                    "github" -> {
                        Timber.tag("GithubAccessToken").e(accessToken)
                        preferences.setGithubAccessToken(accessToken!!)
                    }
                    "spotify" -> {
                        Timber.tag("SpotifyAccessToken").e(accessToken)
                        preferences.setSpotifyAccessToken(accessToken!!)
                        Timber.tag("SpotifyRefreshToken").e(refreshToken)
                        preferences.setSpotifyRefreshToken(refreshToken)
                    }
                }
            } else {
                when(authDestination) {
                    "github" -> Timber.tag("GithubAuth").e(exception)
                    "spotify" -> Timber.tag("SpotifyAuth").e(exception)
                }
            }
        }
    }

    private fun buildAuthIntent(authUrl: String, tokenUrl: String, clientId: String, scopes: String): AuthorizationRequest {

        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(authUrl),
            Uri.parse(tokenUrl)
        )

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(CALLBACK)
        )
            .setScopes(scopes)
            .build()

        return authRequest
    }

}