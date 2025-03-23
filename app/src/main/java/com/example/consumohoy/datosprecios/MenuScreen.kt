package com.example.consumohoy.datosprecios

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.composecatalog.navigation.DatosPreciosScreen


//Funcion para la pantalla de menu principal
@Composable
fun MenuScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            // Acción opción 1
            navController.navigate(DatosPreciosScreen)
        }) {
            Text("Ver precios")
        }
        Button(onClick = {
            // Acción opción 2
        }) {
            Text("Opción 2")
        }
        Button(onClick = {
            // Acción opción 3
        }) {
            Text("Opción 3")
        }
    }
}