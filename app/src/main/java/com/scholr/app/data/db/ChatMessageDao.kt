package com.scholr.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query(
        "SELECT * FROM chat_messages WHERE paperId = :paperId ORDER BY timestampMs ASC"
    )
    fun observeForPaper(paperId: String): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insert(msg: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE paperId = :paperId")
    suspend fun clearForPaper(paperId: String)
}