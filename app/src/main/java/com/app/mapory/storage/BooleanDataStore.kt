package com.app.mapory.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "boolean_data_store")

object BooleanDataStore {
    private fun getBooleanKey(key: String) = booleanPreferencesKey(key)

    suspend fun saveBoolean(context: Context, key: String, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[getBooleanKey(key)] = value
        }
    }

    suspend fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[getBooleanKey(key)] ?: defaultValue
    }

    suspend fun clearBoolean(context: Context, key: String) {
        context.dataStore.edit { preferences ->
            preferences.remove(getBooleanKey(key))
        }
    }
}