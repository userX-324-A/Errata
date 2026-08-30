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
            return GoogleLinkResult.Failed("auth")
        } catch (_: Exception) {
            return GoogleLinkResult.Failed("auth")
        }
        if (email.isBlank()) return GoogleLinkResult.Failed("auth")
        return authorizeDrive(activity, email)
    }

    fun completeLinkFromIntent(activity: Activity, email: String, data: Intent?): GoogleLinkResult {
        if (data == null) return GoogleLinkResult.Failed("auth")
        return try {
            val result = Identity.getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (token.isNullOrBlank()) {
                GoogleLinkResult.Failed("auth")
            } else if (!accountMatches(email, result.toGoogleSignInAccount()?.email)) {
                GoogleLinkResult.Failed("auth")
            } else {
                cachedAccessToken = token
                GoogleLinkResult.Linked(email)
            }
        } catch (e: Exception) {
            Log.w(TAG, "consent result", e)
            GoogleLinkResult.Failed("auth")
        }
    }

    suspend fun accessToken(context: Context, email: String? = null): String? {
        cachedAccessToken?.let { return it }
        if (!isConfigured() || !playServicesAvailable(context)) return null
        if (email.isNullOrBlank()) return null
        return try {
            val result = Identity.getAuthorizationClient(context)
                .authorize(driveAuthorizationRequest(email))
                .await()
            if (result.hasResolution()) return null
            if (!accountMatches(email, result.toGoogleSignInAccount()?.email)) return null
            result.accessToken?.also { cachedAccessToken = it }
        } catch (e: Exception) {
            Log.w(TAG, "access token", e)
            null
        }
    }

    suspend fun clearCredential(context: Context) {
        cachedAccessToken = null
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
                        ?: return GoogleLinkResult.Failed("auth")
                    GoogleLinkResult.NeedsConsent(sender, email)
                }
                result.accessToken.isNullOrBlank() -> GoogleLinkResult.Failed("auth")
                !accountMatches(email, result.toGoogleSignInAccount()?.email) ->
                    GoogleLinkResult.Failed("auth")
                else -> {
                    cachedAccessToken = result.accessToken
                    GoogleLinkResult.Linked(email)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "authorize drive", e)
            GoogleLinkResult.Failed("auth")
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
}
