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
        const val DB_NAME = "clearance.db"

        fun build(context: Context): ClearanceDatabase =
            Room.databaseBuilder(context.applicationContext, ClearanceDatabase::class.java, DB_NAME).build()
    }
}
