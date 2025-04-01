package com.example.consumohoy.datosprecios

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val datos by viewModel.datos.collectAsState()
    val error by viewModel.error.collectAsState()

    var connectionTimedOut by remember { mutableStateOf(false) }
    var retryConnection by remember { mutableStateOf(false) }

    val now = LocalTime.now()
    val fechaBase = if (now.hour >= 20) LocalDate.now().plusDays(1) else LocalDate.now()

    fun calcularPrecioConImpuestos(valorOriginal: Double): Double {
        val peaje = 0.05 // por ejemplo, 0.05 €/kWh = 50 €/MWh
        val iva = 0.010   // IVA reducido del 10%
        val baseMasPeaje = valorOriginal + (peaje * 1000) // convertimos peaje a €/MWh
        return baseMasPeaje * (1 + iva)
    }

    val context = LocalContext.current
    // Cargar precios al inicio o cuando se reintente
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

    val formatterAPI = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
    val formatterDate = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    val formatterHour = SimpleDateFormat("HH:mm", Locale.US)

    LaunchedEffect(datos, retryConnection) {
        if (datos == null) {
            delay(5000) // Espera 20 segundos
            if (datos == null) {
                connectionTimedOut = true
            }
        }
    }
    Column(modifier = Modifier.padding(16.dp)) {
        when {
            error != null -> {
                Text("Error: $error")
            }

            //Error al cargar, conexion perdida
            datos == null -> {
                if (connectionTimedOut) {
                    Column {
                        Text(
                            text = stringResource(R.string.connection_timed_out),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Button(onClick = {
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
                val titlePrincipal = datos!!.data.attributes.title
                Text(titlePrincipal, style = MaterialTheme.typography.titleLarge)

                // Obtener todos los tipos de tarifas disponibles
                val allTypes = mutableSetOf<String>()
                datos?.included?.let { includedList ->
                    for (item in includedList) {
                        allTypes.add(item.type)
                    }
                }

                var selectedType by remember { mutableStateOf(allTypes.firstOrNull() ?: "") }
                var expanded by remember { mutableStateOf(false) }

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

                // Menu elegir tipo de mercado
                //Abre y cierra menu desplegable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        //Filtro de ordenar
                        ExposedDropdownMenuBox(
                            expanded = ordenExpanded,
                            onExpandedChange = { ordenExpanded = !ordenExpanded }
                        ) {
                            TextField(
                                modifier = Modifier
                                    .menuAnchor()
                                    .width(180.dp),
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

                        // Filtro de tipo de mercado
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            TextField(
                                modifier = Modifier
                                    .menuAnchor()
                                    .width(180.dp),
                                readOnly = true,
                                value = when (selectedType) {
                                    "precio-mercado" -> "Mercado SPOT"
                                    "pvpc" -> "Tarifa PVPC"
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
                val selectedPricing = datos!!.included.orEmpty().find { it.type == selectedType }

                fun extraerHora(datetime: String?): Int {
                    return datetime
                        ?.substringAfter("T")
                        ?.take(2) // más seguro que substring(0, 2)
                        ?.toIntOrNull() ?: -1
                }

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

                //Filtro de ordenes
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
                Log.d("ScreenPriceData", "------ Comparativa con impuestos ($selectedType) ------")
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
                    val formattedDateTime = formatterDate.format(parsedDateTime)
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
