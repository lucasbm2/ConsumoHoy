package com.example.composecatalog.navigation

import kotlinx.serialization.Serializable

//Definimos las pantallas de la app como objetos serializables para poder pasarlos entre pantallas
//En el NavigationWrapper

@Serializable
object MenuScreen

@Serializable
object DatosPreciosScreen

@Serializable
object ElectricityConsumption