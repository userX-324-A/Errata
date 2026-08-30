package com.errata.app.sync

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.errata.app.BuildConfig
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.tasks.await

sealed class GoogleLinkResult {
    data class Linked(val email: String) : GoogleLinkResult()
    data class NeedsConsent(val sender: IntentSender, val email: String) : GoogleLinkResult()
    data object Cancelled : GoogleLinkResult()
    data class Failed(val reason: String) : GoogleLinkResult()
}

object GoogleAuth {
    const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val TAG = "ErrataSync"
    private const val GOOGLE_ACCOUNT_TYPE = "com.google"

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedAtElapsedMs: Long = 0

    fun webClientId(): String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    fun isConfigured(): Boolean = webClientId().isNotBlank()

    fun playServicesAvailable(context: Context): Boolean =
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    suspend fun beginLink(activity: Activity): GoogleLinkResult {
        if (!isConfigured()) return GoogleLinkResult.Failed("not_configured")
        if (!playServicesAvailable(activity)) return GoogleLinkResult.Failed("play_services")
        val email = try {
            signInEmail(activity)
        } catch (_: GetCredentialCancellationException) {
            return GoogleLinkResult.Cancelled
        } catch (_: GetCredentialException) {
            return GoogleLinkResult.Failed("sign_in")
        } catch (_: Exception) {
            return GoogleLinkResult.Failed("sign_in")
        }
        if (email.isBlank()) return GoogleLinkResult.Failed("sign_in")
        return authorizeDrive(activity, email)
    }

    fun completeLinkFromIntent(activity: Activity, email: String, data: Intent?): GoogleLinkResult {
        if (data == null) return GoogleLinkResult.Failed("sign_in")
        return try {
            val result = Identity.getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (token.isNullOrBlank()) {
                GoogleLinkResult.Failed("sign_in")
            } else if (!accountMatches(email, result.toGoogleSignInAccount()?.email)) {
                GoogleLinkResult.Failed("sign_in")
            } else {
                rememberAccessToken(token)
                GoogleLinkResult.Linked(email)
            }
        } catch (e: Exception) {
            Log.w(TAG, "consent result", e)
            GoogleLinkResult.Failed("sign_in")
        }
    }

    suspend fun accessToken(context: Context, email: String? = null): String? {
        val cached = cachedAccessToken
        if (
            cached != null &&
            AccessTokenCache.isFresh(android.os.SystemClock.elapsedRealtime(), cachedAtElapsedMs)
        ) {
            return cached
        }
        cachedAccessToken = null
        if (!isConfigured() || !playServicesAvailable(context)) return null
        if (email.isNullOrBlank()) return null
        return try {
            val result = Identity.getAuthorizationClient(context)
                .authorize(driveAuthorizationRequest(email))
                .await()
            if (result.hasResolution()) return null
            if (!accountMatches(email, result.toGoogleSignInAccount()?.email)) return null
            result.accessToken?.also { rememberAccessToken(it) }
        } catch (e: Exception) {
            Log.w(TAG, "access token", e)
            null
        }
    }

    fun clearAccessToken() {
        cachedAccessToken = null
        cachedAtElapsedMs = 0L
    }

    suspend fun clearCredential(context: Context) {
        clearAccessToken()
        runCatching {
            CredentialManager.create(context)
                .clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private suspend fun signInEmail(activity: Activity): String {
        val option = GetSignInWithGoogleOption.Builder(webClientId()).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val credential = CredentialManager.create(activity)
            .getCredential(activity, request)
            .credential
        val googleId = if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenCredential.createFrom(credential.data)
        } else {
            throw IllegalStateException("auth")
        }
        return googleId.id
    }

    private suspend fun authorizeDrive(activity: Activity, email: String): GoogleLinkResult {
        return try {
            val result = Identity.getAuthorizationClient(activity)
                .authorize(driveAuthorizationRequest(email))
                .await()
            when {
                result.hasResolution() -> {
                    val sender = result.pendingIntent?.intentSender
                        ?: return GoogleLinkResult.Failed("sign_in")
                    GoogleLinkResult.NeedsConsent(sender, email)
                }
                result.accessToken.isNullOrBlank() -> GoogleLinkResult.Failed("sign_in")
                !accountMatches(email, result.toGoogleSignInAccount()?.email) ->
                    GoogleLinkResult.Failed("sign_in")
                else -> {
                    rememberAccessToken(result.accessToken)
                    GoogleLinkResult.Linked(email)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "authorize drive", e)
            GoogleLinkResult.Failed("sign_in")
        }
    }

    internal fun driveAuthorizationRequest(email: String): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .setAccount(Account(email, GOOGLE_ACCOUNT_TYPE))
            .build()

    internal fun accountMatches(expected: String, authorized: String?): Boolean {
        if (authorized.isNullOrBlank()) return true
        return expected.equals(authorized, ignoreCase = true)
    }

    private fun rememberAccessToken(token: String?) {
        cachedAccessToken = token
        cachedAtElapsedMs = if (token.isNullOrBlank()) 0L else android.os.SystemClock.elapsedRealtime()
    }
}

internal object AccessTokenCache {
    const val TTL_MS = 50L * 60L * 1000L

    fun isFresh(nowElapsedMs: Long, cachedAtElapsedMs: Long): Boolean {
        val age = nowElapsedMs - cachedAtElapsedMs
        return age in 0 until TTL_MS
    }
}
