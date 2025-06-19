package com.app.mapory.model


data class User(
    val userId : String = "",
    val email : String = "",
    val latLng: LatLng = LatLng(0.0, 0.0),
    val username : String = "",
    val profilePhoto : String = "",
) {
}