package com.scholr.app.data.model

import androidx.compose.ui.graphics.Color

/** A single academic paper in the discovery feed. */
data class Paper(
    val id: String,
    val title: String,
    val authors: String,
    val venue: String,
    val year: Int,
    val field: String,
    val citations: Int,
    val readMinutes: Int,
    val abstractText: String,
    val keyFindings: List<String>,
    val tags: List<String>,
    val openAccess: Boolean = true
)

/** An upcoming conference / CFP entry. */
data class Conference(
    val id: String,
    val acronym: String,
    val name: String,
    val location: String,
    val dates: String,
    val deadline: String,
    val daysLeft: Int,
    val field: String,
    val tier: String
)

/** One tile in the interest-selection grid. */
data class Interest(
    val id: String,
    val label: String,
    val emoji: String
)

/** A message in the AI analysis chat. */
data class ChatMessage(
    val id: Long,
    val text: String,
    val fromUser: Boolean
)

/** A saved-papers collection in the Library. Named to avoid colliding with
 *  kotlin.collections.Collection at call sites. */
data class PaperCollection(
    val id: String,
    val name: String,
    val paperIds: List<String>,
    val emoji: String
)

/** Maps a field name onto its accent colour. Single source of truth. */
object FieldPalette {
    private val map = mapOf(
        "Computer Science" to Color(0xFF6366F1),
        "Neuroscience" to Color(0xFFEC4899),
        "Biology" to Color(0xFF10B981),
        "Physics" to Color(0xFF0EA5E9),
        "Chemistry" to Color(0xFFF59E0B),
        "Mathematics" to Color(0xFF8B5CF6),
        "Medicine" to Color(0xFFEF4444),
        "Psychology" to Color(0xFF14B8A6),
        "Climate Science" to Color(0xFF22C55E),
        "Economics" to Color(0xFF64748B)
    )

    fun colorFor(field: String): Color = map[field] ?: Color(0xFF6366F1)
}
