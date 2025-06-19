package com.app.mapory.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.app.mapory.repo.RegisterScreenRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterScreenViewModel(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val registerScreenRepo: RegisterScreenRepo) : ViewModel() {


    fun register(
        context: Context,
        email: String,
        password: String,
        username : String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ){
        registerScreenRepo.register(context,email,password,username,firebaseAuth,firestore,onSuccess,onFailure)

    }

}