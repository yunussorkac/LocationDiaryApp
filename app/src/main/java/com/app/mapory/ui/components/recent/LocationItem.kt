package com.app.mapory.ui.components.recent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.app.mapory.R
import com.app.mapory.model.LocationCategory
import com.app.mapory.model.MapLocation
import com.app.mapory.ui.theme.AppBlue
import com.app.mapory.ui.theme.AppOrange

@Composable
fun LocationItem(location: MapLocation, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(location.id) }
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (location.images.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(location.images.first())
                        .crossfade(true)
                        .build(),
                    contentDescription = "Location image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.location)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = AppOrange,
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = location.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = AppOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${location.locationDetails.country}, ${location.locationDetails.city}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = location.date,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                CategoryChip(location.category)
            }
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

