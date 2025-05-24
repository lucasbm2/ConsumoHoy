package com.proyecto.consumohoy.optimization

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.consumohoy.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizationScreen(viewModel: OptimizationViewModel = viewModel()) {
    val consumos by viewModel.consumptionList.collectAsState()
    val hourlyPrices by viewModel.hourlyPrices.collectAsState()
    val latestPrices by viewModel.latestPrices.collectAsState()
    val tipoTarifa by viewModel.tarifaFuente.collectAsState()

    val hourFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val cardColors = listOf(
        Color(0xFFE8F5E9), // verde claro
        Color(0xFFE3F2FD), // azul claro
        Color(0xFFFFF9C4), // amarillo claro / beige
        Color(0xFFF3E5F5)  // lila claro
    )

    var pricesSectionExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.fondo_bombilla),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCCFFFFFF)))

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0D47A1), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Optimizar Consumo",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Sección desplegable de precios
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pricesSectionExpanded = !pricesSectionExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Precios horarios del día ($tipoTarifa)", fontWeight = FontWeight.Medium)
                            Icon(
                                imageVector = if (pricesSectionExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(pricesSectionExpanded) {
                            if (hourlyPrices.isEmpty()) {
                                Text("Cargando precios...", fontSize = 14.sp)
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 250.dp)
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    itemsIndexed(hourlyPrices) { _, price ->
                                        val timeStr = try {
                                            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
                                            hourFormatter.format(fmt.parse(price.datetime.replace("Z", "+0000")))
                                        } catch (e: Exception) { price.datetime }

                                        val kwh = price.value / 1000f
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(timeStr, fontSize = 14.sp)
                                            Text("%.3f €/kWh".format(kwh), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Tus aparatos guardados:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (consumos.isEmpty()) {
                    Text("No has añadido ningún aparato aún. Ve a 'Registrar aparato' para añadirlos.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(consumos) { index, consumo ->
                            val backgroundColor = cardColors[index % cardColors.size]

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = backgroundColor)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Nombre: ${consumo.name}")
                                    Text("Potencia: ${consumo.power} W")
                                    Text("Prioridad: ${consumo.priority}")
                                    Text("Uso diario: ${consumo.usageMinutes} min")

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Estrategia de optimización:", fontSize = 14.sp)

                                    var expanded by remember { mutableStateOf(false) }
                                    val estrategias = listOf(
                                        "Hora más cercana/barata",
                                        "Evitar horas punta", "Hora más barata del día"
                                    )
                                    var seleccion by remember { mutableStateOf(estrategias.first()) }

                                    ExposedDropdownMenuBox(
                                        expanded = expanded,
                                        onExpandedChange = { expanded = !expanded }
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = seleccion,
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
                                                        seleccion = opcion
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    val energiaKwh = consumo.power

                                    val resultado = if (energiaKwh > 0f)
                                        viewModel.calcularHoraOptimizada(
                                            seleccion,
                                            consumo.priority,
                                            energiaKwh,
                                            consumo.usageMinutes
                                        )
                                    else null

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (resultado.isNullOrEmpty()) {
                                        Text("Introduce datos válidos o espera precios.")
                                    } else {
                                        val costeTotal = resultado.sumOf { it.second.toDouble() }.toFloat()
                                        val horaMasCara = hourlyPrices.maxByOrNull { it.value }
                                        val precioMasCaro = horaMasCara?.value ?: 0f

                                        val consumoTotalMensual = consumo.power / 1000f * (consumo.usageMinutes / 60f) * 7 * 4
                                        val costeMensualOptimo = costeTotal * 7 * 4
                                        val costeMensualCaro = (precioMasCaro / 1000f) * consumoTotalMensual
                                        val ahorroEstimado = costeMensualCaro - costeMensualOptimo
                                        val facturaMensualMedia = 60f
                                        val porcentajeAhorro = (ahorroEstimado / facturaMensualMedia) * 100

                                        resultado.forEach { (hora, precio) ->
                                            Text("🕒 Hora sugerida: $hora (%.1f cts/kWh)".format(precio * 100), fontWeight = FontWeight.SemiBold)
                                        }



                                        Text(
                                            text = "💶 Coste estimado: %.3f €".format(costeTotal),
                                            fontSize = 14.sp,
                                            color = Color(0xFF388E3C),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                        if (ahorroEstimado > 0.01f) {
                                            Surface(
                                                color = Color(0xFFDFF5E3),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                            ) {
                                                Text(
                                                    text = "Si pusieras tu ${consumo.name} 7 días a la semana durante 1 mes en hora correcta,\n" +
                                                            "ahorrarías aproximadamente %.2f € respecto a usarla en la hora más cara.".format(ahorroEstimado) +
                                                            "Eso representa un %.1f%% de una factura media mensual de 60 €.".format(ahorroEstimado, porcentajeAhorro),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(12.dp)
                                                )
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
    }
}
