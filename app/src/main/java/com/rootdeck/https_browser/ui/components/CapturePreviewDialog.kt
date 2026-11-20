// Built by MD.Nirob
package com.rootdeck.https_browser.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rootdeck.https_browser.R
import com.rootdeck.https_browser.capture.FullPageCapture
import com.rootdeck.https_browser.pdf.PdfViewerActivity
import com.rootdeck.https_browser.ui.theme.ChromeBlue
import com.rootdeck.https_browser.ui.theme.SurfaceDark
import com.rootdeck.https_browser.ui.theme.TextPrimaryDark
import com.rootdeck.https_browser.ui.theme.TextSecondaryDark

@Composable
fun CapturePreviewDialog(
    bitmap: Bitmap,
    pageTitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .border(1.dp, Color(0xFF3C4043), RoundedCornerShape(16.dp))
                .testTag("capture_preview_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.new_capture_preview_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "${bitmap.width} × ${bitmap.height} px",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141517))
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured page preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val ok = FullPageCapture.saveBitmapToGallery(context, bitmap, pageTitle)
                            Toast.makeText(
                                context,
                                if (ok) "Screenshot saved" else context.getString(R.string.new_capture_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("capture_save_png_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.new_capture_save_png), fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val pdfFile = FullPageCapture.exportBitmapToPdf(context, bitmap, pageTitle)
                            if (pdfFile != null) {
                                val uri = FullPageCapture.getShareUri(context, pdfFile)
                                onDismiss()
                                context.startActivity(
                                    Intent(context, PdfViewerActivity::class.java).apply {
                                        data = uri
                                    }
                                )
                            } else {
                                Toast.makeText(context, context.getString(R.string.new_pdf_error), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChromeBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("capture_export_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.new_capture_export_pdf), fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = {
                            val file = FullPageCapture.saveBitmapToCache(context, bitmap)
                            if (file != null) {
                                val uri = FullPageCapture.getShareUri(context, file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.new_capture_share)))
                            }
                        },
                        modifier = Modifier.testTag("capture_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.new_capture_share), tint = ChromeBlue)
                    }
                }
            }
        }
    }
}
