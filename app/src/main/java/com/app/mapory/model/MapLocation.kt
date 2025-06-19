package com.app.mapory.model


data class MapLocation(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date : String = "",
    val dateMillis : Long = 0,
    val latLng: LatLng = LatLng(),
    val category: LocationCategory = LocationCategory.FOOD_AND_DRINK,
    val locationDetails: LocationDetails = LocationDetails(),
    val images: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
    val audios: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val videoThumbnails: List<String> = emptyList(),


)

data class LatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class LocationDetails(
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val neighborhood: String = "",
    val country: String = "",
    val knownName: String = ""
)

enum class LocationCategory {
    FOOD_AND_DRINK,
    CULTURE_AND_ART,
    ENTERTAINMENT,
    NATURE_AND_SCENERY,
    TRAVEL_AND_TOURISM,
    SHOPPING,
    ACCOMMODATION,
    OTHER;

    override fun toString(): String {
        return when (this) {
            FOOD_AND_DRINK -> "Food & Drink"
            CULTURE_AND_ART -> "Culture & Art"
            ENTERTAINMENT -> "Entertainment"
            NATURE_AND_SCENERY -> "Nature & Scenery"
            TRAVEL_AND_TOURISM -> "Travel & Tourism"
            SHOPPING -> "Shopping"
            ACCOMMODATION -> "Accommodation"
            OTHER -> "Other"
        }
    }

    companion object {
        fun fromString(value: String): LocationCategory {
            return when (value) {
                "Food & Drink" -> FOOD_AND_DRINK
                "Culture & Art" -> CULTURE_AND_ART
                "Entertainment" -> ENTERTAINMENT
                "Nature & Scenery" -> NATURE_AND_SCENERY
                "Travel & Tourism" -> TRAVEL_AND_TOURISM
                "Shopping" -> SHOPPING
                "Accommodation" -> ACCOMMODATION
                else -> OTHER
            }
        }
    }
}