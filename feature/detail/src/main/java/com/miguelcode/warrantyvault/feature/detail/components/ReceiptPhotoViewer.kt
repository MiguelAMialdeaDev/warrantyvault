package com.miguelcode.warrantyvault.feature.detail.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.miguelcode.warrantyvault.feature.detail.R
import java.io.File

/**
 * El ticket a pantalla completa, entero y con zoom. En el detalle va recortado a 220dp
 * y, justo al reclamar, lo que hace falta leer (fecha y total) suele quedar fuera.
 */
@Composable
fun ReceiptPhotoViewer(
    photo: File,
    productName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = photo,
                contentDescription = stringResource(R.string.detail_photo_ticket),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            offset = if (scale == MIN_SCALE) Offset.Zero else offset + pan
                        }
                    }
                    .pointerInput(Unit) {
                        // Doble toque: alterna entre ver el ticket entero y ampliarlo.
                        detectTapGestures(onDoubleTap = {
                            if (scale > MIN_SCALE) {
                                scale = MIN_SCALE
                                offset = Offset.Zero
                            } else {
                                scale = DOUBLE_TAP_SCALE
                            }
                        })
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .align(Alignment.TopCenter),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.detail_photo_close), tint = Color.White)
                }
                IconButton(onClick = { shareReceipt(context, photo, productName) }) {
                    Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.detail_photo_share), tint = Color.White)
                }
            }
        }
    }
}

/**
 * Comparte el ticket con la app que elija el usuario (correo al servicio técnico,
 * WhatsApp a la tienda…). Sale por FileProvider con permiso de lectura temporal: la
 * ruta privada de la app nunca se expone.
 */
private fun shareReceipt(context: Context, photo: File, productName: String) {
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo)
    }.getOrNull() ?: return
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        // Sin ClipData, el permiso de lectura solo viaja con el extra y el selector del
        // sistema no puede abrir la uri para la vista previa (Permission Denial en logcat).
        clipData = ClipData.newRawUri(null, uri)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.detail_photo_share_subject, productName))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(send, null))
    } catch (_: ActivityNotFoundException) {
        // Ninguna app puede recibirla: no hay nada útil que hacer.
    }
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
