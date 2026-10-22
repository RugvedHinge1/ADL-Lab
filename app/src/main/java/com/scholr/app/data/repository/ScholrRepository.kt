package com.scholr.app.data.repository

import com.google.firebase.auth.FirebaseUser
import com.scholr.app.data.db.ScholrDatabase
import com.scholr.app.data.db.toDomain
import com.scholr.app.data.db.toEntity
import com.scholr.app.data.firebase.AuthRepository
import com.scholr.app.data.conferences.ConferenceRepository
import com.scholr.app.data.geo.LatLng
import com.scholr.app.data.geo.PlaceGeocoder
import com.scholr.app.data.firebase.UserDataRepository
import com.scholr.app.data.model.ChatMessage
import com.scholr.app.data.model.Conference
import com.scholr.app.data.model.Paper
import com.scholr.app.data.model.PaperCollection
import com.scholr.app.data.network.GeminiRepository
import com.scholr.app.data.network.PaperApiRepository
import com.scholr.app.data.recommend.RecommendationEngine
import com.scholr.app.data.alerts.AlertEngine
import com.scholr.app.data.alerts.AlertNotifier
import com.scholr.app.data.alerts.AlertStore
import com.scholr.app.data.alerts.AppAlert
import com.scholr.app.data.scan.Book
import com.scholr.app.data.scan.BookRepository
import java.util.UUID
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
    private val conferenceSource: ConferenceRepository,
    private val geocoder: PlaceGeocoder,
    private val userData: UserDataRepository,
    private val paperApi: PaperApiRepository,
    private val gemini: GeminiRepository,
    private val books: BookRepository = BookRepository(),
    val alertStore: AlertStore,
    private val alertEngine: AlertEngine,
    val notifier: AlertNotifier
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

    private val recommender = RecommendationEngine(paperApi)

    /** Embedding-based "For you" feed — see [RecommendationEngine]. */
    suspend fun recommendPapers(
        interestLabels: List<String>,
        bookmarkedIds: List<String>,
        limit: Int = 30
    ): List<Paper> = recommender.recommend(interestLabels, bookmarkedIds, limit)

    suspend fun searchPapers(query: String, limit: Int = 25): List<Paper> = paperApi.search(query, limit)

    suspend fun paperById(id: String): Paper? = paperApi.getPaper(id)

    suspend fun paperByExternalId(lookupId: String): Paper? = paperApi.getPaperByExternalId(lookupId)

    suspend fun papersByIds(ids: List<String>): List<Paper> = paperApi.papersByIds(ids)

    suspend fun lookupBook(isbn13: String): Book? = books.lookup(isbn13)

    // ── Conferences (live: CCF Deadlines + WikiCFP, geocoded on-device) ─────

    suspend fun loadConferences(forceRefresh: Boolean = false): List<Conference> =
        conferenceSource.load(forceRefresh)

    suspend fun loadCountryConferences(country: String): List<Conference> =
        conferenceSource.loadCountry(country)

    suspend fun geocode(place: String, allowFallback: Boolean = true): LatLng? =
        geocoder.resolve(place, allowFallback)

    suspend fun countryOf(point: LatLng): String? = geocoder.countryOf(point)

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

    suspend fun createCollection(name: String, emoji: String, paperIds: List<String>) {
        auth.currentUser?.let {
            userData.addCollection(it.uid, PaperCollection(UUID.randomUUID().toString(), name, paperIds, emoji))
        }
    }

    // ── Alerts (conference deadlines + AI nudges) ────────────────────────────

    suspend fun refreshAlerts(
        interestLabels: List<String>,
        savedTitles: List<String>,
        paperHints: List<AlertEngine.PaperHint>,
        force: Boolean
    ): List<AppAlert> = alertEngine.refresh(interestLabels, savedTitles, paperHints, force)

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
