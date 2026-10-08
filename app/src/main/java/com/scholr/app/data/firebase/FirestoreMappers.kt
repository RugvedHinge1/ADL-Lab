package com.scholr.app.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.scholr.app.data.model.Conference
import com.scholr.app.data.model.Paper
import com.scholr.app.data.model.PaperCollection

// ── Paper ─────────────────────────────────────────────────────────────────

fun Paper.toFirestoreMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "authors" to authors,
    "venue" to venue,
    "year" to year,
    "field" to field,
    "citations" to citations,
    "readMinutes" to readMinutes,
    "abstractText" to abstractText,
    "keyFindings" to keyFindings,
    "tags" to tags,
    "openAccess" to openAccess
)

fun DocumentSnapshot.toPaper(): Paper? {
    if (!exists()) return null
    return Paper(
        id = id,
        title = getString("title") ?: return null,
        authors = getString("authors").orEmpty(),
        venue = getString("venue").orEmpty(),
        year = (getLong("year") ?: 0L).toInt(),
        field = getString("field").orEmpty(),
        citations = (getLong("citations") ?: 0L).toInt(),
        readMinutes = (getLong("readMinutes") ?: 0L).toInt(),
        abstractText = getString("abstractText").orEmpty(),
        keyFindings = (get("keyFindings") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        tags = (get("tags") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        openAccess = getBoolean("openAccess") ?: true
    )
}

// ── Conference ───────────────────────────────────────────────────────────

fun Conference.toFirestoreMap(): Map<String, Any?> = mapOf(
    "acronym" to acronym,
    "name" to name,
    "location" to location,
    "dates" to dates,
    "deadline" to deadline,
    "daysLeft" to daysLeft,
    "field" to field,
    "tier" to tier
)

fun DocumentSnapshot.toConference(): Conference? {
    if (!exists()) return null
    return Conference(
        id = id,
        acronym = getString("acronym").orEmpty(),
        name = getString("name").orEmpty(),
        location = getString("location").orEmpty(),
        dates = getString("dates").orEmpty(),
        deadline = getString("deadline").orEmpty(),
        daysLeft = (getLong("daysLeft") ?: 0L).toInt(),
        field = getString("field").orEmpty(),
        tier = getString("tier").orEmpty()
    )
}

// ── PaperCollection (embedded inside a user document) ───────────────────

fun PaperCollection.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "paperIds" to paperIds,
    "emoji" to emoji
)

fun Map<String, Any?>.toPaperCollection(): PaperCollection? {
    val id = this["id"] as? String ?: return null
    return PaperCollection(
        id = id,
        name = this["name"] as? String ?: "",
        paperIds = (this["paperIds"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
        emoji = this["emoji"] as? String ?: "📁"
    )
}
