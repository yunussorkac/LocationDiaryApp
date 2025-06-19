package com.app.mapory.screen

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import com.app.mapory.viewmodel.LocationDetailsScreenViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.mapory.viewmodel.LocationDetailsState

import androidx.compose.material3.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.font.FontWeight
import com.app.mapory.model.LocationCategory
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.DialogProperties
import com.app.mapory.model.MapLocation

import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.app.mapory.ui.theme.AlternativeWhite
import com.app.mapory.ui.theme.AppBlue
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.mapory.R
import com.app.mapory.ui.components.audio.AudioSection
import com.app.mapory.ui.components.image.ImageCarousel
import com.app.mapory.ui.components.map.MapPreview
import com.app.mapory.ui.components.note.NotesSection
import com.app.mapory.ui.components.video.VideoSection
import com.app.mapory.ui.Screens

@Composable
fun LocationDetailsScreen(navHostController: NavHostController, locationId: String) {


    val context = LocalContext.current
    val locationDetailsScreenViewModel = koinViewModel<LocationDetailsScreenViewModel>()
    val state by locationDetailsScreenViewModel.locationState.collectAsStateWithLifecycle()


    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }




    LaunchedEffect(locationId) {
        locationDetailsScreenViewModel.getLocationDetails(locationId)
    }



    


    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is LocationDetailsState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is LocationDetailsState.Success -> {
                val location = currentState.location
                location.PrintDebugInfo()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .background(AlternativeWhite)
                        .padding(bottom = 10.dp)
                ) {
                    if (location.images.isNotEmpty()) {
                        Box {
                            ImageCarousel(images = location.images)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BackButton(navHostController)
                                Row {
                                    IconButton(
                                        onClick = {
                                            navHostController.navigate(Screens.AddLocation(location.id))
                                        },
                                        modifier = Modifier
                                            .padding(start = 10.dp, top = 10.dp)
                                            .size(40.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { showDeleteDialog = true },
                                        modifier = Modifier
                                            .padding(start = 10.dp, top = 10.dp)
                                            .size(40.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BackButton(navHostController)
                                Row {
                                    IconButton(
                                        onClick = {
                                            navHostController.navigate(Screens.AddLocation(location.id))
                                        },
                                        modifier = Modifier
                                            .padding(start = 10.dp, top = 10.dp)
                                            .size(40.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { showDeleteDialog = true },
                                        modifier = Modifier
                                            .padding(start = 10.dp, top = 10.dp)
                                            .size(40.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    LocationHeader(
                        title = location.title,
                        date = location.date,
                        category = location.category
                    )

                    LocationDescription(description = location.description)

                    Spacer(modifier = Modifier.height(15.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "${location.locationDetails.city}/${location.locationDetails.district}, ${location.locationDetails.address}",
                                color = Color.Black,
                                modifier = Modifier.padding(start = 20.dp),
                                fontWeight = FontWeight.Bold
                            )

                            MapPreview(latLng = location.latLng)

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    if (location.videos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        VideoSection(videos = location.videos, thumbnails = location.videoThumbnails)
                    }

                    if (location.audios.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AudioSection(audios = location.audios)
                    }

                    if (location.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NotesSection(notes = location.notes, viewModel = locationDetailsScreenViewModel)
                    }
                }
            }

            is LocationDetailsState.Error -> {
                Log.e("LocationDetailsScreen", "Error: ${currentState.message}")
            }
        }
    }



    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Delete Location",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this location?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            locationDetailsScreenViewModel.deleteLocation(
                                locationId,
                                {
                                    showDeleteDialog = false
                                    navHostController.previousBackStackEntry?.savedStateHandle?.set("locationDeleted", true)
                                    navHostController.navigateUp()
                                },
                                { showDeleteDialog = false; navHostController.navigateUp() }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.8f)
                    )
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

}

@Composable
private fun BackButton(navHostController: NavHostController) {
    IconButton(
        onClick = { navHostController.navigateUp() },
        modifier = Modifier
            .padding(start = 10.dp, top = 10.dp)
            .size(40.dp)
            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun LocationDescription(description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Composable
private fun LocationHeader(
    title: String,
    date: String,
    category: LocationCategory,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = AppBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            CategoryChip(category = category)
        }
    }
}


@Composable
fun MapLocation.PrintDebugInfo() {
    LaunchedEffect(Unit) {
        println("Debug: Location Images:")
        images.forEachIndexed { index, url ->
            println("Image $index: $url")
        }
    }
}

@Composable
fun CategoryChip(category: LocationCategory) {
    Surface(
        modifier = Modifier.wrapContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = AppBlue.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = when (category) {
                        LocationCategory.FOOD_AND_DRINK -> R.drawable.food
                        LocationCategory.CULTURE_AND_ART -> R.drawable.culture
                        LocationCategory.ENTERTAINMENT -> R.drawable.entertainment
                        LocationCategory.NATURE_AND_SCENERY -> R.drawable.landscape
                        LocationCategory.TRAVEL_AND_TOURISM -> R.drawable.map
                        LocationCategory.SHOPPING -> R.drawable.shopping
                        LocationCategory.ACCOMMODATION -> R.drawable.apartment
                        LocationCategory.OTHER -> R.drawable.location_map
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Unspecified
            )

            Text(
                text = when (category) {
                    LocationCategory.FOOD_AND_DRINK -> "Food & Drink"
                    LocationCategory.CULTURE_AND_ART -> "Culture & Art"
                    LocationCategory.ENTERTAINMENT -> "Entertainment"
                    LocationCategory.NATURE_AND_SCENERY -> "Nature & Scenery"
                    LocationCategory.TRAVEL_AND_TOURISM -> "Travel & Tourism"
                    LocationCategory.SHOPPING -> "Shopping"
                    LocationCategory.ACCOMMODATION -> "Accommodation"
                    LocationCategory.OTHER -> "Other"
                },
                fontSize = 12.sp,
                color = AppBlue
            )
        }

    }

}


