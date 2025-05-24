package com.proyecto.consumohoy.optimization

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizationScreen(
    // Recibe el ViewModel como parámetro. NavigationWrapper se encargará de crearlo y pasarlo.
    viewModel: OptimizationViewModel = viewModel()
) {
    val consumos by viewModel.consumptionList.collectAsState()

    // --- Observa la lista de precios HORARIOS desde el ViewModel ---
    val hourlyPrices by viewModel.hourlyPrices.collectAsState() // <-- Observa la lista horaria COMPLETA

    // --- Estado para controlar si la sección de precios horarios está desplegada ---
    var pricesSectionExpanded by remember { mutableStateOf(false) }

    // --- Obtener los precios actuales/recentes para el cálculo rápido en las tarjetas de consumo ---
    val latestPrices by viewModel.latestPrices.collectAsState()
    val currentSpotPrice = latestPrices["SPOT"] ?: 0f
    val currentPvpcPrice = latestPrices["PVPC"] ?: 0f

    // Formateador para mostrar solo la hora
    val hourFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Optimizar Consumo") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {

            // --- Sección Desplegable para los Precios Horarios ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pricesSectionExpanded = !pricesSectionExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tipoTarifa by viewModel.tarifaFuente.collectAsState()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Precios horarios del día",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (tipoTarifa.isNotBlank()) {
                                Text(
                                    text = "($tipoTarifa)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Icon(
                            imageVector = if (pricesSectionExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = if (pricesSectionExpanded) "Contraer precios" else "Desplegar precios"
                        )
                    }

                    AnimatedVisibility(visible = pricesSectionExpanded) {
                        if (hourlyPrices.isEmpty()) {
                            Text("Cargando precios horarios o no disponibles...", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 250.dp)
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(hourlyPrices) { priceEntry ->
                                    val datetime = priceEntry.datetime
                                    val timeString = try {
                                        val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
                                        val date = apiFormat.parse(datetime.replace("Z", "+0000"))
                                        hourFormatter.format(date)
                                    } catch (e: Exception) {
                                        datetime
                                    }
                                    val pricePerKwh = priceEntry.value / 1000f

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(timeString, style = MaterialTheme.typography.bodySmall)
                                        Text("%.3f €/kWh".format(pricePerKwh), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Sección para mostrar los Consumos Guardados ---
            Text(
                text = "Tus aparatos guardados:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (consumos.isEmpty()) {
                    item {
                        Text("No has añadido ningún aparato aún. Ve a 'Aparatos' para añadirlos.")
                    }
                } else {
                    items(consumos) { consumo ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Nombre: ${consumo.name}", style = MaterialTheme.typography.bodyMedium)
                                Text("Potencia: ${consumo.power} W", style = MaterialTheme.typography.bodyMedium)
                                Text("Prioridad: ${consumo.priority}", style = MaterialTheme.typography.bodyMedium)
                                Text("Uso: ${consumo.usageMinutes} minutos", style = MaterialTheme.typography.bodyMedium)
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val estrategias = listOf(
                                        "Hora más barata",
                                        "Hora más cercana",
                                        "Top 3 horas más baratas",
                                        "Evitar horas punta (18:00–21:00)",
                                        "2 horas baratas consecutivas",
                                        "3 horas baratas consecutivas",
                                        "5 horas baratas consecutivas"
                                    )
                                    var estrategiaSeleccionada by remember { mutableStateOf(estrategias.first()) }
                                    var expanded by remember { mutableStateOf(false) }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Estrategia de optimización:", style = MaterialTheme.typography.bodySmall)

                                    ExposedDropdownMenuBox(
                                        expanded = expanded,
                                        onExpandedChange = { expanded = !expanded }
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = estrategiaSeleccionada,
                                            onValueChange = {},
                                            label = { Text("Selecciona estrategia") },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                        )

                                        ExposedDropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            estrategias.forEach { opcion ->
                                                DropdownMenuItem(
                                                    text = { Text(opcion) },
                                                    onClick = {
                                                        estrategiaSeleccionada = opcion
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    val energiaKwh = consumo.power.toFloat() ?: 0f  // ya está en kWh
                                    val mejorHora = if (energiaKwh > 0f) viewModel.calcularHoraOptimizada(
                                        estrategiaSeleccionada,
                                        consumo.priority,
                                        energiaKwh,
                                        consumo.usageMinutes
                                    ) else null



                                    if (mejorHora != null && mejorHora.isNotEmpty()) {
                                        val costeTotal: Float = mejorHora.sumOf { it.second.toDouble() }.toFloat()

                                        val horaMasCara = hourlyPrices.maxByOrNull { it.value }
                                        val precioMasCaro = horaMasCara?.value ?: 0f
                                        val consumoTotalMensual = consumo.power / 1000f * (consumo.usageMinutes / 60f) * 7 * 4 // kWh en 1 mes

                                        val costeMensualOptimo = costeTotal * 7 * 4
                                        val costeMensualCaro = (precioMasCaro / 1000f) * consumoTotalMensual

                                        val ahorroEstimado = costeMensualCaro - costeMensualOptimo


                                        Column {
                                            mejorHora.forEach { (hora, _) ->
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "\uD83D\uDD5B Hora sugerida: $hora",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "\uD83D\uDCB6 Coste estimado total: %.3f €".format(costeTotal),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF388E3C),
                                                fontWeight = FontWeight.Medium
                                            )

                                            if (ahorroEstimado > 0.01f) {
                                                Text(
                                                    text = "■ Si pusieras tu ${consumo.name} 7 días a la semana durante 1 mes en hora correcta,\nahorrarías aproximadamente %.2f € respecto a usarla en la hora más cara.".format(ahorroEstimado),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF00796B),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                    } else {
                                        Text("Introduce un consumo válido o espera precios.", style = MaterialTheme.typography.bodyMedium)
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