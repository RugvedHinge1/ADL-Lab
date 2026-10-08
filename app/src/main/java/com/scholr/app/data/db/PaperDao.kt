package com.scholr.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {

    @Query("SELECT * FROM papers")
    fun observeAll(): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE id = :id")
    suspend fun getById(id: String): PaperEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(papers: List<PaperEntity>)

    @Query("SELECT COUNT(*) FROM papers")
    suspend fun count(): Int
}