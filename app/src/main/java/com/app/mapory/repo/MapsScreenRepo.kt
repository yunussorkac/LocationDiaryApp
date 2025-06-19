package com.app.mapory.repo

import android.util.Log
import com.app.mapory.model.MapLocation
import com.app.mapory.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resumeWithException

class MapsScreenRepo {

    fun getLatestLocation(
        firestore: FirebaseFirestore,
        userId: String,
        onLocationUpdate: (MapLocation?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("Locations")
            .document(userId)
            .collection("Locations")
            .orderBy("id", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }



                if (snapshot == null || snapshot.isEmpty) {
                    onLocationUpdate(null)
                } else {
                    try {
                        val location = snapshot.documents[0].toObject(MapLocation::class.java)
                        onLocationUpdate(location)
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
            }
    }



    suspend fun saveUserToFirestore(user: User, firestore: FirebaseFirestore, firebaseAuth: FirebaseAuth) {
        return suspendCancellableCoroutine { continuation ->
            try {
                firebaseAuth.currentUser?.uid?.let { userId ->
                    firestore.collection("Users")
                        .document(userId)
                        .set(user)
                        .addOnSuccessListener {
                            Log.d("MapsRepo", "User location successfully saved to Firestore")
                            continuation.resume(Unit) {}
                        }
                        .addOnFailureListener { e ->
                            Log.e("MapsRepo", "Error saving user location to Firestore", e)
                            continuation.resumeWithException(e)
                        }
                } ?: run {
                    continuation.resumeWithException(Exception("User not authenticated"))
                }
            } catch (e: Exception) {
                Log.e("MapsRepo", "Error saving to Firestore", e)
                continuation.resumeWithException(e)
            }
        }
    }

    fun getLocations(userId: String, firestore: FirebaseFirestore): Flow<List<MapLocation>> = callbackFlow {
        val collectionRef = firestore.collection("Locations")
            .document(userId)
            .collection("Locations")

        val snapshotListener = collectionRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                close(exception)
                return@addSnapshotListener
            }

            val locations = snapshot?.documents?.mapNotNull { document ->
                document.toObject(MapLocation::class.java)
            } ?: emptyList()

            trySend(locations)
        }

        awaitClose {
            snapshotListener.remove()
        }
    }



}