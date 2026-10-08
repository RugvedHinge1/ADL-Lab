package com.scholr.app.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.scholr.app.data.model.PaperCollection
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val USERS = "users"

/**
 * Per-user data — profile, onboarding state, interests, bookmarks and
 * collections — stored as a single document at `users/{uid}` so it stays in
 * one snapshot listener and syncs across every device the user signs into.
 */
class UserDataRepository(private val firestore: FirebaseFirestore = FirebaseModule.firestore) {

    private fun userDoc(uid: String) = firestore.collection(USERS).document(uid)

    private fun userDocFlow(uid: String): Flow<DocumentSnapshot?> = callbackFlow {
        val registration = userDoc(uid).addSnapshotListener { snapshot, _ -> trySend(snapshot) }
        awaitClose { registration.remove() }
    }

    /** Called once right after sign-up to seed a fresh, empty profile. Bookmarks and
     *  collections start empty rather than pre-filled — the paper feed is live now, so
     *  there's no fixed set of ids that's guaranteed to still resolve later. */
    suspend fun initProfile(uid: String, name: String) {
        userDoc(uid).set(
            mapOf(
                "userName" to name,
                "onboardingComplete" to false,
                "selectedInterests" to listOf("cs", "neuro"),
                "bookmarkedIds" to emptyList<String>(),
                "collections" to emptyList<Map<String, Any?>>()
            )
        ).await()
    }

    fun bookmarkedIds(uid: String): Flow<List<String>> =
        userDocFlow(uid).map { snapshot ->
            (snapshot?.get("bookmarkedIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
        }

    suspend fun addBookmark(uid: String, paperId: String) {
        userDoc(uid).update("bookmarkedIds", FieldValue.arrayUnion(paperId)).await()
    }

    suspend fun removeBookmark(uid: String, paperId: String) {
        userDoc(uid).update("bookmarkedIds", FieldValue.arrayRemove(paperId)).await()
    }

    fun collections(uid: String): Flow<List<PaperCollection>> =
        userDocFlow(uid).map { snapshot ->
            @Suppress("UNCHECKED_CAST")
            (snapshot?.get("collections") as? List<Map<String, Any?>>)
                ?.mapNotNull { it.toPaperCollection() }.orEmpty()
        }

    suspend fun getUserName(uid: String): String =
        userDoc(uid).get().await().getString("userName") ?: "User"

    suspend fun setUserName(uid: String, name: String) {
        userDoc(uid).set(mapOf("userName" to name), SetOptions.merge()).await()
    }

    suspend fun isOnboardingComplete(uid: String): Boolean =
        userDoc(uid).get().await().getBoolean("onboardingComplete") ?: false

    suspend fun markOnboardingComplete(uid: String) {
        userDoc(uid).set(mapOf("onboardingComplete" to true), SetOptions.merge()).await()
    }

    suspend fun getSelectedInterests(uid: String): List<String> {
        val snapshot = userDoc(uid).get().await()
        return (snapshot.get("selectedInterests") as? List<*>)?.filterIsInstance<String>().orEmpty()
    }

    suspend fun saveSelectedInterests(uid: String, ids: List<String>) {
        userDoc(uid).set(mapOf("selectedInterests" to ids), SetOptions.merge()).await()
    }
}
