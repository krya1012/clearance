package com.krya1012.clearance.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromChecklistType(value: ChecklistType): String = value.name

    @TypeConverter
    fun toChecklistType(value: String): ChecklistType = ChecklistType.valueOf(value)
}
