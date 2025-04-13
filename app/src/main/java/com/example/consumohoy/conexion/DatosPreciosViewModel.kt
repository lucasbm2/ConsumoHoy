package com.example.consumohoy.conexion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.consumohoy.entities.Root
import com.example.consumohoy.firebase.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DatosPreciosViewModel : ViewModel() {

    //Mantengo los precios recogidos de la API
    private val datosInitial = MutableStateFlow<Root?>(null)
    val datos = datosInitial.asStateFlow()

    //Pido precios a la API con Retrofit y si funciona bien guardo en datosInitial
    fun getPrices(context: Context, startDate: String, endDate: String, timeTrunc: String) {
        viewModelScope.launch {
            runCatching {
                RetrofitClient.apiService.obtenerPrecios(startDate, endDate, timeTrunc)
            }.onSuccess { result ->
                datosInitial.value = result

                try {
                    val included = result.included

                    // mete valor Kwh bruto a respuesta de la api
                    val spotPriceTotal = included
                        ?.firstOrNull { it.id.contains("spot") }
                        ?.attributes?.values?.lastOrNull()

                    val spotValue = spotPriceTotal?.value?.toFloat()?.div(1000)  // €/kWh

                    // Obtener hora para aplicar peajes
                    val hora = spotPriceTotal?.datetime
                        ?.substringAfter("T")
                        ?.substring(0, 2)
                        ?.toIntOrNull() ?: 0

                    // Cálculo del peaje por franja horaria
                    val peaje = when (hora) {
                        in 0..7 -> 0.030f     // Valle
                        in 8..9, in 14..17, in 22..23 -> 0.045f // Llano
                        in 10..13, in 18..21 -> 0.060f // Punta
                        else -> 0.045f
                    }


                    val margen = 0.001f // Comercializador

                    val spotFinal = spotValue?.plus(peaje)?.plus(margen)

                    // Igual para PVPC
                    val pvpcPrim = included
                        ?.firstOrNull { it.id.contains("pvpcValue") }
                        ?.attributes?.values?.lastOrNull()

                    val pvpcValue = pvpcPrim?.value?.toFloat()?.div(1000) // €/kWh

                    val prefs = context.getSharedPreferences("precios", Context.MODE_PRIVATE)
                    val lastSpot = prefs.getFloat("spot", -1f)
                    val lastPvpc = prefs.getFloat("pvpc", -1f)

                    //Detecto si el precio SPOT ha cambiado desde el ultimo guardado
                    @Suppress("MissingPermission")
                    if (spotFinal != null && spotFinal != lastSpot) {
                        NotificationHelper.showNotification(
                            context,
                            "Precio SPOT actualizado",
                            "Nuevo: %.3f €/kWh".format(spotFinal)
                        )
                        prefs.edit().putFloat("spot", spotFinal).apply()
                    }

                    //Detecto si el precio PVPC ha cambiado desde el ultimo guardado
                    @Suppress("MissingPermission")
                    if (pvpcValue != null && pvpcValue != lastPvpc) {
                        NotificationHelper.showNotification(
                            context,
                            "Precio PVPC actualizado",
                            "Nuevo: %.3f €/kWh".format(pvpcValue)
                        )
                        prefs.edit().putFloat("pvpc", pvpcValue).apply()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }.onFailure {
                datosInitial.value = null
            }
        }
    }

}
