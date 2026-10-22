package com.scholr.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.scholr.app.ScholrViewModel
import com.scholr.app.data.model.FieldPalette
import com.scholr.app.data.model.Paper
import com.scholr.app.data.scan.Book
import com.scholr.app.data.scan.ScanUiState
import com.scholr.app.ui.clay.ClayButton
import com.scholr.app.ui.clay.ClayGhostButton
import com.scholr.app.ui.clay.ClayIconButton
import com.scholr.app.ui.clay.ClayPanel
import com.scholr.app.ui.clay.ClaySearchField
import com.scholr.app.ui.clay.SheetShape
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.theme.AcademicGreen
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.ClaySpacing
import com.scholr.app.ui.theme.FieldMedicine
import com.scholr.app.ui.theme.GreenClayBrush
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading
import java.util.concurrent.Executors

private val ScanFormats = intArrayOf(
    Barcode.FORMAT_QR_CODE,
    Barcode.FORMAT_EAN_13,
    Barcode.FORMAT_EAN_8,
    Barcode.FORMAT_UPC_A
)

/**
 * Live scanner: reads Scholr QR codes (papers and collections), paper links,
 * DOIs and book ISBN barcodes. Results open in a bottom sheet over the camera.
 */
@Composable
fun ScanScreen(
    vm: ScholrViewModel,
    onBack: () -> Unit,
    onOpenPaper: (String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionAsked by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }
    var manualOpen by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        permissionAsked = true
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scanImage(context, uri, vm)
    }

    // Leaving the screen shouldn't leave a stale result sheet waiting for next time.
    DisposableEffect(Unit) { onDispose { vm.dismissScan() } }

    val scanning = vm.scanState is ScanUiState.Idle

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            BarcodeCameraView(
                active = scanning,
                torchOn = torchOn,
                onTorchAvailable = { hasTorch = it },
                onCode = vm::handleScan,
                modifier = Modifier.fillMaxSize()
            )
            Viewfinder(active = scanning, modifier = Modifier.align(Alignment.Center))
        } else {
            PermissionPanel(
                askedAndDenied = permissionAsked,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = ClaySpacing.ListPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ClayIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                tint = TextHeading
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasPermission && hasTorch) {
                    ClayIconButton(
                        icon = if (torchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        contentDescription = "Toggle flashlight",
                        onClick = { torchOn = !torchOn },
                        tint = if (torchOn) ScholrPurple else TextHeading
                    )
                }
                ClayIconButton(
                    icon = Icons.Rounded.PhotoLibrary,
                    contentDescription = "Scan from a photo",
                    onClick = { galleryLauncher.launch("image/*") },
                    tint = TextHeading
                )
            }
        }

        // Bottom: hint + manual entry
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = ClaySpacing.ListPadding, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasPermission && !manualOpen) {
                Text(
                    text = "Point at a Scholr QR code, a paper link, or the barcode on the back of a book.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            if (manualOpen) {
                ClayPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Enter an ISBN, DOI or paper link",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = TextBody,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(ClayRadiusShape)
                                    .clickable { manualOpen = false }
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        ClaySearchField(
                            value = manualText,
                            onValueChange = { manualText = it },
                            placeholder = "978-0-262-03384-8 or 10.1145/…",
                            onSearch = { if (manualText.isNotBlank()) vm.handleScan(manualText) }
                        )
                        Spacer(Modifier.height(12.dp))
                        ClayButton(
                            text = "Look up",
                            onClick = { vm.handleScan(manualText) },
                            enabled = manualText.isNotBlank(),
                            brush = PurpleClayBrush,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                ClayGhostButton(
                    text = "Type an ISBN or DOI instead",
                    onClick = { manualOpen = true },
                    leadingIcon = Icons.Rounded.Keyboard,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    ScanResultSheet(vm = vm, onOpenPaper = onOpenPaper)
}

private val ClayRadiusShape = androidx.compose.foundation.shape.RoundedCornerShape(11.dp)

// ── Camera + ML Kit ──────────────────────────────────────────────────────────

@OptIn(ExperimentalGetImage::class)
@Composable
private fun BarcodeCameraView(
    active: Boolean,
    torchOn: Boolean,
    onTorchAvailable: (Boolean) -> Unit,
    onCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val currentActive by rememberUpdatedState(active)
    val currentOnCode by rememberUpdatedState(onCode)
    val currentOnTorch by rememberUpdatedState(onTorchAvailable)
    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(ScanFormats.first(), *ScanFormats.drop(1).toIntArray())
                .build()
        )
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        providerFuture.addListener({
            val cameraProvider = providerFuture.get()
            provider = cameraProvider

            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analysisExecutor) { proxy ->
                val media = proxy.image
                if (!currentActive || media == null) {
                    proxy.close()
                    return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { codes ->
                        val raw = codes.firstNotNullOfOrNull { it.rawValue }
                        if (raw != null) mainExecutor.execute { if (currentActive) currentOnCode(raw) }
                    }
                    .addOnCompleteListener { proxy.close() }
            }

            cameraProvider.unbindAll()
            val bound = cameraProvider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
            camera = bound
            currentOnTorch(bound.cameraInfo.hasFlashUnit())
        }, mainExecutor)

        onDispose {
            provider?.unbindAll()
            analysisExecutor.shutdown()
            scanner.close()
        }
    }

    LaunchedEffect(camera, torchOn) { camera?.cameraControl?.enableTorch(torchOn) }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Decodes a code from a picked image (a screenshot of a QR, a photo of a book cover…). */
private fun scanImage(context: android.content.Context, uri: Uri, vm: ScholrViewModel) {
    val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(ScanFormats.first(), *ScanFormats.drop(1).toIntArray())
            .build()
    )
    val image = try {
        InputImage.fromFilePath(context, uri)
    } catch (e: Exception) {
        vm.showScanError("Couldn't open that image.")
        scanner.close()
        return
    }
    scanner.process(image)
        .addOnSuccessListener { codes ->
            val raw = codes.firstNotNullOfOrNull { it.rawValue }
            if (raw != null) vm.handleScan(raw)
            else vm.showScanError("No QR code or barcode was found in that image.")
        }
        .addOnFailureListener { vm.showScanError("Couldn't read that image.") }
        .addOnCompleteListener { scanner.close() }
}

// ── Viewfinder overlay ───────────────────────────────────────────────────────

@Composable
private fun Viewfinder(active: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scanLine")
    val line by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanLineY"
    )
    val tint = if (active) AcademicGreen else Color.White
    Canvas(modifier = modifier.size(264.dp)) {
        val w = size.width
        val arm = w * 0.2f
        val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        val r = 18.dp.toPx()

        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(tint, Offset(x, y + dy * r), Offset(x, y + dy * arm), stroke.width, StrokeCap.Round)
            drawLine(tint, Offset(x + dx * r, y), Offset(x + dx * arm, y), stroke.width, StrokeCap.Round)
            drawArc(
                color = tint,
                startAngle = when {
                    dx > 0 && dy > 0 -> 180f
                    dx < 0 && dy > 0 -> 270f
                    dx < 0 && dy < 0 -> 0f
                    else -> 90f
                },
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(if (dx > 0) x else x - 2 * r, if (dy > 0) y else y - 2 * r),
                size = Size(2 * r, 2 * r),
                style = stroke
            )
        }
        corner(0f, 0f, 1f, 1f)
        corner(w, 0f, -1f, 1f)
        corner(0f, w, 1f, -1f)
        corner(w, w, -1f, -1f)

        if (active) {
            drawRoundRect(
                color = tint.copy(alpha = 0.85f),
                topLeft = Offset(w * 0.08f, w * line),
                size = Size(w * 0.84f, 3.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}

@Composable
private fun PermissionPanel(askedAndDenied: Boolean, onRequest: () -> Unit, modifier: Modifier = Modifier) {
    ClayPanel(modifier = modifier.padding(horizontal = 28.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .claySurface(cornerRadius = 26.dp, brush = GreenClayBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Camera access needed", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (askedAndDenied)
                    "Scholr can't see the camera. Allow it in system settings, or use the photo and manual options."
                else
                    "Scholr uses the camera only to read QR codes and barcodes — nothing is recorded.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            ClayButton(text = "Allow camera", onClick = onRequest, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── Result sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanResultSheet(vm: ScholrViewModel, onOpenPaper: (String) -> Unit) {
    val state = vm.scanState
    if (state is ScanUiState.Idle) return

    ModalBottomSheet(
        onDismissRequest = { vm.dismissScan() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = state !is ScanUiState.Loading),
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
                .padding(bottom = 24.dp)
        ) {
            when (state) {
                is ScanUiState.Idle -> Unit
                is ScanUiState.Loading -> LoadingBody(state.label)
                is ScanUiState.Failed -> FailedBody(state.message, onRetry = { vm.dismissScan() })
                is ScanUiState.PaperFound -> PaperBody(
                    paper = state.paper,
                    bookmarked = vm.isBookmarked(state.paper.id),
                    onToggleBookmark = { vm.toggleBookmark(state.paper.id) },
                    onOpen = { vm.dismissScan(); onOpenPaper(state.paper.id) }
                )
                is ScanUiState.CollectionFound -> CollectionBody(
                    state = state,
                    onImport = {
                        vm.importCollection(state.name, state.emoji, state.paperIds)
                        vm.dismissScan()
                    },
                    onOpenPaper = { vm.dismissScan(); onOpenPaper(it) }
                )
                is ScanUiState.BookFound -> BookBody(
                    book = state.book,
                    related = state.related,
                    onOpenPaper = { vm.dismissScan(); onOpenPaper(it) }
                )
            }
        }
    }
}

@Composable
private fun LoadingBody(label: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = ScholrPurple)
        Spacer(Modifier.height(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}

@Composable
private fun FailedBody(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Text("Couldn't use that code", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = FieldMedicine,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        ClayButton(text = "Scan again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PaperBody(
    paper: Paper,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onOpen: () -> Unit
) {
    Text("PAPER FOUND", style = MaterialTheme.typography.labelSmall, color = AcademicGreen)
    Spacer(Modifier.height(8.dp))
    Text(paper.title, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(6.dp))
    Text(paper.authors, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    Spacer(Modifier.height(4.dp))
    Text(
        "${paper.venue} · ${paper.year}",
        style = MaterialTheme.typography.titleSmall,
        color = FieldPalette.colorFor(paper.field)
    )
    Spacer(Modifier.height(10.dp))
    Text(
        paper.abstractText,
        style = MaterialTheme.typography.bodySmall,
        color = TextBody,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ClayGhostButton(
            text = if (bookmarked) "Saved" else "Save",
            onClick = onToggleBookmark,
            leadingIcon = if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            modifier = Modifier.weight(1f)
        )
        ClayButton(
            text = "Open paper",
            onClick = onOpen,
            brush = PurpleClayBrush,
            modifier = Modifier.weight(1.4f)
        )
    }
}

@Composable
private fun CollectionBody(
    state: ScanUiState.CollectionFound,
    onImport: () -> Unit,
    onOpenPaper: (String) -> Unit
) {
    Text("COLLECTION FOUND", style = MaterialTheme.typography.labelSmall, color = AcademicGreen)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(state.emoji, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(state.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = if (state.papers.size == state.paperIds.size)
                    (if (state.paperIds.size == 1) "1 paper" else "${state.paperIds.size} papers")
                else "${state.papers.size} of ${state.paperIds.size} papers could be loaded",
                style = MaterialTheme.typography.bodySmall,
                color = TextBody
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    state.papers.take(6).forEach { paper ->
        PaperRow(paper, onClick = { onOpenPaper(paper.id) })
        Spacer(Modifier.height(10.dp))
    }
    if (state.papers.size > 6) {
        Text(
            "+ ${state.papers.size - 6} more",
            style = MaterialTheme.typography.labelMedium,
            color = TextBody
        )
        Spacer(Modifier.height(10.dp))
    }
    Spacer(Modifier.height(6.dp))
    ClayButton(
        text = "Add to my collections",
        onClick = onImport,
        leadingIcon = Icons.Rounded.PlaylistAdd,
        brush = GreenClayBrush,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BookBody(book: Book, related: List<Paper>, onOpenPaper: (String) -> Unit) {
    Text("BOOK FOUND · ISBN ${book.isbn13}", style = MaterialTheme.typography.labelSmall, color = AcademicGreen)
    Spacer(Modifier.height(8.dp))
    Text(book.title, style = MaterialTheme.typography.headlineSmall)
    if (book.authors.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(book.authors.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
    val meta = listOf(book.publisher, book.year).filter { it.isNotBlank() }.joinToString(" · ")
    if (meta.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(meta, style = MaterialTheme.typography.titleSmall, color = ScholrPurple)
    }
    if (book.description.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(
            book.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextBody,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
    Spacer(Modifier.height(18.dp))
    Text("Related research", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    if (related.isEmpty()) {
        Text(
            "No closely related papers turned up for this title.",
            style = MaterialTheme.typography.bodySmall,
            color = TextBody
        )
    } else {
        related.forEach { paper ->
            PaperRow(paper, onClick = { onOpenPaper(paper.id) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
internal fun PaperRow(paper: Paper, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(cornerRadius = ClayRadius.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            paper.title,
            style = MaterialTheme.typography.titleSmall,
            color = TextHeading,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "${paper.authors.substringBefore(',')} · ${paper.venue} · ${paper.year}",
            style = MaterialTheme.typography.labelSmall,
            color = TextBody,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
