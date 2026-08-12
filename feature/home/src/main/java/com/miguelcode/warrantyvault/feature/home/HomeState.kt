package com.miguelcode.warrantyvault.feature.home

import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyStatus

data class HomeState(
    val isLoading: Boolean = true,
    val warranties: List<Warranty> = emptyList(),
    val selectedFilter: WarrantyFilter = WarrantyFilter.ALL,
    val searchQuery: String = "",
    val isPremium: Boolean = false,
    val usedSlots: Int = 0
) {
    val filteredWarranties: List<Warranty>
        get() {
            val filtered = when (selectedFilter) {
                WarrantyFilter.ALL -> warranties
                WarrantyFilter.ACTIVE -> warranties.filter { it.status == WarrantyStatus.ACTIVE }
                WarrantyFilter.EXPIRING_SOON -> warranties.filter { it.status == WarrantyStatus.EXPIRING_SOON }
                WarrantyFilter.EXPIRED -> warranties.filter { it.status == WarrantyStatus.EXPIRED }
            }
            return if (searchQuery.isBlank()) filtered
            else filtered.filter {
                it.productName.contains(searchQuery, ignoreCase = true) ||
                        it.store.contains(searchQuery, ignoreCase = true)
            }
        }

    val activeCount: Int get() = warranties.count { it.status == WarrantyStatus.ACTIVE }
    val expiringCount: Int get() = warranties.count { it.status == WarrantyStatus.EXPIRING_SOON }
    val expiredCount: Int get() = warranties.count { it.status == WarrantyStatus.EXPIRED }
    val showPremiumBanner: Boolean get() = !isPremium && usedSlots >= 2
    /** Free + 3+ garantías → no se puede añadir más sin hacerse premium. */
    val limitReached: Boolean get() = !isPremium && usedSlots >= MAX_FREE_WARRANTIES
}

const val MAX_FREE_WARRANTIES = 3

enum class WarrantyFilter {
    ALL, ACTIVE, EXPIRING_SOON, EXPIRED
}
