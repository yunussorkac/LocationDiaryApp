package com.app.mapory.ui.components.media.video

import android.content.ContentUris
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.app.mapory.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

@OptIn(UnstableApi::class, ExperimentalFoundationApi::class)
@Composable
fun VideoPreview(
    uri: Uri,
    onDismiss: () -> Unit,
    allVideos: List<Uri> = emptyList()
) {
    val context = LocalContext.current
    val initialPage = allVideos.indexOf(uri).takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { allVideos.size })

    var offsetY by remember { mutableFloatStateOf(0f) }
    var startTouchY by remember { mutableFloatStateOf(0f) }


    val videos = if (allVideos.isEmpty()) listOf(uri) else allVideos
    val players = remember(videos) {
        videos.map { videoUri ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUri))
                prepare()
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        players.forEachIndexed { index, player ->
            if (index == pagerState.currentPage) {
                player.play()
            } else {
                player.pause()
                player.seekTo(0)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            players.forEach {
                it.stop()
                it.release()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            players.forEach { it.stop() }
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .graphicsLayer {
                    translationY = offsetY
                    alpha = 1f - (offsetY.absoluteValue / 1000f).coerceIn(0f, 0.5f)
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            startTouchY = offset.y
                        },
                        onDragEnd = {
                            if (offsetY > 100f) {
                                players.forEach { it.stop() }
                                onDismiss()
                            } else {
                                offsetY = 0f
                            }
                        },
                        onDragCancel = {
                            offsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (dragAmount.y > 0 || offsetY > 0) {
                                offsetY += dragAmount.y
                            }
                        }
                    )
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = players[page]
                                layoutParams = ViewGroup.LayoutParams(
                                    MATCH_PARENT,
                                    MATCH_PARENT
                                )
                                useController = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setShowPreviousButton(false)
                                setShowNextButton(false)
                                setShowRewindButton(false)
                                setShowFastForwardButton(false)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            IconButton(
                onClick = {
                    players.forEach { it.stop() }
                    onDismiss()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun VideoThumbnail(
    videoUri: Uri,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(videoUri) {
        if (thumbnailUrl.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, videoUri)
                    bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    retriever.release()

                    if (bitmap == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        bitmap = context.contentResolver.loadThumbnail(
                            videoUri,
                            Size(100, 100),
                            null
                        )
                    }

                    if (bitmap == null) {
                        bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                            context.contentResolver,
                            ContentUris.parseId(videoUri),
                            MediaStore.Video.Thumbnails.MINI_KIND,
                            null
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bitmap?.recycle()
        }
    }


    Box(
        modifier = modifier
            .background(Color.Gray)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            !thumbnailUrl.isNullOrEmpty() -> {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Video thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Video thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            isLoading -> {
                CircularProgressIndicator(color = Color.White)
            }
            else -> {
                Icon(
                    painter = painterResource(R.drawable.baseline_videocam_24),
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play video",
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )
    }
}
