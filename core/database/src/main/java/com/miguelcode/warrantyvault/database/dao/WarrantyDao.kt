package com.miguelcode.warrantyvault.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.miguelcode.warrantyvault.database.entity.WarrantyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WarrantyDao {
    @Query("SELECT * FROM warranties ORDER BY warrantyEndDate ASC")
    fun getAllWarranties(): Flow<List<WarrantyEntity>>

    @Query("SELECT * FROM warranties WHERE id = :id")
    suspend fun getWarrantyById(id: Long): WarrantyEntity?

    @Query("SELECT * FROM warranties WHERE id = :id")
    fun getWarrantyByIdFlow(id: Long): Flow<WarrantyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(warranty: WarrantyEntity): Long

    @Update
    suspend fun update(warranty: WarrantyEntity)

    @Delete
    suspend fun delete(warranty: WarrantyEntity)

    @Query("SELECT COUNT(*) FROM warranties")
    suspend fun getCount(): Int
}
