package com.scholr.app.data.scan

import java.net.URLDecoder
import java.net.URLEncoder

/** What a scanned (or typed) string turned out to mean. */
sealed interface ScanTarget {
    /** A Scholr paper id — Semantic Scholar (40-char hex) or OpenAlex ("W123…"). */
    data class PaperRef(val id: String) : ScanTarget

    /** A paper looked up through an external identifier ("DOI:10.1/x", "ARXIV:2101.00001"). */
    data class ExternalPaper(val lookupId: String) : ScanTarget

    /** A collection exported from another Scholr install. */
    data class CollectionRef(val name: String, val emoji: String, val paperIds: List<String>) : ScanTarget

    /** A normalised ISBN-13. */
    data class Isbn(val isbn13: String) : ScanTarget

    data class Unsupported(val raw: String) : ScanTarget
}

/**
 * Turns papers and collections into QR payloads, and scanned text back into
 * a [ScanTarget]. Pure Kotlin so it can be unit-tested without a device.
 *
 * Papers are encoded as ordinary https links (Semantic Scholar / OpenAlex) so
 * *any* phone camera opens something useful, and Scholr recognises them too.
 * Collections have no public URL, so they use a `scholr://collection` link.
 */
object ScanCodec {

    /** Keeps collection QR codes sparse enough to scan reliably off a screen. */
    const val MAX_PAPERS_PER_QR = 30

    private const val S2_PAPER_URL = "https://www.semanticscholar.org/paper/"
    private const val OPENALEX_URL = "https://openalex.org/"
    private const val COLLECTION_PREFIX = "scholr://collection?"

    private val s2UrlRegex = Regex("""semanticscholar\.org/paper/(?:[^/?#]+/)?([0-9a-fA-F]{40})""")
    private val openAlexRegex = Regex("""openalex\.org/(?:works/)?(W\d+)""", RegexOption.IGNORE_CASE)
    private val doiRegex = Regex("""(10\.\d{4,9}/[^\s"<>?#]+)""")
    private val arxivRegex =
        Regex("""arxiv\.org/(?:abs|pdf)/([\w.\-/]+?)(?:\.pdf)?(?:v\d+)?(?:[?#].*)?$""", RegexOption.IGNORE_CASE)

    // ── Encoding ─────────────────────────────────────────────────────────────

    fun paperPayload(paperId: String): String =
        if (paperId.length == 40 && paperId.all { it.isHex() }) S2_PAPER_URL + paperId
        else OPENALEX_URL + paperId

    /** Encodes at most [MAX_PAPERS_PER_QR] ids; any beyond that are dropped. */
    fun collectionPayload(name: String, emoji: String, paperIds: List<String>): String {
        val ids = paperIds.take(MAX_PAPERS_PER_QR).joinToString(",")
        return COLLECTION_PREFIX + "n=${enc(name)}&e=${enc(emoji)}&p=${enc(ids)}"
    }

    // ── Decoding ─────────────────────────────────────────────────────────────

    fun parse(raw: String): ScanTarget {
        val text = raw.trim()
        if (text.isEmpty()) return ScanTarget.Unsupported(raw)

        if (text.startsWith(COLLECTION_PREFIX, ignoreCase = true)) {
            return parseCollection(text) ?: ScanTarget.Unsupported(raw)
        }

        s2UrlRegex.find(text)?.let { return ScanTarget.PaperRef(it.groupValues[1].lowercase()) }
        openAlexRegex.find(text)?.let { return ScanTarget.PaperRef(it.groupValues[1].uppercase()) }

        parseIsbn(text)?.let { return ScanTarget.Isbn(it) }

        arxivRegex.find(text)?.let { return ScanTarget.ExternalPaper("ARXIV:" + it.groupValues[1]) }
        doiRegex.find(text)?.let {
            return ScanTarget.ExternalPaper("DOI:" + it.groupValues[1].trimEnd('.', ',', ';', ')'))
        }

        return ScanTarget.Unsupported(raw)
    }

    private fun parseCollection(text: String): ScanTarget.CollectionRef? {
        val params = text.substring(COLLECTION_PREFIX.length).split('&')
            .mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null else part.substring(0, i) to dec(part.substring(i + 1))
            }.toMap()
        val ids = params["p"].orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (ids.isEmpty()) return null
        return ScanTarget.CollectionRef(
            name = params["n"]?.takeIf { it.isNotBlank() } ?: "Shared collection",
            emoji = params["e"]?.takeIf { it.isNotBlank() } ?: "📚",
            paperIds = ids.take(MAX_PAPERS_PER_QR)
        )
    }

    /** Accepts "978-0-262-03384-8", "ISBN 0262033844", a bare EAN-13, etc. Returns an ISBN-13 or null. */
    fun parseIsbn(text: String): String? {
        val compact = text.trim().removePrefix("ISBN").removePrefix("isbn").trimStart(':', ' ')
            .replace("-", "").replace(" ", "")
        return when {
            compact.length == 13 && compact.all { it.isDigit() } &&
                (compact.startsWith("978") || compact.startsWith("979")) &&
                isbn13CheckOk(compact) -> compact
            compact.length == 10 && isbn10CheckOk(compact) -> isbn10To13(compact)
            else -> null
        }
    }

    private fun isbn13CheckOk(d: String): Boolean {
        val sum = d.mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 1 else 3 }.sum()
        return sum % 10 == 0
    }

    private fun isbn10CheckOk(s: String): Boolean {
        if (!s.take(9).all { it.isDigit() }) return false
        val last = s[9]
        if (!(last.isDigit() || last == 'X' || last == 'x')) return false
        val sum = s.mapIndexed { i, c ->
            val v = if (c == 'X' || c == 'x') 10 else c - '0'
            v * (10 - i)
        }.sum()
        return sum % 11 == 0
    }

    private fun isbn10To13(isbn10: String): String {
        val body = "978" + isbn10.take(9)
        val sum = body.mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 1 else 3 }.sum()
        return body + ((10 - sum % 10) % 10)
    }

    private fun Char.isHex() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun dec(s: String) = runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)
}
