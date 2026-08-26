package com.nandomclaren.watery.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.nandomclaren.watery.data.HealthConnectManager
import com.nandomclaren.watery.data.WaterPreferencesRepository

class AddGlassAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            val repository = WaterPreferencesRepository(context)
            val healthConnectManager = HealthConnectManager(context)
            repository.addGlass(healthConnectManager)
            WaterWidget().updateAll(context)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao registrar copo: ${e::class.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

class UndoGlassAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            val repository = WaterPreferencesRepository(context)
            val healthConnectManager = HealthConnectManager(context)
            repository.undoLastGlass(healthConnectManager)
            WaterWidget().updateAll(context)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao desfazer: ${e::class.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
