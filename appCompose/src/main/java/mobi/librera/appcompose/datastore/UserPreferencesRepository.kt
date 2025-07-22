package mobi.librera.appcompose.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_settings"
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val IS_DARK_MODE_ENABLED = booleanPreferencesKey("is_dark_mode_enabled")
        val APP_LAUNCH_COUNT = intPreferencesKey("app_launch_count")
    }

    val userName: Flow<String> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USER_NAME] ?: "Guest"
        }

    val isDarkModeEnabled: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE_ENABLED] ?: false
        }

    val appLaunchCount: Flow<Int> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_LAUNCH_COUNT] ?: 0
        }

    suspend fun saveUserName(name: String) {
        context.userPreferencesDataStore.edit { settings ->
            settings[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { settings ->
            settings[PreferencesKeys.IS_DARK_MODE_ENABLED] = enabled
        }
    }

    suspend fun incrementAppLaunchCount() {
        context.userPreferencesDataStore.edit { settings ->
            val currentCount = settings[PreferencesKeys.APP_LAUNCH_COUNT] ?: 0
            settings[PreferencesKeys.APP_LAUNCH_COUNT] = currentCount + 1
        }
    }
}