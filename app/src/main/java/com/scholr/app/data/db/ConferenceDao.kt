package com.scholr.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConferenceDao {

    @Query("SELECT * FROM conferences ORDER BY daysLeft ASC")
    fun observeAll(): Flow<List<ConferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ConferenceEntity>)

    @Query("SELECT COUNT(*) FROM conferences")
    suspend fun count(): Int
}