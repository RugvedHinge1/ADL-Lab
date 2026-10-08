package com.scholr.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections")
    fun observeAll(): Flow<List<CollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<CollectionEntity>)

    @Update
    suspend fun update(item: CollectionEntity)

    @Query("SELECT COUNT(*) FROM collections")
    suspend fun count(): Int
}