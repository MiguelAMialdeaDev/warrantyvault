package com.miguelcode.warrantyvault.domain.repository

import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import kotlinx.coroutines.flow.Flow

interface WarrantyRepository {
    fun getAllWarranties(): Flow<List<Warranty>>
    suspend fun getWarrantyById(id: Long): Result<Warranty>
    /** Flow reactivo: re-emite el valor cuando la fila cambia (insert/update/delete). */
    fun getWarrantyByIdFlow(id: Long): Flow<Warranty?>
    suspend fun addWarranty(warranty: Warranty): Result<Long>
    suspend fun updateWarranty(warranty: Warranty): Result<Unit>
    suspend fun deleteWarranty(warranty: Warranty): Result<Unit>
    suspend fun getCount(): Int
}
