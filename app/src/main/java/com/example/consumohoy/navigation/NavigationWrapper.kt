package com.example.consumohoy.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.composecatalog.navigation.DatosPreciosScreen
import com.example.composecatalog.navigation.MenuScreen
import com.example.consumohoy.datosprecios.ScreenPriceData
import com.example.consumohoy.datosprecios.MenuScreen

//Funcion para la navegacion
@Composable
fun NavigationWrapper() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MenuScreen) {
        composable<MenuScreen> { MenuScreen(navController) }
        composable<DatosPreciosScreen> { ScreenPriceData() }
        // composable("opcion2") {  }
        // composable("opcion3") {  }
    }
}