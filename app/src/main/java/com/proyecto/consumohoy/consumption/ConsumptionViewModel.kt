package com.proyecto.consumohoy.consumption


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.consumohoy.database.ConsumptionDao
import com.proyecto.consumohoy.database.ConsumptionEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//Gestionar lógica pantalla de consumo
class ConsumptionViewModel(private val dao: ConsumptionDao) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsumptionUser())
    //StateFlow para el formulario de consumo de energía
    val uiState: StateFlow<ConsumptionUser> = _uiState

    // Lista de todos los consumos
    private val _consumptionList = MutableStateFlow<List<ConsumptionEntry>>(emptyList())
    val consumptionList: StateFlow<List<ConsumptionEntry>> = _consumptionList

    init {
        loadAll()
    }

    // Cargar todos los consumos
    private fun loadAll() {
        viewModelScope.launch {
            _consumptionList.value = dao.getAll()
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onPowerChange(value: String) = _uiState.update { it.copy(power = value) }
    fun onPriorityChange(value: String) = _uiState.update { it.copy(priority = value) }

    // Guardar un nuevo consumo
    fun saveEntry() {
        val current = _uiState.value
        viewModelScope.launch {
            if (current.id != null) {
                // Actualizar entrada existente
                dao.update(
                    ConsumptionEntry(
                        id = current.id,
                        name = current.name,
                        power = current.power,
                        priority = current.priority
                    )
                )
            } else {
                // Insertar nuevo
                dao.insert(
                    ConsumptionEntry(
                        name = current.name,
                        power = current.power,
                        priority = current.priority
                    )
                )
            }
            loadAll()
            _uiState.value = ConsumptionUser()
        }
    }


    // Borrar
    fun deleteEntry(entry: ConsumptionEntry) {
        viewModelScope.launch {
            dao.delete(entry)
            loadAll()
        }
    }

    // Editar (carga los datos en el formulario para modificar)
    fun editEntry(entry: ConsumptionEntry) {
        _uiState.value = ConsumptionUser(
            id = entry.id, // Guardamos el ID
            name = entry.name,
            power = entry.power,
            priority = entry.priority
        )
    }
}
