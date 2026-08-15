package com.darkjade.streamlib.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

/**
 * Lightweight app-wide preferences (not tied to a specific profile), backed
 * by Jetpack DataStore. Currently just the preferred external video player —
 * when set, playback skips Android's app chooser and launches that package
 * directly, matching how a "default player" setting typically works.
 */
class PreferencesRepository(private val context: Context) {

    private val preferredPlayerKey = stringPreferencesKey("preferred_player_package")
    private val comicVineApiKeyKey = stringPreferencesKey("comicvine_api_key")

    fun observePreferredPlayerPackage(): Flow<String?> =
        context.settingsDataStore.data.map { it[preferredPlayerKey] }

    suspend fun getPreferredPlayerPackage(): String? =
        context.settingsDataStore.data.map { it[preferredPlayerKey] }.first()

    suspend fun setPreferredPlayerPackage(packageName: String?) {
        context.settingsDataStore.edit { prefs ->
            if (packageName.isNullOrBlank()) prefs.remove(preferredPlayerKey)
            else prefs[preferredPlayerKey] = packageName
        }
    }

    fun observeComicVineApiKey(): Flow<String?> =
        context.settingsDataStore.data.map { it[comicVineApiKeyKey] }

    suspend fun getComicVineApiKey(): String? =
        context.settingsDataStore.data.map { it[comicVineApiKeyKey] }.first()

    suspend fun setComicVineApiKey(key: String) {
        context.settingsDataStore.edit { prefs -> prefs[comicVineApiKeyKey] = key }
    }
}
