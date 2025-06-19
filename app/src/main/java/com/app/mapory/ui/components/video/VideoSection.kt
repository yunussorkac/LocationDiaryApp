package com.app.mapory.ui.components.video

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.app.mapory.R
import com.app.mapory.ui.theme.AlternativeWhite
import com.app.mapory.ui.theme.AppBlue
import kotlin.math.absoluteValue


@Composable
fun VideoSection(videos: List<String>, thumbnails: List<String> = emptyList()) {
    var selectedVideoIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Videos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow {
            items(videos.size) { index ->
                val videoUrl = videos[index]
                val thumbnailUrl = thumbnails.getOrNull(index)

                Card(
                    modifier = Modifier
                        .size(150.dp)
                        .padding(end = 8.dp)
                        .clickable {
                            selectedVideoIndex = index
                        },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (thumbnailUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(thumbnailUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                AppBlue,
                                                AlternativeWhite
                                            )
                                        )
                                    )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        )

                        // Play button
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }

    selectedVideoIndex?.let { index ->
        VideoPlayerDialog(
            videos = videos,
            initialPage = index,
            onDismiss = { selectedVideoIndex = null }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerDialog(
    videos: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { videos.size }
    )

    var offsetY by remember { mutableFloatStateOf(0f) }
    var startTouchY by remember { mutableFloatStateOf(0f) }

    val players = remember {
        videos.map { videoUrl ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
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
                        factory = { context ->
                            PlayerView(context).apply {
                                this.player = players[page]
                                this.layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                this.useController = true
                                this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
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