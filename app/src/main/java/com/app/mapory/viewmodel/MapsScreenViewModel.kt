package com.app.mapory.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.mapory.model.LatLng
import com.app.mapory.model.LocationCategory
import com.app.mapory.model.MapLocation
import com.app.mapory.model.User
import com.app.mapory.repo.MapsScreenRepo
import com.app.mapory.storage.UserDataStore
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MapsScreenViewModel(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val repo: MapsScreenRepo,
    private val application: Application
) : ViewModel() {

    private val _locations = MutableStateFlow<List<MapLocation>>(emptyList())
    val locations: StateFlow<List<MapLocation>> = _locations

    private val _selectedCategories = MutableStateFlow(LocationCategory.entries.toSet())
    val selectedCategories: StateFlow<Set<LocationCategory>> = _selectedCategories

    private val _allLocations = MutableStateFlow<List<MapLocation>>(emptyList())

    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted

    private val _isGpsEnabled = MutableStateFlow(false)
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    private val _startDate = MutableStateFlow("")
    val startDate: StateFlow<String> = _startDate

    private val _endDate = MutableStateFlow("")
    val endDate: StateFlow<String> = _endDate

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible

    private val _latestLocation = MutableStateFlow<MapLocation?>(null)
    val latestLocation = _latestLocation.asStateFlow()

    private var locationListenerRegistration: ListenerRegistration? = null

    init {
        viewModelScope.launch {
            loadSavedUserLocation()
        }
        fetchLatestLocation()
    }

    private fun fetchLatestLocation() {
        locationListenerRegistration?.remove()

        firebaseAuth.currentUser?.let { user ->
            locationListenerRegistration = repo.getLatestLocation(
                firestore = firestore,
                userId = user.uid,
                onLocationUpdate = { location ->
                    viewModelScope.launch {
                        if (location != null) {
                            _latestLocation.value = location
                            Log.d("MapsViewModel", "Latest location: $location")
                        } else {
                            _latestLocation.value = null
                        }
                    }
                },
                onError = { exception ->
                    viewModelScope.launch {
                        Log.e("MapsViewModel", "Error fetching latest location", exception)
                    }
                }
            )
        }
    }



    fun toggleSearch() {
        _isSearchVisible.value = !_isSearchVisible.value
        if (!_isSearchVisible.value) {
            _searchQuery.value = ""
            filterLocations()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterLocations()
    }

    fun resetFilters() {
        viewModelScope.launch {
            _selectedCategories.value = LocationCategory.entries.toSet()
            _startDate.value = ""
            _endDate.value = ""
            filterLocations()
        }
    }

    fun setGpsEnabled(isEnabled: Boolean) {
          _isGpsEnabled.value = isEnabled
    }

    fun setLocationPermissionGranted(isGranted: Boolean) {
         _locationPermissionGranted.value = isGranted
    }

    private fun filterLocations() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val filteredList = _allLocations.value.filter { location ->
            val matchesCategory = location.category in _selectedCategories.value

            val matchesSearch = if (_searchQuery.value.isEmpty()) {
                true
            } else {
                location.title.contains(_searchQuery.value, ignoreCase = true)
            }

            val matchesDate = if (_startDate.value.isEmpty() || _endDate.value.isEmpty()) {
                true
            } else {
                try {
                    location.date.let { locationDateStr ->
                        val locationDate = dateFormat.parse(locationDateStr)
                        val startDate = dateFormat.parse(_startDate.value)
                        val endDate = dateFormat.parse(_endDate.value)

                        locationDate != null && startDate != null && endDate != null &&
                                (locationDate >= startDate && locationDate <= endDate)
                    }
                } catch (e: Exception) {
                    Log.e("MapsViewModel", "Date parsing error", e)
                    false
                }
            }

            matchesCategory && matchesDate && matchesSearch
        }

        _locations.value = filteredList
    }

    fun updateDateFilter(startDate: String, endDate: String) {
        viewModelScope.launch {
            _startDate.value = startDate
            _endDate.value = endDate
            filterLocations()
        }
    }


    private suspend fun loadSavedUserLocation() {
        try {
            val savedUser = UserDataStore.getUser(application)
            savedUser?.latLng?.let { latLng ->
                _currentLocation.value = latLng
            }
        } catch (e: Exception) {
            Log.e("MapsViewModel", "Error loading saved location", e)
        }
    }


    fun checkLocationPermission(context: Context) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        _locationPermissionGranted.value = hasPermission

        viewModelScope.launch {
            if (hasPermission) {
                checkGpsStatus(context)
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun checkGpsStatus(context: Context) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            _isGpsEnabled.value = isEnabled

            if (isEnabled) {
                viewModelScope.launch {
                    getCurrentLocation(context)
                }
            }
        } catch (e: Exception) {
            Log.e("MapsViewModel", "Error checking GPS status", e)
        }
    }



    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getCurrentLocation(context: Context) {
        if (!_locationPermissionGranted.value || !_isGpsEnabled.value) return

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        viewModelScope.launch {
                            _currentLocation.value = latLng

                            val existingUser = UserDataStore.getUser(context)
                            val updatedUser = existingUser?.copy(
                                latLng = latLng
                            ) ?: User(
                                userId = firebaseAuth.currentUser?.uid ?: "",
                                email = firebaseAuth.currentUser?.email ?: "",
                                latLng = latLng
                            )
                            UserDataStore.saveUser(context, updatedUser)

                            try {
                                repo.saveUserToFirestore(updatedUser, firestore, firebaseAuth)
                            } catch (e: Exception) {
                            }

                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Location", "Error getting location", e)
                }
        } catch (e: Exception) {
            Log.e("Location", "Error requesting location", e)
        }
    }

    fun updateSelectedCategories(categories: Set<LocationCategory>) {
        Log.d("FilterDebug", "UpdateSelectedCategories called with categories: $categories")
        viewModelScope.launch {
            _selectedCategories.value = categories
            filterLocations()
        }
    }



    fun getLocations() {
        viewModelScope.launch {
            firebaseAuth.currentUser?.let { user ->
                repo.getLocations(user.uid, firestore).collect { locationList ->
                    Log.d("FilterDebug", "Received locations: ${locationList.size}")
                    _allLocations.value = locationList
                    filterLocations()
                }
            }
        }
    }



}