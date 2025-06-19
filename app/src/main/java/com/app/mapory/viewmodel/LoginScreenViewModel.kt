package com.app.mapory.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.app.mapory.repo.LoginScreenRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginScreenViewModel(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val loginScreenRepo: LoginScreenRepo) : ViewModel() {


    fun login(email : String, password : String, context : Context
              , onSuccess : () -> Unit,onFailure : () -> Unit){
        loginScreenRepo.login(email,password,context,firebaseAuth,firestore,onSuccess,onFailure)

    }

}