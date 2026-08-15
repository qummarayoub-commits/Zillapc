package com.darkjade.streamlib.data.db

import androidx.room.TypeConverter
import com.darkjade.streamlib.data.db.entity.MediaType
import com.darkjade.streamlib.data.db.entity.NewsCategory
import com.darkjade.streamlib.data.db.entity.ScanState

class Converters {
    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = MediaType.valueOf(value)

    @TypeConverter
    fun fromScanState(value: ScanState): String = value.name

    @TypeConverter
    fun toScanState(value: String): ScanState = ScanState.valueOf(value)

    @TypeConverter
    fun fromNewsCategory(value: NewsCategory): String = value.name

    @TypeConverter
    fun toNewsCategory(value: String): NewsCategory = NewsCategory.valueOf(value)
}
