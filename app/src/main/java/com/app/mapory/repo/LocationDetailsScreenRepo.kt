package com.app.mapory.repo

import com.app.mapory.model.MapLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationDetailsScreenRepo {


    fun getLocationDetails(
        userId: String,
        locationId: String,
        firestore: FirebaseFirestore
    ): Flow<MapLocation> = callbackFlow {
        val documentRef = firestore
            .collection("Locations")
            .document(userId)
            .collection("Locations")
            .document(locationId)

        val snapshotListener = documentRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                close(exception)
                return@addSnapshotListener
            }

            snapshot?.toObject(MapLocation::class.java)?.let { location ->
                trySend(location)
            } ?: close(NoSuchElementException("Location not found"))
        }

        awaitClose {
            snapshotListener.remove()
        }
    }

    suspend fun deleteLocation(userId: String, locationId: String, firestore: FirebaseFirestore): Result<Unit> {
        return try {
            // Önce Firestore'dan silelim
            firestore.collection("Locations")
                .document(userId)
                .collection("Locations")
                .document(locationId)
                .delete()
                .await()

            val storage = FirebaseStorage.getInstance()
            val locationRef = storage.reference
                .child("users")
                .child(userId)
                .child("locations")
                .child(locationId)

            try {
                val result = locationRef.listAll().await()

                val mainDeletions = result.items.map { item ->
                    item.delete()
                }

                Tasks.whenAll(mainDeletions).await()

                val subFolderDeletions = mutableListOf<Task<Void>>()

                result.prefixes.forEach { prefix ->
                    val subFolderItems = prefix.listAll().await()
                    subFolderItems.items.forEach { item ->
                        subFolderDeletions.add(item.delete())
                    }
                }

                if (subFolderDeletions.isNotEmpty()) {
                    Tasks.whenAll(subFolderDeletions).await()
                }

                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }


}