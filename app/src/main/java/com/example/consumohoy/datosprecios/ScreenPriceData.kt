package com.example.consumohoy.datosprecios

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.consumohoy.R
import com.example.consumohoy.conexion.DatosPreciosViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenPriceData(viewModel: DatosPreciosViewModel = viewModel()) {
    //Actualizar datos automaticamente al recibir datos de la API
    val datos by viewModel.datos.collectAsState()

    //Si no hay respuesta, intentar de nuevo recibir los datos
    var connectionTimedOut by remember { mutableStateOf(false) }
    var retryConnection by remember { mutableStateOf(false) }

    //Para que si es despues de las 20, se muestre el precio de mañana
    val now = LocalTime.now()
    val fechaBase = if (now.hour >= 20) LocalDate.now().plusDays(1) else LocalDate.now()

    //Funcion para calcular el precio con impuestos
    fun calcularPrecioConImpuestos(valorOriginal: Float): Double {
        val peaje = 0.05
        val iva = 0.010
        val baseMasPeaje = valorOriginal + (peaje * 1000)
        return baseMasPeaje * (1 + iva)
    }

    //Context para acceder a SharedPreferences para guardar precios
    val context = LocalContext.current

    //Recibir datos de la API y guardarlos en datos
    LaunchedEffect(retryConnection) {
        connectionTimedOut = false
        val fechaStr = fechaBase.toString()
        Log.d("ScreenPriceData", "Pidiendo datos de fecha: $fechaStr")
        viewModel.getPrices(
            context,
            "${fechaStr}T00:00",
            "${fechaStr}T23:59",
            "hour"
        )
    }

    //Formato de fecha para la API
    val formatterAPI = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
    val formatterDate = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    val formatterHour = SimpleDateFormat("HH:mm", Locale.US)

    //Si no hay datos, intentar de nuevo despues de 5 segundos
    LaunchedEffect(datos, retryConnection) {
        if (datos == null) {
            delay(5000)
            if (datos == null) {
                connectionTimedOut = true
            }
        }
    }

    //Mostrar datos de la API en pantalla
    Column(modifier = Modifier.padding(16.dp)) {
        when {
            datos == null -> {
                if (connectionTimedOut) {
                    Column {
                        Text(stringResource(R.string.connection_timed_out), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            connectionTimedOut = false
                            retryConnection = !retryConnection
                            val fechaStr = fechaBase.toString()
                            viewModel.getPrices(context, "${fechaStr}T00:00", "${fechaStr}T23:59", "hour")
                        }) {
                            Text(text = stringResource(R.string.retry))
                        }
                    }
                } else {
                    Text(text = stringResource(R.string.loading_data))
                }
            }
            else -> {
                val titlePrincipal = datos!!.data.attributes.title
                Text(titlePrincipal, style = MaterialTheme.typography.titleLarge)

                val includedMutable = datos!!.included?.toMutableList() ?: mutableListOf()

                val ahora = LocalTime.now()

                // Filtramos el tipo 'pvpc-estimado' si aún no es después de las 20:00
                val tiposFiltrados = includedMutable.filterNot {
                    it.type == "pvpc-estimado" && ahora.isBefore(LocalTime.of(20, 0))
                }

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
                    val yaIncluido = includedMutable.any { it.type == "pvpc-estimado" }
                    if (!yaIncluido) {
                        includedMutable.add(estimado)
                        Log.d("ScreenPriceData", "🟢 PVPC estimado generado desde SPOT")
                    }
                } else {
                    Log.e("ScreenPriceData", "❌ No se puede crear PVPC estimado porque spot o lastUpdate es null")
                }


                Log.d("ScreenPriceData", "🔍 Tipo spot detectado: ${spot?.type}")
                Log.d("ScreenPriceData", "🔍 Título spot: ${spot?.attributes?.title}")
                Log.d("ScreenPriceData", "🔍 Valores spot: ${spot?.attributes?.values?.size} items")


                //Si hay pvpc en SharedPreferences, cargarlo en included para mostrar en pantalla
                val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)
                val pvpcEstimadoJson = prefs.getString("pvpc_estimado_json", null)
                if (pvpcEstimadoJson != null) {
                    val gson = com.google.gson.Gson()
                    val pvpcEstimado = gson.fromJson(pvpcEstimadoJson, com.example.consumohoy.entities.Included::class.java)
                    val yaIncluido = includedMutable.any { it.type == "pvpc-estimado" }
                    if (!yaIncluido) {
                        includedMutable.add(pvpcEstimado)
                        Log.d("ScreenPriceData", "🟡 PVPC estimado cargado desde SharedPreferences")
                    }
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = ordenExpanded,
                            onExpandedChange = { ordenExpanded = !ordenExpanded }
                        ) {
                            TextField(
                                modifier = Modifier.menuAnchor().width(180.dp),
                                readOnly = true,
                                value = selectedOrden,
                                onValueChange = {},
                                label = { Text("Ordenar por") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ordenExpanded) },
                                colors = ExposedDropdownMenuDefaults.textFieldColors()
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
                                modifier = Modifier.menuAnchor().width(180.dp),
                                readOnly = true,
                                value = when (selectedType) {
                                    "precio-mercado" -> "Mercado SPOT"
                                    "pvpc" -> "Tarifa PVPC"
                                    "pvpc-estimado" -> "Tarifa PVPC (estimado)"
                                    else -> selectedType
                                },
                                onValueChange = {},
                                label = { Text(stringResource(R.string.pricing_type_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.textFieldColors()
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

                    else -> selectedPricing?.attributes?.values
                        ?.sortedBy {
                            val date = it.datetime?.replace(Regex(":(\\d{2})$"), "$1")?.let { dateStr -> formatterAPI.parse(dateStr) }
                            date
                        }
                } ?: emptyList()
                Log.d("ScreenPriceData", "------ Comparado con impuestos ($selectedType) ------")
                ordenatedValues.forEach {
                    val valorOriginal = it.value
                    val valorFinal = calcularPrecioConImpuestos(valorOriginal)
                    Log.d("ScreenPriceData", "Hora: ${it.datetime} - Original: ${"%.2f".format(valorOriginal)} €/MWh - Con impuestos: ${"%.2f".format(valorFinal)} €/MWh")
                }



                // Mostrar fecha del primer valor
                ordenatedValues.firstOrNull()?.datetime?.let { unformattedDateTime ->
                    //regex para eliminar segundos de la fecha.-
                    val cleanedDateTime = unformattedDateTime.replace(Regex(":(\\d{2})$"), "$1")
                    val parsedDateTime = formatterAPI.parse(cleanedDateTime)
                    formatterDate.format(parsedDateTime)
                    Text(
                        "Fecha: $fechaBase",
                        style = MaterialTheme.typography.headlineMedium
                    )
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
