package com.app.mapory.repo

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.app.mapory.model.LocationDetails
import com.app.mapory.model.MapLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.coroutines.resume

class AddLocationScreenRepo {

    suspend fun generateAndUploadVideoThumbnail(
        context: Context,
        videoUri: Uri,
        storage: FirebaseStorage,
        userId: String,
        locationId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            var bitmap: Bitmap? = null

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)
                bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (bitmap == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    bitmap = context.contentResolver.loadThumbnail(
                        videoUri,
                        android.util.Size(320, 180),
                        null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (bitmap == null) {
                try {
                    bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        ContentUris.parseId(videoUri),
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (bitmap == null) return@withContext ""

            val file = File.createTempFile("thumbnail_", ".jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()

            val thumbnailRef = storage.reference
                .child("users")
                .child(userId)
                .child("locations")
                .child(locationId)
                .child("thumbnails")
                .child("thumb_${System.currentTimeMillis()}.jpg")

            thumbnailRef.putFile(Uri.fromFile(file)).await()
            val downloadUrl = thumbnailRef.downloadUrl.await().toString()

            file.delete()
            bitmap.recycle()

            downloadUrl
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }



    suspend fun getLocationById(
        firestore: FirebaseFirestore,
        userId: String,
        locationId: String
    ): MapLocation? = withContext(Dispatchers.IO) {
        try {
            val document = firestore.collection("Locations")
                .document(userId)
                .collection("Locations")
                .document(locationId)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(MapLocation::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateLocationInFirestore(
        mapLocation: MapLocation,
        firestore: FirebaseFirestore,
        userId: String
    ) = withContext(Dispatchers.IO) {
        firestore.collection("Locations")
            .document(userId)
            .collection("Locations")
            .document(mapLocation.id)
            .set(mapLocation)
            .await()
    }

    suspend fun downloadMediaUrlsToUris(urls: List<String>): List<Uri> = withContext(Dispatchers.IO) {
        urls.mapNotNull { url ->
            try {
                url.toUri()
            } catch (e: Exception) {
                null
            }
        }

    }



    suspend fun uploadNotesAndGetUrls(
        uris: List<Uri>,
        storage: FirebaseStorage,
        userId: String,
        locationId: String,
        context: Context
    ): List<String> = withContext(Dispatchers.IO) {
        val noteUrls = mutableListOf<String>()

        val existingNotes = try {
            storage.reference
                .child("users")
                .child(userId)
                .child("locations")
                .child(locationId)
                .child("notes")
                .listAll()
                .await()
                .items
        } catch (e: Exception) {
            emptyList()
        }

        val startIndex = existingNotes.size

        uris.forEachIndexed { index, uri ->
            val extension = getFileExtension(uri, context) ?: "txt"

            val noteRef = storage.reference
                .child("users")
                .child(userId)
                .child("locations")
                .child(locationId)
                .child("notes")
                .child("note_${startIndex + index}.$extension")

            try {
                val uploadTask = noteRef.putFile(uri).await()
                val downloadUrl = noteRef.downloadUrl.await()
                noteUrls.add(downloadUrl.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        noteUrls
    }

    private fun getFileExtension(uri: Uri, context: Context): String? {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val mime = context.contentResolver.getType(uri)
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        } else {
            MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        }
    }

    suspend fun uploadAudiosAndGetUrls(
        uris: List<Uri>,
        storage: FirebaseStorage,
        userId: String,
        locationId: String
    ): List<String> = withContext(Dispatchers.IO) {
        val audioUrls = mutableListOf<String>()

        val existingAudios = try {
            storage.reference
                .child("users")
                .child(userId)
                .child("locations")
                .child(locationId)
                .child("audios")
                .listAll()
                .await()
                .items
        } catch (e: Exception) {
            emptyList()
        }

        val startIndex = existingAudios.size

        uris.forEachIndexed { index, uri ->
            val audioRef = storage.reference
                .child("users")
                .child(userId)
                .child("locations")
                .child(locationId)
                .child("audios")
                .child("audio_${startIndex + index}.mp3")

            try {
                val uploadTask = audioRef.putFile(uri).await()
                val downloadUrl = audioRef.downloadUrl.await()
                audioUrls.add(downloadUrl.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        audioUrls
    }

    suspend fun uploadVideosAndGetUrls(
        context: Context,
        uris: List<Uri>,
        storage: FirebaseStorage,
        userId: String,
        locationId: String
    ): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val videoData = mutableListOf<Pair<String, String>>()

        val existingVideos = try {
            storage.reference
                .child("users")
                .child(userId)
                .child("locations")
                .child(locationId)
                .child("videos")
                .listAll()
                .await()
                .items
        } catch (e: Exception) {
            emptyList()
        }

        val startIndex = existingVideos.size

        uris.forEachIndexed { index, uri ->
            val videoRef = storage.reference
                .child("users")
                .child(userId)
                .child("locations")
                .child(locationId)
                .child("videos")
                .child("video_${startIndex + index}.mp4")

            try {
                val uploadTask = videoRef.putFile(uri).await()
                val downloadUrl = videoRef.downloadUrl.await().toString()

                val thumbnailUrl = generateAndUploadVideoThumbnail(
                    context,
                    uri,
                    storage,
                    userId,
                    locationId
                )

                videoData.add(Pair(downloadUrl, thumbnailUrl))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        videoData
    }

    suspend fun uploadImagesAndGetUrls(
        uris: List<Uri>,
        storage: FirebaseStorage,
        userId: String,
        locationId: String
    ): List<String> = withContext(Dispatchers.IO) {
        val imageUrls = mutableListOf<String>()

        val existingImages = try {
            storage.reference
                .child("users")
                .child(userId)
                .child("locations")
                .child(locationId)
                .child("images")
                .listAll()
                .await()
                .items
        } catch (e: Exception) {
            emptyList()
        }

        val startIndex = existingImages.size

        uris.forEachIndexed { index, uri ->
            val imageRef = storage.reference
                .child("users")
                .child(userId)
                .child("locations")
                .child(locationId)
                .child("images")
                .child("image_${startIndex + index}.jpg")

            try {
                val uploadTask = imageRef.putFile(uri).await()
                val downloadUrl = imageRef.downloadUrl.await()
                imageUrls.add(downloadUrl.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        imageUrls
    }

    suspend fun addLocationToFirestore(
        mapLocation: MapLocation,
        firestore: FirebaseFirestore,
        userId: String
    ) {
        val collectionRef = firestore.collection("Locations")
        collectionRef.document(userId)
            .collection("Locations")
            .document(mapLocation.id)
            .set(mapLocation)
            .await()
    }


    suspend fun getCurrentLocation(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener { exception ->
                continuation.resume(null)
            }
        } catch (e: SecurityException) {
            continuation.resume(null)
        }
    }



    suspend fun getLocationDetails(context: Context, latLng: LatLng): LocationDetails = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            continuation.resume(createLocationDetails(addresses[0]))
                        } else {
                            continuation.resume(LocationDetails())
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    createLocationDetails(addresses[0])
                } else {
                    LocationDetails()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LocationDetails()
        }
    }

    private fun createLocationDetails(address: Address): LocationDetails {
        return LocationDetails(
            address = buildString {
                for (i in 0..address.maxAddressLineIndex) {
                    append(address.getAddressLine(i))
                    if (i < address.maxAddressLineIndex) append(", ")
                }
            },
            city = address.adminArea ?: "",
            district = address.subAdminArea ?: "",
            neighborhood = address.subLocality ?: "",
            country = address.countryName ?: "",
            knownName = address.featureName ?: ""
        )
    }
}