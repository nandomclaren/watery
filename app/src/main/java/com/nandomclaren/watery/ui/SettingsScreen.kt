package com.nandomclaren.watery.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.glance.appwidget.updateAll
import androidx.compose.material3.MaterialTheme
import com.nandomclaren.watery.data.HealthConnectManager
import com.nandomclaren.watery.data.WaterPreferencesRepository
import com.nandomclaren.watery.data.WaterUiState
import com.nandomclaren.watery.widget.WaterWidget
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember { WaterPreferencesRepository(context) }
    val healthConnectManager = remember { HealthConnectManager(context) }
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(WaterUiState()) }
    var hasHealthConnectPermissions by remember { mutableStateOf(false) }
    var goalText by remember { mutableStateOf("") }
    var glassText by remember { mutableStateOf("") }

    val requestPermissionsContract = remember { healthConnectManager.createPermissionRequestContract() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = requestPermissionsContract,
    ) { granted ->
        scope.launch {
            hasHealthConnectPermissions = healthConnectManager.hasAllPermissions()
            state = repository.syncAndGetState(healthConnectManager)
            WaterWidget().updateAll(context)
            if (granted.isEmpty()) {
                Toast.makeText(
                    context,
                    "Nenhuma permissão concedida. Abra o app Health Connect > Apps conectados > Watery para conceder manualmente.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    fun openHealthConnectPermissions() {
        try {
            permissionLauncher.launch(healthConnectManager.permissions)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Não foi possível abrir o Health Connect: ${e.message}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun openHealthConnectStore() {
        try {
            healthConnectManager.openHealthConnectInPlayStore()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Não foi possível abrir a Play Store: ${e.message}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    suspend fun refresh() {
        hasHealthConnectPermissions = healthConnectManager.hasAllPermissions()
        state = repository.syncAndGetState(healthConnectManager)
        goalText = state.goalMl.toString()
        glassText = state.glassMl.toString()
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Watery") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Card(colors = CardDefaults.cardColors()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hoje", style = MaterialTheme.typography.titleMedium)
                    Text("${state.drunkGlasses}/${state.goalGlasses} copos · ${state.drunkLiters}L de ${state.goalMl / 1000.0}L")
                    LinearProgressIndicator(
                        progress = { state.progressFraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {
                            scope.launch {
                                repository.addGlass(healthConnectManager)
                                refresh()
                                WaterWidget().updateAll(context)
                            }
                        }) { Text("+1 copo") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                repository.undoLastGlass(healthConnectManager)
                                refresh()
                                WaterWidget().updateAll(context)
                            }
                        }) { Text("Desfazer") }
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Meta e copo", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it.filter(Char::isDigit) },
                        label = { Text("Meta diária (mL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = glassText,
                        onValueChange = { glassText = it.filter(Char::isDigit) },
                        label = { Text("Tamanho do copo (mL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            val goal = goalText.toIntOrNull() ?: state.goalMl
                            val glass = glassText.toIntOrNull() ?: state.glassMl
                            scope.launch {
                                repository.updateSettings(goalMl = goal, glassMl = glass)
                                refresh()
                                WaterWidget().updateAll(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Salvar") }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Health Connect", style = MaterialTheme.typography.titleMedium)
                    when {
                        !healthConnectManager.isAvailable && !healthConnectManager.needsProviderUpdate -> {
                            Text("O Health Connect não está instalado neste aparelho.")
                            Button(onClick = { openHealthConnectStore() }) {
                                Text("Instalar Health Connect")
                            }
                        }
                        healthConnectManager.needsProviderUpdate -> {
                            Text("O Health Connect precisa ser atualizado.")
                            Button(onClick = { openHealthConnectStore() }) {
                                Text("Atualizar Health Connect")
                            }
                        }
                        hasHealthConnectPermissions -> {
                            Text("Conectado. A ingestão de água é lida e gravada no Health Connect.")
                        }
                        else -> {
                            Text("Conceda permissão para sincronizar sua ingestão de água com o Health Connect.")
                            Button(onClick = { openHealthConnectPermissions() }) {
                                Text("Conectar ao Health Connect")
                            }
                        }
                    }
                }
            }
        }
    }
}
