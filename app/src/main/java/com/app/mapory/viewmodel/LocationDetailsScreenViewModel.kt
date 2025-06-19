package com.app.mapory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.mapory.model.MapLocation
import com.app.mapory.repo.LocationDetailsScreenRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.nio.charset.Charset

class LocationDetailsScreenViewModel(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val repo : LocationDetailsScreenRepo,
    private val firebaseStorage : FirebaseStorage) : ViewModel(){

    private val _locationState = MutableStateFlow<LocationDetailsState>(LocationDetailsState.Loading)
    val locationState: StateFlow<LocationDetailsState> = _locationState.asStateFlow()

    private val _noteContent = MutableStateFlow<String?>(null)
    val noteContent: StateFlow<String?> = _noteContent.asStateFlow()

    fun getNoteContent(noteUrl: String) {
        viewModelScope.launch {
            try {
                val httpsReference = firebaseStorage.getReferenceFromUrl(noteUrl)

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

    fun getLocationDetails(locationId: String) {
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.let { user ->
                    repo.getLocationDetails(user.uid, locationId, firestore)
                        .catch { exception ->
                            _locationState.value = LocationDetailsState.Error(
                                exception.message ?: "An error occurred"
                            )
                        }
                        .collect { location ->
                            _locationState.value = LocationDetailsState.Success(location)
                        }
                } ?: run {
                    _locationState.value = LocationDetailsState.Error("User not authenticated")
                }
            } catch (e: Exception) {
                _locationState.value = LocationDetailsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteLocation(locationId: String, onSuccess : () -> Unit, onError : () -> Unit) {
        viewModelScope.launch {
            try {
                firebaseAuth.currentUser?.let { user ->
                    repo.deleteLocation(user.uid, locationId, firestore)
                        .onSuccess {
                            onSuccess()
                            println("Location and all associated files deleted successfully")
                        }
                        .onFailure { exception ->
                            onError()
                            println("Error deleting location: ${exception.message}")
                        }
                }
            } catch (e: Exception) {
                println("Error in deleteLocation: ${e.message}")
                e.printStackTrace()
            }
        }
    }



}

sealed class LocationDetailsState {
    object Loading : LocationDetailsState()
    data class Success(val location: MapLocation) : LocationDetailsState()
    data class Error(val message: String) : LocationDetailsState()
}
