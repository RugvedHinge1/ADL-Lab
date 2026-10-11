package com.scholr.app.data.conferences

import com.scholr.app.data.model.Conference
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

private fun monthOf(name: String): Int? =
    MONTHS.indexOf(name.lowercase().take(3)).takeIf { it >= 0 }?.plus(1)

/** Fills in the deadline text and `daysLeft` for [today] from the stored deadline day. */
internal fun Conference.withDeadlineRelativeTo(today: LocalDate): Conference {
    if (deadlineEpochDay == 0L) {
        return copy(deadline = "Deadline not listed", daysLeft = NO_DEADLINE)
    }
    val day = LocalDate.ofEpochDay(deadlineEpochDay)
    val left = ChronoUnit.DAYS.between(today, day).toInt()
    return if (left >= 0) {
        copy(deadline = "Submissions due ${day.format(DISPLAY_DATE)}", daysLeft = left)
    } else {
        copy(deadline = "Submissions closed ${day.format(DISPLAY_DATE)}", daysLeft = CLOSED)
    }
}

/** `daysLeft` sentinels: no deadline published, or the deadline has passed. */
const val NO_DEADLINE = -1
const val CLOSED = -2

// ──────────────────────────────────────────────────────────────────────────────
// CCF Deadlines — curated, ranked computer-science conferences.
// https://github.com/ccfddl/ccf-deadlines  (open data, published as one YAML file)
// ──────────────────────────────────────────────────────────────────────────────

internal object CcfDeadlinesParser {
    const val URL = "https://ccfddl.com/conference/allconf.yml"

    private val line = Regex("""^(\s*)(?:-\s+)?([A-Za-z_]+):\s*(.*)$""")
    private val startDate = Regex("""([A-Za-z]{3,9})\.?\s+(\d{1,2})""")
    private val year = Regex("""\d{4}""")
    private val validRanks = setOf("A*", "A", "B", "C")

    private fun clean(v: String) = v.trim().trim('\'', '"').trim()

    /**
     * The file is a regular, two-level YAML list (series -> editions), so a small
     * line scanner is enough and avoids pulling a YAML library onto Android.
     */
    fun parse(yaml: String, today: LocalDate, horizonDays: Long = 600): List<Conference> {
        val out = ArrayList<Conference>()

        var title = ""
        var description = ""
        var core = ""
        var ccf = ""

        var inEdition = false
        var editionYear = 0
        var id = ""
        var link = ""
        var date = ""
        var place = ""
        val deadlines = ArrayList<LocalDate>()

        fun flush() {
            if (!inEdition) return
            inEdition = false
            val start = parseStart(date) ?: return
            if (start.isBefore(today) || start.isAfter(today.plusDays(horizonDays))) return

            val upcoming = deadlines.filter { !it.isBefore(today) }.minOrNull()
            val deadlineDay = upcoming ?: deadlines.maxOrNull()
            val rank = core.takeIf { it in validRanks } ?: ccf.takeIf { it in validRanks } ?: ""

            out += Conference(
                id = "ccf-$id",
                acronym = title,
                name = description.ifBlank { title },
                location = place,
                dates = date,
                deadline = "",
                daysLeft = NO_DEADLINE,
                field = "Computer Science",
                tier = rank,
                startEpochDay = start.toEpochDay(),
                deadlineEpochDay = deadlineDay?.toEpochDay() ?: 0L,
                link = link
            ).withDeadlineRelativeTo(today)
        }

        for (raw in yaml.lineSequence()) {
            if (raw.isBlank()) continue
            val m = line.matchEntire(raw) ?: continue
            val indent = m.groupValues[1].length
            val isItem = raw.trimStart().startsWith("- ")
            val key = m.groupValues[2]
            val value = clean(m.groupValues[3])

            when {
                indent == 0 && isItem && key == "title" -> {
                    flush()
                    title = value; description = ""; core = ""; ccf = ""
                }
                indent == 2 && isItem && key == "year" -> {
                    flush()
                    inEdition = true
                    editionYear = value.toIntOrNull() ?: 0
                    id = ""; link = ""; date = ""; place = ""; deadlines.clear()
                }
                !inEdition && indent == 2 && key == "description" -> description = value
                !inEdition && indent == 4 && key == "core" -> core = value
                !inEdition && indent == 4 && key == "ccf" -> ccf = value
                inEdition && indent == 4 && key == "id" -> id = value
                inEdition && indent == 4 && key == "link" -> link = value
                inEdition && indent == 4 && key == "date" -> date = value
                inEdition && indent == 4 && key == "place" -> place = value
                inEdition && key == "deadline" ->
                    runCatching { LocalDate.parse(value.take(10)) }.getOrNull()?.let(deadlines::add)
            }
        }
        flush()
        return out
    }

    /** "February 4-7, 2021", "July 28 - August 2, 2025", "Jun 8-12, 2026" -> first day. */
    fun parseStart(date: String): LocalDate? {
        val m = startDate.find(date) ?: return null
        val month = monthOf(m.groupValues[1]) ?: return null
        val day = m.groupValues[2].toIntOrNull() ?: return null
        val y = year.find(date, m.range.last)?.value?.toIntOrNull() ?: return null
        return runCatching { LocalDate.of(y, month, day) }.getOrNull()
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// WikiCFP — community call-for-papers listings across every field.
// ──────────────────────────────────────────────────────────────────────────────

internal object WikiCfpParser {
    /** WikiCFP search topic -> the field name used by the app's interests. */
    val topics: List<Pair<String, String>> = listOf(
        "neuroscience" to "Neuroscience",
        "chemistry" to "Chemistry",
        "physics" to "Physics",
        "climate" to "Climate Science",
        "biology" to "Biology",
        "medicine" to "Medicine",
        "psychology" to "Psychology",
        "mathematics" to "Mathematics",
        "economics" to "Economics",
        "robotics" to "Computer Science",
        "machine learning" to "Computer Science"
    )

    fun url(topic: String): String =
        "http://www.wikicfp.com/cfp/call?conference=" + java.net.URLEncoder.encode(topic, "UTF-8") + "&page=1"

    private val event = Regex(
        """<a href="/cfp/servlet/event\.showcfp\?eventid=(\d+)[^"]*">(.*?)</a></td>\s*""" +
            """<td[^>]*colspan="3">(.*?)</td>.*?</tr>\s*<tr[^>]*>\s*""" +
            """<td[^>]*>(.*?)</td>\s*<td[^>]*>(.*?)</td>\s*<td[^>]*>(.*?)</td>""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val shortDate = Regex("""([A-Z][a-z]{2})\s+(\d{1,2}),\s+(\d{4})""")

    fun parse(html: String, field: String, today: LocalDate, horizonDays: Long = 600): List<Conference> =
        event.findAll(html).mapNotNull { m ->
            val (eventId, rawAcronym, rawName, rawWhen, rawWhere, rawDeadline) = m.destructured
            val start = firstDate(rawWhen) ?: return@mapNotNull null
            if (start.isBefore(today) || start.isAfter(today.plusDays(horizonDays))) return@mapNotNull null

            val deadlineDay = firstDate(rawDeadline)
            val where = text(rawWhere)

            Conference(
                id = "wcfp-$eventId",
                acronym = text(rawAcronym),
                name = text(rawName).ifBlank { text(rawAcronym) },
                location = where,
                dates = text(rawWhen).replace(Regex("""\s+-\s+"""), " – "),
                deadline = "",
                daysLeft = NO_DEADLINE,
                field = field,
                tier = "CFP",
                startEpochDay = start.toEpochDay(),
                deadlineEpochDay = deadlineDay?.toEpochDay() ?: 0L,
                link = "http://www.wikicfp.com/cfp/servlet/event.showcfp?eventid=$eventId"
            ).withDeadlineRelativeTo(today)
        }.toList()

    private fun firstDate(s: String): LocalDate? {
        val m = shortDate.find(s) ?: return null
        val month = monthOf(m.groupValues[1]) ?: return null
        return runCatching {
            LocalDate.of(m.groupValues[3].toInt(), month, m.groupValues[2].toInt())
        }.getOrNull()
    }

    private fun text(html: String): String =
        html.replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ").trim()
}

// ──────────────────────────────────────────────────────────────────────────────
// conferenceindex.org — per-country listings with city + date. Used for events
// near the user, since neither CCF nor WikiCFP can be searched by location.
// ──────────────────────────────────────────────────────────────────────────────

internal object ConferenceIndexParser {

    fun url(country: String): String =
        "https://conferenceindex.org/conferences/" +
            country.lowercase().trim().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    /** Either a "October, 2026" month header, or one event line. */
    private val token = Regex(
        """<strong>([A-Z][a-z]+),\s*(\d{4})</strong>|""" +
            """<li>\s*([A-Z][a-z]{2})\s+(\d{1,2})\s*<a href="([^"]+)"[^>]*>(.*?)</a>\s*-\s*([^<]*?)\s*</li>""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val abbreviation = Regex("""\(([A-Za-z0-9&\-]{2,12})\)""")

    fun parse(html: String, today: LocalDate, horizonDays: Long = 600): List<Conference> {
        val out = ArrayList<Conference>()
        var year = 0
        for (m in token.findAll(html)) {
            if (m.groups[1] != null) {
                year = m.groupValues[2].toIntOrNull() ?: 0
                continue
            }
            if (year == 0) continue
            val month = monthOf(m.groupValues[3]) ?: continue
            val day = m.groupValues[4].toIntOrNull() ?: continue
            val start = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: continue
            if (start.isBefore(today) || start.isAfter(today.plusDays(horizonDays))) continue

            val link = m.groupValues[5]
            val name = text(m.groupValues[6])
            val place = text(m.groupValues[7])
            if (name.isBlank()) continue

            out += Conference(
                id = "ci-" + link.substringAfterLast('/'),
                acronym = abbreviation.find(name)?.groupValues?.get(1) ?: name.split(' ').take(2).joinToString(" "),
                name = name.replace(abbreviation, "").replace(Regex("\\s+"), " ").trim(),
                location = place,
                dates = start.format(DISPLAY_DATE),
                deadline = "",
                daysLeft = NO_DEADLINE,
                field = classify(name),
                tier = "",
                startEpochDay = start.toEpochDay(),
                deadlineEpochDay = 0L,
                link = link
            ).withDeadlineRelativeTo(today)
        }
        return out
    }

    private val fieldRules: List<Pair<Regex, String>> = listOf(
        Regex("neuro|brain|cognit", RegexOption.IGNORE_CASE) to "Neuroscience",
        Regex("psycholog|mental|behaviou?r", RegexOption.IGNORE_CASE) to "Psychology",
        Regex("medic|health|cancer|oncolog|nurs|clinic|surg|pharma|disease|anaesth|cardio|dental|diabet", RegexOption.IGNORE_CASE) to "Medicine",
        Regex("chemi|material|polymer|catalys", RegexOption.IGNORE_CASE) to "Chemistry",
        Regex("physic|quantum|optic|astro", RegexOption.IGNORE_CASE) to "Physics",
        Regex("climate|environment|earth|geo|sustainab|ecolog|energy", RegexOption.IGNORE_CASE) to "Climate Science",
        Regex("biolog|genet|biotech|microb|molecul|agricultur", RegexOption.IGNORE_CASE) to "Biology",
        Regex("math|statist", RegexOption.IGNORE_CASE) to "Mathematics",
        Regex("econom|financ|business|manag|marketing", RegexOption.IGNORE_CASE) to "Economics",
        Regex("comput|artificial|machine learning|data|software|cyber|network|robot|iot|electronic|communicat|engineering|technolog", RegexOption.IGNORE_CASE) to "Computer Science"
    )

    private fun classify(name: String): String =
        fieldRules.firstOrNull { it.first.containsMatchIn(name) }?.second ?: "General"

    private fun text(html: String): String =
        html.replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ").trim()
}
