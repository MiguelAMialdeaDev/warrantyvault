package com.miguelcode.warrantyvault.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.miguelcode.warrantyvault.feature.home.R
import com.miguelcode.warrantyvault.feature.home.WarrantyFilter
import com.miguelcode.warrantyvault.ui.theme.WarrantyVaultTheme

@Composable
fun FilterChipsRow(
    selectedFilter: WarrantyFilter,
    onFilterSelected: (WarrantyFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        WarrantyFilter.ALL to stringResource(R.string.home_filter_all),
        WarrantyFilter.ACTIVE to stringResource(R.string.home_filter_active),
        WarrantyFilter.EXPIRING_SOON to stringResource(R.string.home_filter_expiring),
        WarrantyFilter.EXPIRED to stringResource(R.string.home_filter_expired)
    )

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (filter, label) ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.surfaceVariant,
                    borderWidth = 1.dp,
                    enabled = true,
                    selected = false
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FilterChipsPreview() {
    WarrantyVaultTheme {
        FilterChipsRow(
            selectedFilter = WarrantyFilter.ALL,
            onFilterSelected = {}
        )
    }
}
