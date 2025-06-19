package com.app.mapory.ui.components.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.app.mapory.ui.components.media.audio.AudioPreview
import com.app.mapory.ui.components.media.note.NotePreview
import com.app.mapory.ui.components.media.photo.PhotoPreview
import com.app.mapory.ui.components.media.video.VideoPreview
import com.app.mapory.ui.theme.AppRed


@Composable
fun MediaPreviewDialog(
    uri: Uri,
    mediaType: MediaType,
    onDismiss: () -> Unit,
    allVideos: List<Uri> = emptyList(),
    allPhotos: List<Uri> = emptyList()
) {
    when (mediaType) {
        MediaType.PHOTO ->  PhotoPreview(uri,allPhotos, onDismiss)
        MediaType.VIDEO -> VideoPreview(uri, onDismiss, allVideos)
        MediaType.AUDIO -> AudioPreview(uri, onDismiss)
        MediaType.NOTE -> NotePreview(uri, onDismiss)
    }
}


@Composable
fun MediaButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(50.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selected) AppRed
                    else AppRed.copy(alpha = 0.1f)
                )
                .border(
                    width = 2.dp,
                    color = if (selected) AppRed
                    else AppRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(25.dp),
                tint = Color.Unspecified
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) AppRed
            else AppRed.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun MediaSection(
    title: String,
    count: Int,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = CircleShape,
                color = AppRed,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
        content()
        Spacer(modifier = Modifier.height(16.dp))
    }
}


enum class MediaType {
    PHOTO, VIDEO, AUDIO, NOTE
}
