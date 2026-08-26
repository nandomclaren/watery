package com.nandomclaren.watery.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.nandomclaren.watery.data.HealthConnectManager
import com.nandomclaren.watery.data.WaterPreferencesRepository

class AddGlassAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repository = WaterPreferencesRepository(context)
        val healthConnectManager = HealthConnectManager(context)
        repository.addGlass(healthConnectManager)
        WaterWidget().updateAll(context)
    }
}

class UndoGlassAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repository = WaterPreferencesRepository(context)
        val healthConnectManager = HealthConnectManager(context)
        repository.undoLastGlass(healthConnectManager)
        WaterWidget().updateAll(context)
    }
}
