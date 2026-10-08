package com.scholr.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Result of a Google sign-in: the Firebase user, plus whether this was their first sign-in. */
data class GoogleSignInResult(val user: FirebaseUser, val isNewUser: Boolean)

/** Thin wrapper around Firebase Auth (email/password + Google). */
class AuthRepository(private val auth: FirebaseAuth = FirebaseModule.auth) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Emits the current user whenever sign-in state changes, starting with the value at subscription time. */
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(name: String, email: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("Sign up failed — no user returned")
        user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        return user
    }

    suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: error("Sign in failed — no user returned")
    }

    suspend fun signInWithGoogle(idToken: String): GoogleSignInResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: error("Google sign-in failed — no user returned")
        return GoogleSignInResult(user, result.additionalUserInfo?.isNewUser ?: false)
    }

    fun signOut() = auth.signOut()
}
