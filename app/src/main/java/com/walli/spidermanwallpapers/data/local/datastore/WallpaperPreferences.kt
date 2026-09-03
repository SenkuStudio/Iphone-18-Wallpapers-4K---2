package com.walli.spidermanwallpapers.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "wallpaper_prefs")

@Singleton
class WallpaperPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val unlockedWallpapersKey = stringSetPreferencesKey("unlocked_wallpapers")
    private val editToolsUnlockTimeKey = longPreferencesKey("edit_tools_unlock_time")
    private val premiumPassUnlockTimeKey = longPreferencesKey("premium_pass_unlock_time")

    val unlockedWallpaperIds: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[unlockedWallpapersKey] ?: emptySet()
        }

    val editToolsUnlockTime: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[editToolsUnlockTimeKey] ?: 0L
        }

    val premiumPassUnlockTime: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[premiumPassUnlockTimeKey] ?: 0L
        }

    suspend fun unlockWallpaper(id: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[unlockedWallpapersKey] ?: emptySet()
            preferences[unlockedWallpapersKey] = current + id
        }
    }

    suspend fun unlockEditTools() {
        context.dataStore.edit { preferences ->
            preferences[editToolsUnlockTimeKey] = System.currentTimeMillis()
        }
    }

    suspend fun unlockPremiumPass() {
        context.dataStore.edit { preferences ->
            preferences[premiumPassUnlockTimeKey] = System.currentTimeMillis()
        }
    }
}
