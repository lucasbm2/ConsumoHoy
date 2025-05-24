package com.proyecto.consumohoy.datosprecios

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.consumohoy.R
import com.proyecto.consumohoy.conexion.DatosPreciosViewModel
import com.proyecto.consumohoy.entities.Value
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import androidx.compose.material3.TextFieldDefaults




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenPriceData(viewModel: DatosPreciosViewModel = viewModel()) {
    val datos by viewModel.datos.collectAsState()
    var connectionTimedOut by remember { mutableStateOf(false) }
    var retryConnection by remember { mutableStateOf(false) }

    val now = LocalTime.now()
    val fechaBase = if (now.hour >= 20) LocalDate.now().plusDays(1) else LocalDate.now()

    fun calcularPrecioConImpuestos(valorOriginal: Float): Double {
        val peaje = 0.05
        val iva = 0.010
        val baseMasPeaje = valorOriginal + (peaje * 1000)
        return baseMasPeaje * (1 + iva)
    }

    val context = LocalContext.current

    LaunchedEffect(retryConnection) {
        connectionTimedOut = false
        val fechaStr = fechaBase.toString()
        viewModel.getPrices(
            context,
            "${fechaStr}T00:00",
            "${fechaStr}T23:59",
            "hour"
        )
    }

    val formatterAPI = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
    val formatterDate = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    val formatterHour = SimpleDateFormat("HH:mm", Locale.US)

    LaunchedEffect(datos, retryConnection) {
        if (datos == null) {
            delay(5000)
            if (datos == null) {
                connectionTimedOut = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.fondo_bombilla),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCCFFFFFF))
        )

    }
    //Mostrar datos de la API en pantalla
    Column(modifier = Modifier.padding(16.dp)) {
        when {
            datos == null -> {
                if (connectionTimedOut) {
                    Column {
                        Text(
                            stringResource(R.string.connection_timed_out),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            connectionTimedOut = false
                            retryConnection = !retryConnection
                            val fechaStr = fechaBase.toString()
                            viewModel.getPrices(
                                context,
                                "${fechaStr}T00:00",
                                "${fechaStr}T23:59",
                                "hour"
                            )
                        }) {
                            Text(text = stringResource(R.string.retry))
                        }
                    }
                } else {
                    Text(text = stringResource(R.string.loading_data))
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D47A1), shape = RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = datos!!.data.attributes.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = fuenteEjecutiva
                    )
                }


                val includedMutable = datos!!.included?.toMutableList() ?: mutableListOf()

                val ahora = LocalTime.now()


                val spot = includedMutable.find { it.type.contains("mercado", ignoreCase = true) }

                //Si spot no es nulo, crear pvpc estimado para mostrar precios
                //estimados de PVPC para el dia siguiente, ya que PVPC no se actualiza
                //hasta pasadas las 00:00
                if (spot != null) {
                    val lastUpdateSafe = spot.attributes.lastUpdate ?: "1970-01-01T00:00:00Z"
                    val promedio = 0.06404f
                    val estimado = spot.copy(
                        id = "pvpc-estimado-from-spot",
                        type = "pvpc-estimado",
                        attributes = spot.attributes.copy(
                            title = "Tarifa PVPC (estimado)",
                            values = spot.attributes.values.map { valor ->
                                valor.copy(value = (valor.value + (promedio * 1000)))
                            },
                            lastUpdate = lastUpdateSafe
                        )
                    )

// ⬇️ Añade esto inmediatamente DESPUÉS del val estimado
                    val gson = com.google.gson.Gson()
                    val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)
                    val json = gson.toJson(estimado)
                    prefs.edit().putString("pvpc_estimado_json", json).apply()
                    Log.d("ScreenPriceData", "🟢 PVPC estimado guardado en SharedPreferences")

                    val yaIncluido = includedMutable.any { it.type == "pvpc-estimado" }
                    if (!yaIncluido) {
                        includedMutable.add(estimado)
                    }
                }


// Filtramos el tipo 'pvpc-estimado' si hay pvpc real ya
                val hayPvpcReal = includedMutable.any { inc ->
                    inc.type.equals("pvpc", ignoreCase = true) && inc.attributes.values.isNotEmpty()
                }
                Log.d("DEBUG", "🎯 Hay PVPC real: $hayPvpcReal")

                val tiposFiltrados = includedMutable.filterNot {
                    it.type == "pvpc-estimado" && hayPvpcReal
                }


                //Obtener todos los tipos de precios disponibles
                val allTypes = tiposFiltrados.map { it.type }.toSet()
                tiposFiltrados.forEach {
                    Log.d("DEBUG", "Tipo incluido: ${it.type}")
                }

                //Filtrar si hay pvpc y pvpc-estimado
                val hasPvpc = allTypes.contains("pvpc")
                val hasPvpcEstimado = allTypes.contains("pvpc-estimado")

                //Esto para que se muestre el correcto en funcion de si hay pvpc o no
                val defaultType = when {
                    hasPvpcEstimado -> "pvpc-estimado"
                    hasPvpc -> "pvpc"
                    else -> allTypes.firstOrNull() ?: ""
                }

                //Si hay pvpc en SharedPreferences, cargarlo en included para mostrar en pantalla
                val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)
                val pvpcEstimadoJson = prefs.getString("pvpc_estimado_json", null)
                if (pvpcEstimadoJson != null) {
                    val gson = com.google.gson.Gson()
                    val pvpcEstimado = gson.fromJson(
                        pvpcEstimadoJson,
                        com.proyecto.consumohoy.entities.Included::class.java
                    )
                    val yaIncluido = includedMutable.any { it.type == "pvpc-estimado" }
                    if (!yaIncluido) {
                        includedMutable.add(pvpcEstimado)
                        Log.d("ScreenPriceData", "🟡 PVPC estimado cargado desde SharedPreferences")
                    }
                }


                var selectedType by remember { mutableStateOf(defaultType) }
                var expanded by remember { mutableStateOf(false) }

                //Seleccionar un filtro por el que mostrar los datos
                val ordenOptions = listOf(
                    stringResource(R.string.orden_hora_cronologica),
                    stringResource(R.string.orden_hora_mas_barata),
                    stringResource(R.string.orden_hora_mas_cara),
                    stringResource(R.string.orden_solo_valle),
                    stringResource(R.string.orden_solo_llano),
                    stringResource(R.string.orden_solo_punta)
                )

                var selectedOrden by remember { mutableStateOf(ordenOptions[0]) }
                var ordenExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = ordenExpanded,
                        onExpandedChange = { ordenExpanded = !ordenExpanded }
                    ) {
                        TextField(
                            value = selectedOrden,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .width(200.dp)
                                .height(65.dp),
                            label = { Text("Ordenar por", fontSize = 14.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ordenExpanded) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFE3F2FD),
                                unfocusedContainerColor = Color(0xFFE3F2FD),
                                disabledContainerColor = Color(0xFFE3F2FD),
                                cursorColor = Color(0xFF0D47A1),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedLabelColor = Color(0xFF0D47A1),
                                unfocusedLabelColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )
                        ExposedDropdownMenu(
                            expanded = ordenExpanded,
                            onDismissRequest = { ordenExpanded = false }
                        ) {
                            ordenOptions.forEach { orden ->
                                DropdownMenuItem(
                                    text = { Text(orden) },
                                    onClick = {
                                        selectedOrden = orden
                                        ordenExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = when (selectedType) {
                                "precio-mercado" -> "Mercado SPOT"
                                "pvpc" -> "Tarifa PVPC"
                                "pvpc-estimado" -> "Tarifa PVPC (estimado)"
                                else -> selectedType
                            },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .width(200.dp)
                                .height(65.dp),
                            label = { Text("Tipo de mercado", fontSize = 12.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFE3F2FD),
                                unfocusedContainerColor = Color(0xFFE3F2FD),
                                disabledContainerColor = Color(0xFFE3F2FD),
                                cursorColor = Color(0xFF0D47A1),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedLabelColor = Color(0xFF0D47A1),
                                unfocusedLabelColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            allTypes.forEach { tipo ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (tipo) {
                                                "precio-mercado" -> "Mercado SPOT"
                                                "pvpc" -> "Tarifa PVPC"
                                                "pvpc-estimado" -> "Tarifa PVPC (estimado)"
                                                else -> tipo
                                            }
                                        )
                                    },
                                    onClick = {
                                        selectedType = tipo
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }



                Spacer(modifier = Modifier.height(16.dp))

                // Filtrar los valores del tipo de mercado seleccionado
                val selectedPricing = tiposFiltrados.find { it.type == selectedType }


                //Para saber el tramo de la hora desde la fecha
                fun obtenerTramoDesdeFecha(datetime: String?): String {
                    if (datetime == null) return "otro"

                    val clean = datetime.replace(Regex(":(\\d{2})$"), "$1")
                    val date = try {
                        formatterAPI.parse(clean)
                    } catch (e: Exception) {
                        return "otro"
                    }

                    val cal = java.util.Calendar.getInstance().apply { time = date }
                    val hora = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    val minuto = cal.get(java.util.Calendar.MINUTE)

                    val totalMinutos = hora * 60 + minuto

                    return when (totalMinutos) {
                        in 0..479 -> "valle" // 00:00–07:59
                        in 480..599, in 840..1079, in 1320..1439 -> "llano" // 08:00–09:59, 14:00–17:59, 22:00–23:59
                        in 600..839, in 1080..1319 -> "punta" // 10:00–13:59, 18:00–21:59
                        else -> "otro"
                    }
                }

                //Filtro de ordenar por parametros
                val ordenatedValues = when (selectedOrden) {
                    stringResource(R.string.orden_hora_mas_barata) -> selectedPricing?.attributes?.values
                        ?.sortedBy { it.value }

                    stringResource(R.string.orden_hora_mas_cara) -> selectedPricing?.attributes?.values
                        ?.sortedByDescending { it.value }

                    stringResource(R.string.orden_solo_valle) -> selectedPricing?.attributes?.values
                        ?.filter { obtenerTramoDesdeFecha(it.datetime) == "valle" }
                        ?.sortedBy {
                            val date = it.datetime?.replace(Regex(":(\\d{2})$"), "$1")
                                ?.let { d -> formatterAPI.parse(d) }
                            date
                        }

                    stringResource(R.string.orden_solo_llano) -> selectedPricing?.attributes?.values
                        ?.filter { obtenerTramoDesdeFecha(it.datetime) == "llano" }
                        ?.sortedBy {
                            val date = it.datetime?.replace(Regex(":(\\d{2})$"), "$1")
                                ?.let { d -> formatterAPI.parse(d) }
                            date
                        }

                    stringResource(R.string.orden_solo_punta) -> selectedPricing?.attributes?.values
                        ?.filter { obtenerTramoDesdeFecha(it.datetime) == "punta" }
                        ?.sortedBy {
                            val date = it.datetime?.replace(Regex(":(\\d{2})$"), "$1")
                                ?.let { d -> formatterAPI.parse(d) }
                            date
                        }

                    else -> {
                        // Crear un mapa para acceder rápidamente a las horas existentes
                        val existingHoursMap = selectedPricing?.attributes?.values?.associateBy {
                            it.datetime?.substring(11, 13)
                        } ?: emptyMap()

                        fun createEmptyHourEntry(hour: String): Value {
                            return Value(
                                datetime = "${fechaBase}T${hour}:00:00.000Z",
                                value = 0.0f,
                                percentage = 0.0
                            )
                        }


                        // Verificar y agregar las horas faltantes
                        val valuesWithMissingHours =
                            selectedPricing?.attributes?.values?.toMutableList() ?: mutableListOf()
                        if (!existingHoursMap.containsKey("00")) {
                            valuesWithMissingHours.add(0, createEmptyHourEntry("00"))
                        }
                        if (!existingHoursMap.containsKey("01")) {
                            valuesWithMissingHours.add(1, createEmptyHourEntry("01"))
                        }

                        valuesWithMissingHours.sortedBy {
                            it.datetime?.substring(11, 13)?.toIntOrNull() ?: 24
                        }.also { sortedValues ->
                            Log.d(
                                "ScreenPriceData",
                                "Valores ordenados: ${sortedValues.joinToString { v -> v.datetime ?: "null" }}"
                            )

                            // Debug: Imprimir las horas después de la ordenación
                            val horas = sortedValues.map { v ->
                                v.datetime?.substring(11, 13)
                            }
                            Log.d("ScreenPriceData", "Horas después de ordenar: $horas")
                        }
                    }
                } ?: emptyList()
                Log.d("ScreenPriceData", "------ Comparado con impuestos ($selectedType) ------")
                ordenatedValues.forEach {
                    val valorOriginal = it.value
                    val valorFinal = calcularPrecioConImpuestos(valorOriginal)
                    Log.d(
                        "ScreenPriceData",
                        "Hora: ${it.datetime} - Original: ${"%.2f".format(valorOriginal)} €/MWh - Con impuestos: ${
                            "%.2f".format(valorFinal)
                        } €/MWh"
                    )
                }


                // Mostrar fecha del primer valor
                ordenatedValues.firstOrNull()?.datetime?.let { unformattedDateTime ->
                    //regex para eliminar segundos de la fecha.-
                    val cleanedDateTime = unformattedDateTime.replace(Regex(":(\\d{2})$"), "$1")
                    val parsedDateTime = formatterAPI.parse(cleanedDateTime)
                    formatterDate.format(parsedDateTime)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE3F2FD), shape = RoundedCornerShape(12.dp))
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Fecha: $fechaBase",
                            fontSize = 20.sp,
                            color = Color(0xFF0D47A1),
                            fontWeight = FontWeight.Medium
                        )
                    }


                }


                LazyColumn {
                    items(ordenatedValues) { value ->
                        val price = value.value / 1000.0 //precio en KWh


                        //regex para eliminar segundos de la hora
                        val date = value.datetime?.replace(Regex(":(\\d{2})$"), "$1")
                        val parsedTime = date?.let { formatterAPI.parse(it) }

                        // formatear hora visible
                        val hour =
                            parsedTime?.let { formatterHour.format(it) } ?: R.string.hour_load_error

                        // hora en formato entero
                        val hourInt = parsedTime?.hours ?: 0

                        // Colores personalizados por tramo
                        val colorValle = Color(0xFFD0F0C0) // verde
                        val colorLlano = Color(0xFFFFF9C4) // amarillo
                        val colorPunta = Color(0xFFFFCDD2) // rojo

                        // comprobar tramo
                        val (tramo, colorFondo) = when (hourInt) {
                            in 0..7 -> R.string.tramo_valle to colorValle
                            in 8..9, in 14..17, in 22..23 -> R.string.tramo_llano to colorLlano
                            in 10..13, in 18..21 -> R.string.tramo_punta to colorPunta
                            else -> R.string.tramo_otro to colorLlano
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colorFondo
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.pricing_type, selectedType),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = stringResource(R.string.price_hour_kw, price),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = stringResource(R.string.hour, hour),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(tramo),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}