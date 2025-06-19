package com.app.mapory.screen.menu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.app.mapory.ui.theme.AppBlue
import com.app.mapory.viewmodel.ProfileScreenViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.app.mapory.activity.MainActivity
import com.app.mapory.model.LocationCategory
import com.app.mapory.storage.UserDataStore
import com.app.mapory.ui.Screens
import com.app.mapory.ui.theme.AlternativeWhite
import com.app.mapory.ui.theme.AppRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navHostController: NavHostController) {
    val context = LocalContext.current
    val viewModel = koinViewModel<ProfileScreenViewModel>()
    val latestLocation by viewModel.latestLocation.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val locationStats by viewModel.locationStats.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()

    val profilePhotoUrl by viewModel.profilePhotoUrl.collectAsStateWithLifecycle()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsStateWithLifecycle()

    val showDialog = remember { mutableStateOf(false) }
    val newUsername = remember { mutableStateOf("") }
    val isUpdatingUsername by viewModel.isUpdatingUsername.collectAsStateWithLifecycle()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.uploadProfilePhoto(context, it)
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            else -> {
                galleryPermissionLauncher.launch(permission)
            }
        }
    }




    LaunchedEffect(navHostController.currentBackStackEntry) {
        viewModel.refreshData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AlternativeWhite)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Profile",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AppBlue
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                showDialog.value = true
                                newUsername.value = username ?: ""
                            },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Username",
                                tint = AppBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        val coroutineScope = rememberCoroutineScope()

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    UserDataStore.clearUser(context)
                                    viewModel.signOut()
                                    val intent = Intent(context, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = AppRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }


                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(AppBlue.copy(alpha = 0.1f), CircleShape)
                                    .clickable { checkAndRequestPermission() }
                            ) {
                                if (isUploadingPhoto) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.Center),
                                        color = AppBlue
                                    )
                                } else {
                                    if (profilePhotoUrl != "") {
                                        AsyncImage(
                                            model = profilePhotoUrl,
                                            contentDescription = "Profile photo",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(com.app.mapory.R.drawable.tourist),
                                            contentDescription = "",
                                            modifier = Modifier
                                                .padding(20.dp)
                                                .fillMaxSize(),
                                            tint = Color.Unspecified
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "$username",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = Color.LightGray,
                                modifier = Modifier.width(50.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = userEmail.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                ItemCard(
                    title = "Last Added Location",
                    icon = Icons.Default.Place
                ) {
                    when {
                        isLoading -> {
                            LoadingIndicator()
                        }
                        error != null -> {
                            ErrorMessage(error ?: "Something went wrong")
                        }
                        latestLocation != null -> {
                            Column (
                                modifier = Modifier.fillMaxWidth().clickable{
                                    navHostController.navigate(Screens.LocationDetails(latestLocation!!.id))
                                }
                            ) {
                                Text(
                                    text = "${latestLocation!!.locationDetails.city}, ${latestLocation!!.locationDetails.district}",
                                    fontSize = 16.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = latestLocation!!.locationDetails.country,
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = latestLocation!!.date,
                                    fontSize = 12.sp,
                                    color = AppBlue
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = "No visited places yet",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            item {
                ItemCard(
                    title = "Statistics",
                    icon = painterResource(com.app.mapory.R.drawable.stats_svgrepo_com)
                ) {
                    if (locationStats != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatItem("Total Visits", locationStats!!.totalVisits)
                            StatItem("This Month", locationStats!!.monthlyVisits)
                            StatItem("This Week", locationStats!!.weeklyVisits)
                        }
                    } else {
                        LoadingIndicator()
                    }
                }
            }

            item {
                ItemCard(
                    title = "Visit Categories",
                    icon = painterResource(com.app.mapory.R.drawable.category_2_svgrepo_com)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        locationStats?.categoryStats?.forEach { (category, count) ->
                            CategoryItem(
                                category = when(category) {
                                    LocationCategory.FOOD_AND_DRINK -> "Food & Drink"
                                    LocationCategory.CULTURE_AND_ART -> "Culture & Art"
                                    LocationCategory.ENTERTAINMENT -> "Entertainment"
                                    LocationCategory.NATURE_AND_SCENERY -> "Nature & Scenery"
                                    LocationCategory.TRAVEL_AND_TOURISM -> "Travel & Tourism"
                                    LocationCategory.SHOPPING -> "Shopping"
                                    LocationCategory.ACCOMMODATION -> "Accommodation"
                                    LocationCategory.OTHER -> "Other"
                                },
                                count = count
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Edit Username") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newUsername.value,
                        onValueChange = { newUsername.value = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdatingUsername
                    )
                    if (isUpdatingUsername) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUsername.value.isNotBlank()) {
                            viewModel.updateUsername(newUsername.value)
                            showDialog.value = false
                        }
                    },
                    enabled = !isUpdatingUsername && newUsername.value.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog.value = false },
                    enabled = !isUpdatingUsername
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ItemCard(
    title: String,
    icon: Any,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                when (icon) {
                    is ImageVector -> Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    is Painter -> Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = AppBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )
            }
            content()
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBlue.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.DarkGray
        )
        Text(
            text = value.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppBlue
        )
    }
}

@Composable
private fun CategoryItem(category: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category,
            fontSize = 14.sp,
            color = Color.DarkGray
        )
        Box(
            modifier = Modifier
                .background(AppBlue, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = AppBlue
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Text(
        text = message,
        color = Color.Red,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}