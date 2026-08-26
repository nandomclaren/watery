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
     *
     * Reading from Health Connect requires the app to be in the foreground unless it
     * holds the (hard-restricted, effectively unobtainable for a sideloaded app)
     * READ_HEALTH_DATA_IN_BACKGROUND permission, so background callers (the widget,
     * its actions, the periodic worker) must pass [allowHealthConnectRead] = false and
     * rely on the local cache, which stays accurate since every glass logged through
     * this app is written straight to Health Connect either way.
     */
    suspend fun syncAndGetState(
        healthConnectManager: HealthConnectManager,
        allowHealthConnectRead: Boolean = true,
    ): WaterUiState {
        val today = LocalDate.now().toString()
        context.dataStore.edit { prefs ->
            if (prefs[Keys.LAST_RESET_DATE] != today) {
                prefs[Keys.DRUNK_ML] = 0
                prefs[Keys.LAST_RESET_DATE] = today
                prefs.remove(Keys.LAST_RECORD_ID)
            }
        }

        if (allowHealthConnectRead && healthConnectManager.hasAllPermissions()) {
            val totalFromHealthConnect = healthConnectManager.readTodayTotalMl()
            if (totalFromHealthConnect != null) {
                context.dataStore.edit { prefs ->
                    prefs[Keys.DRUNK_ML] = totalFromHealthConnect
                }
            }
        }

        return state.first()
    }

    /**
     * Bumps the local counter by one glass and returns its size in mL. This is the
     * fast, always-reliable half of adding a glass - callers that render UI (the
     * widget in particular) should refresh right after this returns, then call
     * [syncLastGlassToHealthConnect] afterwards so a slow or failing Health Connect
     * call never delays the on-screen update.
     */
    suspend fun addGlassLocal(): Int {
        val glassMl = state.first().glassMl
        context.dataStore.edit { prefs ->
            prefs[Keys.DRUNK_ML] = (prefs[Keys.DRUNK_ML] ?: 0) + glassMl
        }
        return glassMl
    }

    /** Best-effort mirror of the most recently added glass into Health Connect. */
    suspend fun syncLastGlassToHealthConnect(healthConnectManager: HealthConnectManager, glassMl: Int) {
        val recordId = try {
            if (healthConnectManager.hasAllPermissions()) {
                healthConnectManager.insertGlass(glassMl)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
        context.dataStore.edit { prefs ->
            if (recordId != null) {
                prefs[Keys.LAST_RECORD_ID] = recordId
            } else {
                prefs.remove(Keys.LAST_RECORD_ID)
            }
        }
    }

    suspend fun addGlass(healthConnectManager: HealthConnectManager) {
        val glassMl = addGlassLocal()
        syncLastGlassToHealthConnect(healthConnectManager, glassMl)
    }

    /** Reverts the local counter by one glass. Fast and always reliable. */
    suspend fun undoLastGlassLocal(): Boolean {
        val current = state.first()
        if (current.drunkMl <= 0) return false
        context.dataStore.edit { prefs ->
            prefs[Keys.DRUNK_ML] = ((prefs[Keys.DRUNK_ML] ?: 0) - current.glassMl).coerceAtLeast(0)
        }
        return true
    }

    /** Best-effort deletes the last glass's Health Connect record, if any. */
    suspend fun syncUndoToHealthConnect(healthConnectManager: HealthConnectManager) {
        val lastRecordId = context.dataStore.data.first()[Keys.LAST_RECORD_ID]
        if (lastRecordId != null) {
            healthConnectManager.deleteRecord(lastRecordId)
        }
        context.dataStore.edit { prefs -> prefs.remove(Keys.LAST_RECORD_ID) }
    }

    suspend fun undoLastGlass(healthConnectManager: HealthConnectManager) {
        if (undoLastGlassLocal()) {
            syncUndoToHealthConnect(healthConnectManager)
        }
    }

    companion object {
        const val DEFAULT_GOAL_ML = 2000
        const val DEFAULT_GLASS_ML = 250
    }
}
