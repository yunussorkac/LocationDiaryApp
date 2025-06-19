package com.app.mapory.ui.components.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.mapory.model.LocationDetails
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSection(
    locationDetails: LocationDetails?,
    onLocationSelected: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(41.0082, 28.9784),
            1f
        )
    }

    val placesClient = remember {
        if (!Places.isInitialized()) {
            Places.initialize(context, "")
        }
        Places.createClient(context)
    }

    fun animateCameraToLocation(latLng: LatLng) {
        scope.launch {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(latLng, 12f)
                ),
                1000
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true
            ),
            onMapClick = { latLng ->
                selectedLocation = latLng
                onLocationSelected(latLng)
                animateCameraToLocation(latLng)
            }
        ) {
            selectedLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
        }

        FloatingActionButton(
            onClick = { showSearchDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White
            )
        }

        if (locationDetails != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Location Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column {
                        locationDetails.address.let {
                            Text(
                                text = "Address: $it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                        locationDetails.city.let {
                            Text(
                                text = "City: $it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                        selectedLocation?.let { location ->
                            Text(
                                text = "Selected Coordinates: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }

    // Search Dialog
    if (showSearchDialog) {
        Dialog(
            onDismissRequest = {
                showSearchDialog = false
                searchQuery = ""
                predictions = emptyList()
            },
            properties = DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            searchJob?.cancel()
                            if (query.length >= 2) {
                                searchJob = scope.launch {
                                    delay(300)
                                    try {
                                        val request = FindAutocompletePredictionsRequest.builder()
                                            .setQuery(query)
                                            .build()

                                        val response = placesClient.findAutocompletePredictions(request).await()
                                        predictions = response.autocompletePredictions
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            } else {
                                predictions = emptyList()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search location...") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(predictions) { prediction ->
                            ListItem(
                                headlineContent = {
                                    Text(prediction.getPrimaryText(null).toString())
                                },
                                supportingContent = {
                                    Text(prediction.getSecondaryText(null).toString())
                                },
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        try {
                                            val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.ADDRESS)
                                            val request = prediction.placeId?.let { placeId ->
                                                FetchPlaceRequest
                                                    .builder(placeId, placeFields)
                                                    .build()
                                            }

                                            request?.let {
                                                val response = placesClient.fetchPlace(it).await()
                                                response.place.latLng?.let { latLng ->
                                                    selectedLocation = latLng
                                                    onLocationSelected(latLng)
                                                    animateCameraToLocation(latLng)

                                                }
                                            }
                                            showSearchDialog = false
                                            searchQuery = ""
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            )
                            if (prediction != predictions.last()) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}