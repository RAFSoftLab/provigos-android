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
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import com.provigos.android.BuildConfig
import com.provigos.android.data.SharedPreferenceDataSource
import com.provigos.android.util.PkceHelper
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import okio.IOException
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.UUID

class OAuthActivity: AppCompatActivity() {

    companion object {
        private const val CALLBACK = "com.provigos.android://callback"

        private const val GITHUB = "github"
        private const val GITHUB_CLIENT_ID = BuildConfig.GITHUB_CLIENT_ID
        private const val GITHUB_CLIENT_SECRET = BuildConfig.GITHUB_CLIENT_SECRET
        private const val GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize"
        private const val GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token"
        private const val GITHUB_SCOPES = "repo"

        private const val SPOTIFY = "spotify"
        private const val SPOTIFY_CLIENT_ID = BuildConfig.SPOTIFY_CLIENT_ID
        private const val SPOTIFY_CLIENT_SECRET = BuildConfig.SPOTIFY_CLIENT_SECRET
        private const val SPOTIFY_AUTH_URL = "https://accounts.spotify.com/authorize"
        private const val SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token"
        private const val SPOTIFY_SCOPES = "user-library-read"
    }

    private var state: String? = null
    private var authDestination: String? = null

    private lateinit var authService: AuthorizationService
    private lateinit var sharedPrefs: SharedPreferenceDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authService = AuthorizationService(this)
        sharedPrefs = SharedPreferenceDataSource(this)

        authDestination = intent.extras?.getString("oauth")
        if(authDestination.isNullOrEmpty()) {
            Timber.tag("OAuthActivity").e("No auth destination provided, finishing activity.")
            finish()
            return
        }

        sharedPrefs.setAuthDestination(authDestination!!)

        startOAuthFlow()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun startOAuthFlow() {
        if(authDestination.isNullOrEmpty()) {
            Timber.tag("OAuthActivity").e("authDestination is null or empty")
            finish()
            return
        }
        try {
            Timber.tag("OAuthActivity").d("Starting OAuth flow for $authDestination")

            val authRequest = when (authDestination) {
                GITHUB -> buildAuthRequest(GITHUB)
                SPOTIFY -> buildAuthRequest(SPOTIFY)
                else -> throw IllegalStateException("Invalid authDestination: $authDestination")
            }
            val authIntent = authService.getAuthorizationRequestIntent(authRequest)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

            startActivity(authIntent)

        } catch (e: Exception) {
            Timber.tag("OAuthActivity").e("Error starting OAuth flow: ${e.message}")
            finish()
        }
    }

    private fun handleIntent(intent: Intent) {
        authDestination = sharedPrefs.getAuthDestination()
        if(authDestination.isNullOrEmpty()) {
            Timber.tag("OAuthActivity").e("authDestination is null or empty")
            finish()
            return
        }

        val uri: Uri? = intent.data
        if( uri != null) {
            val code = uri.getQueryParameter("code")
            val actualState = uri.getQueryParameter("state")
            val error = uri.getQueryParameter("error")

            if(code != null && actualState != null) {
                val expectedState = sharedPrefs.getState()
                if(expectedState == actualState) {
                    Timber.tag("OAuthActivity").d("State validation successful")
                    exchangeCodeForToken(code)
                } else {
                    Timber.tag("OAuthActivity").e("State parameter mismatch: possible CSRF attack")
                }
            } else if(code == null && actualState != null) {
                Timber.tag("OAuthActivity").e("OAuth response missing code: $error")
            } else {
                Timber.tag("OAuthActivity").e("OAuth response missing code & state: $error")
            }
        } else {
            Timber.tag("OAuthActivity").e("OAuth response is null: User canceled auth or an error occurred")
        }

        sharedPrefs.setState("")
        sharedPrefs.setAuthDestination("")
        finish()
    }

    private fun exchangeCodeForToken(code: String) {
        Timber.tag("OAuthActivity").d("Exchanging code for token for $authDestination")
        try {

            val codeVerifier = sharedPrefs.getCodeVerifier()

            val serviceConfig = buildServiceConfig(authDestination!!)
            val clientId = getClientId(authDestination!!)
            val scopes = getScopes(authDestination!!)

            val tokenRequestBuilder = TokenRequest.Builder(
                serviceConfig,
                clientId
            )
                .setGrantType("authorization_code")
                .setAuthorizationCode(code)
                .setRedirectUri(Uri.parse(CALLBACK))
                .setScopes(scopes)

            if(codeVerifier != null) {
                tokenRequestBuilder.setCodeVerifier(codeVerifier)
            } else {
                tokenRequestBuilder.setAdditionalParameters(
                    mapOf("client_secret" to getClientSecret(authDestination!!))
                )
            }

            val tokenRequest = tokenRequestBuilder.build()

            authService.performTokenRequest(tokenRequest) { resp, exception ->
                if (resp != null) {
                    handleTokenResponse(resp)
                } else if (exception != null) {
                    Timber.tag("OAuthActivity").e("Token exchange failed: ${exception.errorDescription}")
                }
            }
        } catch (e: Exception) {
            Timber.tag("OAuthActivity").e("Error during token exchange")
        }
    }

    private fun handleTokenResponse(response: TokenResponse) {
        try {
            val accessToken = response.accessToken ?: throw IllegalStateException("Access token is null")
            val refreshToken = response.refreshToken?: ""
            when(authDestination) {
                GITHUB -> saveToken(GITHUB, accessToken)
                SPOTIFY -> saveToken(SPOTIFY, accessToken, refreshToken)
                else -> Timber.tag("OAuthActivity").e("Unknown auth destination: $authDestination")
            }
        } catch (e: Exception) {
            Timber.tag("OAuthActivity").e("Error handling token response")
        }
    }

    private fun buildAuthRequest(destination: String): AuthorizationRequest {

        val serviceConfig = buildServiceConfig(destination)
        val clientId = getClientId(destination)
        val scopes = getScopes(destination)

        state = UUID.randomUUID().toString()
        sharedPrefs.setState(state!!)
        Timber.tag("OAuthActivity").d("Generated and saved state: $state")

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(CALLBACK)
        )
            .setScopes(scopes)
            .setState(state)

        if(destination == SPOTIFY) {
            val codeVerifier = PkceHelper.generateCodeVerifier()
            val codeChallenge = PkceHelper.generateCodeChallenge(codeVerifier)
            sharedPrefs.setCodeVerifier(codeVerifier)

            authRequest.setCodeVerifier(codeVerifier, codeChallenge, AuthorizationRequest.CODE_CHALLENGE_METHOD_S256)
        }

        return authRequest.build()
    }

    private fun refreshToken(destination: String) {
        val refreshToken = when (destination) {
            SPOTIFY -> sharedPrefs.getSpotifyRefreshToken()
            else -> throw IllegalArgumentException("Implementation for the designated OAuth destination is not available")
        }
        if(refreshToken.isNullOrEmpty()) {
            Timber.tag("OAuthActivity").e("No refresh token found. User must re-authenticate")
            return
        }

        try {

            val serviceConfig = buildServiceConfig(destination)
            val clientId = getClientId(destination)
            val scopes = getScopes(destination)
            val clientSecret = getClientSecret(destination)

            val tokenRequestBuilder = TokenRequest.Builder(
                serviceConfig,
                clientId
            )
                .setGrantType("refresh_token")
                .setRefreshToken(refreshToken)
                .setScopes(scopes)

            tokenRequestBuilder.setAdditionalParameters(mapOf("client_secret" to clientSecret))

            val tokenRequest = tokenRequestBuilder.build()

            authService.performTokenRequest(tokenRequest) { response, exception ->
                if (response != null) {
                    val newAccessToken = response.accessToken ?: ""
                    val newRefreshToken = response.refreshToken ?: refreshToken

                    saveToken(destination, newAccessToken, newRefreshToken)
                } else if (exception != null) {
                    Timber.tag("OAuthActivity").e("Token refresh failed for $destination: ${exception.errorDescription}")
                }
            }
        } catch (e: Exception) {
            Timber.tag("OAuthActivity").e("Error during $destination token refresh: ${e.message}")
        }
    }

    private fun buildServiceConfig(destination: String): AuthorizationServiceConfiguration {
        return when(destination) {
            GITHUB -> AuthorizationServiceConfiguration(Uri.parse(GITHUB_AUTH_URL), Uri.parse(GITHUB_TOKEN_URL))
            SPOTIFY -> AuthorizationServiceConfiguration(Uri.parse(SPOTIFY_AUTH_URL), Uri.parse(SPOTIFY_TOKEN_URL))
            else -> throw IllegalArgumentException("Implementation for the designated OAuth destination is not available")
        }
    }

    private fun getClientId(destination: String): String {
        return when(destination) {
            GITHUB -> GITHUB_CLIENT_ID
            SPOTIFY -> SPOTIFY_CLIENT_ID
            else -> throw IllegalArgumentException("Implementation for the designated OAuth destination is not available")
        }
    }

    private fun getScopes(destination: String): String {
        return when(destination) {
            GITHUB -> GITHUB_SCOPES
            SPOTIFY -> SPOTIFY_SCOPES
            else -> throw IllegalArgumentException("Implementation for the designated OAuth destination is not available")
        }
    }

    private fun getClientSecret(destination: String): String {
        return when(destination) {
            GITHUB -> GITHUB_CLIENT_SECRET
            SPOTIFY -> SPOTIFY_CLIENT_SECRET
            else -> throw IllegalArgumentException("Implementation for the designated OAuth destination is not available")
        }
    }

    private fun saveToken(destination: String, accessToken: String, refreshToken: String = "") {
        when(destination) {
            GITHUB -> {
                Timber.tag("OAuthActivity").i("GitHub access token received: $accessToken")
                sharedPrefs.setGithubAccessToken(accessToken)
            }
            SPOTIFY -> {
                Timber.tag("OAuthActivity").i("Spotify access token received: $accessToken")
                sharedPrefs.setSpotifyAccessToken(accessToken)
                if(refreshToken.isNotBlank()) {
                    Timber.tag("OAuthActivity").i("Spotify refresh token received: $refreshToken")
                    sharedPrefs.setSpotifyRefreshToken(refreshToken)
                }
            }
            else -> throw IllegalArgumentException("Implementation for the designated OAuth destination is not available")
        }
    }
}