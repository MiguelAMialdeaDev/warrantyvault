package com.miguelcode.warrantyvault.domain.usecase

import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository

class CheckPremiumLimitUseCase(
    private val warrantyRepository: WarrantyRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): Boolean {
        if (preferencesRepository.isPremium()) return true
        return warrantyRepository.getCount() < MAX_FREE_WARRANTIES
    }

    companion object {
        const val MAX_FREE_WARRANTIES = 3
    }
}
