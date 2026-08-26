package com.nandomclaren.watery.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.nandomclaren.watery.data.HealthConnectManager
import com.nandomclaren.watery.data.WaterPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private suspend fun showToast(context: Context, message: String) {
    withContext(Dispatchers.Main) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

class AddGlassAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            val repository = WaterPreferencesRepository(context)
            // Update the counter and repaint the widget immediately - a slow or
            // failing Health Connect call must never delay what the tap is for.
            val glassMl = repository.addGlassLocal()
            WaterWidget().updateAll(context)
            val healthConnectManager = HealthConnectManager(context)
            repository.syncLastGlassToHealthConnect(healthConnectManager, glassMl)
        } catch (e: Exception) {
            showToast(context, "Erro ao registrar copo: ${e::class.simpleName}: ${e.message}")
        }
    }
}

class UndoGlassAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            val repository = WaterPreferencesRepository(context)
            val didUndo = repository.undoLastGlassLocal()
            WaterWidget().updateAll(context)
            if (didUndo) {
                val healthConnectManager = HealthConnectManager(context)
                repository.syncUndoToHealthConnect(healthConnectManager)
            }
        } catch (e: Exception) {
            showToast(context, "Erro ao desfazer: ${e::class.simpleName}: ${e.message}")
        }
    }
}
