package com.scholr.app.data.network

import com.scholr.app.data.model.Paper

private const val WORDS_PER_MINUTE = 200

/**
 * Maps an OpenAlex work onto our domain model. OpenAlex never returns plain
 * abstract text (copyright reasons) — only an "inverted index" of
 * word -> positions, which [reconstructAbstract] turns back into prose.
 * The UI assumes [Paper.keyFindings] always has at least two entries, so
 * [buildKeyFindings] guarantees that regardless of abstract length.
 */
fun OpenAlexWorkDto.toDomain(): Paper? {
    val shortId = id?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: return null
    val safeTitle = title?.takeIf { it.isNotBlank() } ?: return null
    val abstractText = reconstructAbstract(abstractInvertedIndex)?.takeIf { it.isNotBlank() }
        ?: "No abstract available for this paper."

    return Paper(
        id = shortId,
        title = safeTitle,
        authors = authorships
            ?.mapNotNull { it.author?.displayName }
            ?.take(4)
            ?.joinToString(", ")
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown authors",
        venue = primaryLocation?.source?.displayName?.takeIf { it.isNotBlank() } ?: "Preprint",
        year = publicationYear ?: 0,
        field = concepts?.firstOrNull()?.displayName ?: "General",
        citations = citedByCount ?: 0,
        readMinutes = estimateReadMinutes(abstractText),
        abstractText = abstractText,
        keyFindings = buildKeyFindings(abstractText),
        tags = concepts?.take(4)?.mapNotNull { it.displayName }?.takeIf { it.isNotEmpty() } ?: listOf("General"),
        openAccess = openAccess?.isOa ?: false
    )
}

private fun reconstructAbstract(invertedIndex: Map<String, List<Int>>?): String? {
    if (invertedIndex.isNullOrEmpty()) return null
    val maxPosition = invertedIndex.values.asSequence().flatten().maxOrNull() ?: return null
    val words = arrayOfNulls<String>(maxPosition + 1)
    for ((word, positions) in invertedIndex) {
        for (position in positions) {
            if (position in words.indices) words[position] = word
        }
    }
    return words.filterNotNull().joinToString(" ")
}

private fun estimateReadMinutes(text: String): Int {
    val words = text.trim().split(Regex("\\s+")).size
    return (words / WORDS_PER_MINUTE).coerceAtLeast(3)
}

private fun buildKeyFindings(abstractText: String): List<String> {
    val sentences = abstractText
        .split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.length > 20 }

    val findings = sentences.take(3).toMutableList()
    if (findings.isEmpty()) findings += "No summary available for this paper yet."
    if (findings.size == 1) findings += "See the full abstract for methodology and results."
    return findings
}
