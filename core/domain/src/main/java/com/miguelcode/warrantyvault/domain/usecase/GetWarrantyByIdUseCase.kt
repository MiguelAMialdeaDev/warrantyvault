package com.miguelcode.warrantyvault.domain.usecase

import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository

class GetWarrantyByIdUseCase(private val repository: WarrantyRepository) {
    suspend operator fun invoke(id: Long): Result<Warranty> = repository.getWarrantyById(id)
}
