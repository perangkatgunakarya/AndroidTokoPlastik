package com.example.tokoplastik.data

import android.content.Context
import androidx.datastore.DataStore
import androidx.datastore.preferences.Preferences
import androidx.datastore.preferences.clear
import androidx.datastore.preferences.createDataStore
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.preferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferences (
    context: Context
) {

    private val applicationContext = context.applicationContext
    private val dataStore: DataStore<Preferences>

    init {
        dataStore = applicationContext.createDataStore(
            name = "auth_data_store"
        )
    }

    val authToken: Flow<String?>
        get() = dataStore.data.map {
            preferences -> preferences[KEY_AUTH]
        }

    val username: Flow<String?>
        get() = dataStore.data.map {
            preferences -> preferences[KEY_USERNAME]
        }

    suspend fun saveAuthToken(authToken: String) {
        dataStore.edit {
            preferences -> preferences[KEY_AUTH] = authToken
        }
    }

    suspend fun saveUsername(username: String) {
        dataStore.edit {
            preferences -> preferences[KEY_USERNAME] = username
        }
    }

    companion object {
        private val KEY_AUTH = preferencesKey<String>("key_auth")
        private val KEY_USERNAME = preferencesKey<String>("key_username")
    }

    suspend fun clearAuthToken() {
        dataStore.edit {
            preferences -> preferences.clear()
        }
    }

    suspend fun clearUsername() {
        dataStore.edit {
            preferences -> preferences.clear()
        }
    }
}