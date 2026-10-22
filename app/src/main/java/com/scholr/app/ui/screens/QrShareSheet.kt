package com.scholr.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.scholr.app.data.scan.QrCode
import com.scholr.app.data.scan.QrMatrix
import com.scholr.app.ui.clay.ClayButton
import com.scholr.app.ui.clay.ClayGhostButton
import com.scholr.app.ui.clay.SheetShape
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.TextBody
import java.io.File
import java.io.FileOutputStream

private val QrDark = Color(0xFF2B2250)

/** A QR code drawn as rounded modules on a white clay tile. */
@Composable
fun QrCodeView(payload: String, modifier: Modifier = Modifier, size: Dp = 240.dp) {
    val matrix = remember(payload) { QrCode.encode(payload) }
    Column(
        modifier = modifier
            .claySurface(cornerRadius = ClayRadius.Card, brush = Brush.linearGradient(listOf(Color.White, Color.White)))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (matrix == null) {
            Text(
                "This is too long to fit in a QR code.",
                style = MaterialTheme.typography.bodySmall,
                color = TextBody,
                textAlign = TextAlign.Center,
                modifier = Modifier.size(size).padding(24.dp)
            )
        } else {
            QrCanvas(matrix, Modifier.size(size))
        }
    }
}

@Composable
private fun QrCanvas(matrix: QrMatrix, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val cell = this.size.minDimension / matrix.size
        val radius = CornerRadius(cell * 0.22f)
        for (y in 0 until matrix.size) for (x in 0 until matrix.size) {
            if (matrix[x, y]) {
                drawRoundRect(
                    color = QrDark,
                    topLeft = Offset(x * cell, y * cell),
                    size = Size(cell + 0.6f, cell + 0.6f),
                    cornerRadius = radius
                )
            }
        }
    }
}

/**
 * Bottom sheet that shows [payload] as a QR code with share actions.
 * [extra] is a slot for content under the code (e.g. a collection's paper list).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrShareSheet(
    title: String,
    subtitle: String,
    payload: String,
    shareCaption: String,
    hint: String,
    onDismiss: () -> Unit,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ClayBase,
        shape = SheetShape,
        dragHandle = { ClayDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextBody,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(20.dp))
            QrCodeView(payload)
            Spacer(Modifier.height(14.dp))
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = TextBody,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            ClayButton(
                text = "Share QR image",
                onClick = { shareQrImage(context, payload, shareCaption) },
                leadingIcon = Icons.Rounded.Image,
                brush = PurpleClayBrush,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            ClayGhostButton(
                text = "Share as text",
                onClick = { shareText(context, "$shareCaption\n$payload") },
                leadingIcon = Icons.Rounded.Share,
                modifier = Modifier.fillMaxWidth()
            )
            if (extra != null) {
                Spacer(Modifier.height(22.dp))
                extra()
            }
        }
    }
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share"))
}

/** Renders the QR to a PNG in the app cache and hands it to the system share sheet. */
fun shareQrImage(context: Context, payload: String, caption: String) {
    val matrix = QrCode.encode(payload) ?: return
    val bitmap = QrCode.toBitmap(matrix)
    val dir = File(context.cacheDir, "qr").apply { mkdirs() }
    val file = File(dir, "scholr-qr.png")
    FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share QR code"))
}
