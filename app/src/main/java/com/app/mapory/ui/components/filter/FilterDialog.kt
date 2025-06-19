package com.app.mapory.ui.components.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.mapory.R
import com.app.mapory.model.LocationCategory
import com.app.mapory.ui.theme.AppBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    selectedCategories: Set<LocationCategory>,
    onDismiss: () -> Unit,
    onSave: (Set<LocationCategory>) -> Unit
) {
    val tempSelectedCategories = remember { mutableStateOf(selectedCategories) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Filter Categories",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppBlue,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LocationCategory.entries.forEach { category ->
                    FilterCategoryItem(
                        category = category,
                        isSelected = category in tempSelectedCategories.value,
                        onSelectionChanged = { checked ->
                            tempSelectedCategories.value = if (checked) {
                                tempSelectedCategories.value + category
                            } else {
                                tempSelectedCategories.value - category
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(tempSelectedCategories.value) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterCategoryItem(
    category: LocationCategory,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChanged,
            colors = CheckboxDefaults.colors(
                checkedColor = AppBlue,
                uncheckedColor = Color.Gray
            )
        )

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
            modifier = Modifier
                .size(24.dp)
                .padding(start = 8.dp),
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
            modifier = Modifier.padding(start = 12.dp),
            fontSize = 16.sp,
            color = Color.DarkGray
        )
    }
}

