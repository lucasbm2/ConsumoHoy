package com.proyecto.consumohoy.optimization

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.consumohoy.database.ConsumptionDao
import com.proyecto.consumohoy.database.ConsumptionEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.proyecto.consumohoy.conexion.ApiService
import com.proyecto.consumohoy.entities.Value
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class OptimizationViewModel(
    private val consumptionDao: ConsumptionDao,
    private val apiService: ApiService, // Para obtener precios horarios desde la API
    private val context: Context
) : ViewModel() {

    private val _consumptionList = MutableStateFlow<List<ConsumptionEntry>>(emptyList())
    val consumptionList: StateFlow<List<ConsumptionEntry>> = _consumptionList

    private val _latestPrices = MutableStateFlow<Map<String, Float>>(emptyMap())
    val latestPrices: StateFlow<Map<String, Float>> = _latestPrices

    // --- StateFlow para la lista COMPLETA de precios HORARIOS ---
    private val _hourlyPrices = MutableStateFlow<List<Value>>(emptyList())
    val hourlyPrices: StateFlow<List<Value>> = _hourlyPrices

    private val _hourlyPricesLoading = MutableStateFlow(false)
    val hourlyPricesLoading: StateFlow<Boolean> = _hourlyPricesLoading

    private val _tarifaFuente = MutableStateFlow<String>("")
    val tarifaFuente: StateFlow<String> = _tarifaFuente


    init {
        loadConsumptions()
        loadLatestPrices()
        loadHourlyPrices() // <--  Ahora llama a la función unificada
    }

    private fun loadConsumptions() {
        viewModelScope.launch {
            try {
                _consumptionList.value = consumptionDao.getAll()
                Log.d(
                    "OptimizationViewModel",
                    "Consumos cargados: ${_consumptionList.value.size} items"
                )
            } catch (e: Exception) {
                Log.e("OptimizationViewModel", "Error loading consumptions from DAO", e)
                _consumptionList.value = emptyList()
            }
        }
    }

    private fun loadLatestPrices() {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)
                val latestSpot = prefs.getFloat("spot", -1f)
                val latestPvpc = prefs.getFloat("pvpc", -1f)

                val pricesMap = mutableMapOf<String, Float>()
                if (latestSpot != -1f && latestSpot != 0f) {
                    pricesMap["SPOT"] = latestSpot / 1000f
                }
                if (latestPvpc != -1f && latestPvpc != 0f) {
                    pricesMap["PVPC"] = latestPvpc / 1000f
                }

                _latestPrices.value = pricesMap
                Log.d(
                    "OptimizationViewModel",
                    "Precios recientes cargados (SharedPreferences): $_latestPrices"
                )
            } catch (e: Exception) {
                Log.e(
                    "OptimizationViewModel",
                    "Error loading recent prices from SharedPreferences",
                    e
                )
            }
        }
    }

    private fun loadHourlyPrices() {
        viewModelScope.launch {
            _hourlyPricesLoading.value = true
            _hourlyPrices.value = emptyList()

            try {
                val fechaHoy = LocalDate.now()
                val fechaStr = fechaHoy.toString()

                val startDateTime = "${fechaStr}T00:00"
                val endDateTime = "${fechaStr}T23:59"

                val response = apiService.obtenerPrecios(
                    startDate = startDateTime,
                    endDate = endDateTime,
                    timeTrunc = "hour"
                )

                // Debug para ver los tipos reales
                response.included?.forEach {
                    Log.d("DEBUG_API", "🔍 ID: ${it.id}, TYPE: ${it.type}")
                }

                val pvpcEstimadoApi = response.included?.firstOrNull {
                    it.id.contains(
                        "pvpc-estimado",
                        ignoreCase = true
                    ) || it.type.contains("pvpc-estimado", ignoreCase = true)
                }

                val pvpcRealApi = response.included?.firstOrNull {
                    (it.id.contains("pvpc", ignoreCase = true) && !it.id.contains(
                        "pvpc-estimado",
                        ignoreCase = true
                    )) ||
                            (it.type.contains(
                                "pvpc",
                                ignoreCase = true
                            ) && !it.type.contains("pvpc-estimado", ignoreCase = true))
                }

                // Aquí se prioriza correctamente el orden deseado
                val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)
                val gson = com.google.gson.Gson()
                val estimadoGuardado = prefs.getString("pvpc_estimado_json", null)?.let {
                    gson.fromJson(it, com.proyecto.consumohoy.entities.Included::class.java)
                }

                val (hourlyValues, fuente) = when {
                    pvpcRealApi?.attributes?.values?.isNotEmpty() == true -> {
                        Log.d("OptimizationViewModel", "✅ Usando PVPC REAL desde API")
                        pvpcRealApi.attributes.values to "PVPC"
                    }

                    pvpcEstimadoApi?.attributes?.values?.isNotEmpty() == true -> {
                        Log.d("OptimizationViewModel", "🟡 Usando PVPC ESTIMADO desde API")
                        pvpcEstimadoApi.attributes.values to "PVPC-estimado (API)"
                    }

                    estimadoGuardado?.attributes?.values?.isNotEmpty() == true -> {
                        Log.d(
                            "OptimizationViewModel",
                            "🟢 Usando PVPC ESTIMADO desde SharedPreferences"
                        )
                        estimadoGuardado.attributes.values to "PVPC-estimado (Local)"
                    }

                    else -> {
                        Log.e("OptimizationViewModel", "❌ No se encontraron datos PVPC válidos")
                        emptyList<Value>() to "Desconocido"
                    }
                }

                _hourlyPrices.value = hourlyValues
                _tarifaFuente.value = fuente
// --- LOGGING PARA DEPURACIÓN DETALLADA ---
                Log.d("OptimizationViewModel", "Datos de la API recibidos:")
                if (hourlyValues.isEmpty()) {
                    Log.d("OptimizationViewModel", "  La lista de precios está vacía.")
                } else {
                    hourlyValues.forEach { value ->
                        Log.d(
                            "OptimizationViewModel",
                            "  Hora: ${value.datetime}, Valor: ${value.value}, Percentage: ${value.percentage}"
                        )
                    }
                }
// --- FIN LOGGING ---
            } catch (e: Exception) {
                Log.e("OptimizationViewModel", "Error al obtener precios horarios desde la API", e)
                _hourlyPrices.value = emptyList()
                _tarifaFuente.value = "Error" // O un valor por defecto que indique el error
            } finally {
                _hourlyPricesLoading.value = false
            }
        }
    }


    fun calcularHoraOptimizada(
        estrategia: String,
        prioridad: String,
        potenciaW: Float,
        minutosUso: Int
    ): List<Pair<String, Float>> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        val salida = DateTimeFormatter.ofPattern("HH:mm")

        val consumoKwh = potenciaW / 1000f

        val ahora = LocalDateTime.now()
        val horasValidas = hourlyPrices.value
            .filter { value ->
                val hora = try {
                    LocalDateTime.parse(value.datetime, formatter).hour
                } catch (e: Exception) {
                    return@filter false
                }

                when (prioridad.uppercase()) {
                    "ALTA" -> hora in 7..22
                    "MEDIA" -> hora in 6..23
                    else -> true
                }
            }

        return when (estrategia) {
            "Hora más barata" -> {
                horasValidas
                    .sortedBy { it.value }
                    .firstOrNull()
                    ?.let { inicio ->
                        val desdeIndice = hourlyPrices.value.indexOfFirst { it.datetime == inicio.datetime }
                        if (desdeIndice != -1) {
                            val sublista = hourlyPrices.value.drop(desdeIndice)
                            calcularCosteDesdeHoras(sublista, formatter, salida, minutosUso, potenciaW)
                        } else emptyList()
                    } ?: emptyList()
            }

            "Top 3 horas más baratas" -> {
                val top = horasValidas.sortedBy { it.value }.take(3).sortedBy {
                    LocalDateTime.parse(it.datetime, formatter)
                }
                calcularCosteDesdeHoras(top, formatter, salida, minutosUso, potenciaW)
            }

            "Hora más cercana" -> {
                val ahoraHora = ahora.hour
                val proximas = horasValidas.filter {
                    LocalDateTime.parse(it.datetime, formatter).hour >= ahoraHora
                }
                calcularCosteDesdeHoras(proximas, formatter, salida, minutosUso, potenciaW)
            }

            "Evitar horas punta (18:00–21:00)" -> {
                val fueraDePunta = horasValidas.filter {
                    val h = LocalDateTime.parse(it.datetime, formatter).hour
                    h !in 18..20
                }.sortedBy { it.value }

                fueraDePunta.firstOrNull()?.let { inicio ->
                    val desdeIndice = hourlyPrices.value.indexOfFirst { it.datetime == inicio.datetime }
                    if (desdeIndice != -1) {
                        val sublista = hourlyPrices.value.drop(desdeIndice)
                        calcularCosteDesdeHoras(sublista, formatter, salida, minutosUso, potenciaW)
                    } else emptyList()
                } ?: emptyList()
            }

            else -> {
                val consecutivas = horasValidas
                    .sortedBy { LocalDateTime.parse(it.datetime, formatter) }

                calcularCosteDesdeHoras(consecutivas, formatter, salida, minutosUso, potenciaW)
            }
        }}

    private fun calcularCosteDesdeHoras(
        horasDesdeInicio: List<Value>,
        formatter: DateTimeFormatter,
        salida: DateTimeFormatter,
        minutosUso: Int,
        potenciaW: Float
    ): List<Pair<String, Float>> {
        val consumoPorMinuto = potenciaW / 1000f / 60f
        val lista = mutableListOf<Pair<String, Float>>()
        var minutosRestantes = minutosUso

        for (hora in horasDesdeInicio) {
            if (minutosRestantes <= 0) break

            val minutosEnEstaHora = if (minutosRestantes >= 60) 60 else minutosRestantes
            val consumoKwh = consumoPorMinuto * minutosEnEstaHora
            val coste = (hora.value / 1000f) * consumoKwh
            val horaStr = salida.format(LocalDateTime.parse(hora.datetime, formatter))

            lista.add(horaStr to coste)
            minutosRestantes -= minutosEnEstaHora
        }

        return lista
    }



}
