package com.scholr.app.data.db

import com.scholr.app.data.model.ChatMessage
import com.scholr.app.data.model.Conference
import com.scholr.app.data.model.Interest
import com.scholr.app.data.model.Paper
import com.scholr.app.data.model.PaperCollection

// ── Paper ────────────────────────────────────────────────────────────────────

fun Paper.toEntity() = PaperEntity(
    id           = id,
    title        = title,
    authors      = authors,
    venue        = venue,
    year         = year,
    field        = field,
    citations    = citations,
    readMinutes  = readMinutes,
    abstractText = abstractText,
    keyFindings  = keyFindings.joinToString("|"),
    tags         = tags.joinToString("|"),
    openAccess   = openAccess
)

fun PaperEntity.toDomain() = Paper(
    id           = id,
    title        = title,
    authors      = authors,
    venue        = venue,
    year         = year,
    field        = field,
    citations    = citations,
    readMinutes  = readMinutes,
    abstractText = abstractText,
    keyFindings  = keyFindings.split("|").filter { it.isNotBlank() },
    tags         = tags.split("|").filter { it.isNotBlank() },
    openAccess   = openAccess
)

// ── Conference ────────────────────────────────────────────────────────────────

fun Conference.toEntity() = ConferenceEntity(
    id       = id,
    acronym  = acronym,
    name     = name,
    location = location,
    dates    = dates,
    deadline = deadline,
    daysLeft = daysLeft,
    field    = field,
    tier     = tier
)

fun ConferenceEntity.toDomain() = Conference(
    id       = id,
    acronym  = acronym,
    name     = name,
    location = location,
    dates    = dates,
    deadline = deadline,
    daysLeft = daysLeft,
    field    = field,
    tier     = tier
)

// ── Interest ──────────────────────────────────────────────────────────────────

fun Interest.toEntity() = InterestEntity(id = id, label = label, emoji = emoji)

fun InterestEntity.toDomain() = Interest(id = id, label = label, emoji = emoji)

// ── PaperCollection ───────────────────────────────────────────────────────────

fun PaperCollection.toEntity() = CollectionEntity(
    id       = id,
    name     = name,
    paperIds = paperIds.joinToString(","),
    emoji    = emoji
)

fun CollectionEntity.toDomain() = PaperCollection(
    id       = id,
    name     = name,
    paperIds = paperIds.split(",").filter { it.isNotBlank() },
    emoji    = emoji
)

// ── ChatMessage ───────────────────────────────────────────────────────────────

fun ChatMessage.toEntity(paperId: String) = ChatMessageEntity(
    msgId    = id,
    paperId  = paperId,
    text     = text,
    fromUser = fromUser
)

fun ChatMessageEntity.toDomain() = ChatMessage(
    id       = msgId,
    text     = text,
    fromUser = fromUser
)