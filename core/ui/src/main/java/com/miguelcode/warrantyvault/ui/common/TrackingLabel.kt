package com.miguelcode.warrantyvault.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.miguelcode.warrantyvault.domain.model.WarrantyTracking
import com.miguelcode.warrantyvault.ui.R

/** Nombre visible del seguimiento, o null cuando la garantía no tiene ninguno. */
@Composable
fun WarrantyTracking.label(): String? = when (this) {
    WarrantyTracking.NONE -> null
    WarrantyTracking.IN_CLAIM -> stringResource(R.string.tracking_in_claim)
    WarrantyTracking.REPAIRED -> stringResource(R.string.tracking_repaired)
    WarrantyTracking.ARCHIVED -> stringResource(R.string.tracking_archived)
}
