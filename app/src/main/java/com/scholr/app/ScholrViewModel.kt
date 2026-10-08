package com.scholr.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scholr.app.data.SampleData
import com.scholr.app.data.model.ChatMessage
import com.scholr.app.data.model.Conference
import com.scholr.app.data.model.Interest
import com.scholr.app.data.model.Paper
import com.scholr.app.data.model.PaperCollection
import com.scholr.app.data.repository.ScholrRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ScholrViewModel(private val repo: ScholrRepository) : ViewModel() {

    // ── Auth state ───────────────────────────────────────────────────────────

    var currentUser by mutableStateOf(repo.currentUser)
        private set

    val isSignedIn: Boolean get() = currentUser != null

    var authLoading by mutableStateOf(false)
        private set

    var authError by mutableStateOf<String?>(null)
        private set

    // ── Persistent state ────────────────────────────────────────────────────

    /** Papers currently loaded (feed or search) — backed live by the paper API. */
    var papers by mutableStateOf<List<Paper>>(emptyList())
        private set

    var papersLoading by mutableStateOf(false)
        private set

    var papersError by mutableStateOf<String?>(null)
        private set

    var conferences by mutableStateOf<List<Conference>>(emptyList())
        private set

    val interests: List<Interest> = SampleData.interests

    var bookmarkedIds by mutableStateOf<List<String>>(emptyList())
        private set

    var collections by mutableStateOf<List<PaperCollection>>(emptyList())
        private set

    var userName by mutableStateOf("")
        private set

    var onboardingComplete by mutableStateOf(false)
        private set

    var selectedInterests by mutableStateOf<List<String>>(emptyList())
        private set

    /** Every paper we've ever loaded (feed, search, or by-id), keyed by id, so a
     *  paper found once (e.g. bookmarked) stays resolvable even after the feed
     *  query changes and no longer includes it. */
    private val paperCache = mutableStateMapOf<String, Paper>()

    // ── UI-only state ─────────────────────────────────────────────────────────

    var activeFilter by mutableStateOf(SampleData.quickFilters.first())
        private set

    private var _searchQuery by mutableStateOf("")
    private val searchQueryFlow = MutableStateFlow("")

    var searchQuery: String
        get() = _searchQuery
        set(value) {
            _searchQuery = value
            searchQueryFlow.value = value
        }

    var searchResults by mutableStateOf<List<Paper>>(emptyList())
        private set

    var searchLoading by mutableStateOf(false)
        private set

    var searchError by mutableStateOf<String?>(null)
        private set

    var aiThinking by mutableStateOf(false)
        private set

    var unreadNotifications by mutableStateOf(3)
        private set

    // ── Init ─────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            repo.authState.collect { user ->
                currentUser = user
                if (user != null) {
                    repo.seedContentIfEmpty()
                    userName           = repo.getUserName()
                    onboardingComplete = repo.isOnboardingComplete()
                    selectedInterests  = repo.getSelectedInterests()
                    refreshFeed()
                }
            }
        }
        viewModelScope.launch { repo.conferences.collect { conferences = it } }
        viewModelScope.launch {
            repo.bookmarkedIds.collect { ids ->
                bookmarkedIds = ids
                loadMissingBookmarks(ids)
            }
        }
        viewModelScope.launch { repo.collections.collect { collections = it } }

        viewModelScope.launch {
            searchQueryFlow
                .debounce(400)
                .distinctUntilChanged()
                .collectLatest { query -> runSearch(query) }
        }
    }

    // ── Auth actions ─────────────────────────────────────────────────────────

    fun signUp(name: String, email: String, password: String, onSuccess: () -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            authError = "Please fill in every field."
            return
        }
        viewModelScope.launch {
            authLoading = true
            authError = null
            try {
                repo.signUp(name.trim(), email.trim(), password)
                userName = name.trim()
                onSuccess()
            } catch (e: Exception) {
                authError = e.message ?: "Sign up failed."
            } finally {
                authLoading = false
            }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            authError = "Please fill in every field."
            return
        }
        viewModelScope.launch {
            authLoading = true
            authError = null
            try {
                repo.signIn(email.trim(), password)
                onSuccess()
            } catch (e: Exception) {
                authError = e.message ?: "Sign in failed."
            } finally {
                authLoading = false
            }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: (isNewUser: Boolean) -> Unit) {
        viewModelScope.launch {
            authLoading = true
            authError = null
            try {
                val isNewUser = repo.signInWithGoogle(idToken)
                onSuccess(isNewUser)
            } catch (e: Exception) {
                authError = e.message ?: "Google sign-in failed."
            } finally {
                authLoading = false
            }
        }
    }

    fun signOut() {
        repo.signOut()
    }

    fun clearAuthError() {
        authError = null
    }

    // ── Onboarding ───────────────────────────────────────────────────────────

    fun completeOnboarding() {
        onboardingComplete = true
        viewModelScope.launch { repo.markOnboardingComplete() }
    }

    fun updateUserName(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.setUserName(name.trim())
            userName = name.trim()
        }
    }

    // ── Interests ────────────────────────────────────────────────────────────

    val canContinueFromInterests: Boolean
        get() = selectedInterests.size >= 3

    fun toggleInterest(id: String) {
        val updated = if (selectedInterests.contains(id))
            selectedInterests - id
        else
            selectedInterests + id
        selectedInterests = updated
        viewModelScope.launch { repo.saveSelectedInterests(updated) }
    }

    // ── Feed (live) ──────────────────────────────────────────────────────────

    fun setFilter(filter: String) { activeFilter = filter }

    /** Called on Home mount, pull-to-refresh, and after interests change. */
    fun refreshFeed() {
        viewModelScope.launch {
            papersLoading = true
            papersError = null
            try {
                val results = repo.searchPapers(feedQuery(), limit = 30)
                cache(results)
                papers = results
            } catch (e: Exception) {
                papersError = e.message ?: "Couldn't load papers — check your connection and try again."
            } finally {
                papersLoading = false
            }
        }
    }

    /**
     * "For you" was just searching a literal join of your selected interest labels —
     * two people who picked the same interests got byte-identical feeds, forever.
     * This weights what you've actually bookmarked (a real per-user signal) above
     * your static onboarding picks, so the feed diverges as soon as usage does.
     * Interests alone are still the fallback for a brand-new account with no
     * bookmarks yet — that's the best any feed can do with zero behavioral data.
     */
    private fun feedQuery(): String {
        val interestLabels = SampleData.interests.filter { it.id in selectedInterests }.map { it.label }

        val bookmarkTopics = bookmarkedIds
            .mapNotNull { paperCache[it] }
            .flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        val combined = (bookmarkTopics + interestLabels).distinct()
        return combined.joinToString(" ").ifBlank { "artificial intelligence" }
    }

    val feed: List<Paper>
        get() = when (activeFilter) {
            "Trending"      -> papers.sortedByDescending { it.citations }
            "New this week" -> papers.sortedByDescending { it.year }
            "Highly cited"  -> papers.filter { it.citations > 200 }.ifEmpty { papers }
            "Open access"   -> papers.filter { it.openAccess }
            "Preprints"     -> papers.sortedBy { it.readMinutes }
            else            -> papers
        }

    // ── Paper lookup (for detail screens / bookmarks not in the current feed) ──

    private fun cache(list: List<Paper>) {
        list.forEach { paperCache[it.id] = it }
    }

    private fun loadMissingBookmarks(ids: List<String>) {
        val missing = ids.filter { it !in paperCache }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            missing.forEach { id ->
                repo.paperById(id)?.let { paperCache[it.id] = it }
            }
        }
    }

    fun paperFor(id: String): Paper? = paperCache[id]

    fun ensurePaperLoaded(id: String) {
        if (paperCache.containsKey(id)) return
        viewModelScope.launch {
            repo.paperById(id)?.let { paperCache[it.id] = it }
        }
    }

    // ── Bookmarks ────────────────────────────────────────────────────────────

    fun isBookmarked(id: String) = bookmarkedIds.contains(id)

    fun toggleBookmark(id: String) {
        viewModelScope.launch {
            if (isBookmarked(id)) repo.removeBookmark(id) else repo.addBookmark(id)
        }
    }

    val bookmarkedPapers: List<Paper>
        get() = bookmarkedIds.mapNotNull { paperCache[it] }

    // ── Search (live) ────────────────────────────────────────────────────────

    private suspend fun runSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            searchLoading = false
            searchError = null
            return
        }
        searchLoading = true
        searchError = null
        searchResults = try {
            repo.searchPapers(query, limit = 25).also { cache(it) }
        } catch (e: Exception) {
            searchError = e.message ?: "Couldn't reach the paper search service — try again in a moment."
            emptyList()
        }
        searchLoading = false
    }

    // ── AI chat (real answers via Gemini) ────────────────────────────────────

    // In-memory cache of per-paper SnapshotStateLists so the UI gets
    // instant recomposition. Each list is also synced to the DB.
    private val chatCache = mutableStateMapOf<String, SnapshotStateList<ChatMessage>>()
    private var msgIdCounter = 0L
    private fun nextId() = msgIdCounter++

    fun chatFor(paperId: String): SnapshotStateList<ChatMessage> {
        return chatCache.getOrPut(paperId) {
            val list = mutableStateListOf<ChatMessage>()
            viewModelScope.launch {
                // Load persisted messages first
                repo.chatMessages(paperId).collect { saved ->
                    if (list.isEmpty() && saved.isEmpty()) {
                        // Fresh paper — insert welcome message
                        val welcome = ChatMessage(
                            id       = nextId(),
                            text     = "I've read the abstract. Ask me anything — methodology, " +
                                    "limitations, how it relates to what you've saved, or just " +
                                    "\"explain it like I'm new to the field\".",
                            fromUser = false
                        )
                        repo.saveChatMessage(paperId, welcome)
                        list.add(welcome)
                    } else if (list.isEmpty()) {
                        list.addAll(saved)
                    }
                }
            }
            list
        }
    }

    fun askAi(paper: Paper, question: String) {
        if (question.isBlank() || aiThinking) return
        val thread = chatFor(paper.id)
        val history = thread.toList()
        val userMsg = ChatMessage(nextId(), question.trim(), fromUser = true)
        thread.add(userMsg)

        viewModelScope.launch {
            repo.saveChatMessage(paper.id, userMsg)
            aiThinking = true
            val answer = repo.askAi(paper, history, question.trim())
            val reply = ChatMessage(nextId(), answer, fromUser = false)
            thread.add(reply)
            repo.saveChatMessage(paper.id, reply)
            aiThinking = false
        }
    }

    fun suggestedPrompts(paper: Paper) = listOf(
        "Explain the method simply",
        "What are the limitations?",
        "How does it compare to ${paper.field} work I saved?",
        "Give me the one-line takeaway"
    )

    // ── Notifications ────────────────────────────────────────────────────────

    fun clearNotifications() { unreadNotifications = 0 }

    // ── Factory ──────────────────────────────────────────────────────────────

    companion object {
        fun factory(repo: ScholrRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ScholrViewModel(repo) as T
            }
    }
}
