package com.krya1012.clearance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityModuleDao {
    @Query("SELECT * FROM modules ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<ActivityModule>>

    @Query("SELECT * FROM modules ORDER BY sortOrder ASC")
    suspend fun getAll(): List<ActivityModule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(module: ActivityModule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modules: List<ActivityModule>)

    @Update
    suspend fun update(module: ActivityModule)

    @Delete
    suspend fun delete(module: ActivityModule)
}
