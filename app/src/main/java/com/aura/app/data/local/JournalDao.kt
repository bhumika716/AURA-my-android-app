package com.aura.app.data.local

import androidx.room.*
import com.aura.app.data.local.entities.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Insert
    suspend fun insertEntry(entry: JournalEntry): Long

    @Update
    suspend fun updateEntry(entry: JournalEntry)

    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentEntries(limit: Int): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE createdAt > :since ORDER BY createdAt ASC")
    fun getEntriesSince(since: Long): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: Int): JournalEntry?

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun getEntryCount(): Int

    @Query("SELECT AVG(moodScore) FROM journal_entries WHERE createdAt > :since")
    suspend fun getAverageMoodSince(since: Long): Float?

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)
}
