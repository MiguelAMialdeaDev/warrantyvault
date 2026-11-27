package com.miguelcode.warrantyvault.feature.addedit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miguelcode.warrantyvault.domain.util.toPriceInput
import com.miguelcode.warrantyvault.feature.addedit.R
import com.miguelcode.warrantyvault.feature.addedit.receipt.ReceiptGuess
import com.miguelcode.warrantyvault.ui.common.rememberDateFormatter

/**
 * Lo que el OCR ha leído del ticket, para rellenar con un toque. Sin Premium se enseña
 * igual (así se ve que funciona) y el botón lleva a la pantalla de planes.
 */
@Composable
fun ReceiptGuessCard(
    guess: ReceiptGuess,
    canAutofill: Boolean,
    onApply: () -> Unit,
    onGoPremium: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fmt = rememberDateFormatter()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = stringResource(R.string.addedit_receipt_found_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            val lines = listOfNotNull(
                guess.store?.let { stringResource(R.string.addedit_receipt_store, it) },
                guess.purchaseDate?.let { stringResource(R.string.addedit_receipt_date, it.format(fmt)) },
                guess.total?.let { stringResource(R.string.addedit_receipt_total, it.toPriceInput()) }
            )
            lines.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.addedit_receipt_dismiss)) }
                Button(onClick = if (canAutofill) onApply else onGoPremium) {
                    Text(
                        stringResource(
                            if (canAutofill) R.string.addedit_receipt_apply else R.string.addedit_receipt_apply_premium
                        )
                    )
                }
            }
        }
    }
}
