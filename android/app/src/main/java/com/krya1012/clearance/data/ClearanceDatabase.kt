package com.krya1012.clearance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ActivityModule::class, ChecklistItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ClearanceDatabase : RoomDatabase() {
    abstract fun moduleDao(): ActivityModuleDao
    abstract fun itemDao(): ChecklistItemDao

    companion object {
        @Volatile private var instance: ClearanceDatabase? = null

        fun getInstance(context: Context): ClearanceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClearanceDatabase::class.java,
                    "clearance.db"
                ).build().also { instance = it }
            }
    }
}
