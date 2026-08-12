package com.miguelcode.warrantyvault.database.datasource

import com.miguelcode.warrantyvault.database.dao.WarrantyDao
import com.miguelcode.warrantyvault.database.entity.WarrantyEntity
import com.miguelcode.warrantyvault.database.mapper.toDomain
import com.miguelcode.warrantyvault.database.mapper.toEntity
import com.miguelcode.warrantyvault.domain.model.Warranty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WarrantyLocalDataSource(private val dao: WarrantyDao) {

    fun getAllWarranties(): Flow<List<Warranty>> =
        dao.getAllWarranties().map { entities -> entities.map { it.toDomain() } }

    suspend fun getWarrantyById(id: Long): Warranty? =
        dao.getWarrantyById(id)?.toDomain()

    fun getWarrantyByIdFlow(id: Long): Flow<Warranty?> =
        dao.getWarrantyByIdFlow(id).map { it?.toDomain() }

    suspend fun insert(warranty: Warranty): Long =
        dao.insert(warranty.toEntity())

    suspend fun update(warranty: Warranty) =
        dao.update(warranty.toEntity())

    suspend fun delete(warranty: Warranty) =
        dao.delete(warranty.toEntity())

    suspend fun getCount(): Int = dao.getCount()
}
