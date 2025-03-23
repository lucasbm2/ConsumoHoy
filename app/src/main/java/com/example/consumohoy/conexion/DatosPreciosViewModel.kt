package com.example.consumohoy.conexion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.consumohoy.entities.Root
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DatosPreciosViewModel : ViewModel() {

    //Almacena los datos de la respuesta de la API
    //MutableStateFlow para que se pueda modificar desde el ViewModel
    private val datosInitial = MutableStateFlow<Root?>(null)
    val datos = datosInitial.asStateFlow()

    //Almacena el error de la respuesta de la API
    private val errorInitial = MutableStateFlow<String?>(null)
    val error = errorInitial.asStateFlow()

    //Función para obtener los datos de la API
    fun getPrices(startDate: String, endDate: String, timeTrunc: String) {
        //ViewModelScope para que se ejecute en un hilo separado del hilo principal
        viewModelScope.launch {
            //runCatching para que si hay un error no se crashee la app
            runCatching {
                val response = RetrofitClient.apiService.obtenerPrecios(startDate, endDate, timeTrunc)
                response
                //onSuccess para que se ejecute si la respuesta es positiva
            }.onSuccess { result ->
                datosInitial.value = result
                //onFailure hace que se ejecute si hay un error pero no se crashee la app
            }.onFailure { error ->
                datosInitial.value = null
            }
        }
    }
}
