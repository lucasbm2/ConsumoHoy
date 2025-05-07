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

                val startDateTime ="${fechaStr}T00:00"
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
                    it.id.contains("pvpc-estimado", ignoreCase = true) || it.type.contains("pvpc-estimado", ignoreCase = true)
                }

                val pvpcRealApi = response.included?.firstOrNull {
                    (it.id.contains("pvpc", ignoreCase = true) && !it.id.contains("pvpc-estimado", ignoreCase = true)) ||
                            (it.type.contains("pvpc", ignoreCase = true) && !it.type.contains("pvpc-estimado", ignoreCase = true))
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
                        Log.d("OptimizationViewModel", "🟢 Usando PVPC ESTIMADO desde SharedPreferences")
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
        potenciaW: Float // 👈 ahora se pasa directamente la potencia en W
    ): List<Pair<String, Float>> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        val salida = DateTimeFormatter.ofPattern("HH:mm")

        val consumoKwh = potenciaW / 1000f // 👈 conversión W → kWh

        val ahora = LocalDateTime.now()
        val horasValidas = hourlyPrices.value
            .filter { value ->
                val hora = try {
                    LocalDateTime.parse(value.datetime, formatter).hour
                } catch (e: Exception) { return@filter false }

                when (prioridad.uppercase()) {
                    "ALTA" -> hora in 7..22
                    "MEDIA" -> hora in 6..23
                    else -> true
                }
            }

        return when (estrategia) {
            "Hora más barata" -> horasValidas.minByOrNull { it.value }?.let {
                listOf(salida.format(LocalDateTime.parse(it.datetime, formatter)) to (it.value / 1000f) * consumoKwh)
            } ?: emptyList()

            "Top 3 horas más baratas" -> horasValidas
                .sortedBy { it.value }
                .take(3)
                .map {
                    salida.format(LocalDateTime.parse(it.datetime, formatter)) to (it.value / 1000f) * consumoKwh
                }

            "Hora más cercana" -> {
                horasValidas
                    .filter {
                        try {
                            val h = LocalDateTime.parse(it.datetime, formatter).hour
                            h >= ahora.hour // Solo para "Hora más cercana"
                        } catch (e: Exception) { false }
                    }
                    .minByOrNull { it.value }
                    ?.let {
                        listOf(salida.format(LocalDateTime.parse(it.datetime, formatter)) to (it.value / 1000f) * consumoKwh)
                    } ?: emptyList()
            }

            "Evitar horas punta (18:00–21:00)" -> horasValidas
                .filter {
                    val hora = try {
                        LocalDateTime.parse(it.datetime, formatter).hour
                    } catch (e: Exception) { return@filter false }
                    hora !in 18..20
                }
                .minByOrNull { it.value }
                ?.let {
                    listOf(salida.format(LocalDateTime.parse(it.datetime, formatter)) to (it.value / 1000f) * consumoKwh)
                } ?: emptyList()

            "2 horas baratas consecutivas" -> calcularMejorVentana(horasValidas, 2, ahora, formatter, salida, consumoKwh)

            "3 horas baratas consecutivas" -> calcularMejorVentana(horasValidas, 3, ahora, formatter, salida, consumoKwh)

            "5 horas baratas consecutivas" -> calcularMejorVentana(horasValidas, 5, ahora, formatter, salida, consumoKwh)

            else -> emptyList()
        }
    }

    // Función auxiliar para calcular las ventanas consecutivas
    private fun calcularMejorVentana(
        horasValidas: List<Value>,
        ventanaSize: Int,
        ahora: LocalDateTime,
        formatter: DateTimeFormatter,
        salida: DateTimeFormatter,
        consumoKwh: Float
    ): List<Pair<String, Float>> {
        var mejorVentana: List<Value>? = null
        var menorSuma = Float.MAX_VALUE

        val ventanas = horasValidas.windowed(ventanaSize)
        for (ventana in ventanas) {
            if (ventana.size < ventanaSize) continue
            if (LocalDateTime.parse(ventana.last().datetime, formatter) < ahora) continue // Saltar ventanas pasadas
            val suma = ventana.sumOf { it.value.toDouble() }.toFloat()
            if (suma < menorSuma) {
                menorSuma = suma
                mejorVentana = ventana
            }
        }

        return mejorVentana?.map {
            val horaStr = salida.format(LocalDateTime.parse(it.datetime, formatter))
            horaStr to (it.value / 1000f) * consumoKwh
        } ?: emptyList()
    }
}