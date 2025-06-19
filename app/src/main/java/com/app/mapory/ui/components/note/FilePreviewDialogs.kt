package com.app.mapory.ui.components.note

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.mapory.util.DummyMethods.Companion.extractWordContent
import com.rizzi.bouquet.ResourceType
import com.rizzi.bouquet.VerticalPDFReader
import com.rizzi.bouquet.rememberVerticalPdfReaderState
import java.net.URLEncoder

@Composable
fun WordDocumentDialog(
    wordUrl: String,
    onDismiss: () -> Unit
) {
    val isLoading = remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading.value = false
                                    }
                                }
                                settings.javaScriptEnabled = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false

                                val encodedUrl = URLEncoder.encode(wordUrl, "UTF-8")
                                loadUrl("https://docs.google.com/gview?embedded=true&url=$encodedUrl")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading.value) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun PDFDocumentDialog(
    pdfUrl: String,
    onDismiss: () -> Unit
) {
    val isLoading = remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading.value = false
                                    }
                                }
                                settings.javaScriptEnabled = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false

                                val encodedUrl = URLEncoder.encode(pdfUrl, "UTF-8")
                                Log.d("PDFDocumentDialog", "Encoded URL: $encodedUrl")
                                loadUrl("https://docs.google.com/gview?embedded=true&url=$encodedUrl")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading.value) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun PDFViewerDialog(
    pdfUri: Uri,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val pdfState = rememberVerticalPdfReaderState(
            resource = ResourceType.Local(pdfUri),
            isZoomEnable = true
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                VerticalPDFReader(
                    state = pdfState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
fun LocalWordDocumentDialog(
    uri: Uri,
    context: Context,
    onDismiss: () -> Unit
) {
    val isLoading = remember { mutableStateOf(true) }
    val htmlContent = remember { mutableStateOf("") }

    LaunchedEffect(uri) {
        try {
            val content = extractWordContent(uri, context)
            htmlContent.value = content
            isLoading.value = false
        } catch (e: Exception) {
            e.printStackTrace()
            htmlContent.value = "<p>Error loading document: ${e.message}</p>"
            isLoading.value = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading.value) {
                        CircularProgressIndicator()
                    } else {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                }
                            },
                            update = { webView ->
                                val htmlWithStyle = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <style>
                                            body {
                                                font-family: Arial, sans-serif;
                                                line-height: 1.6;
                                                padding: 16px;
                                                margin: 0;
                                            }
                                            img {
                                                max-width: 100%;
                                                height: auto;
                                            }
                                        </style>
                                    </head>
                                    <body>
                                        ${htmlContent.value}
                                    </body>
                                    </html>
                                """.trimIndent()
                                webView.loadDataWithBaseURL(null, htmlWithStyle, "text/html", "UTF-8", null)
                            }
                        )
                    }
                }
            }
        }
    }
}