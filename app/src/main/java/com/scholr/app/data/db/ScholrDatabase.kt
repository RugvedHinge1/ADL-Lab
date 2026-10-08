package com.scholr.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PaperEntity::class,
        ConferenceEntity::class,
        InterestEntity::class,
        ChatMessageEntity::class,
        BookmarkEntity::class,
        CollectionEntity::class,
        UserSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ScholrDatabase : RoomDatabase() {

    abstract fun paperDao(): PaperDao
    abstract fun conferenceDao(): ConferenceDao
    abstract fun interestDao(): InterestDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun collectionDao(): CollectionDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile private var INSTANCE: ScholrDatabase? = null

        fun getInstance(context: Context): ScholrDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        ScholrDatabase::class.java,
                        "scholr.db"
                    )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}