package com.app.mapory.ui.components.location

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.app.mapory.model.LocationDetails
import com.app.mapory.ui.theme.AppBlue
import com.app.mapory.util.DummyMethods
import com.app.mapory.viewmodel.LocationUiState
import www.sanju.motiontoast.MotionToastStyle
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.vector.ImageVector
import com.app.mapory.R

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationFormSection(
    uiState: LocationUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDropdownExpandedChange: () -> Unit,
    onSaveClick: () -> Unit,
    onShowLocationTabChange: (Boolean) -> Unit,
    context: Context,
    isEditMode: Boolean = false

) {
    var showDatePicker by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = null,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val selectedDate = Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    return !selectedDate.isAfter(today)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            val formattedDate = selectedDate.format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy")
                            )
                            onDateChange(formattedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                headline = { Text("Select a date") }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = if (isEditMode) "Update Location" else "Add Location",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        LinearProgressIndicator(
            progress = calculateProgress(uiState),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = AppBlue,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = if (uiState.showLocationTab)
                        AppBlue.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    onClick = { onShowLocationTabChange(!uiState.showLocationTab) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = uiState.showLocationTab,
                            onCheckedChange = { onShowLocationTabChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppBlue,
                                checkedTrackColor = AppBlue.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Manual Location",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Toggle to add location details manually",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                FormTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    label = "Location Title",
                    icon = Icons.Rounded.Edit
                )

                FormTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    label = "Description",
                    icon = Icons.Rounded.Edit,
                    singleLine = false,
                    minLines = 3
                )

                FormTextField(
                    value = uiState.date,
                    onValueChange = {

                    },
                    label = "Select Date",
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Rounded.DateRange,
                                contentDescription = "Select Date",
                                tint = AppBlue
                            )
                        }
                    },
                    readOnly = true,

                )

                ExposedDropdownMenuBox(
                    expanded = uiState.isDropdownExpanded,
                    onExpandedChange = { onDropdownExpandedChange() }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = null,
                                tint = AppBlue
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = uiState.isDropdownExpanded
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = uiState.isDropdownExpanded,
                        onDismissRequest = onDropdownExpandedChange
                    ) {
                        uiState.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    onCategoryChange(category)
                                    onDropdownExpandedChange()
                                },
                                leadingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = when (category) {
                                                    "Food & Drink" -> R.drawable.food
                                                    "Culture & Art" -> R.drawable.culture
                                                    "Entertainment" -> R.drawable.entertainment
                                                    "Nature & Scenery" -> R.drawable.landscape
                                                    "Travel & Tourism" -> R.drawable.map
                                                    "Shopping" -> R.drawable.shopping
                                                    "Accommodation" -> R.drawable.apartment
                                                    else -> R.drawable.location_map
                                                }
                                            ),
                                            contentDescription = null,
                                            tint = Color.Unspecified
                                        )

                                        AnimatedVisibility(
                                            visible = category == uiState.selectedCategory,
                                            enter = fadeIn() + expandHorizontally(),
                                            exit = fadeOut() + shrinkHorizontally()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = AppBlue
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = uiState.locationDetails != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    uiState.locationDetails?.let { details ->
                        LocationDetailsCard(details)
                    }
                }
            }
        }


        Button(
            onClick = {
                validateAndSave(uiState, onSaveClick, context)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppBlue
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) "Update Location" else "Save Location",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppBlue
                )
            }
        },
        trailingIcon = trailingIcon,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        singleLine = singleLine,
        minLines = minLines,
        readOnly = readOnly
    )
}

@Composable
private fun LocationDetailsCard(details: LocationDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppBlue.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = AppBlue,
                modifier = Modifier.size(30.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Selected Location",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (details.address.isEmpty()){
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AppBlue
                    )
                }

                Text(
                    text = details.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            }


        }
    }
}

private fun calculateProgress(uiState: LocationUiState): Float {
    var completedFields = 0
    var totalFields = 4

    if (uiState.title.isNotEmpty()) completedFields++
    if (uiState.description.isNotEmpty()) completedFields++
    if (uiState.selectedCategory.isNotEmpty()) completedFields++
    if (uiState.locationDetails != null) completedFields++

    return completedFields.toFloat() / totalFields
}

private fun validateAndSave(
    uiState: LocationUiState,
    onSaveClick: () -> Unit,
    context: Context
) {
    when {
        uiState.title.isEmpty() -> {
            DummyMethods.showMotionToast(context, "Please enter a title", "", MotionToastStyle.ERROR)
        }
        uiState.description.isEmpty() -> {
            DummyMethods.showMotionToast(context, "Please enter a description", "", MotionToastStyle.ERROR)
        }
        uiState.selectedCategory.isEmpty() -> {
            DummyMethods.showMotionToast(context, "Please select a category", "", MotionToastStyle.ERROR)
        }
        uiState.locationDetails == null -> {
            DummyMethods.showMotionToast(context, "Please add location information", "", MotionToastStyle.ERROR)
        }
        uiState.locationDetails.address.isEmpty() -> {
            DummyMethods.showMotionToast(context, "Please add location information", "", MotionToastStyle.ERROR)
        }
        else -> {
            onSaveClick()
        }
    }
}