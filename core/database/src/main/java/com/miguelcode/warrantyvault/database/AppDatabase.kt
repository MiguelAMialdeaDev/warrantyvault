package com.miguelcode.warrantyvault.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.miguelcode.warrantyvault.database.converter.Converters
import com.miguelcode.warrantyvault.database.dao.WarrantyDao
import com.miguelcode.warrantyvault.database.entity.WarrantyEntity

@Database(
    entities = [WarrantyEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun warrantyDao(): WarrantyDao
}
