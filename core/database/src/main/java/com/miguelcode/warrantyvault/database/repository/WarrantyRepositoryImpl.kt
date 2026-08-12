package com.miguelcode.warrantyvault.database.repository

import com.miguelcode.warrantyvault.database.datasource.WarrantyLocalDataSource
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import kotlinx.coroutines.flow.Flow

class WarrantyRepositoryImpl(
    private val localDataSource: WarrantyLocalDataSource
) : WarrantyRepository {

    override fun getAllWarranties(): Flow<List<Warranty>> =
        localDataSource.getAllWarranties()

    override suspend fun getWarrantyById(id: Long): Result<Warranty> =
        Result.runCatching {
            localDataSource.getWarrantyById(id)
                ?: throw NoSuchElementException("Warranty with id $id not found")
        }

    override fun getWarrantyByIdFlow(id: Long): Flow<Warranty?> =
        localDataSource.getWarrantyByIdFlow(id)

    override suspend fun addWarranty(warranty: Warranty): Result<Long> =
        Result.runCatching { localDataSource.insert(warranty) }

    override suspend fun updateWarranty(warranty: Warranty): Result<Unit> =
        Result.runCatching { localDataSource.update(warranty) }

    override suspend fun deleteWarranty(warranty: Warranty): Result<Unit> =
        Result.runCatching { localDataSource.delete(warranty) }

    override suspend fun getCount(): Int = localDataSource.getCount()
}
