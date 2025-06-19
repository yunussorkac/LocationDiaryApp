package com.app.mapory.ui

import com.app.mapory.R
import kotlinx.serialization.Serializable


sealed class Screens() {

    @Serializable
    data object Login

    @Serializable
    data object Register

    @Serializable
    data class AddLocation(val locationId: String? = null)

    @Serializable
    data class LocationDetails(val locationId : String)

}

sealed class NavigationItem(val route: String, val title: String, val icon: Int) {

    data object Maps : NavigationItem("maps", "Maps", R.drawable.bottom_map)
    data object Recent : NavigationItem("recent", "History", R.drawable.bottom_history)
    data object Profile : NavigationItem("profile", "Profile", R.drawable.bottom_profile)


}