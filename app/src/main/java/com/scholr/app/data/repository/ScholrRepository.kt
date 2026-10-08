package com.scholr.app.data.repository

import com.google.firebase.auth.FirebaseUser
import com.scholr.app.data.db.ScholrDatabase
import com.scholr.app.data.db.toDomain
import com.scholr.app.data.db.toEntity
import com.scholr.app.data.firebase.AuthRepository
import com.scholr.app.data.firebase.ContentRepository
import com.scholr.app.data.firebase.UserDataRepository
import com.scholr.app.data.model.ChatMessage
import com.scholr.app.data.model.Conference
import com.scholr.app.data.model.Paper
import com.scholr.app.data.model.PaperCollection
import com.scholr.app.data.network.GeminiRepository
import com.scholr.app.data.network.PaperApiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Per-user data (bookmarks, collections, profile) and the conference list are
 * backed by Firebase — Firestore for storage, Auth for identity. The paper
 * feed is live, pulled from the OpenAlex API on demand rather than
 * cached in Firestore. The AI-chat scratchpad stays local-only in Room, but
 * answers themselves come from a real LLM via Gemini.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScholrRepository(
    private val db: ScholrDatabase,
    private val auth: AuthRepository,
    private val content: ContentRepository,
    private val userData: UserDataRepository,
    private val paperApi: PaperApiRepository,
    private val gemini: GeminiRepository
) {

    // ── Auth ──────────────────────────────────────────────────────────────────

    val authState: Flow<FirebaseUser?> = auth.authState
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signUp(name: String, email: String, password: String) {
        val user = auth.signUp(name, email, password)
        userData.initProfile(user.uid, name)
    }

    suspend fun signIn(email: String, password: String) {
        auth.signIn(email, password)
    }

    /** Returns true if this Google account just created a Scholr account for the first time. */
    suspend fun signInWithGoogle(idToken: String): Boolean {
        val result = auth.signInWithGoogle(idToken)
        if (result.isNewUser) {
            userData.initProfile(result.user.uid, result.user.displayName ?: "User")
        }
        return result.isNewUser
    }

    fun signOut() = auth.signOut()

    // ── Papers (live) ────────────────────────────────────────────────────────

    suspend fun searchPapers(query: String, limit: Int = 25): List<Paper> = paperApi.search(query, limit)

    suspend fun paperById(id: String): Paper? = paperApi.getPaper(id)

    // ── Conferences (Firestore, curated) ────────────────────────────────────

    val conferences: Flow<List<Conference>> = content.conferences

    suspend fun seedContentIfEmpty() = content.seedIfEmpty()

    // ── Bookmarks ─────────────────────────────────────────────────────────────

    val bookmarkedIds: Flow<List<String>> = auth.authState.flatMapLatest { user ->
        if (user != null) userData.bookmarkedIds(user.uid) else flowOf(emptyList<String>())
    }

    suspend fun addBookmark(paperId: String) {
        auth.currentUser?.let { userData.addBookmark(it.uid, paperId) }
    }

    suspend fun removeBookmark(paperId: String) {
        auth.currentUser?.let { userData.removeBookmark(it.uid, paperId) }
    }

    // ── Collections ───────────────────────────────────────────────────────────

    val collections: Flow<List<PaperCollection>> = auth.authState.flatMapLatest { user ->
        if (user != null) userData.collections(user.uid) else flowOf(emptyList<PaperCollection>())
    }

    // ── Chat (local history in Room, live answers from Gemini) ──────────────

    fun chatMessages(paperId: String): Flow<List<ChatMessage>> =
        db.chatMessageDao().observeForPaper(paperId)
            .map { list -> list.map { it.toDomain() } }

    suspend fun saveChatMessage(paperId: String, message: ChatMessage) =
        db.chatMessageDao().insert(message.toEntity(paperId))

    suspend fun clearChat(paperId: String) =
        db.chatMessageDao().clearForPaper(paperId)

    suspend fun askAi(paper: Paper, history: List<ChatMessage>, question: String): String =
        gemini.ask(paper, history, question)

    // ── User profile / onboarding / interests ────────────────────────────────

    suspend fun getUserName(): String =
        auth.currentUser?.let { userData.getUserName(it.uid) } ?: "User"

    suspend fun setUserName(name: String) {
        auth.currentUser?.let { userData.setUserName(it.uid, name) }
    }

    suspend fun isOnboardingComplete(): Boolean =
        auth.currentUser?.let { userData.isOnboardingComplete(it.uid) } ?: false

    suspend fun markOnboardingComplete() {
        auth.currentUser?.let { userData.markOnboardingComplete(it.uid) }
    }

    suspend fun getSelectedInterests(): List<String> =
        auth.currentUser?.let { userData.getSelectedInterests(it.uid) } ?: emptyList()

    suspend fun saveSelectedInterests(ids: List<String>) {
        auth.currentUser?.let { userData.saveSelectedInterests(it.uid, ids) }
    }
}
