package com.proyecto.consumohoy.consumption

//la creo vacia, son valores pordefecto para no tener que pasarle nada
data class ConsumptionUser(
    val id: Int? = null,
    val name: String = "",
    val power: String = "",
    val usageMinutes: String = "60",
    val priority: String = ""
)