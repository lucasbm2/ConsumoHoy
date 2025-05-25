package com.proyecto.consumohoy.consumption

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.consumohoy.database.ConsumptionDao
import com.proyecto.consumohoy.database.ConsumptionEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Gestionar lógica pantalla de consumo
class ConsumptionViewModel(private val dao: ConsumptionDao) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsumptionUser())
    val uiState: StateFlow<ConsumptionUser> = _uiState

    private val _consumptionList = MutableStateFlow<List<ConsumptionEntry>>(emptyList())
    val consumptionList: StateFlow<List<ConsumptionEntry>> = _consumptionList

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _consumptionList.value = dao.getAll()
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onPowerChange(value: String) = _uiState.update { it.copy(power = value) }
    fun onUsageMinutesChange(value: String) = _uiState.update { it.copy(usageMinutes = value) }
    fun onPriorityChange(value: String) = _uiState.update { it.copy(priority = value) }

    fun saveEntry() {
        val current = _uiState.value
        viewModelScope.launch {
            val powerFloat = current.power.toFloatOrNull() ?: 0f
            val minutesInt = current.usageMinutes.toIntOrNull() ?: 60

            if (current.id != null) {
                dao.update(
                    ConsumptionEntry(
                        id = current.id,
                        name = current.name,
                        power = powerFloat,
                        usageMinutes = minutesInt,
                        priority = current.priority
                    )
                )
            } else {
                dao.insert(
                    ConsumptionEntry(
                        name = current.name,
                        power = powerFloat,
                        usageMinutes = minutesInt,
                        priority = current.priority
                    )
                )
            }
            loadAll()
            _uiState.value = ConsumptionUser()
        }
    }

    fun deleteEntry(entry: ConsumptionEntry) {
        viewModelScope.launch {
            dao.delete(entry)
            loadAll()
        }
    }

    fun editEntry(entry: ConsumptionEntry) {
        _uiState.value = ConsumptionUser(
            id = entry.id,
            name = entry.name,
            power = entry.power.toString(),
            usageMinutes = entry.usageMinutes.toString(),
            priority = entry.priority
        )
    }
}
