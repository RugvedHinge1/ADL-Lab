package com.scholr.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InterestDao {

    @Query("SELECT * FROM interests")
    fun observeAll(): Flow<List<InterestEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<InterestEntity>)

    @Query("SELECT COUNT(*) FROM interests")
    suspend fun count(): Int
}