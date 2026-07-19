package com.krya1012.clearance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {
    @Query("SELECT * FROM items ORDER BY orderIndex ASC")
    fun observeAll(): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM items ORDER BY orderIndex ASC")
    suspend fun getAll(): List<ChecklistItem>

    @Query("SELECT DISTINCT associatedModule FROM items")
    suspend fun getModulesWithItems(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChecklistItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChecklistItem>)

    @Update
    suspend fun update(item: ChecklistItem)

    @Delete
    suspend fun delete(item: ChecklistItem)

    @Query("DELETE FROM items WHERE associatedModule = :moduleId")
    suspend fun deleteByModule(moduleId: String)
}
