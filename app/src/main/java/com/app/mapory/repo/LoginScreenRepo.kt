package com.app.mapory.repo

import android.content.Context
import com.app.mapory.model.User
import com.app.mapory.storage.UserDataStore
import com.app.mapory.util.DummyMethods
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import www.sanju.motiontoast.MotionToastStyle

class LoginScreenRepo {



    fun login(email : String, password : String, context : Context,firebaseAuth: FirebaseAuth
              , firestore: FirebaseFirestore
              , onSuccess : () -> Unit,onFailure : () -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(
                    email.trim().lowercase(),
                    password.trim()
                ).await()

                val user = authResult.user
                user?.reload()

                val myUser = user?.let {
                    firestore.collection("Users").document(it.uid)
                        .get().await().toObject(User::class.java)
                }

                if (myUser != null) {
                    UserDataStore.saveUser(context, myUser)

                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onFailure()
                    }
                }

            } catch (e: FirebaseAuthException) {
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    onFailure()
                    DummyMethods.showMotionToast(
                        context,
                        "Something went wrong.",
                        "${e.message}",
                        MotionToastStyle.ERROR
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onFailure()
                    DummyMethods.showMotionToast(
                        context,
                        "Something went wrong.",
                        "${e.message}",
                        MotionToastStyle.ERROR
                    )
                }
            }
        }
    }
}
