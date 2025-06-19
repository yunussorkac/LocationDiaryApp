package com.app.mapory.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.app.mapory.model.User
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "user_data_store")

object UserDataStore {
    private val USER_JSON_KEY = stringPreferencesKey("user_json")

    suspend fun saveUser(context: Context, user: User) {
        val userJson = Gson().toJson(user)
        context.dataStore.edit { preferences ->
            preferences[USER_JSON_KEY] = userJson
        }
    }

    suspend fun getUser(context: Context): User? {
        val preferences = context.dataStore.data.first()
        val userJson = preferences[USER_JSON_KEY]
        return userJson?.let { Gson().fromJson(it, User::class.java) }
    }

    suspend fun clearUser(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_JSON_KEY)
        }
    }
}