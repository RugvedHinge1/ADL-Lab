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
import com.scholr.app.data.geo.LatLng
import com.scholr.app.data.geo.PlaceGeocoder
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.scholr.app.data.model.ChatMessage
import com.scholr.app.data.model.Conference
import com.scholr.app.data.model.Interest
import com.scholr.app.data.model.Paper
import com.scholr.app.data.model.PaperCollection
import com.scholr.app.data.alerts.AlertEngine
import com.scholr.app.data.alerts.AppAlert
import com.scholr.app.data.repository.ScholrRepository
import com.scholr.app.data.scan.ScanCodec
import com.scholr.app.data.scan.ScanTarget
import com.scholr.app.data.scan.ScanUiState
import kotlinx.coroutines.CancellationException
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

    var conferencesLoading by mutableStateOf(false)
        private set

    var conferencesError by mutableStateOf<String?>(null)
        private set

    /** Where the user is, once they have granted location access. */
    var userLocation by mutableStateOf<LatLng?>(null)
        private set

    /** Search terms the user has actually run this session, newest first. */
    val recentSearches = mutableStateListOf<String>()

    /** Venue strings the geocoders could not place; excluded from the "pending" count. */
    private var unplaceable by mutableStateOf<Set<String>>(emptySet())

    /** Conferences whose venue is still waiting to be placed on the map. */
    val pendingVenueCount: Int
        get() = conferences.count { !it.hasLocation && it.hasPlaceName && it.location !in unplaceable }

    private var userCountry: String? = null
    private val GEOCODE_PARALLELISM = 6
    private var geocodeJob: Job? = null

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

    // ── Alerts state (see "Alerts" section below) ────────────────────────────

    var alerts by mutableStateOf<List<AppAlert>>(emptyList())
        private set

    var alertsLoading by mutableStateOf(false)
        private set

    private var readAlertIds by mutableStateOf(repo.alertStore.readIds)

    val unreadAlertCount: Int get() = alerts.count { it.id !in readAlertIds }

    var pushEnabled by mutableStateOf(repo.alertStore.pushEnabled)
        private set

    /** Set when the user tapped a system notification; the nav host consumes it. */
    var openAlertsRequested by mutableStateOf(false)
        private set

    // ── Init ─────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            repo.authState.collect { user ->
                currentUser = user
                if (user != null) {
                    userName           = repo.getUserName()
                    onboardingComplete = repo.isOnboardingComplete()
                    selectedInterests  = repo.getSelectedInterests()
                    refreshFeed()
                }
            }
        }
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

    // ── Conferences (live) ───────────────────────────────────────────────────

    fun loadConferences(forceRefresh: Boolean = false) {
        if (conferencesLoading) return
        if (conferences.isNotEmpty() && !forceRefresh) {
            startGeocoding()
            return
        }
        viewModelScope.launch {
            conferencesLoading = true
            conferencesError = null
            try {
                conferences = repo.loadConferences(forceRefresh)
                startGeocoding()
            } catch (e: Exception) {
                conferencesError = "Couldn't load conferences \u2014 check your connection and try again."
            } finally {
                conferencesLoading = false
            }
        }
    }

    fun updateUserLocation(location: LatLng?) {
        userLocation = location
        if (location == null) return
        viewModelScope.launch {
            userCountry = repo.countryOf(location)
            // Pull in the venue list for the user's own country so there is
            // genuinely something nearby to show, then place those venues first.
            userCountry?.let { country ->
                val local = runCatching { repo.loadCountryConferences(country) }.getOrDefault(emptyList())
                if (local.isNotEmpty()) {
                    val known = conferences.mapTo(HashSet()) { it.id }
                    conferences = (conferences + local.filter { it.id !in known }).sortedBy { it.startEpochDay }
                }
            }
            startGeocoding()
        }
    }

    /**
     * Resolves venue names to coordinates in the background, starting with
     * venues in the user's own country so "nearby" fills in first. Lookups are
     * cached on-device, so this is instant after the first run.
     */
    private fun startGeocoding() {
        geocodeJob?.cancel()
        val country = userCountry?.let(PlaceGeocoder::normalizeCountry)
        val places = conferences
            .filter { !it.hasLocation && it.hasPlaceName && it.location !in unplaceable }
            .map { it.location }
            .distinct()
            .sortedByDescending { place ->
                country != null && PlaceGeocoder.normalizeCountry(place.substringAfterLast(',')) == country
            }
        if (places.isEmpty()) return

        geocodeJob = viewModelScope.launch {
            val resolved = HashMap<String, LatLng>()
            val needsFallback = ArrayList<String>()

            // Pass 1: fast Open-Meteo lookups, several at a time, nearest country first.
            for (batch in places.chunked(GEOCODE_PARALLELISM)) {
                val results = batch.map { place -> async { place to repo.geocode(place, allowFallback = false) } }
                    .awaitAll()
                for ((place, point) in results) {
                    if (point != null) resolved[place] = point else needsFallback += place
                }
                applyResolved(resolved)
            }

            // Pass 2: slower rate-limited OpenStreetMap lookups for what is left.
            for (place in needsFallback) {
                val point = repo.geocode(place)
                if (point == null) {
                    unplaceable = unplaceable + place
                } else {
                    resolved[place] = point
                    applyResolved(resolved)
                }
            }
        }
    }

    private fun applyResolved(resolved: Map<String, LatLng>) {
        conferences = conferences.map { c ->
            val p = resolved[c.location]
            if (p != null && !c.hasLocation) c.copy(lat = p.lat, lng = p.lng) else c
        }
    }

    // ── Feed (live) ──────────────────────────────────────────────────────────

    fun setFilter(filter: String) { activeFilter = filter }

    /** Called on Home mount, pull-to-refresh, and after interests change. */
    fun refreshFeed() {
        viewModelScope.launch {
            papersLoading = true
            papersError = null
            try {
                val interestLabels = SampleData.interests
                    .filter { it.id in selectedInterests }.map { it.label }

                // Phase 1 — a quick keyword search so the feed is never an empty,
                // unscrollable screen while the (rate-limited) ranking runs.
                if (papers.isEmpty()) {
                    runCatching { repo.searchPapers(feedQuery(), limit = 30) }
                        .getOrNull()?.takeIf { it.isNotEmpty() }
                        ?.let { cache(it); papers = it; papersLoading = false }
                }

                // Phase 2 — embedding-ranked recommendations replace it when ready.
                val ranked = runCatching { repo.recommendPapers(interestLabels, bookmarkedIds) }
                    .getOrDefault(emptyList())
                if (ranked.isNotEmpty()) {
                    cache(ranked)
                    papers = ranked
                } else if (papers.isEmpty()) {
                    error("No papers returned")
                }
            } catch (e: Exception) {
                papersError = e.message ?: "Couldn't load papers — check your connection and try again."
            } finally {
                papersLoading = false
            }
            loadAlerts()
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

    private fun rememberSearch(query: String) {
        val q = query.trim()
        recentSearches.remove(q)
        recentSearches.add(0, q)
        while (recentSearches.size > 6) recentSearches.removeAt(recentSearches.lastIndex)
    }

    fun clearRecentSearches() = recentSearches.clear()

    /** Most common topics across the papers currently in the feed. */
    val trendingTopics: List<String>
        get() = papers.flatMap { it.tags }
            .filter { it != "General" }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(5).map { it.key }

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
            repo.searchPapers(query, limit = 25).also {
                cache(it)
                rememberSearch(query)
            }
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

    // ── Scanner (QR codes + book barcodes) ───────────────────────────────────

    var scanState by mutableStateOf<ScanUiState>(ScanUiState.Idle)
        private set

    private var scanJob: Job? = null

    /** Feed anything the camera (or the manual-entry box) read; the result lands in [scanState]. */
    fun handleScan(raw: String) {
        if (scanState !is ScanUiState.Idle) return
        scanState = ScanUiState.Loading("Reading code…")
        scanJob = viewModelScope.launch {
            scanState = try {
                resolveScan(ScanCodec.parse(raw))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ScanUiState.Failed(e.message ?: "Something went wrong reading that code.")
            }
        }
    }

    /** For failures that happen before there is anything to resolve (e.g. an image with no code in it). */
    fun showScanError(message: String) {
        if (scanState is ScanUiState.Idle) scanState = ScanUiState.Failed(message)
    }

    fun dismissScan() {
        scanJob?.cancel()
        scanState = ScanUiState.Idle
    }

    private suspend fun resolveScan(target: ScanTarget): ScanUiState = when (target) {
        is ScanTarget.PaperRef -> {
            scanState = ScanUiState.Loading("Opening paper…")
            repo.paperById(target.id)?.let { cache(listOf(it)); ScanUiState.PaperFound(it) }
                ?: ScanUiState.Failed("That code points to a paper, but it couldn't be loaded right now.")
        }

        is ScanTarget.ExternalPaper -> {
            scanState = ScanUiState.Loading("Finding paper…")
            repo.paperByExternalId(target.lookupId)?.let { cache(listOf(it)); ScanUiState.PaperFound(it) }
                ?: ScanUiState.Failed("No paper found for ${target.lookupId.substringAfter(':')}.")
        }

        is ScanTarget.CollectionRef -> {
            scanState = ScanUiState.Loading("Loading collection…")
            val papers = repo.papersByIds(target.paperIds).also { cache(it) }
            ScanUiState.CollectionFound(target.name, target.emoji, target.paperIds, papers)
        }

        is ScanTarget.Isbn -> {
            scanState = ScanUiState.Loading("Looking up book…")
            val book = repo.lookupBook(target.isbn13)
            if (book == null) {
                ScanUiState.Failed("No book found for ISBN ${target.isbn13}. Check the barcode and try again.")
            } else {
                scanState = ScanUiState.Loading("Finding related papers…")
                val related = findRelatedPapers(book)
                cache(related)
                ScanUiState.BookFound(book, related)
            }
        }

        is ScanTarget.Unsupported ->
            ScanUiState.Failed("This code isn't a Scholr QR, a paper link, a DOI or a book ISBN.")
    }

    /** Papers on the book's subject. Other editions/listings of the book itself are filtered out. */
    private suspend fun findRelatedPapers(book: com.scholr.app.data.scan.Book): List<Paper> {
        val bookTitle = book.title.lowercase().trim()
        fun List<Paper>.withoutTheBook() = filterNot {
            val t = it.title.lowercase()
            t.contains(bookTitle) || bookTitle.contains(t)
        }
        suspend fun search(q: String) = try {
            repo.searchPapers(q, limit = 20).withoutTheBook()
        } catch (e: Exception) {
            emptyList()
        }

        val topic = book.categories.lastOrNull().orEmpty()
        val results = linkedMapOf<String, Paper>()
        if (topic.isNotBlank()) search(topic).forEach { results[it.id] = it }
        if (results.size < 4) search(book.title).forEach { results.putIfAbsent(it.id, it) }
        return results.values.take(8)
    }

    /** Saves a scanned collection into the user's own library. */
    fun importCollection(name: String, emoji: String, paperIds: List<String>) {
        viewModelScope.launch { repo.createCollection(name, emoji, paperIds) }
    }

    /** Makes a new collection from saved papers so it can be shared as a QR code. */
    fun createCollection(name: String, paperIds: List<String>) {
        val emoji = COLLECTION_EMOJIS[(name.hashCode() and Int.MAX_VALUE) % COLLECTION_EMOJIS.size]
        viewModelScope.launch { repo.createCollection(name.trim(), emoji, paperIds) }
    }

    /** Make sure the papers behind a collection are in the cache so they can be listed. */
    fun ensurePapersLoaded(ids: List<String>) {
        val missing = ids.filter { it !in paperCache }
        if (missing.isEmpty()) return
        viewModelScope.launch { cache(repo.papersByIds(missing)) }
    }

    // ── Notifications ────────────────────────────────────────────────────────

    // ── Alerts (conference deadlines + AI nudges, with real system notifications) ──

    fun isAlertRead(id: String) = id in readAlertIds

    /** Loads cached alerts instantly, then refreshes them (deadlines + a Gemini-written digest). */
    fun loadAlerts(force: Boolean = false) {
        if (alertsLoading) return
        viewModelScope.launch {
            alertsLoading = true
            readAlertIds = repo.alertStore.readIds
            if (alerts.isEmpty()) alerts = repo.alertStore.alerts
            try {
                val labels = SampleData.interests.filter { it.id in selectedInterests }.map { it.label }
                val saved = bookmarkedPapers.map { it.title }
                val hints = papers.take(8).map { AlertEngine.PaperHint(it.id, it.title) }
                val fresh = repo.refreshAlerts(labels, saved, hints, force)
                alerts = fresh
                repo.notifier.notifyNew(fresh)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep whatever we already show; alerts are best-effort.
            } finally {
                alertsLoading = false
            }
        }
    }

    fun markAlertRead(id: String) {
        readAlertIds = readAlertIds + id
        repo.alertStore.readIds = readAlertIds
    }

    fun markAllAlertsRead() {
        readAlertIds = readAlertIds + alerts.map { it.id }
        repo.alertStore.readIds = readAlertIds
    }

    fun updatePushEnabled(enabled: Boolean) {
        pushEnabled = enabled
        repo.alertStore.pushEnabled = enabled
        if (enabled) repo.notifier.notifyNew(alerts)
    }

    /** True once if we haven't asked for the notification permission yet. */
    fun shouldAskNotificationPermission(): Boolean = !repo.alertStore.permissionAsked

    fun onNotificationPermissionAsked(granted: Boolean) {
        repo.alertStore.permissionAsked = true
        if (granted) repo.notifier.notifyNew(alerts)
    }

    fun requestOpenAlerts() { openAlertsRequested = true }
    fun consumeOpenAlertsRequest() { openAlertsRequested = false }

    // ── Factory ──────────────────────────────────────────────────────────────

    companion object {
        private val COLLECTION_EMOJIS = listOf("📚", "🧪", "🧠", "🔬", "📈", "💡", "🌍", "🧬")

        fun factory(repo: ScholrRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ScholrViewModel(repo) as T
            }
    }
}
