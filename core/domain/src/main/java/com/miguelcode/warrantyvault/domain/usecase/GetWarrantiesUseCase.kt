package com.miguelcode.warrantyvault.domain.usecase

import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import kotlinx.coroutines.flow.Flow

class GetWarrantiesUseCase(private val repository: WarrantyRepository) {
    operator fun invoke(): Flow<List<Warranty>> = repository.getAllWarranties()
}
