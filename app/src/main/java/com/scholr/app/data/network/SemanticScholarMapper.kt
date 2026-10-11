package com.scholr.app.data.network

import com.scholr.app.data.model.Paper

/** A paper plus its SPECTER2 document embedding (null when S2 has none). */
data class EmbeddedPaper(val paper: Paper, val vector: FloatArray?)

private fun String.unescapeHtml(): String =
    replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")

fun S2PaperDto.toDomain(): Paper? {
    val id = paperId?.takeIf { it.isNotBlank() } ?: return null
    val safeTitle = title?.takeIf { it.isNotBlank() }?.unescapeHtml() ?: return null
    val abstractText = abstract?.takeIf { it.isNotBlank() } ?: "No abstract available for this paper."
    val fields = fieldsOfStudy.orEmpty()

    return Paper(
        id = id,
        title = safeTitle,
        authors = authors?.mapNotNull { it.name }?.take(4)?.joinToString(", ")
            ?.takeIf { it.isNotBlank() } ?: "Unknown authors",
        venue = venue?.takeIf { it.isNotBlank() }?.unescapeHtml() ?: "Preprint",
        year = year ?: 0,
        field = fields.firstOrNull() ?: "General",
        citations = citationCount ?: 0,
        readMinutes = estimateReadMinutes(abstractText),
        abstractText = abstractText,
        keyFindings = buildKeyFindings(abstractText),
        tags = fields.ifEmpty { listOf("General") },
        openAccess = isOpenAccess ?: false
    )
}

fun S2PaperDto.toEmbedded(): EmbeddedPaper? {
    val paper = toDomain() ?: return null
    return EmbeddedPaper(paper, embedding?.vector?.toFloatArray())
}

/** Semantic Scholar ids are 40-char hex; OpenAlex ids look like "W2741809807". */
fun isSemanticScholarId(id: String): Boolean =
    id.length == 40 && id.all { it in '0'..'9' || it in 'a'..'f' }
