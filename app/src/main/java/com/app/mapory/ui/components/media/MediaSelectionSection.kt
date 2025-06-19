package com.app.mapory.ui.components.media

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.app.mapory.R
import com.app.mapory.ui.components.audio.AudioItem
import com.app.mapory.ui.components.media.capture.CaptureOptionItem
import com.app.mapory.ui.components.media.video.VideoThumbnail
import com.app.mapory.ui.components.note.LocalWordDocumentDialog
import com.app.mapory.ui.components.note.NoteViewerDialog
import com.app.mapory.ui.components.note.PDFDocumentDialog
import com.app.mapory.ui.components.note.PDFViewerDialog
import com.app.mapory.ui.components.note.WordDocumentDialog
import com.app.mapory.ui.theme.AppRed
import com.app.mapory.util.DummyMethods.Companion.extractWordContent
import com.app.mapory.util.DummyMethods.Companion.getFileType
import com.app.mapory.viewmodel.AddLocationScreenViewModel
import com.app.mapory.viewmodel.LocationUiState
import com.rizzi.bouquet.ResourceType
import com.rizzi.bouquet.VerticalPDFReader
import com.rizzi.bouquet.rememberVerticalPdfReaderState
import java.io.File

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MediaSelectionSection(
    uiState: LocationUiState,
    onPhotoPickerClick: () -> Unit,
    onVideoPickerClick: () -> Unit,
    onAudioPickerClick: () -> Unit,
    onNotePickerClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDeletePhoto: (Uri) -> Unit,
    onDeleteVideo: (Uri) -> Unit,
    onDeleteAudio: (Uri) -> Unit,
    onDeleteNote: (Uri) -> Unit,
    onVideoRecordClick: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    context: Context,
    viewModel : AddLocationScreenViewModel

) {
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var previewType by remember { mutableStateOf<MediaType?>(null) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }

    var showNoteDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    val noteContent by viewModel.noteContent.collectAsStateWithLifecycle()
    var selectedPdfUrl by remember { mutableStateOf<String?>(null) }
    var selectedWordUrl by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MediaButton(
                icon = R.drawable.gallery,
                contentDescription = "Photos",
                onClick = { showPhotoDialog = true },
                label = "Photos",
                selected = uiState.selectedPhotos.isNotEmpty()
            )

            MediaButton(
                icon = R.drawable.video,
                contentDescription = "Videos",
                onClick = { showVideoDialog = true },
                label = "Videos",
                selected = uiState.selectedVideos.isNotEmpty()
            )

            MediaButton(
                icon = R.drawable.wave,
                contentDescription = "Audio",
                onClick = { showAudioDialog = true },
                label = "Audio",
                selected = uiState.selectedAudios.isNotEmpty()
            )

            MediaButton(
                icon = R.drawable.note,
                contentDescription = "Notes",
                onClick = { showNoteDialog = true },
                label = "Notes",
                selected = uiState.selectedNotes.isNotEmpty()
            )
        }

        if (showNoteDialog) {
            MediaOptionDialog(
                title = "Add Note",
                captureOption = CaptureOptionItem(
                    icon = R.drawable.baseline_edit_square_24,
                    label = "Add Note",
                    onClick = {
                        showNoteDialog = false
                        showAddNoteDialog = true
                    }
                ),
                pickOption = CaptureOptionItem(
                    icon = R.drawable.note_text_svgrepo_com,
                    label = "Pick from Device",
                    onClick = {
                        onNotePickerClick()
                        showNoteDialog = false
                    }
                ),
                onDismiss = { showNoteDialog = false }
            )
        }

        fun saveNoteAsTextFile(noteText: String, context: Context): Uri {
            return try {
                val fileName = "note_${System.currentTimeMillis()}.txt"

                val file = File(context.cacheDir, fileName)

                file.writeText(noteText)

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )

                uri
            } catch (e: Exception) {
                e.printStackTrace()
                Uri.EMPTY
            }
        }

        if (showAddNoteDialog) {
            Dialog(
                onDismissRequest = { showAddNoteDialog = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("Enter your note here") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            maxLines = Int.MAX_VALUE
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { showAddNoteDialog = false }) {
                                Text("Cancel")
                            }
                            TextButton(onClick = {
                                if (noteText.isNotEmpty()) {
                                    val noteUri = saveNoteAsTextFile(noteText, context)
                                    if (noteUri != Uri.EMPTY) {
                                        viewModel.updateNotes(listOf(noteUri))
                                    }
                                    noteText = ""
                                }
                                showAddNoteDialog = false
                            },
                                enabled = noteText.isNotBlank()
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }

        if (showPhotoDialog) {
            MediaOptionDialog(
                title = "Add Photo",
                captureOption = CaptureOptionItem(
                    icon = R.drawable.baseline_camera_alt_24,
                    label = "Take Photo",
                    onClick = {
                        onCameraClick()
                        showPhotoDialog = false
                    }
                ),
                pickOption = CaptureOptionItem(
                    icon = R.drawable.gallery_svgrepo_com,
                    label = "Choose from Gallery",
                    onClick = {
                        onPhotoPickerClick()
                        showPhotoDialog = false
                    }
                ),
                onDismiss = { showPhotoDialog = false }
            )
        }

        if (showVideoDialog) {
            MediaOptionDialog(
                title = "Add Video",
                captureOption = CaptureOptionItem(
                    icon = R.drawable.baseline_videocam_24,
                    label = "Record Video",
                    onClick = {
                        onVideoRecordClick()
                        showVideoDialog = false
                    }
                ),
                pickOption = CaptureOptionItem(
                    icon = R.drawable.video_library_svgrepo_com,
                    label = "Choose from Gallery",
                    onClick = {
                        onVideoPickerClick()
                        showVideoDialog = false
                    }
                ),
                onDismiss = { showVideoDialog = false }
            )
        }

        if (showAudioDialog) {
            MediaOptionDialog(
                title = "Add Audio",
                captureOption = CaptureOptionItem(
                    icon = R.drawable.baseline_mic_24,
                    label = "Press and Hold to Record",
                    onClick = { }
                ),
                pickOption = CaptureOptionItem(
                    icon = R.drawable.music_library_2_svgrepo_com,
                    label = "Choose from Device",
                    onClick = {
                        onAudioPickerClick()
                        showAudioDialog = false
                    }
                ),
                onDismiss = { showAudioDialog = false },
                isAudioRecording = true,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording
            )
        }


        AnimatedVisibility(
            visible = uiState.selectedPhotos.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            MediaSection(
                title = "Selected Photos",
                count = uiState.selectedPhotos.size
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.selectedPhotos) { _, uri ->
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected photo",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                                    .clickable {
                                        previewUri = uri
                                        previewType = MediaType.PHOTO
                                    },
                                contentScale = ContentScale.Crop
                            )

                            IconButton(
                                onClick = { onDeletePhoto(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                }
            }
        }

        AnimatedVisibility(
            visible = uiState.selectedVideos.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            MediaSection(
                title = "Selected Videos",
                count = uiState.selectedVideos.size
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.selectedVideos) { _, videoUri ->
                        val thumbnailUri = uiState.selectedVideoThumbnails[videoUri]

                        Box {
                            VideoThumbnail(
                                videoUri = videoUri,
                                thumbnailUrl = thumbnailUri?.toString(),
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                                    .clickable {
                                        previewUri = videoUri
                                        previewType = MediaType.VIDEO
                                    }
                            )

                            IconButton(
                                onClick = { onDeleteVideo(videoUri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete video",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                }
            }
        }


        AnimatedVisibility(
            visible = uiState.selectedAudios.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            MediaSection(
                title = "Selected Audio Files",
                count = uiState.selectedAudios.size
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.selectedAudios) { _, uri ->
                        Box {
                            AudioItem(
                                audioUri = uri,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                                    .clickable {
                                        previewUri = uri
                                        previewType = MediaType.AUDIO
                                    }
                            )

                            IconButton(
                                onClick = { onDeleteAudio(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete audio",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.selectedNotes.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            MediaSection(
                title = "Selected Notes",
                count = uiState.selectedNotes.size
            ) {
                var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
                var selectedWordUri by remember { mutableStateOf<Uri?>(null) }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.selectedNotes) { _, uri ->
                        val fileType = getFileType(uri,context)

                        Box {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .clickable {
                                        val uriString = uri.toString()
                                        val isFirebaseUrl = uriString.startsWith("https://")
                                        val isEditMode = viewModel.uiState.value.isEditMode
                                        when {
                                            isEditMode && isFirebaseUrl -> {
                                                when {
                                                    uriString.contains(".txt") -> viewModel.getNoteContent(uriString)
                                                    uriString.contains(".pdf") -> selectedPdfUrl = uriString
                                                    uriString.contains(".doc") || uriString.contains(".docx") -> selectedWordUrl = uriString
                                                }
                                            }
                                            else -> {
                                                when (fileType) {
                                                    "txt" -> {
                                                        previewUri = uri
                                                        previewType = MediaType.NOTE
                                                    }
                                                    "pdf" -> selectedPdfUri = uri
                                                    "word" -> selectedWordUri = uri
                                                }
                                            }
                                        }
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.note_text_svgrepo_com),
                                    contentDescription = "Note",
                                    tint = AppRed,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDeleteNote(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete note",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                }

                if (viewModel.uiState.value.isEditMode) {
                    noteContent?.let { content ->
                        NoteViewerDialog(
                            content = content,
                            onDismiss = {
                                viewModel.clearNoteContent()
                            }
                        )
                    }

                    selectedPdfUrl?.let { pdfUrl ->
                        PDFDocumentDialog(
                            pdfUrl = pdfUrl,
                            onDismiss = { selectedPdfUrl = null }
                        )
                    }

                    selectedWordUrl?.let { wordUrl ->
                        WordDocumentDialog(
                            wordUrl = wordUrl,
                            onDismiss = { selectedWordUrl = null }
                        )
                    }


                }

                selectedWordUri?.let { wordUri ->
                    LocalWordDocumentDialog(
                        uri = wordUri,
                        context = context,
                        onDismiss = { selectedWordUri = null }
                    )
                }

                selectedPdfUri?.let { pdfUri ->
                    PDFViewerDialog(
                        pdfUri = pdfUri,
                        onDismiss = { selectedPdfUri = null }
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        var isRecording by remember { mutableStateOf(false) }
        var recordingFeedback by remember { mutableStateOf("") }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(16.dp)
        ) {
            AnimatedVisibility(
                visible = isRecording,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = recordingFeedback,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (isRecording) AppRed else Color.Black
                    )
                }
            }


        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        previewUri?.let { uri ->
            previewType?.let { type ->
                MediaPreviewDialog(
                    uri = uri,
                    mediaType = type,
                    onDismiss = {
                        previewUri = null
                        previewType = null
                    },
                    allVideos = if (type == MediaType.VIDEO) uiState.selectedVideos else emptyList(),
                    allPhotos = if (type == MediaType.PHOTO) uiState.selectedPhotos else emptyList(),
                )
            }
        }





    }
}



