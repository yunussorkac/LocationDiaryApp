package com.app.mapory.ui.components.audio

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.mapory.R
import com.app.mapory.ui.theme.AlternativeWhite
import com.app.mapory.ui.theme.AppBlue
import kotlinx.coroutines.delay

@Composable
fun AudioSection(audios: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Audios",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        audios.forEach { audioUrl ->
            key(audioUrl) {
                AudioPlayer(
                    audioUrl = audioUrl,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AudioPlayer(
    audioUrl: String,
    modifier: Modifier = Modifier
) {
    val mediaPlayer = remember { MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(0) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        try {
            mediaPlayer.apply {
                reset()
                setDataSource(audioUrl)
                prepareAsync()
                setOnPreparedListener { player ->
                    duration = player.duration
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                    sliderPosition = 0f
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        }
    }

    LaunchedEffect(isPlaying, isDragging) {
        while (isPlaying && !isDragging) {
            try {
                delay(100)
                currentPosition = mediaPlayer.currentPosition
                sliderPosition = currentPosition.toFloat()
            } catch (e: Exception) {
                break
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        mediaPlayer.pause()
                    } else {
                        mediaPlayer.start()
                    }
                    isPlaying = !isPlaying
                }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying)
                            R.drawable.baseline_pause_24
                        else
                            R.drawable.baseline_play_arrow_24
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = AppBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                ) {
                    Slider(
                        value = sliderPosition,
                        onValueChange = { newPosition ->
                            isDragging = true
                            sliderPosition = newPosition
                            currentPosition = newPosition.toInt()
                        },
                        onValueChangeFinished = {
                            mediaPlayer.seekTo(sliderPosition.toInt())
                            isDragging = false
                            if (!isPlaying) {
                                currentPosition = sliderPosition.toInt()
                            }
                        },
                        valueRange = 0f..duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = AppBlue,
                            activeTrackColor = AppBlue,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun AudioItem(
    audioUri: Uri,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.LightGray)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.audio_spectrum_analyzer_svgrepo_com),
            contentDescription = "Audio file",
            modifier = Modifier.size(40.dp),
            tint = Color.Black
        )
    }
}


private fun formatDuration(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

