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
import androidx.appcompat.app.AppCompatActivity
import com.provigos.android.BuildConfig
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.util.PkceHelper
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import timber.log.Timber
import java.util.UUID

class OAuthActivity: AppCompatActivity() {

    companion object {
        private const val CALLBACK = "com.provigos.android://callback"

        private const val GITHUB = "github"
        private const val SPOTIFY = "spotify"

        private val AUTH_CONFIGS = mapOf(
            GITHUB to OAuthConfig(
                clientId = BuildConfig.GITHUB_CLIENT_ID,
                clientSecret = BuildConfig.GITHUB_CLIENT_SECRET,
                authUrl = "https://github.com/login/oauth/authorize",
                tokenUrl = "https://github.com/login/oauth/access_token",
                scopes = "repo read:org",
                pkce = false
            ),
            SPOTIFY to OAuthConfig(
                clientId = BuildConfig.SPOTIFY_CLIENT_ID,
                clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET,
                authUrl = "https://accounts.spotify.com/authorize",
                tokenUrl = "https://accounts.spotify.com/api/token",
                scopes = "user-top-read",
                pkce = true
            )
        )

        private const val ERROR_NO_IMPLEMENTATION = "Implementation for the designated OAuth destination is not available"
    }

    private val sharedPrefs = SharedPreferenceManager.get()
    private var authDestination: String? = null
    private lateinit var authService: AuthorizationService

    private var authFlowLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authService = AuthorizationService(this)

        authDestination = savedInstanceState?.getString("oauth_destination")
            ?: intent.extras?.getString("oauth")
            ?: sharedPrefs.getAuthDestination()

        if(authDestination.isNullOrEmpty()) {
            Timber.tag("OAuthActivity").e("No auth destination provided, finishing activity")
            finish()
            return
        }

        sharedPrefs.setAuthDestination(authDestination!!)

        if(intent.data != null) {
            handleIntent(intent)
        } else {
            startOAuthFlow()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("oauth_destination", authDestination)
    }

    override fun onResume() {
        super.onResume()
        if(authFlowLaunched) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        authFlowLaunched = false
        handleIntent(intent)
    }

    private fun startOAuthFlow() {
        val destination = authDestination ?: run {
            Timber.tag("OAuthActivity").e("Auth destination is null or empty")
            finish()
            return
        }
        val authConfig = AUTH_CONFIGS[destination] ?: run {
            Timber.tag("OAuthActivity").e("Invalid auth destination")
            finish()
            return
        }
        try {
            Timber.tag("OAuthActivity").d("Starting OAuth flow for $destination")

            val authRequest = buildAuthRequest(authConfig)
            val authIntent = authService.getAuthorizationRequestIntent(authRequest)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

            authFlowLaunched = true
            startActivity(authIntent)

        } catch (e: Exception) {
            Timber.tag("OAuthActivity").e("Error starting OAuth flow: ${e.message}")
            finish()
        }
    }

    private fun handleIntent(intent: Intent) {
        val destination = sharedPrefs.getAuthDestination()
        if(destination.isNullOrEmpty()) {
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
                    exchangeCodeForToken(destination, code)
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


        sharedPrefs.setCodeVerifier("")
        sharedPrefs.setState("")
        sharedPrefs.setAuthDestination("")

       startActivity(Intent(
           this@OAuthActivity,
           MainActivity::class.java
       ).apply {
           putExtra("open_settings", destination)
           putExtra("select_tab", 1)
           flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
       })
        finish()
    }

    private fun exchangeCodeForToken(destination: String, code: String) {
        Timber.tag("OAuthActivity").d("Exchanging code for token for $destination")

        val config = AUTH_CONFIGS[destination] ?: run {
            Timber.tag("OAuthActivity").d("Invalid destination $destination")
            return
        }

        try {
            val tokenRequestBuilder = TokenRequest.Builder(
                AuthorizationServiceConfiguration(
                    Uri.parse(config.authUrl),
                    Uri.parse(config.tokenUrl)
                ),
                config.clientId
            )
                .setGrantType("authorization_code")
                .setAuthorizationCode(code)
                .setRedirectUri(Uri.parse(CALLBACK))
                .setScopes(config.scopes)

            if(config.pkce) {
                val codeVerifier = sharedPrefs.getCodeVerifier()
                tokenRequestBuilder.setCodeVerifier(codeVerifier)
            } else {
                tokenRequestBuilder.setAdditionalParameters(
                    mapOf("client_secret" to config.clientSecret)
                )
            }

            val tokenRequest = tokenRequestBuilder.build()

            authService.performTokenRequest(tokenRequest) { resp, exception ->
                if (resp != null) {
                    handleTokenResponse(destination, resp)
                } else if (exception != null) {
                    Timber.tag("OAuthActivity").e("Token exchange failed: ${exception.errorDescription}")
                }
            }
        } catch (e: Exception) {
            Timber.tag("OAuthActivity").e("Error during token exchange")
        }
    }

    private fun handleTokenResponse(destination: String, response: TokenResponse) {
        try {
            val accessToken = response.accessToken ?: throw IllegalStateException("Access token is null")
            val refreshToken = response.refreshToken?: ""
            saveToken(destination, accessToken, refreshToken)
        } catch (e: Exception) {
            Timber.tag("OAuthActivity").e("Error handling token response")
        }
    }

    private fun buildAuthRequest(config: OAuthConfig): AuthorizationRequest {

        val state = UUID.randomUUID().toString()
        sharedPrefs.setState(state)

        Timber.tag("OAuthActivity").d("Generated and saved state: $state")

        val authRequest = AuthorizationRequest.Builder(
            AuthorizationServiceConfiguration(
                Uri.parse(config.authUrl),
                Uri.parse(config.tokenUrl)
            ),
            config.clientId,
            ResponseTypeValues.CODE,
            Uri.parse(CALLBACK)
        )
            .setScopes(config.scopes)
            .setState(state)

        if(config.pkce) {
            val codeVerifier = PkceHelper.generateCodeVerifier()
            val codeChallenge = PkceHelper.generateCodeChallenge(codeVerifier)
            sharedPrefs.setCodeVerifier(codeVerifier)

            authRequest.setCodeVerifier(codeVerifier, codeChallenge, AuthorizationRequest.CODE_CHALLENGE_METHOD_S256)
        }

        return authRequest.build()
    }

    fun refreshToken(destination: String) {

        val authConfig = AUTH_CONFIGS[destination] ?: run {
            Timber.tag("OAuthActivity").e("Invalid auth destination for refresh token $destination")
            return
        }

        val refreshToken = when (destination) {
            SPOTIFY -> sharedPrefs.getSpotifyRefreshToken()
            else -> throw IllegalArgumentException("Implementation for the designated OAuth destination is not available")
        }

        if(refreshToken.isNullOrEmpty()) {
            Timber.tag("OAuthActivity").e("No refresh token found, re-authenticate")
            return
        }

        try {

            val tokenRequestBuilder = TokenRequest.Builder(
                AuthorizationServiceConfiguration(
                    Uri.parse(authConfig.authUrl),
                    Uri.parse(authConfig.tokenUrl)
                ),
                authConfig.clientId
            )
                .setGrantType("refresh_token")
                .setRefreshToken(refreshToken)
                .setScopes(authConfig.scopes)

            tokenRequestBuilder.setAdditionalParameters(mapOf("client_secret" to authConfig.clientSecret))

            val tokenRequest = tokenRequestBuilder.build()

            authService.performTokenRequest(tokenRequest) { response, exception ->
                if (response != null) {

                    handleTokenResponse(destination, response)
                } else if (exception != null) {
                    Timber.tag("OAuthActivity").e("Token refresh failed for $destination: ${exception.errorDescription}")
                }
            }
        } catch (e: Exception) {
            Timber.tag("OAuthActivity").e("Error during $destination token refresh: ${e.message}")
        }
    }

    private fun saveToken(destination: String, accessToken: String, refreshToken: String = "") {
        when(destination) {
            GITHUB -> {
                Timber.tag("OAuthActivity").i("GitHub access token received: $accessToken")
                sharedPrefs.setGithubAccessToken(accessToken)
                sharedPrefs.setGithubUser(true)
            }
            SPOTIFY -> {
                Timber.tag("OAuthActivity").i("Spotify access token received: $accessToken")
                sharedPrefs.setSpotifyAccessToken(accessToken)
                sharedPrefs.setSpotifyTokenExpiration(System.currentTimeMillis() + (3600 * 1000))
                sharedPrefs.setSpotifyUser(true)
                if(refreshToken.isNotBlank()) {
                    Timber.tag("OAuthActivity").i("Spotify refresh token received: $refreshToken")
                    sharedPrefs.setSpotifyRefreshToken(refreshToken)
                    sharedPrefs.setSpotifyUser(true)
                }
            }
            else -> throw IllegalArgumentException(ERROR_NO_IMPLEMENTATION)
        }
    }

    private data class OAuthConfig(
        val clientId: String,
        val clientSecret: String,
        val authUrl: String,
        val tokenUrl: String,
        val scopes: String,
        val pkce: Boolean
    )
}