package com.app.mapory.ui.components.media.photo

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import kotlin.math.abs


@Composable
fun PhotoPreview(
    uri: Uri,
    photoList: List<Uri>,
    onDismiss: () -> Unit
) {
    val startIndex = photoList.indexOf(uri).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { photoList.size })
    val scope = rememberCoroutineScope()

    var offsetY by remember { mutableFloatStateOf(0f) }
    var dialogScale by remember { mutableFloatStateOf(1f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = (1f - (abs(offsetY) / 800f)).coerceIn(0f, 1f)
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            val threshold = 250f
                            if (abs(offsetY) > threshold) {
                                onDismiss()
                            } else {
                                scope.launch {
                                    offsetY = 0f
                                    dialogScale = 1f
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            offsetY += dragAmount.y
                            dialogScale = (1f - (abs(offsetY) / 1200f)).coerceIn(0.75f, 1f)
                        }
                    )
                }
                .graphicsLayer {
                    translationY = offsetY
                    scaleX = dialogScale
                    scaleY = dialogScale
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->

                val zoomState = rememberZoomState()

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoList[page])
                            .crossfade(true)
                            .build(),
                        contentDescription = "Full Screen Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(
                                zoomState = zoomState,
                            )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}




