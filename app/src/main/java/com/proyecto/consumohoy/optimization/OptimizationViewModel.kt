package com.proyecto.consumohoy.optimization

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.consumohoy.database.ConsumptionDao
import com.proyecto.consumohoy.database.ConsumptionEntry
import com.proyecto.consumohoy.entities.Value
import com.proyecto.consumohoy.conexion.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class OptimizationViewModel(
    private val consumptionDao: ConsumptionDao,
    private val apiService: ApiService,
    private val context: Context
) : ViewModel() {

    private val _consumptionList = MutableStateFlow<List<ConsumptionEntry>>(emptyList())
    val consumptionList: StateFlow<List<ConsumptionEntry>> = _consumptionList

    private val _latestPrices = MutableStateFlow<Map<String, Float>>(emptyMap())
    val latestPrices: StateFlow<Map<String, Float>> = _latestPrices

    private val _hourlyPrices = MutableStateFlow<List<Value>>(emptyList())
    val hourlyPrices: StateFlow<List<Value>> = _hourlyPrices

    private val _hourlyPricesLoading = MutableStateFlow(false)
    val hourlyPricesLoading: StateFlow<Boolean> = _hourlyPricesLoading

    private val _tarifaFuente = MutableStateFlow<String>("")
    val tarifaFuente: StateFlow<String> = _tarifaFuente

    init {
        loadConsumptions()
        loadLatestPrices()
        loadHourlyPrices()
    }

    private fun loadConsumptions() {
        viewModelScope.launch {
            try {
                _consumptionList.value = consumptionDao.getAll()
            } catch (e: Exception) {
                _consumptionList.value = emptyList()
                Log.e("OptimizationViewModel", "Error loading consumptions", e)
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
                if (latestSpot > 0f) pricesMap["SPOT"] = latestSpot / 1000f
                if (latestPvpc > 0f) pricesMap["PVPC"] = latestPvpc / 1000f

                _latestPrices.value = pricesMap
            } catch (e: Exception) {
                Log.e("OptimizationViewModel", "Error loading recent prices", e)
            }
        }
    }

    private fun loadHourlyPrices() {
        viewModelScope.launch {
            _hourlyPricesLoading.value = true
            _hourlyPrices.value = emptyList()

            try {
                val ahora = LocalTime.now()
                val fechaHoy = if (ahora.hour >= 20) LocalDate.now().plusDays(1).toString() else LocalDate.now().toString()

                val response = apiService.obtenerPrecios(
                    startDate = "${fechaHoy}T00:00",
                    endDate = "${fechaHoy}T23:59",
                    timeTrunc = "hour"
                )

                val pvpcRealApi = response.included?.firstOrNull {
                    it.id.contains("pvpc", ignoreCase = true) &&
                            !it.id.contains("pvpc-estimado", ignoreCase = true)
                }


                val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)
                val gson = com.google.gson.Gson()
                val estimadoGuardado = prefs.getString("pvpc_estimado_json", null)?.let {
                    gson.fromJson(it, com.proyecto.consumohoy.entities.Included::class.java)
                }

                val hoy = fechaHoy
                fun esDeHoy(datetime: String?): Boolean {
                    return datetime?.substring(0, 10) == hoy
                }

                val esHoyPvpcReal = pvpcRealApi?.attributes?.values?.any { esDeHoy(it.datetime) } == true
               val esHoyEstimadoGuardado = estimadoGuardado?.attributes?.values?.any { esDeHoy(it.datetime) } == true

                val (hourlyValues, fuente) = when {
                    esHoyPvpcReal -> {
                        pvpcRealApi!!.attributes.values to "Precio mercado PVPC"
                    }
                    esHoyEstimadoGuardado -> {
                        estimadoGuardado!!.attributes.values to "Tarifa PVPC (estimado)"
                    }
                    else -> {
                        emptyList<Value>() to "Desconocido"
                    }
                }

                _hourlyPrices.value = hourlyValues
                _tarifaFuente.value = fuente

            } catch (e: Exception) {
                Log.e("OptimizationViewModel", "Error al obtener precios horarios", e)
                _hourlyPrices.value = emptyList()
                _tarifaFuente.value = "Error"
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
        val horasValidas = hourlyPrices.value.filter {
            val hora = try {
                LocalDateTime.parse(it.datetime, formatter).hour
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
            "Top 3 horas más baratas" -> {
                val top = horasValidas.sortedBy { it.value }.take(3).sortedBy {
                    LocalDateTime.parse(it.datetime, formatter)
                }
                calcularCosteDesdeHoras(top, formatter, salida, minutosUso, potenciaW)
            }

            "Hora más cercana/barata" -> {
                val ahoraHora = ahora.hour
                val futuras = horasValidas.filter {
                    LocalDateTime.parse(it.datetime, formatter).hour >= ahoraHora
                }
                val inicio = futuras.minByOrNull { it.value }

                if (inicio != null) {
                    val desdeIndice = hourlyPrices.value.indexOfFirst { it.datetime == inicio.datetime }
                    if (desdeIndice != -1) {
                        val sublista = hourlyPrices.value.drop(desdeIndice)
                        calcularCosteDesdeHoras(sublista, formatter, salida, minutosUso, potenciaW)
                    } else emptyList()
                } else emptyList()
            }

            "Hora más barata del día" -> {
                horasValidas.sortedBy { it.value }.firstOrNull()?.let { inicio ->
                    val desdeIndice = hourlyPrices.value.indexOfFirst { it.datetime == inicio.datetime }
                    if (desdeIndice != -1) {
                        val sublista = hourlyPrices.value.drop(desdeIndice)
                        calcularCosteDesdeHoras(sublista, formatter, salida, minutosUso, potenciaW)
                    } else emptyList()
                } ?: emptyList()
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
                val consecutivas = horasValidas.sortedBy {
                    LocalDateTime.parse(it.datetime, formatter)
                }
                calcularCosteDesdeHoras(consecutivas, formatter, salida, minutosUso, potenciaW)
            }
        }
    }

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
