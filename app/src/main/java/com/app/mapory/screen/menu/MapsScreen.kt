package com.app.mapory.screen.menu

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.app.mapory.R
import com.app.mapory.model.LocationCategory
import com.app.mapory.storage.BooleanDataStore
import com.app.mapory.ui.Screens
import com.app.mapory.ui.components.filter.DateRangeDialog
import com.app.mapory.ui.components.filter.FilterDialog
import com.app.mapory.ui.theme.AppBlue
import com.app.mapory.ui.theme.AppOrange
import com.app.mapory.viewmodel.MapsScreenViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(navHostController: NavHostController) {

    val context = LocalContext.current

    val mapsScreenViewModel = koinViewModel<MapsScreenViewModel>()
    val locationList by mapsScreenViewModel.locations.collectAsStateWithLifecycle()

    val selectedCategories by mapsScreenViewModel.selectedCategories.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }


    val istanbul = LatLng(41.015137,28.979530)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(istanbul, 10f)
    }
    var showGpsDialog by remember { mutableStateOf(false) }

    val locationPermissionGranted by mapsScreenViewModel.locationPermissionGranted.collectAsStateWithLifecycle()
    val isGpsEnabled by mapsScreenViewModel.isGpsEnabled.collectAsStateWithLifecycle()
    val userLocation by mapsScreenViewModel.currentLocation.collectAsStateWithLifecycle()

    var showDateFilterDialog by remember { mutableStateOf(false) }
    val startDate by mapsScreenViewModel.startDate.collectAsStateWithLifecycle()
    val endDate by mapsScreenViewModel.endDate.collectAsStateWithLifecycle()

    var isReturningFromSettings by remember { mutableStateOf(false) }
    val isSearchVisible by mapsScreenViewModel.isSearchVisible.collectAsStateWithLifecycle()
    val searchQuery by mapsScreenViewModel.searchQuery.collectAsStateWithLifecycle()
    val latestLocation by mapsScreenViewModel.latestLocation.collectAsStateWithLifecycle()

    val activity = LocalActivity.current as ComponentActivity
    var locationSaved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        locationSaved = BooleanDataStore.getBoolean(context,"location_saved")
    }


    class LocationPermissionObserver(
        private val onPermissionResult: (Boolean) -> Unit
    ) : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_RESUME) {
                val isPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                onPermissionResult(isPermissionGranted)
            }
        }
    }

    val locationPermissionObserver = remember {
        LocationPermissionObserver { isGranted ->
            mapsScreenViewModel.setLocationPermissionGranted(isGranted)

            if (isGranted) {
                mapsScreenViewModel.checkGpsStatus(context)
                mapsScreenViewModel.getCurrentLocation(context)
            }
        }
    }

    DisposableEffect(activity) {
        val lifecycle = activity.lifecycle
        lifecycle.addObserver(locationPermissionObserver)

        onDispose {
            lifecycle.removeObserver(locationPermissionObserver)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        mapsScreenViewModel.setLocationPermissionGranted(isGranted)

        if (isGranted) {
            mapsScreenViewModel.checkGpsStatus(context)
            mapsScreenViewModel.getCurrentLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        mapsScreenViewModel.getLocations()
    }

    LaunchedEffect(Unit) {
        val isPermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        mapsScreenViewModel.setLocationPermissionGranted(isPermissionGranted)

        if (!isPermissionGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            mapsScreenViewModel.checkGpsStatus(context)
            mapsScreenViewModel.getCurrentLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        snapshotFlow { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }
            .collect { isEnabled ->
                mapsScreenViewModel.setGpsEnabled(isEnabled)

                if (isEnabled && isReturningFromSettings) {
                    isReturningFromSettings = false
                    mapsScreenViewModel.getCurrentLocation(context)
                }
            }
    }

    LaunchedEffect(isGpsEnabled, locationPermissionGranted) {
        if (locationPermissionGranted) {
            if (isGpsEnabled) {
                showGpsDialog = false
                mapsScreenViewModel.getCurrentLocation(context)
            } else {
                if (userLocation?.latitude == 0.0 && userLocation?.longitude == 0.0) {
                    showGpsDialog = true
                } else {
                    showGpsDialog = false
                }
            }
        } else {
            showGpsDialog = false
        }
    }

    LaunchedEffect(latestLocation, userLocation, isGpsEnabled, locationSaved) {
        Log.d("MapsScreen", "latestLocation: $latestLocation, userLocation: $userLocation, isGpsEnabled: $isGpsEnabled")

        val isReturnFromDeletion = navHostController.previousBackStackEntry
            ?.savedStateHandle?.get<Boolean>("locationDeleted") ?: false

        when {
            isReturnFromDeletion -> {
                if (isGpsEnabled && userLocation != null && (userLocation!!.latitude != 0.0 || userLocation!!.longitude != 0.0)) {
                    val latLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(latLng, 10f),
                        durationMs = 500
                    )
                } else if (latestLocation != null) {
                    val latLng = LatLng(latestLocation!!.latLng.latitude, latestLocation!!.latLng.longitude)
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(latLng, 10f),
                        durationMs = 500
                    )
                } else {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(istanbul, 10f),
                        durationMs = 500
                    )
                }
                navHostController.previousBackStackEntry?.savedStateHandle?.remove<Boolean>("locationDeleted")
            }

            locationSaved -> {
                if (latestLocation != null){
                    val latLng = LatLng(latestLocation!!.latLng.latitude, latestLocation!!.latLng.longitude)
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(latLng, 10f),
                        durationMs = 500
                    )
                    BooleanDataStore.saveBoolean(context,"location_saved",false)
                }

            }


            isGpsEnabled && userLocation != null && (userLocation!!.latitude != 0.0 || userLocation!!.longitude != 0.0) -> {
                val latLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(latLng, 10f),
                    durationMs = 500
                )
            }
            !isGpsEnabled && latestLocation != null -> {
                val latLng = LatLng(latestLocation!!.latLng.latitude, latestLocation!!.latLng.longitude)
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(latLng, 10f),
                    durationMs = 500
                )
            }
            userLocation != null && (userLocation!!.latitude != 0.0 || userLocation!!.longitude != 0.0) -> {
                val latLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(latLng, 10f),
                    durationMs = 500
                )
            }
            else -> {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(istanbul, 2f),
                    durationMs = 500
                )
            }
        }
    }

    if (showGpsDialog && !isGpsEnabled && locationPermissionGranted ) {
        AlertDialog(
            onDismissRequest = { showGpsDialog = false },
            title = { Text("GPS Required") },
            text = { Text("Please enable GPS to use location features") },
            confirmButton = {
                Button(
                    onClick = {
                        isReturningFromSettings = true
                        showGpsDialog = false
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                ) {
                    Text("Enable GPS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    fun getCategoryIcon(category: LocationCategory): BitmapDescriptor {
        val iconResId = when (category) {
            LocationCategory.FOOD_AND_DRINK -> R.drawable.food
            LocationCategory.CULTURE_AND_ART -> R.drawable.culture
            LocationCategory.ENTERTAINMENT -> R.drawable.entertainment
            LocationCategory.NATURE_AND_SCENERY -> R.drawable.landscape
            LocationCategory.TRAVEL_AND_TOURISM -> R.drawable.map
            LocationCategory.SHOPPING -> R.drawable.shopping
            LocationCategory.ACCOMMODATION -> R.drawable.apartment
            LocationCategory.OTHER -> R.drawable.location_map
        }
        return BitmapDescriptorFactory.fromResource(iconResId)
    }



    if (showDateFilterDialog) {
        DateRangeDialog(
            startDate = startDate,
            endDate = endDate,
            onDismiss = { showDateFilterDialog = false },
            onSave = { start, end ->
                mapsScreenViewModel.updateDateFilter(start, end)
                showDateFilterDialog = false
            }
        )
    }


    if (showFilterDialog) {
        FilterDialog (
            selectedCategories = selectedCategories,
            onDismiss = { showFilterDialog = false },
            onSave = { categories ->
                Log.d("FilterDebug", "FilterDialog onSave called with categories: $categories")
                mapsScreenViewModel.updateSelectedCategories(categories)
                showFilterDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            if (isGpsEnabled && userLocation != null && (userLocation!!.latitude != 0.0 || userLocation!!.longitude != 0.0)) {
                val latLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                Marker(
                    state = MarkerState(position = latLng),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                    title = "Your Location"
                )
            }

            locationList.forEach { location ->
                Marker(
                    state = MarkerState(position = LatLng(location.latLng.latitude, location.latLng.longitude)),
                    icon = getCategoryIcon(location.category),
                    onClick = {
                        navHostController.navigate(Screens.LocationDetails(location.id))
                        true
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
                .zIndex(1f)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(isSearchVisible) {
                if (isSearchVisible) {
                    focusRequester.requestFocus()
                }
            }


            if (isSearchVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { mapsScreenViewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text("Search by title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppBlue,
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = AppBlue
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )
                    IconButton(
                        onClick = { mapsScreenViewModel.toggleSearch() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Search",
                            tint = Color.Gray
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppBlue
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (startDate.isNotEmpty() || endDate.isNotEmpty() ||
                            selectedCategories.size != LocationCategory.entries.size) {
                            IconButton(
                                onClick = { mapsScreenViewModel.resetFilters() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AppOrange.copy(alpha = 0.9f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Filters",
                                    tint = Color.White
                                )
                            }
                        }


                        IconButton(
                            onClick = { mapsScreenViewModel.toggleSearch() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AppBlue.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = AppBlue
                            )
                        }

                        IconButton(
                            onClick = { showDateFilterDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AppBlue.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Filter by Date",
                                tint = AppBlue
                            )
                        }

                        IconButton(
                            onClick = { showFilterDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AppBlue.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_filter_alt_24),
                                contentDescription = "Filter Categories",
                                tint = AppBlue
                            )
                        }



                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { navHostController.navigate(Screens.AddLocation(null)) },
            modifier = Modifier
                .padding(24.dp)
                .shadow(12.dp, CircleShape)
                .align(Alignment.BottomEnd),
            containerColor = AppBlue,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Location",
                tint = Color.White
            )
        }
    }
}



