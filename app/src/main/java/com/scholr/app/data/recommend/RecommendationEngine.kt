package com.scholr.app.data.recommend

import android.util.Log
import com.scholr.app.data.model.Paper
import com.scholr.app.data.network.EmbeddedPaper
import com.scholr.app.data.network.PaperApiRepository
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Content-based paper recommender.
 *
 * 1. **Profile vector** — every paper has a SPECTER2 document embedding
 *    (served by Semantic Scholar). The user profile is a weighted blend of
 *    (a) the centroid of their bookmarked papers and (b) one centroid per
 *    selected interest, built from that interest's top search results.
 * 2. **Candidates** — interest search results plus Semantic Scholar's own
 *    "similar to these papers" recommendations seeded from bookmarks.
 * 3. **Ranking** — cosine similarity to the profile, plus a small log-citation
 *    prior, then Maximal Marginal Relevance so one interest can't crowd out the
 *    rest of the batch.
 *
 * Cold start (no bookmarks) uses interest centroids only; a user with no
 * signals at all gets an empty list and the caller falls back to keyword search.
 */
class RecommendationEngine(private val api: PaperApiRepository) {

    private class InterestProfile(val centroid: FloatArray?, val papers: List<EmbeddedPaper>)

    /** label -> profile; interest searches are the costly (rate-limited) part, so cache per session. */
    private val interestCache = HashMap<String, InterestProfile>()

    suspend fun recommend(
        interestLabels: List<String>,
        bookmarkedIds: List<String>,
        limit: Int = 30
    ): List<Paper> {
        val labels = interestLabels.distinct().take(MAX_INTERESTS)
        val bookmarked = bookmarkedIds.toSet()

        // (b) interest centroids + their candidate pools
        val interests = labels.mapNotNull { label ->
            interestCache[label] ?: runCatching {
                val papers = api.searchEmbedded(label, PER_INTEREST).map { it.withField(label) }
                InterestProfile(centroid(papers.mapNotNull { it.vector }), papers)
            }.onFailure { Log.w(TAG, "interest '$label' failed", it) }
                .getOrNull()?.also { interestCache[label] = it }
        }

        // (a) bookmark centroid + S2's own neighbours of the bookmarks
        val seedIds = bookmarkedIds.takeLast(MAX_SEEDS)
        val seedPapers = runCatching { api.embeddedByIds(seedIds) }
            .onFailure { Log.w(TAG, "bookmark hydrate failed", it) }
            .getOrDefault(emptyList())
        val similar = if (seedPapers.isEmpty()) emptyList() else runCatching {
            val have = (interests.flatMap { p -> p.papers.map { it.paper.id } } + seedIds).toSet()
            val ids = api.similarPaperIds(seedIds, SIMILAR_LIMIT).filter { it !in have }
            api.embeddedByIds(ids)
        }.onFailure { Log.w(TAG, "similar-papers failed", it) }.getOrDefault(emptyList())

        val bookmarkCentroid = centroid(seedPapers.mapNotNull { it.vector })
        val interestCentroid = centroid(interests.mapNotNull { it.centroid })

        val profile = blend(bookmarkCentroid, BOOKMARK_WEIGHT, interestCentroid, 1f - BOOKMARK_WEIGHT)
        val pool = (interests.flatMap { it.papers } + similar)
            .distinctBy { it.paper.id }
            .filter { it.paper.id !in bookmarked && it.paper.abstractText.length > 40 }

        if (pool.isEmpty()) return emptyList()

        val labelTokens = labels.flatMap(::tokens).toSet()
        val maxLogCites = pool.maxOf { ln(1.0 + it.paper.citations) }.coerceAtLeast(1.0)

        val scored = pool.map { candidate ->
            val vector = candidate.vector
            val relevance = if (profile != null && vector != null) {
                cosine(profile, vector).toDouble()
            } else {
                lexicalOverlap(labelTokens, candidate.paper) // no embedding available
            }
            val prior = ln(1.0 + candidate.paper.citations) / maxLogCites
            Scored(candidate, relevance + CITATION_PRIOR * prior)
        }

        return mmr(scored, limit).map { it.candidate.paper }
    }

    /** S2 leaves fieldsOfStudy empty for many papers; fall back to the interest that found it. */
    private fun EmbeddedPaper.withField(label: String): EmbeddedPaper =
        if (paper.field == "General") {
            copy(paper = paper.copy(field = label, tags = listOf(label)))
        } else this

    private class Scored(val candidate: EmbeddedPaper, val score: Double)

    /** Maximal Marginal Relevance: greedily pick high-score items that differ from those already picked. */
    private fun mmr(items: List<Scored>, k: Int): List<Scored> {
        val remaining = items.sortedByDescending { it.score }.toMutableList()
        val picked = ArrayList<Scored>(k)
        while (picked.size < k && remaining.isNotEmpty()) {
            val best = remaining.maxByOrNull { item ->
                val redundancy = picked.maxOfOrNull { other ->
                    val a = item.candidate.vector
                    val b = other.candidate.vector
                    if (a != null && b != null) cosine(a, b).toDouble() else 0.0
                } ?: 0.0
                MMR_LAMBDA * item.score - (1 - MMR_LAMBDA) * redundancy
            } ?: break
            picked += best
            remaining -= best
        }
        return picked
    }

    private fun lexicalOverlap(labelTokens: Set<String>, paper: Paper): Double {
        if (labelTokens.isEmpty()) return 0.0
        val doc = tokens(paper.title + " " + paper.tags.joinToString(" "))
        return doc.count { it in labelTokens }.toDouble() / labelTokens.size * 0.5
    }

    private fun tokens(text: String): List<String> =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 2 }

    private companion object {
        const val TAG = "RecommendationEngine"
        const val MAX_INTERESTS = 4
        const val PER_INTEREST = 15
        const val MAX_SEEDS = 10
        const val SIMILAR_LIMIT = 30
        const val BOOKMARK_WEIGHT = 0.6f
        const val CITATION_PRIOR = 0.04
        const val MMR_LAMBDA = 0.75
    }
}

// ── Vector maths (free of Android types so it is trivially unit-testable) ──

internal fun cosine(a: FloatArray, b: FloatArray): Float {
    val n = minOf(a.size, b.size)
    var dot = 0f
    var na = 0f
    var nb = 0f
    for (i in 0 until n) {
        dot += a[i] * b[i]
        na += a[i] * a[i]
        nb += b[i] * b[i]
    }
    val denom = sqrt(na) * sqrt(nb)
    return if (denom == 0f) 0f else dot / denom
}

/** Mean of unit-normalised vectors, re-normalised; null for an empty input. */
internal fun centroid(vectors: List<FloatArray>): FloatArray? {
    if (vectors.isEmpty()) return null
    val sum = FloatArray(vectors.first().size)
    for (v in vectors) {
        val norm = l2(v)
        if (norm == 0f) continue
        for (i in sum.indices) sum[i] += v[i] / norm
    }
    return normalize(sum)
}

private fun l2(v: FloatArray): Float {
    var s = 0f
    for (x in v) s += x * x
    return sqrt(s)
}

private fun normalize(v: FloatArray): FloatArray? {
    val norm = l2(v)
    if (norm == 0f) return null
    return FloatArray(v.size) { v[it] / norm }
}

private fun blend(a: FloatArray?, wa: Float, b: FloatArray?, wb: Float): FloatArray? = when {
    a != null && b != null -> normalize(FloatArray(a.size) { a[it] * wa + b[it] * wb })
    else -> a ?: b
}
