package com.nandomclaren.watery.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "watery_prefs")

private object Keys {
    val GOAL_ML = intPreferencesKey("goal_ml")
    val GLASS_ML = intPreferencesKey("glass_ml")
    val DRUNK_ML = intPreferencesKey("drunk_ml")
    val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
    val LAST_RECORD_ID = stringPreferencesKey("last_record_id")
}

class WaterPreferencesRepository(private val context: Context) {

    val state: Flow<WaterUiState> = context.dataStore.data.map { prefs ->
        WaterUiState(
            goalMl = prefs[Keys.GOAL_ML] ?: DEFAULT_GOAL_ML,
            glassMl = prefs[Keys.GLASS_ML] ?: DEFAULT_GLASS_ML,
            drunkMl = prefs[Keys.DRUNK_ML] ?: 0,
        )
    }

    suspend fun updateSettings(goalMl: Int, glassMl: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOAL_ML] = goalMl.coerceAtLeast(1)
            prefs[Keys.GLASS_ML] = glassMl.coerceAtLeast(1)
        }
    }

    /**
     * Resets the daily counter when the calendar day has changed, then (when Health
     * Connect is available and authorized) refreshes today's total from it, so the
     * widget stays in sync with intake logged from any other Health Connect app.
     */
    suspend fun syncAndGetState(healthConnectManager: HealthConnectManager): WaterUiState {
        val today = LocalDate.now().toString()
        context.dataStore.edit { prefs ->
            if (prefs[Keys.LAST_RESET_DATE] != today) {
                prefs[Keys.DRUNK_ML] = 0
                prefs[Keys.LAST_RESET_DATE] = today
                prefs.remove(Keys.LAST_RECORD_ID)
            }
        }

        if (healthConnectManager.hasAllPermissions()) {
            val totalFromHealthConnect = healthConnectManager.readTodayTotalMl()
            if (totalFromHealthConnect != null) {
                context.dataStore.edit { prefs ->
                    prefs[Keys.DRUNK_ML] = totalFromHealthConnect
                }
            }
        }

        return state.first()
    }

    suspend fun addGlass(healthConnectManager: HealthConnectManager) {
        val current = state.first()
        val recordId = if (healthConnectManager.hasAllPermissions()) {
            healthConnectManager.insertGlass(current.glassMl)
        } else {
            null
        }
        context.dataStore.edit { prefs ->
            prefs[Keys.DRUNK_ML] = (prefs[Keys.DRUNK_ML] ?: 0) + current.glassMl
            if (recordId != null) {
                prefs[Keys.LAST_RECORD_ID] = recordId
            } else {
                prefs.remove(Keys.LAST_RECORD_ID)
            }
        }
    }

    suspend fun undoLastGlass(healthConnectManager: HealthConnectManager) {
        val current = state.first()
        if (current.drunkMl <= 0) return
        val lastRecordId = context.dataStore.data.first()[Keys.LAST_RECORD_ID]
        if (lastRecordId != null) {
            healthConnectManager.deleteRecord(lastRecordId)
        }
        context.dataStore.edit { prefs ->
            prefs[Keys.DRUNK_ML] = ((prefs[Keys.DRUNK_ML] ?: 0) - current.glassMl).coerceAtLeast(0)
            prefs.remove(Keys.LAST_RECORD_ID)
        }
    }

    companion object {
        const val DEFAULT_GOAL_ML = 2000
        const val DEFAULT_GLASS_ML = 250
    }
}
