package com.app.mapory.repo

import android.content.Context
import com.app.mapory.model.LatLng
import com.app.mapory.model.User
import com.app.mapory.storage.UserDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterScreenRepo {

    fun register(
        context: Context,
        email: String,
        password: String,
        username: String,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(
                    email.trim().lowercase(),
                    password.trim()
                ).await()

                val userId = authResult.user?.uid ?: ""

                val user = User(
                    userId = userId,
                    email = email.trim().lowercase(),
                    latLng = LatLng(0.0, 0.0),
                    username = username,
                    profilePhoto = ""
                )

                firestore.collection("Users").document(userId).set(user).await()

                UserDataStore.saveUser(context, user)

                withContext(Dispatchers.Main) {
                    onSuccess()
                }

            } catch (e: FirebaseAuthException) {
                withContext(Dispatchers.Main) {
                    val errorMessage = when {
                        e.message?.contains("email address is already in use") == true ->
                            "This email is already in use."
                        e.message?.contains("password is invalid") == true ->
                            "Password must be at least 6 characters long."
                        e.message?.contains("badly formatted") == true ->
                            "Please enter a valid email address."
                        e.message?.contains("network error") == true ->
                            "Network error occurred. Please check your internet connection."
                        else -> "Registration failed: ${e.message}"
                    }
                    onFailure(errorMessage)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onFailure("An unexpected error occurred. Please try again.")
                }
            }
        }
    }

}