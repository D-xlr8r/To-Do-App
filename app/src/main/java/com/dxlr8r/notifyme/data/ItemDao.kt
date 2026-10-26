package com.dxlr8r.notifyme.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE kind = 0 ORDER BY done ASC, priority DESC, createdAt ASC")
    fun observeTasks(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE kind = 1 ORDER BY anchor ASC")
    fun observeEvents(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE kind = 0 AND done = 0 ORDER BY priority DESC, createdAt ASC")
    suspend fun pendingTasks(): List<Item>

    @Query("SELECT * FROM items WHERE kind = 1")
    suspend fun allEvents(): List<Item>

    @Query("SELECT * FROM items WHERE anchor IS NOT NULL AND done = 0")
    suspend fun withReminders(): List<Item>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT * FROM items WHERE kind = 0 AND done = 1")
    suspend fun completedTasks(): List<Item>

    @Query("DELETE FROM items WHERE kind = 0 AND done = 1")
    suspend fun clearCompleted()
}
