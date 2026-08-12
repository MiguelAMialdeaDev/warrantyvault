package com.miguelcode.warrantyvault.feature.detail

import com.miguelcode.warrantyvault.domain.model.Warranty

data class DetailState(
    val isLoading: Boolean = true,
    val warranty: Warranty? = null,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    /** El id pedido no existe: nunca llegó a cargarse ninguna garantía. */
    val notFound: Boolean = false,
    val error: String? = null
)
