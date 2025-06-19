package com.app.mapory.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.mapory.model.MapLocation
import com.app.mapory.repo.LocationStats
import com.app.mapory.repo.ProfileScreenRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileScreenViewModel(
    private val repo : ProfileScreenRepo,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage : FirebaseStorage
) : ViewModel() {

    private val _latestLocation = MutableStateFlow<MapLocation?>(null)
    val latestLocation = _latestLocation.asStateFlow()

    private val _locationStats = MutableStateFlow<LocationStats?>(null)
    val locationStats = _locationStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail = _userEmail.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username = _username.asStateFlow()

    private val _profilePhotoUrl = MutableStateFlow<String?>(null)
    val profilePhotoUrl = _profilePhotoUrl.asStateFlow()

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto = _isUploadingPhoto.asStateFlow()

    private val _isUpdatingUsername = MutableStateFlow(false)
    val isUpdatingUsername = _isUpdatingUsername.asStateFlow()

    init {
        fetchLatestLocation()
        fetchLocationStats()
        fetchUserEmail()
        fetchUsername()
        fetchProfilePhoto()

    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            _isUpdatingUsername.value = true
            try {
                auth.currentUser?.let { user ->
                    firestore.collection("Users")
                        .document(user.uid)
                        .update("username", newUsername)
                        .await()

                    _username.value = newUsername
                }
            } catch (e: Exception) {
                _error.value = "Failed to update username: ${e.message}"
            } finally {
                _isUpdatingUsername.value = false
            }
        }
    }

    fun uploadProfilePhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            try {
                auth.currentUser?.let { user ->
                    val storageRef = storage.reference
                        .child("profilephotos")
                        .child(user.uid)
                        .child("profile.jpg")

                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val uploadTask = storageRef.putStream(inputStream)
                        uploadTask.await()

                        val downloadUrl = storageRef.downloadUrl.await().toString()

                        firestore.collection("Users")
                            .document(user.uid)
                            .update("profilePhoto", downloadUrl)
                            .await()

                        _profilePhotoUrl.value = downloadUrl
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to upload photo: ${e.message}"
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }

    private fun fetchProfilePhoto() {
        viewModelScope.launch {
            try {
                auth.currentUser?.let { user ->
                    repo.getUserProfile(firestore, user.uid)?.let { profile ->
                        _profilePhotoUrl.value = profile.profilePhoto
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun refreshData(){
        fetchLatestLocation()
        fetchLocationStats()
        fetchUserEmail()
        fetchUsername()
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: Exception) {
                _error.value = "Failed to sign out"
            }
        }
    }

    private fun fetchUsername() {
        viewModelScope.launch {
            try {
                auth.currentUser?.let { repo.getUsername(firestore,it.uid) }?.onSuccess { username ->
                    _username.value = username
                }?.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun fetchUserEmail() {
        viewModelScope.launch {
            try {
                auth.currentUser?.let { repo.getUserEmail(firestore,it.uid) }?.onSuccess { email ->
                    _userEmail.value = email
                }?.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun fetchLatestLocation() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.currentUser?.let { repo.getLatestLocationById(firestore,it.uid) }
                    ?.onSuccess { location ->
                    _latestLocation.value = location
                }?.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchLocationStats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.currentUser?.let { repo.getLocationStats(firestore,it.uid) }
                    ?.onSuccess { stats ->
                    _locationStats.value = stats
                }?.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

}