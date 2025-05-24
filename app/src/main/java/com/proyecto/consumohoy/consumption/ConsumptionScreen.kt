package com.proyecto.consumohoy.consumption

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.consumohoy.R
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
    var ayudaVisible by remember { mutableStateOf(false) }
    var ayudaGeneralVisible by remember { mutableStateOf(false) }


    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.fondo_bombilla),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCCFFFFFF))
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .background(color = Color(0xFF0D47A1), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Registro de aparatos",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
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

                // Botón para mostrar/ocultar ayuda
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { ayudaVisible = !ayudaVisible }) {
                            Text(
                                text = if (ayudaVisible) "Ocultar explicación" else "¿Qué significa cada prioridad?",
                                fontSize = 14.sp,
                                color = Color(0xFF0D47A1),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(onClick = { ayudaGeneralVisible = !ayudaGeneralVisible }) {
                            Text(
                                text = if (ayudaGeneralVisible) "Ocultar ayuda" else "¿Cómo funciona esta pantalla?",
                                fontSize = 14.sp,
                                color = Color(0xFF0D47A1),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }


                item {
                    AnimatedVisibility(visible = ayudaGeneralVisible) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                " Explicación de campos",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "• Nombre del dispositivo: identifica el aparato (por ejemplo, Lavadora, Aire acondicionado...).",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "• Potencia (W): consumo eléctrico del aparato en vatios (W). Puedes encontrarlo en la etiqueta del fabricante.",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "• Minutos de uso: cuánto tiempo se utiliza el aparato al día, en minutos.",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "• Prioridad de uso: importancia que tiene usar ese aparato. Puede ser Alta, Media o Baja según lo urgente que sea su uso diario.",
                                fontSize = 14.sp
                            )
                        }
                    }
                    }

                // Explicación de las prioridades
                item {
                    AnimatedVisibility(visible = ayudaVisible) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text("• Alta: Uso imprescindible o en horarios con luz más barata.", fontSize = 14.sp)
                            Text("• Media: Uso recomendable, pero no urgente.", fontSize = 14.sp)
                            Text("• Baja: Puede posponerse sin problema.", fontSize = 14.sp)
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF90CAF9),
                            contentColor = Color.White
                        )
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
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
}
