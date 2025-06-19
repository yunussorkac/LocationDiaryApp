package com.app.mapory.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.mapory.ui.components.media.capture.CaptureOption
import com.app.mapory.ui.components.media.capture.CaptureOptionItem
import com.app.mapory.ui.theme.AppRed

@Composable
fun MediaOptionDialog(
    title: String,
    captureOption: CaptureOptionItem,
    pickOption: CaptureOptionItem,
    onDismiss: () -> Unit,
    isAudioRecording: Boolean = false,
    onStartRecording: (() -> Unit)? = null,
    onStopRecording: (() -> Unit)? = null
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingFeedback by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isRecording) {
                    Text(
                        text = recordingFeedback,
                        color = AppRed,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (isAudioRecording) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                when (event.type) {
                                                    PointerEventType.Press -> {
                                                        isRecording = true
                                                        recordingFeedback = "Recording..."
                                                        onStartRecording?.invoke()
                                                    }
                                                    PointerEventType.Release -> {
                                                        isRecording = false
                                                        recordingFeedback = ""
                                                        onStopRecording?.invoke()
                                                    }
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isRecording) AppRed else AppRed.copy(alpha = 0.1f))
                                        .border(
                                            width = 2.dp,
                                            color = AppRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(captureOption.icon),
                                        contentDescription = captureOption.label,
                                        modifier = Modifier.size(32.dp),
                                        tint = if (isRecording) Color.White else AppRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isRecording) "Recording..." else captureOption.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppRed.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        CaptureOption(
                            icon = captureOption.icon,
                            label = captureOption.label,
                            onClick = captureOption.onClick
                        )
                    }

                    CaptureOption(
                        icon = pickOption.icon,
                        label = pickOption.label,
                        onClick = pickOption.onClick
                    )
                }
            }
        }
    }
}