package com.proyecto.consumohoy.consumption

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumptionScreen(
    viewModel: ConsumptionViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val consumos by viewModel.consumptionList.collectAsState()
    val expanded = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val priorityOptions = listOf("Alta", "Media", "Baja")
    var minutosUso by remember { mutableStateOf("60") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Página de Consumo") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) } // 🟣 Snackbar visible
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChange(it) },
                    label = { Text("Nombre del dispositivo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.power,
                    onValueChange = { viewModel.onPowerChange(it) },
                    label = { Text("Potencia (W)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.usageMinutes,
                    onValueChange = { viewModel.onUsageMinutesChange(it) },
                    label = { Text("Minutos de uso") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = expanded.value,
                    onExpandedChange = { expanded.value = !expanded.value }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = uiState.priority,
                        onValueChange = {},
                        label = { Text("Prioridad de uso") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false }
                    ) {
                        priorityOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.onPriorityChange(option)
                                    expanded.value = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (uiState.name.isBlank() || uiState.power.isBlank() || uiState.priority.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Por favor, completa todos los campos.")
                            }
                        } else {
                            viewModel.saveEntry()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Consumo guardado con éxito.")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar Consumo")
                }
            }


            if (consumos.isNotEmpty()) {
                item {
                    Text(
                        text = "Consumos guardados",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(consumos) { consumo ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Nombre: ${consumo.name}")
                            Text("Potencia: ${consumo.power} W")
                            Text("Prioridad: ${consumo.priority}")

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.editEntry(consumo) }) {
                                    Text("Editar")
                                }
                                Button(onClick = { viewModel.deleteEntry(consumo) }) {
                                    Text("Borrar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
