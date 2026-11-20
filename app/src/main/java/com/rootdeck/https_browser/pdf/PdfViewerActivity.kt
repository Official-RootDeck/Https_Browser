// Built by MD.Nirob
package com.rootdeck.https_browser.pdf

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.rootdeck.https_browser.R
import com.rootdeck.https_browser.ui.theme.ChromeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class PdfViewerActivity : ComponentActivity() {

    private var localPdfFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri: Uri? = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
        val urlString = intent.getStringExtra("pdf_url")

        setContent {
            ChromeTheme(isIncognito = false) {
                PdfViewerScreen(
                    uri = uri,
                    rawUrl = urlString,
                    onBack = { finish() },
                    onOpenSystemViewer = { openInSystemViewer() }
                )
            }
        }
    }

    private fun openInSystemViewer() {
        val file = localPdfFile ?: return
        try {
            val contentUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.new_pdf_error), Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PdfViewerScreen(
        uri: Uri?,
        rawUrl: String?,
        onBack: () -> Unit,
        onOpenSystemViewer: () -> Unit
    ) {
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isPasswordProtected by remember { mutableStateOf(false) }
        var pageBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale = (scale * zoomChange).coerceIn(0.8f, 3.5f)
            offset += offsetChange
        }

        LaunchedEffect(uri, rawUrl) {
            withContext(Dispatchers.IO) {
                try {
                    val pdfDir = File(cacheDir, "pdfs").apply { mkdirs() }
                    val targetFile = File(pdfDir, "viewer_${System.currentTimeMillis()}.pdf")

                    if (uri != null) {
                        contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } else if (!rawUrl.isNullOrEmpty()) {
                        val url = URL(rawUrl)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 15000
                        conn.readTimeout = 15000
                        conn.inputStream.use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    if (!targetFile.exists() || targetFile.length() == 0L) {
                        throw IllegalStateException("Failed to load PDF file")
                    }

                    localPdfFile = targetFile

                    // Render pages using PdfRenderer
                    try {
                        val pfd = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(pfd)
                        val bitmaps = mutableListOf<Bitmap>()
                        val count = renderer.pageCount
                        for (i in 0 until count) {
                            val page = renderer.openPage(i)
                            // Scale down slightly if large for memory safety
                            val width = (page.width * 1.5).toInt().coerceAtMost(2048)
                            val height = (page.height * 1.5).toInt().coerceAtMost(2048)
                            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmaps.add(bmp)
                            page.close()
                        }
                        renderer.close()
                        pfd.close()

                        withContext(Dispatchers.Main) {
                            pageBitmaps = bitmaps
                            isLoading = false
                        }
                    } catch (secEx: SecurityException) {
                        withContext(Dispatchers.Main) {
                            isPasswordProtected = true
                            isLoading = false
                            Toast.makeText(
                                this@PdfViewerActivity,
                                getString(R.string.new_pdf_password_required),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage = e.message ?: "Failed to open PDF"
                        isLoading = false
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (pageBitmaps.isNotEmpty()) "PDF (${pageBitmaps.size} pages)" else "PDF Viewer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("pdf_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { scale = (scale + 0.25f).coerceAtMost(3.5f) }) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                        }
                        IconButton(onClick = {
                            scale = (scale - 0.25f).coerceAtLeast(0.8f)
                            if (scale <= 1f) offset = Offset.Zero
                        }) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                        }
                        IconButton(
                            onClick = onOpenSystemViewer,
                            modifier = Modifier.testTag("pdf_open_system_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = stringResource(R.string.new_pdf_open_system)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF1E2124)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.new_pdf_loading),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                    isPasswordProtected -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.new_pdf_password_required),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onOpenSystemViewer) {
                                Text(stringResource(R.string.new_pdf_open_system))
                            }
                        }
                    }
                    errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.new_pdf_error),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onOpenSystemViewer) {
                                Text(stringResource(R.string.new_pdf_open_system))
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .transformable(state = transformState)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                itemsIndexed(pageBitmaps) { index, bmp ->
                                    Card(
                                        shape = RoundedCornerShape(4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .fillMaxWidth(0.95f)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "PDF Page ${index + 1}",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                            Text(
                                                text = "Page ${index + 1} of ${pageBitmaps.size}",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
