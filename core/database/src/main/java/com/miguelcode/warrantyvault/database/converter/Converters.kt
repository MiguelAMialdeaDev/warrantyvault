package com.miguelcode.warrantyvault.database.converter

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(epoch: Long?): LocalDate? = epoch?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): Long? =
        dateTime?.toEpochSecond(ZoneOffset.UTC)

    @TypeConverter
    fun toLocalDateTime(epoch: Long?): LocalDateTime? =
        epoch?.let { LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) }
}
