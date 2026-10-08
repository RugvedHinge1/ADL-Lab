package com.scholr.app.data.firebase

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.scholr.app.R

/**
 * Launches the system Google account picker via Credential Manager and hands
 * back a Google ID token, which [AuthRepository.signInWithGoogle] exchanges
 * for a Firebase session. [context] must be an Activity context — Credential
 * Manager needs it to host the picker UI.
 */
class GoogleSignInHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun requestIdToken(): String {
        val option = GetSignInWithGoogleOption.Builder(
            serverClientId = context.getString(R.string.default_web_client_id)
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = credentialManager.getCredential(context, request)
        val credential = response.credential
        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Unexpected credential type returned by Credential Manager" }

        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
