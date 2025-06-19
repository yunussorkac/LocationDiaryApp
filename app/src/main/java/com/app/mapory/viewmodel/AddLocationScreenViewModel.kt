package com.app.mapory.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.mapory.model.LocationCategory
import com.app.mapory.model.LocationDetails
import com.app.mapory.model.MapLocation
import com.app.mapory.repo.AddLocationScreenRepo
import com.app.mapory.storage.UserDataStore
import com.app.mapory.util.DummyMethods
import com.app.mapory.util.DummyMethods.Companion.getCurrentFormattedDate
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import java.nio.charset.Charset


class AddLocationScreenViewModel(
     private val appContext: Context,
     private val firestore: FirebaseFirestore,
     private val firebaseAuth: FirebaseAuth,
     private val storage: FirebaseStorage,
     private val repo : AddLocationScreenRepo
    ) : ViewModel() {


    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private val _locationDetails = MutableStateFlow<LocationDetails?>(null)
    val locationDetails: StateFlow<LocationDetails?> = _locationDetails

    private val _permissionRequest = MutableSharedFlow<List<String>>()
    val permissionRequest: SharedFlow<List<String>> = _permissionRequest

    private var isEditMode = false
    private var currentLocationId: String? = null

    private val _noteContent = MutableStateFlow<String?>(null)
    val noteContent: StateFlow<String?> = _noteContent.asStateFlow()


    var currentLatLng: LatLng = LatLng(0.0, 0.0)
        private set

    fun getNoteContent(noteUrl: String) {
        viewModelScope.launch {
            try {
                val httpsReference = storage.getReferenceFromUrl(noteUrl)

                val tenMb: Long = 10 * 1024 * 1024

                httpsReference.getBytes(tenMb).addOnSuccessListener { bytes ->
                    val content = String(bytes, Charset.defaultCharset())
                    _noteContent.value = content
                }.addOnFailureListener {
                    _noteContent.value = "Error loading note content: ${it.message}"
                }
            } catch (e: Exception) {
                _noteContent.value = "Error loading note content: ${e.message}"
            }
        }
    }

    fun clearNoteContent() {
        _noteContent.value = null
    }


    fun initializeWithLocationId(locationId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                currentLocationId = locationId
                isEditMode = true

                firebaseAuth.currentUser?.let { user ->
                    val location = repo.getLocationById(firestore, user.uid, locationId)
                    location?.let { mapLocation ->
                        currentLatLng = LatLng(
                            mapLocation.latLng.latitude,
                            mapLocation.latLng.longitude
                        )

                        val originalVideoUrls = mapLocation.videos
                        val originalThumbnailUrls = mapLocation.videoThumbnails

                        _uiState.update { state ->
                            state.copy(
                                title = mapLocation.title,
                                description = mapLocation.description,
                                date = mapLocation.date,
                                selectedCategory = mapLocation.category.toString(),
                                locationDetails = mapLocation.locationDetails,
                                isLoading = false,
                                isEditMode = true
                            )
                        }

                        updateLocationDetails(appContext, currentLatLng)

                        viewModelScope.launch {
                            val photos = repo.downloadMediaUrlsToUris(mapLocation.images)
                            val videos = repo.downloadMediaUrlsToUris(originalVideoUrls)
                            val videoThumbs = repo.downloadMediaUrlsToUris(originalThumbnailUrls)
                            val audios = repo.downloadMediaUrlsToUris(mapLocation.audios)
                            val notes = repo.downloadMediaUrlsToUris(mapLocation.notes)


                            val videoThumbnailMap = videos.zip(videoThumbs).toMap()
                            val uriToOriginalThumbnailUrlMap = videos.mapIndexed { index, videoUri ->
                                videoUri to originalThumbnailUrls.getOrNull(index)
                            }.filter { it.second != null }.toMap() as Map<Uri, String>




                            _uiState.update { state ->
                                state.copy(
                                    selectedPhotos = photos,
                                    selectedVideos = videos,
                                    selectedVideoThumbnails = videoThumbnailMap,
                                    videoUriToOriginalThumbnailUrlMap = uriToOriginalThumbnailUrlMap,
                                    selectedAudios = audios,
                                    selectedNotes = notes
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                e.printStackTrace()
            }
        }
    }

    fun updateExistingLocation(mapLocation: MapLocation) {
        viewModelScope.launch {
            firebaseAuth.currentUser?.let { user ->
                try {
                    _uiState.update { it.copy(isLoading = true) }

                    val (existingPhotos, newPhotos) = uiState.value.selectedPhotos
                        .partition { !it.toString().startsWith("content://") }
                    val (existingVideos, newVideos) = uiState.value.selectedVideos
                        .partition { !it.toString().startsWith("content://") }
                    val (existingAudios, newAudios) = uiState.value.selectedAudios
                        .partition { !it.toString().startsWith("content://") }
                    val (existingNotes, newNotes) = uiState.value.selectedNotes
                        .partition { !it.toString().startsWith("content://") }

                    val newImageUrls = if (newPhotos.isNotEmpty()) {
                        repo.uploadImagesAndGetUrls(
                            uris = newPhotos,
                            storage = storage,
                            userId = user.uid,
                            locationId = mapLocation.id
                        )
                    } else emptyList()

                    val newVideoResults = if (newVideos.isNotEmpty()) {
                        repo.uploadVideosAndGetUrls(
                            context = appContext,
                            uris = newVideos,
                            storage = storage,
                            userId = user.uid,
                            locationId = mapLocation.id
                        )
                    } else emptyList()

                    val newVideoUrls = newVideoResults.map { it.first }
                    val newThumbnailUrls = newVideoResults.map { it.second }

                    val newAudioUrls = if (newAudios.isNotEmpty()) {
                        repo.uploadAudiosAndGetUrls(
                            uris = newAudios,
                            storage = storage,
                            userId = user.uid,
                            locationId = mapLocation.id
                        )
                    } else emptyList()

                    val newNoteUrls = if (newNotes.isNotEmpty()) {
                        repo.uploadNotesAndGetUrls(
                            uris = newNotes,
                            storage = storage,
                            userId = user.uid,
                            locationId = mapLocation.id,
                            context = appContext
                        )
                    } else emptyList()

                    val existingPhotoUrls = existingPhotos.map { it.toString() }
                    val existingVideoUrls = existingVideos.map { it.toString() }
                    val existingAudioUrls = existingAudios.map { it.toString() }
                    val existingNoteUrls = existingNotes.map { it.toString() }

                    val uriToOriginalThumbnailUrlMap = uiState.value.videoUriToOriginalThumbnailUrlMap
                    val finalExistingThumbnailUrls = existingVideos.mapNotNull { videoUri ->
                        uriToOriginalThumbnailUrlMap[videoUri]
                    }




                    val updatedMapLocation = mapLocation.copy(
                        images = existingPhotoUrls + newImageUrls,
                        videos = existingVideoUrls + newVideoUrls,
                        videoThumbnails = finalExistingThumbnailUrls + newThumbnailUrls,
                        audios = existingAudioUrls + newAudioUrls,
                        notes = existingNoteUrls + newNoteUrls
                    )

                    repo.updateLocationInFirestore(updatedMapLocation, firestore, user.uid)

                    _uiState.update { it.copy(
                        isLoading = false,
                        uploadComplete = true
                    ) }

                } catch (e: Exception) {
                    e.printStackTrace()
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }




    fun deletePhoto(uri: Uri) {
        _uiState.update { state ->
            state.copy(selectedPhotos = state.selectedPhotos.filter { it != uri })
        }
    }

    fun deleteVideo(uri: Uri) {
        _uiState.update { state ->
            state.copy(selectedVideos = state.selectedVideos.filter { it != uri })
        }
    }

    fun deleteAudio(uri: Uri) {
        _uiState.update { state ->
            state.copy(selectedAudios = state.selectedAudios.filter { it != uri })
        }
    }

    fun deleteNote(uri: Uri) {
        _uiState.update { state ->
            state.copy(selectedNotes = state.selectedNotes.filter { it != uri })
        }
    }

    fun addLocationToFirestore(mapLocation: MapLocation) {
        viewModelScope.launch {
            firebaseAuth.currentUser?.let { user ->
                try {
                    _uiState.update { it.copy(isLoading = true) }

                    val imageUrls = if (uiState.value.selectedPhotos.isNotEmpty()) {
                        repo.uploadImagesAndGetUrls(
                            uris = uiState.value.selectedPhotos,
                            storage = storage,
                            userId = user.uid,
                            locationId = mapLocation.id
                        )
                    } else emptyList()

                    val videoResults = if (uiState.value.selectedVideos.isNotEmpty()) {
                        repo.uploadVideosAndGetUrls(
                            context = appContext,
                            uris = uiState.value.selectedVideos,
                            storage = storage,
                            userId = user.uid,
                            locationId = mapLocation.id
                        )
                    } else emptyList()

                    val videoUrls = videoResults.map { it.first }
                    val thumbnailUrls = videoResults.map { it.second }

                    val audioUrls = if (uiState.value.selectedAudios.isNotEmpty()) {
                        repo.uploadAudiosAndGetUrls(
                            uris = uiState.value.selectedAudios,
                            storage = storage,
                            userId = user.uid,
                            locationId = mapLocation.id
                        )
                    } else emptyList()

                    val noteUrls = if (uiState.value.selectedNotes.isNotEmpty()) {
                        repo.uploadNotesAndGetUrls(
                            uris = uiState.value.selectedNotes,
                            storage = storage,
                            userId = user.uid,
                            locationId = mapLocation.id,
                            context = appContext
                        )
                    } else emptyList()

                    val updatedMapLocation = mapLocation.copy(
                        images = imageUrls,
                        videos = videoUrls,
                        videoThumbnails = thumbnailUrls,
                        audios = audioUrls,
                        notes = noteUrls
                    )

                    repo.addLocationToFirestore(updatedMapLocation, firestore, user.uid)

                    _uiState.update { it.copy(
                        isLoading = false,
                        uploadComplete = true
                    ) }

                } catch (e: Exception) {
                    e.printStackTrace()
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun resetUploadComplete() {
        _uiState.update { it.copy(uploadComplete = false) }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateDate(date : String){
        _uiState.update { it.copy(date = date) }
    }

    fun updateCategory(category: String) {
        _uiState.update { it.copy(
            selectedCategory = category,
            isDropdownExpanded = true
        )}
    }

    fun updatePhotos(newPhotos: List<Uri>) {
        _uiState.update { currentState ->
            when {
                isEditMode -> {
                    val firestoreUrls = currentState.selectedPhotos
                        .filter { it.toString().startsWith("https://") }
                        .distinct()

                    val existingContentUris = currentState.selectedPhotos
                        .filter { it.toString().startsWith("content://") }
                        .distinct()

                    currentState.copy(
                        selectedPhotos = (firestoreUrls + existingContentUris + newPhotos).distinct()
                    )
                }
                newPhotos.size == 1 -> {
                    currentState.copy(
                        selectedPhotos = (currentState.selectedPhotos + newPhotos).distinct()
                    )
                }
                else -> {
                    currentState.copy(selectedPhotos = newPhotos)
                }
            }
        }
    }

    fun updateVideos(newVideos: List<Uri>) {
        _uiState.update { currentState ->
            when {
                isEditMode -> {
                    val firestoreUrls = currentState.selectedVideos
                        .filter { it.toString().startsWith("https://") }
                        .distinct()

                    val existingContentUris = currentState.selectedVideos
                        .filter { it.toString().startsWith("content://") }
                        .distinct()

                    currentState.copy(
                        selectedVideos = (firestoreUrls + existingContentUris + newVideos).distinct()
                    )
                }
                newVideos.size == 1 -> {
                    currentState.copy(
                        selectedVideos = (currentState.selectedVideos + newVideos).distinct()
                    )
                }
                else -> {
                    currentState.copy(selectedVideos = newVideos)
                }
            }
        }
    }

    fun updateAudios(newAudios: List<Uri>) {
        _uiState.update { currentState ->
            when {
                isEditMode -> {
                    val firestoreUrls = currentState.selectedAudios
                        .filter { it.toString().startsWith("https://") }
                        .distinct()

                    val existingContentUris = currentState.selectedAudios
                        .filter { it.toString().startsWith("content://") }
                        .distinct()

                    currentState.copy(
                        selectedAudios = (firestoreUrls + existingContentUris + newAudios).distinct()
                    )
                }
                newAudios.size == 1 -> {
                    currentState.copy(
                        selectedAudios = (currentState.selectedAudios + newAudios).distinct()
                    )
                }
                else -> {
                    currentState.copy(selectedAudios = newAudios)
                }
            }
        }
    }

    fun updateNotes(newNotes: List<Uri>) {
        if (isEditMode) {
            _uiState.update { currentState ->
                currentState.copy(
                    selectedNotes = currentState.selectedNotes + newNotes
                )
            }
        } else {
            _uiState.update { currentState ->
                currentState.copy(
                    selectedNotes = currentState.selectedNotes + newNotes
                )
            }
        }
    }


    fun updateSelectedLocation(latLng: LatLng) {
        currentLatLng = latLng
        viewModelScope.launch {
            val details = repo.getLocationDetails(appContext , latLng)
            _locationDetails.value = details
            _uiState.update { it.copy(locationDetails = details) }
        }
    }

    fun toggleDropdown() {
        _uiState.update { it.copy(isDropdownExpanded = !it.isDropdownExpanded) }
    }

    fun updateShowLocationTab(show: Boolean) {
        _uiState.update { it.copy(showLocationTab = show) }
    }

    fun hideGpsDialog() {
        _uiState.update { it.copy(showGpsDialog = false) }
    }



    fun checkAndRequestLocationPermission(context: Context) {
        if (isEditMode) return

        println("Checking location permission")
        when {
            hasLocationPermission(context) -> {
                println("Has location permission, checking GPS")
                checkGpsAndGetLocation(context)
            }
            else -> {
                println("Requesting location permission")
                viewModelScope.launch {
                    _permissionRequest.emit(listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }
            }
        }
    }

    fun handlePermissionResult(context: Context, permissions: Map<String, Boolean>) {
        val hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasLocationPermission) {
            checkGpsAndGetLocation(context)
        } else {
            viewModelScope.launch {
            }
        }
    }


     fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun updateCurrentLocation(context: Context) {
        viewModelScope.launch {
            val location = repo.getCurrentLocation(context)
            location?.let {
                currentLatLng = LatLng(it.latitude, it.longitude)
                val details = repo.getLocationDetails(context, currentLatLng)
                _locationDetails.value = details
                _uiState.update { currentState ->
                    currentState.copy(locationDetails = details)
                }
                updateSelectedLocation(currentLatLng)
            }
        }
    }


    private fun checkGpsAndGetLocation(context: Context) {
        viewModelScope.launch {
            if (isGpsEnabled(context)) {
                updateCurrentLocation(context)
            } else {
                val user = UserDataStore.getUser(context)
                if (user != null){
                    val latLng = LatLng(user.latLng.latitude, user.latLng.longitude)
                    updateSelectedLocation(latLng)
                }
                _uiState.update { currentState ->
                    if (!currentState.showGpsDialog) {
                        currentState.copy(showGpsDialog = true)
                    } else {
                        currentState
                    }
                }
            }
        }
    }

    private fun updateLocationDetails(context: Context, latLng: LatLng) {
        currentLatLng = latLng
        viewModelScope.launch {
            val details = repo.getLocationDetails(context, latLng)
            _locationDetails.value = details
            _uiState.update { it.copy(locationDetails = details) }

        }
    }

     fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun createMapLocation(): MapLocation {
        return MapLocation(
            id = currentLocationId ?: System.currentTimeMillis().toString(),
            title = uiState.value.title,
            description = uiState.value.description,
            date = uiState.value.date,
            dateMillis = DummyMethods.dateToMillis(uiState.value.date),
            latLng = com.app.mapory.model.LatLng(currentLatLng.latitude, currentLatLng.longitude),
            category = LocationCategory.fromString(uiState.value.selectedCategory),
            locationDetails = locationDetails.value ?: LocationDetails(),
            images = emptyList(),
            videos = emptyList(),
            audios = emptyList(),
            notes = emptyList(),
            videoThumbnails = emptyList()
        )
    }


}

data class LocationUiState(
    val title: String = "",
    val description: String = "",
    val date : String = getCurrentFormattedDate(),
    val dateMillis : Long = 0,
    val selectedCategory: String = "",
    val categories: List<String> = listOf(
        "Food & Drink",
        "Culture & Art",
        "Entertainment",
        "Nature & Scenery",
        "Travel & Tourism",
        "Shopping",
        "Accommodation",
        "Other"
    ),
    val isDropdownExpanded: Boolean = false,
    val showGpsDialog: Boolean = false,
    val selectedPhotos: List<Uri> = emptyList(),
    val selectedVideos: List<Uri> = emptyList(),
    val selectedVideoThumbnails: Map<Uri, Uri> = emptyMap(),
    val selectedAudios: List<Uri> = emptyList(),
    val selectedNotes: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val uploadComplete: Boolean = false,
    val locationDetails: LocationDetails? = null,
    val showLocationTab: Boolean = false,
    val isEditMode: Boolean = false,
    val videoUriToOriginalThumbnailUrlMap: Map<Uri, String> = emptyMap()



)