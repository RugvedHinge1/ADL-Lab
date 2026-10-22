package com.scholr.app.data.alerts

import kotlinx.serialization.Serializable

/** One entry in the notifications list. */
@Serializable
data class AppAlert(
    val id: String,
    /** [KIND_AI] or [KIND_DEADLINE]. */
    val kind: String,
    val title: String,
    val body: String,
    /** "conference", "paper" or "none" — what tapping the alert opens. */
    val refType: String = REF_NONE,
    val refId: String = "",
    val createdAt: Long
) {
    companion object {
        const val KIND_AI = "ai"
        const val KIND_DEADLINE = "deadline"
        const val REF_NONE = "none"
        const val REF_CONFERENCE = "conference"
        const val REF_PAPER = "paper"
    }
}

/** The shape Gemini is asked to return, before it is validated against real data. */
@Serializable
data class AiAlertDraft(
    val title: String = "",
    val body: String = "",
    val refType: String = AppAlert.REF_NONE,
    val refId: String = ""
)
