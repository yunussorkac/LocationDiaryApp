package com.app.mapory

import android.app.Application
import com.app.mapory.di.Modules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class MaporyApp  : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin{
            androidContext(this@MaporyApp)
            modules(
                Modules.firebaseModule,
                Modules.repositoryModule,
                Modules.viewModelModule
            )
        }
    }

}