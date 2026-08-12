package com.miguelcode.warrantyvault.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "warranties")
data class WarrantyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productName: String,
    val category: String,
    val store: String,
    val price: Double?,
    val purchaseDate: LocalDate,
    val warrantyEndDate: LocalDate,
    val receiptPhotoPath: String?,
    val notes: String,
    val createdAt: LocalDateTime
)
