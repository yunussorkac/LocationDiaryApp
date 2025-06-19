package com.app.mapory.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.app.mapory.storage.BooleanDataStore
import com.app.mapory.ui.components.location.LocationFormSection
import com.app.mapory.ui.components.location.LocationSection
import com.app.mapory.ui.components.media.MediaSelectionSection
import com.app.mapory.ui.theme.AppBlue
import com.app.mapory.util.DummyMethods
import com.app.mapory.viewmodel.AddLocationScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import www.sanju.motiontoast.MotionToastStyle
import java.io.File

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLocationScreen(navHostController: NavHostController, locationId: String? = null) {
    val context = LocalContext.current
    val viewModel = koinViewModel<AddLocationScreenViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val tabTitles = remember(uiState.showLocationTab) {
        if (uiState.showLocationTab) {
            listOf("Data", "Media", "Location")
        } else {
            listOf("Data", "Media")
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabTitles.size }
    )
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }


    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    var tempVideoRecordUri by remember { mutableStateOf<Uri?>(null) }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioOutputFile by remember { mutableStateOf<File?>(null) }


    var sureExitDialog by remember { mutableStateOf(false) }

    val shouldWarnBeforeExit = remember(uiState) {
        uiState.title.isNotBlank() ||
                uiState.description.isNotBlank() ||
                uiState.selectedCategory.isNotBlank() ||
                uiState.selectedPhotos.isNotEmpty() ||
                uiState.selectedVideos.isNotEmpty() ||
                uiState.selectedAudios.isNotEmpty() ||
                uiState.selectedNotes.isNotEmpty()
    }

    BackHandlerWithLifecycle {
        Log.d("BackHandler", "Back button pressed")
        if (shouldWarnBeforeExit){
            sureExitDialog = true
        } else {
            navHostController.navigateUp()
        }
    }


    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }


    LaunchedEffect(locationId) {
        locationId?.let {
            viewModel.initializeWithLocationId(it)
        } ?: viewModel.checkAndRequestLocationPermission(context)
    }

    LaunchedEffect(uiState.showLocationTab) {
        if (!uiState.showLocationTab){
            delay(100)
            viewModel.checkAndRequestLocationPermission(context)
        }
    }



    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 0) {
            viewModel.updateSelectedLocation(viewModel.currentLatLng)
        }
    }

    LaunchedEffect(uiState.showLocationTab) {
        if (uiState.showLocationTab) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(2)
                selectedTabIndex = 2
            }
        }
    }

    LaunchedEffect(uiState.uploadComplete) {
        if (uiState.uploadComplete) {
            DummyMethods.showMotionToast(
                context, "Location saved successfully!", "",
                MotionToastStyle.SUCCESS
            )
            BooleanDataStore.saveBoolean(context,"location_saved",true)
            viewModel.resetUploadComplete()
            navHostController.navigateUp()

        }
    }



    if (uiState.isLoading) {
        Dialog(
            onDismissRequest = {

            }
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(50.dp),
                        color = AppBlue
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Uploading...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                }
            }
        }
    }



    if (uiState.showGpsDialog ) {
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = { viewModel.hideGpsDialog() },
            title = { Text("GPS Required", color = Color.Black) },
            text = { Text("Please turn on GPS to get your location.", color = Color.Black) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.hideGpsDialog()
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Open GPS")
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.hideGpsDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val audioRecordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                context,
                "Microphone permission is required to record audio",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    val videoRecordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { isSuccessful ->
        if (isSuccessful) {
            tempVideoRecordUri?.let { uri ->
                viewModel.updateVideos(viewModel.uiState.value.selectedVideos + listOf(uri))
            }
        }
    }

    val videoRecordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tempVideoRecordUri = DummyMethods.createVideoUri(context)
            tempVideoRecordUri?.let { uri ->
                videoRecordLauncher.launch(uri)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccessful ->
        if (isSuccessful) {
            tempCameraUri?.let { uri ->
                viewModel.updatePhotos(viewModel.uiState.value.selectedPhotos + listOf(uri))
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tempCameraUri = DummyMethods.createImageUri(context)
            tempCameraUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        }
    }

    val notePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val filteredUris = uris.filter { uri ->
                val mimeType = context.contentResolver.getType(uri)
                mimeType == "text/plain" || mimeType == "application/pdf" ||
                        mimeType == "application/msword" ||
                        mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            }
            viewModel.updateNotes(filteredUris)
        }
    }

    val notePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notePickerLauncher.launch("*/*")
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.updateAudios(uris)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            audioPickerLauncher.launch("audio/*")
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.updateVideos(uris)
        }
    }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            videoPickerLauncher.launch("video/*")
        }
    }


    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.updatePhotos(uris)
        }
    }


    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            multiplePhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.handlePermissionResult(context, permissions)
    }


    LaunchedEffect(Unit) {
        viewModel.permissionRequest.collect { permissions ->
            if (permissions.isNotEmpty()) {
                locationPermissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }





    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (locationId != null) "Update Location" else "Add Location",
                    fontSize = 20.sp,
                    color = Color.White
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBlue),
            navigationIcon = {
                IconButton(onClick = {

                    if (shouldWarnBeforeExit){
                        sureExitDialog = true
                    } else {
                        navHostController.navigateUp()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        )

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = AppBlue,
            contentColor = Color.White
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title, color = Color.White) }
                )
            }
        }



        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = selectedTabIndex != 2

        ) { page ->
            when (page) {


                0 -> LocationFormSection(
                    uiState = uiState,
                    onTitleChange = { viewModel.updateTitle(it) },
                    onDescriptionChange = { viewModel.updateDescription(it) },
                    onDateChange = { viewModel.updateDate(it) },
                    onCategoryChange = { viewModel.updateCategory(it) },
                    onDropdownExpandedChange = { viewModel.toggleDropdown() },
                    onSaveClick = {
                        val mapLocation = viewModel.createMapLocation()
                        if (locationId != null) {
                            viewModel.updateExistingLocation(mapLocation)
                        } else {
                            viewModel.addLocationToFirestore(mapLocation)
                        }
                    },
                    onShowLocationTabChange = { viewModel.updateShowLocationTab(it) },
                    context,
                    isEditMode = locationId != null
                )

                1 ->

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 5.dp)
                    ) {
                        MediaSelectionSection(
                            uiState = uiState,
                            onPhotoPickerClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.READ_MEDIA_IMAGES
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            multiplePhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }

                                        else -> {
                                            galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                        }
                                    }
                                } else {
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            multiplePhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }

                                        else -> {
                                            galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                    }
                                }
                            },
                            onVideoPickerClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.READ_MEDIA_VIDEO
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            videoPickerLauncher.launch("video/*")
                                        }

                                        else -> {
                                            videoPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                                        }
                                    }
                                } else {
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            videoPickerLauncher.launch("video/*")
                                        }

                                        else -> {
                                            videoPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                    }
                                }
                            },
                            onAudioPickerClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.READ_MEDIA_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            audioPickerLauncher.launch("audio/*")
                                        }

                                        else -> {
                                            audioPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                                        }
                                    }
                                } else {
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            audioPickerLauncher.launch("audio/*")
                                        }

                                        else -> {
                                            audioPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                    }
                                }
                            },
                            onNotePickerClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.READ_MEDIA_IMAGES
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            notePickerLauncher.launch("*/*")
                                        }

                                        else -> {
                                            notePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                        }
                                    }
                                } else {
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            notePickerLauncher.launch("*/*")
                                        }

                                        else -> {
                                            notePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                    }
                                }
                            },
                            onCameraClick = {
                                when {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED -> {
                                        tempCameraUri = DummyMethods.createImageUri(context)
                                        tempCameraUri?.let { uri ->
                                            cameraLauncher.launch(uri)
                                        }
                                    }

                                    else -> {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            },
                            onDeletePhoto = { viewModel.deletePhoto(it) },
                            onDeleteVideo = { viewModel.deleteVideo(it) },
                            onDeleteAudio = { viewModel.deleteAudio(it) },
                            onDeleteNote = { viewModel.deleteNote(it) },
                            onVideoRecordClick = {
                                when {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED -> {
                                        tempVideoRecordUri = DummyMethods.createVideoUri(context)
                                        tempVideoRecordUri?.let { uri ->
                                            videoRecordLauncher.launch(uri)
                                        }
                                    }

                                    else -> {
                                        videoRecordPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            },
                            onStartRecording = {
                                when {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED -> {
                                        audioOutputFile = File(
                                            context.cacheDir,
                                            "audio_recording_${System.currentTimeMillis()}.mp3"
                                        )

                                        mediaRecorder =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                MediaRecorder(context)
                                            } else {
                                                @Suppress("DEPRECATION")
                                                MediaRecorder()
                                            }.apply {
                                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                setOutputFile(audioOutputFile?.absolutePath)
                                                prepare()
                                                start()
                                            }
                                    }

                                    else -> {
                                        audioRecordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }


                            },
                            onStopRecording = {
                                try {
                                    mediaRecorder?.apply {
                                        stop()
                                        release()
                                    }
                                    mediaRecorder = null

                                    audioOutputFile?.let { file ->
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        viewModel.updateAudios(listOf(uri))
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            context,
                            viewModel
                            )
                    }

                2 -> LocationSection(
                    locationDetails = uiState.locationDetails,
                    onLocationSelected = { latLng ->
                        viewModel.updateSelectedLocation(latLng)
                    }
                )

            }

        }

    }

    if (sureExitDialog) {
        SureExitDialog(onExit = {
            sureExitDialog = false
            navHostController.navigateUp()
        }, onCancel = {
            sureExitDialog = false
        })
    }



}


@Composable
fun SureExitDialog(onExit: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Text(
                text = "Are you sure you want to exit?",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = onExit) {
                Text(
                    text = "Exit",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
@Composable
fun BackHandlerWithLifecycle(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshBackHandler by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> refreshBackHandler = false
                Lifecycle.Event.ON_START -> refreshBackHandler = true
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (refreshBackHandler) {
        BackHandler(enabled = enabled) {
            onBack()
        }
    }
}