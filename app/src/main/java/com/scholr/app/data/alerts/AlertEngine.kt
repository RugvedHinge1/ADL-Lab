package com.scholr.app.data.alerts

import com.scholr.app.data.conferences.ConferenceRepository
import com.scholr.app.data.model.Conference
import com.scholr.app.data.network.GeminiRepository
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Builds the alert list from real data:
 *  - **Deadline alerts** — upcoming submission deadlines from the live conference
 *    list, ranked by how well they match the user's interests.
 *  - **AI alerts** — Gemini reads the user's interests, saved papers and those
 *    same deadlines and writes a few short, specific nudges. Every item it
 *    references is checked against the data it was given, so it can't invent
 *    a conference or paper.
 *
 * Results are cached for [REFRESH_INTERVAL_MS] so opening the app doesn't
 * spend an LLM call every time.
 */
class AlertEngine(
    private val store: AlertStore,
    private val gemini: GeminiRepository,
    private val conferenceSource: ConferenceRepository
) {

    /** A paper the AI is allowed to point the user at. */
    data class PaperHint(val id: String, val title: String)

    suspend fun refresh(
        interestLabels: List<String>,
        savedTitles: List<String>,
        paperHints: List<PaperHint> = emptyList(),
        force: Boolean = false
    ): List<AppAlert> {
        val now = System.currentTimeMillis()
        val cached = store.alerts
        if (!force && cached.isNotEmpty() && now - store.generatedAt < REFRESH_INTERVAL_MS) return cached

        val conferences = runCatching { conferenceSource.load() }.getOrDefault(emptyList())
        val deadlines = deadlineAlerts(conferences, interestLabels, now)
        val ai = aiAlerts(interestLabels, savedTitles, paperHints, conferences, now)

        // Keep recent AI alerts from earlier runs as history; deadlines are always recomputed
        // because their "N days left" wording goes stale.
        val history = cached.filter {
            it.kind == AppAlert.KIND_AI && now - it.createdAt < HISTORY_MS &&
                ai.none { fresh -> fresh.id == it.id }
        }
        val merged = (ai + deadlines + history).distinctBy { it.id }.take(MAX_ALERTS)

        // Only overwrite the cache if we produced something, so a failed run doesn't wipe good data.
        if (merged.isNotEmpty()) {
            store.alerts = merged
            store.generatedAt = now
        }
        return merged.ifEmpty { cached }
    }

    private fun deadlineAlerts(
        conferences: List<Conference>,
        interestLabels: List<String>,
        now: Long
    ): List<AppAlert> {
        val upcoming = conferences.filter { it.deadlineEpochDay > 0 && it.daysLeft in 0..DEADLINE_WINDOW_DAYS }
        val relevant = upcoming.filter { matchesInterests(it, interestLabels) }
        val picked = (relevant.ifEmpty { upcoming }).sortedBy { it.daysLeft }.take(3)
        return picked.map { c ->
            val whenText = when (c.daysLeft) {
                0 -> "today"
                1 -> "tomorrow"
                else -> "in ${c.daysLeft} days"
            }
            AppAlert(
                id = "deadline-${c.id}-${c.deadlineEpochDay}",
                kind = AppAlert.KIND_DEADLINE,
                title = "${c.acronym} deadline $whenText",
                body = listOf(c.name, c.location).filter { it.isNotBlank() }.joinToString(" · "),
                refType = AppAlert.REF_CONFERENCE,
                refId = c.id,
                createdAt = now
            )
        }
    }

    private fun matchesInterests(c: Conference, interestLabels: List<String>): Boolean {
        if (interestLabels.isEmpty()) return true
        val cs = setOf("Computer Science", "Machine Learning", "Robotics")
        return interestLabels.any { it.equals(c.field, ignoreCase = true) } ||
            (c.field.equals("Computer Science", ignoreCase = true) && interestLabels.any { it in cs })
    }

    private suspend fun aiAlerts(
        interestLabels: List<String>,
        savedTitles: List<String>,
        paperHints: List<PaperHint>,
        conferences: List<Conference>,
        now: Long
    ): List<AppAlert> {
        val soon = conferences
            .filter { it.deadlineEpochDay > 0 && it.daysLeft in 0..60 }
            .filter { matchesInterests(it, interestLabels) }
            .sortedBy { it.daysLeft }
            .take(10)
        if (interestLabels.isEmpty() && savedTitles.isEmpty() && soon.isEmpty()) return emptyList()

        val context = buildString {
            appendLine("Today: ${LocalDate.now()}")
            appendLine("Research interests: ${interestLabels.joinToString(", ").ifBlank { "not set" }}")
            appendLine("Papers the user saved: ${savedTitles.take(8).joinToString("; ").ifBlank { "none yet" }}")
            appendLine("Upcoming conference deadlines (id | acronym | name | field | days left):")
            if (soon.isEmpty()) appendLine("none")
            soon.forEach { appendLine("${it.id} | ${it.acronym} | ${it.name} | ${it.field} | ${it.daysLeft}") }
            appendLine("Recent papers in the user's feed (id | title):")
            if (paperHints.isEmpty()) appendLine("none")
            paperHints.take(8).forEach { appendLine("${it.id} | ${it.title}") }
        }

        val validConferences = soon.associateBy { it.id }
        val validPapers = paperHints.associateBy { it.id }
        val day = LocalDate.now().toEpochDay()

        return gemini.generateAlerts(context).mapIndexedNotNull { index, draft ->
            val title = draft.title.trim().take(80)
            val body = draft.body.trim().take(200)
            if (title.isEmpty() || body.isEmpty()) return@mapIndexedNotNull null
            val (refType, refId) = when {
                draft.refType == AppAlert.REF_CONFERENCE && draft.refId in validConferences ->
                    AppAlert.REF_CONFERENCE to draft.refId
                draft.refType == AppAlert.REF_PAPER && draft.refId in validPapers ->
                    AppAlert.REF_PAPER to draft.refId
                else -> AppAlert.REF_NONE to ""
            }
            AppAlert(
                id = "ai-$day-${title.lowercase().hashCode()}",
                kind = AppAlert.KIND_AI,
                title = title,
                body = body,
                refType = refType,
                refId = refId,
                // Stagger by a millisecond so list order is stable.
                createdAt = now - index
            )
        }
    }

    private companion object {
        val REFRESH_INTERVAL_MS = TimeUnit.HOURS.toMillis(6)
        val HISTORY_MS = TimeUnit.DAYS.toMillis(7)
        const val DEADLINE_WINDOW_DAYS = 30
        const val MAX_ALERTS = 20
    }
}
