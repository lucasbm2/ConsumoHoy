package com.example.consumohoy.datosprecios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.consumohoy.R
import com.example.consumohoy.conexion.DatosPreciosViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenPriceData(viewModel: DatosPreciosViewModel = viewModel()) {
    val datos by viewModel.datos.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getPrices("2024-03-22T00:00", "2024-03-22T23:59", "hour")
    }

    val formatterAPI = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
    val formatterDate = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    val formatterHour = SimpleDateFormat("HH:mm", Locale.US)
    var connectionTimedOut by remember { mutableStateOf(false) }
    var retryConnection by remember { mutableStateOf(false) }

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
                            viewModel.getPrices("2024-03-22T00:00", "2024-03-22T23:59", "hour")
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

                // Menu elegir tipo de mercado
                //Abre y cierra menu desplegable
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        value = selectedType,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.pricing_type_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    //Campo visual para activar menu desplegable al pulsarlo
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allTypes.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo) },
                                onClick = {
                                    selectedType = tipo
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filtrar los valores del tipo de mercado seleccionado
                val selectedPricing = datos!!.included.orEmpty().find { it.type == selectedType }

                // Ordenar los valores por fecha
                val ordenatedValues = selectedPricing?.attributes?.values?.sortedBy {
                    //regex para eliminar segundos de la fecha
                    val cleaned = it.datetime?.replace(Regex(":(\\d{2})$"), "$1")
                    // parsea la fecha de cada valor
                    val date = cleaned?.let { date -> formatterAPI.parse(date) }
                    date
                } ?: emptyList()

                // Mostrar fecha del primer valor
                ordenatedValues.firstOrNull()?.datetime?.let { unformattedDateTime ->
                    //regex para eliminar segundos de la fecha.-
                    val cleanedDateTime = unformattedDateTime.replace(Regex(":(\\d{2})$"), "$1")
                    val parsedDateTime = formatterAPI.parse(cleanedDateTime)
                    val formattedDateTime = formatterDate.format(parsedDateTime)
                    Text(
                        "Fecha: $formattedDateTime",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                LazyColumn {
                    items(ordenatedValues) { value ->
                        val price = value.value / 1000.0 //precio en KWh
                        val hour = value.datetime.let {
                            //regex para eliminar segundos de la hora
                            val timeWithoutSeconds = it.replace(Regex(":(\\d{2})$"), "$1")
                            formatterAPI.parse(timeWithoutSeconds)
                                ?.let { formattedTime -> formatterHour.format(formattedTime) }
                        } ?: R.string.hour_load_error

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(MaterialTheme.colorScheme.surface)
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
                            }
                        }
                    }
                }
            }
        }
    }
}
