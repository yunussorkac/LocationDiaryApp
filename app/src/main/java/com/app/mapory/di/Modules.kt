package com.app.mapory.di

import com.app.mapory.repo.AddLocationScreenRepo
import com.app.mapory.repo.LocationDetailsScreenRepo
import com.app.mapory.repo.LoginScreenRepo
import com.app.mapory.repo.MapsScreenRepo
import com.app.mapory.repo.ProfileScreenRepo
import com.app.mapory.repo.RecentScreenRepo
import com.app.mapory.repo.RegisterScreenRepo
import com.app.mapory.viewmodel.AddLocationScreenViewModel
import com.app.mapory.viewmodel.LocationDetailsScreenViewModel
import com.app.mapory.viewmodel.LoginScreenViewModel
import com.app.mapory.viewmodel.MapsScreenViewModel
import com.app.mapory.viewmodel.ProfileScreenViewModel
import com.app.mapory.viewmodel.RecentScreenViewModel
import com.app.mapory.viewmodel.RegisterScreenViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object Modules {

    val firebaseModule = module {
        single { FirebaseAuth.getInstance() }
        single { FirebaseFirestore.getInstance() }
        single { FirebaseStorage.getInstance() }
    }

    val repositoryModule = module {
        single { MapsScreenRepo() }
        single { AddLocationScreenRepo() }
        single { LoginScreenRepo() }
        single { RegisterScreenRepo() }
        single { LocationDetailsScreenRepo() }
        single { RecentScreenRepo() }
        single { ProfileScreenRepo() }
    }

    val viewModelModule = module {
        viewModel {
            LoginScreenViewModel(
                get(),get(),get()
            )
        }

        viewModel {
            RegisterScreenViewModel(
                get(),get(),get()

            )
        }

        viewModel {
            AddLocationScreenViewModel(
                get(),get(),get(),get(),get()

            )
        }

        viewModel {
            MapsScreenViewModel(
                get(),get(),get(),get()
            )
        }

        viewModel {
            LocationDetailsScreenViewModel(
                get(),get(),get(),get()
            )
        }

        viewModel {
            RecentScreenViewModel(
                get(),get(),get()
            )
        }

        viewModel {
            ProfileScreenViewModel(
                get(),get(),get(),get()
            )
        }


    }
}





