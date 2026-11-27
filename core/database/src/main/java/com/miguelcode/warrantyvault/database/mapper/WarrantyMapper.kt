package com.miguelcode.warrantyvault.database.mapper

import com.miguelcode.warrantyvault.database.entity.WarrantyEntity
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.model.WarrantyTracking

fun WarrantyEntity.toDomain(): Warranty = Warranty(
    id = id,
    productName = productName,
    category = try { WarrantyCategory.valueOf(category) } catch (_: Exception) { WarrantyCategory.OTHER },
    store = store,
    price = price,
    purchaseDate = purchaseDate,
    warrantyEndDate = warrantyEndDate,
    receiptPhotoPath = receiptPhotoPath,
    notes = notes,
    createdAt = createdAt,
    tracking = WarrantyTracking.entries.firstOrNull { it.name == tracking } ?: WarrantyTracking.NONE,
    trackingDate = trackingDate
)

fun Warranty.toEntity(): WarrantyEntity = WarrantyEntity(
    id = id,
    productName = productName,
    category = category.name,
    store = store,
    price = price,
    purchaseDate = purchaseDate,
    warrantyEndDate = warrantyEndDate,
    receiptPhotoPath = receiptPhotoPath,
    notes = notes,
    createdAt = createdAt,
    tracking = tracking.name,
    trackingDate = trackingDate
)
