package com.scholr.app.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.scholr.app.data.SampleData
import com.scholr.app.data.model.Conference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val CONFERENCES = "conferences"

/**
 * Shared (non-user-specific) content: the conference/CFP list. Backed by
 * Firestore so it can be updated without an app release. The paper feed
 * itself comes from the live OpenAlex API (see [PaperApiRepository])
 * rather than from here — there's no equivalently good free API for
 * conference deadlines, so that stays a curated seed.
 */
class ContentRepository(private val firestore: FirebaseFirestore = FirebaseModule.firestore) {

    val conferences: Flow<List<Conference>> = callbackFlow {
        val registration = firestore.collection(CONFERENCES).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.documents?.mapNotNull { it.toConference() }.orEmpty())
        }
        awaitClose { registration.remove() }
    }

    /** One-time seed from the bundled sample content, called after the first sign-in. */
    suspend fun seedIfEmpty() {
        val existing = firestore.collection(CONFERENCES).limit(1).get().await()
        if (!existing.isEmpty) return

        val batch = firestore.batch()
        SampleData.conferences.forEach { conference ->
            batch.set(firestore.collection(CONFERENCES).document(conference.id), conference.toFirestoreMap())
        }
        batch.commit().await()
    }
}
