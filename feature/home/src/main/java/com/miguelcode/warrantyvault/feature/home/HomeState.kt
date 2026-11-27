package com.miguelcode.warrantyvault.feature.home

import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyStatus

data class HomeState(
    val isLoading: Boolean = true,
    val warranties: List<Warranty> = emptyList(),
    val selectedFilter: WarrantyFilter = WarrantyFilter.ALL,
    val searchQuery: String = "",
    val isPremium: Boolean = false,
    val usedSlots: Int = 0,
    /** No se pudo leer la base de datos. No es lo mismo que no tener garantías. */
    val loadError: Boolean = false
) {
    /**
     * Calculada una vez por estado (antes en cada acceso, varias veces por frame) y en
     * orden de urgencia: primero las que están por vencer, luego las activas por fecha de
     * fin y al final las caducadas, las más recientes antes. Con el orden de la consulta
     * (fecha de fin ascendente) la lista empezaba por las caducadas más antiguas.
     */
    val filteredWarranties: List<Warranty> by lazy {
        val filtered = when (selectedFilter) {
            WarrantyFilter.ALL -> warranties
            WarrantyFilter.ACTIVE -> warranties.filter { it.status == WarrantyStatus.ACTIVE }
            WarrantyFilter.EXPIRING_SOON -> warranties.filter { it.status == WarrantyStatus.EXPIRING_SOON }
            WarrantyFilter.EXPIRED -> warranties.filter { it.status == WarrantyStatus.EXPIRED }
        }
        val searched = if (searchQuery.isBlank()) filtered
        else filtered.filter {
            it.productName.contains(searchQuery, ignoreCase = true) ||
                    it.store.contains(searchQuery, ignoreCase = true)
        }
        searched.sortedWith(byUrgency)
    }

    val activeCount: Int get() = warranties.count { it.status == WarrantyStatus.ACTIVE }
    val expiringCount: Int get() = warranties.count { it.status == WarrantyStatus.EXPIRING_SOON }
    val expiredCount: Int get() = warranties.count { it.status == WarrantyStatus.EXPIRED }
    val showPremiumBanner: Boolean get() = !isPremium && usedSlots >= 2
    /** Free + 3+ garantías → no se puede añadir más sin hacerse premium. */
    val limitReached: Boolean get() = !isPremium && usedSlots >= MAX_FREE_WARRANTIES
}

const val MAX_FREE_WARRANTIES = 3

private val byUrgency: Comparator<Warranty> = Comparator { a, b ->
    val rank = urgencyRank(a.status).compareTo(urgencyRank(b.status))
    when {
        rank != 0 -> rank
        a.status == WarrantyStatus.EXPIRED -> b.warrantyEndDate.compareTo(a.warrantyEndDate)
        else -> a.warrantyEndDate.compareTo(b.warrantyEndDate)
    }
}

private fun urgencyRank(status: WarrantyStatus): Int = when (status) {
    WarrantyStatus.EXPIRING_SOON -> 0
    WarrantyStatus.ACTIVE -> 1
    WarrantyStatus.EXPIRED -> 2
}

enum class WarrantyFilter {
    ALL, ACTIVE, EXPIRING_SOON, EXPIRED
}
