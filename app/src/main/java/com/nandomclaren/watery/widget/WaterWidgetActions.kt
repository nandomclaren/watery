package com.nandomclaren.watery.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.nandomclaren.watery.data.HealthConnectManager
import com.nandomclaren.watery.data.WaterPreferencesRepository
import kotlinx.coroutines.flow.first

class AddGlassAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            val repository = WaterPreferencesRepository(context)
            // Update the counter and repaint the widget immediately - a slow or
            // failing Health Connect call must never delay what the tap is for.
            val glassMl = repository.addGlassLocal()
            val newTotal = repository.state.first().drunkMl
            Toast.makeText(context, "DEBUG: contador local agora = ${newTotal}ml, chamando updateAll...", Toast.LENGTH_SHORT).show()
            WaterWidget().updateAll(context)
            Toast.makeText(context, "DEBUG: updateAll terminou", Toast.LENGTH_SHORT).show()
            val healthConnectManager = HealthConnectManager(context)
            repository.syncLastGlassToHealthConnect(healthConnectManager, glassMl)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao registrar copo: ${e::class.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

class UndoGlassAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            val repository = WaterPreferencesRepository(context)
            val didUndo = repository.undoLastGlassLocal()
            val newTotal = repository.state.first().drunkMl
            Toast.makeText(context, "DEBUG: desfeito=$didUndo, contador local agora = ${newTotal}ml", Toast.LENGTH_SHORT).show()
            WaterWidget().updateAll(context)
            Toast.makeText(context, "DEBUG: updateAll terminou", Toast.LENGTH_SHORT).show()
            if (didUndo) {
                val healthConnectManager = HealthConnectManager(context)
                repository.syncUndoToHealthConnect(healthConnectManager)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao desfazer: ${e::class.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
