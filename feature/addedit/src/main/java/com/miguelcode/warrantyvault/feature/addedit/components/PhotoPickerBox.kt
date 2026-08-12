package com.miguelcode.warrantyvault.feature.addedit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.miguelcode.warrantyvault.feature.addedit.R

@Composable
fun PhotoPickerBox(
    photoPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)

    if (isLoading) {
        // Copiar una foto que solo está en la nube tarda: sin este indicador la pantalla
        // parecía no responder al elegirla.
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 16.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (photoPath != null) {
        AsyncImage(
            model = photoPath,
            contentDescription = stringResource(R.string.addedit_photo_ticket),
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 16.dp)
                .clip(shape)
                .clickable(onClick = onClick),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 16.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape = shape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.AddAPhoto,
                    contentDescription = stringResource(R.string.addedit_photo_add),
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.addedit_photo_add_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
