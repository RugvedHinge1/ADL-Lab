package com.scholr.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per academic paper. List fields stored pipe-separated. */
@Entity(tableName = "papers")
data class PaperEntity(
    @PrimaryKey val id: String,
    val title: String,
    val authors: String,
    val venue: String,
    val year: Int,
    val field: String,
    val citations: Int,
    val readMinutes: Int,
    val abstractText: String,
    val keyFindings: String,   // pipe-separated  e.g. "Finding A|Finding B"
    val tags: String,          // pipe-separated
    val openAccess: Boolean
)

/** One conference / CFP row. */
@Entity(tableName = "conferences")
data class ConferenceEntity(
    @PrimaryKey val id: String,
    val acronym: String,
    val name: String,
    val location: String,
    val dates: String,
    val deadline: String,
    val daysLeft: Int,
    val field: String,
    val tier: String
)

/** One interest tile. */
@Entity(tableName = "interests")
data class InterestEntity(
    @PrimaryKey val id: String,
    val label: String,
    val emoji: String
)

/** A single turn in the per-paper AI chat. */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val msgId: Long,           // app-side id used by ChatMessage.id
    val paperId: String,
    val text: String,
    val fromUser: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

/** A bookmarked paper id. */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val paperId: String
)

/** A user-created collection of papers. */
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val paperIds: String,      // comma-separated paper ids
    val emoji: String
)

/**
 * Flat key-value store for simple settings:
 * "userName", "onboardingComplete", "selectedInterests" (comma-separated ids).
 */
@Entity(tableName = "user_settings")
data class UserSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)