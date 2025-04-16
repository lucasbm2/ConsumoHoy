package com.proyecto.consumohoy.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.proyecto.composecatalog.navigation.ConsumptionScreen
import com.proyecto.composecatalog.navigation.DatosPreciosScreen
import com.proyecto.composecatalog.navigation.MenuScreen
import com.proyecto.consumohoy.consumption.ConsumptionScreen
import com.proyecto.consumohoy.consumption.ConsumptionViewModel
import com.proyecto.consumohoy.database.AppDatabase
import com.proyecto.consumohoy.datosprecios.*
import com.proyecto.consumohoy.datosprecios.ScreenPriceData

//Funcion para la navegacion de pantallas
@Composable
fun NavigationWrapper() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MenuScreen) {
        composable<MenuScreen> { MenuScreen(navController) }
        composable<DatosPreciosScreen> { ScreenPriceData() }
        composable<ConsumptionScreen> {
            val context = LocalContext.current

            val db = remember {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "consumo-db"
                ).build()
            }

            val dao = db.consumptionDao()
            val viewModel = remember { ConsumptionViewModel(dao) }

            ConsumptionScreen(viewModel = viewModel)
        }
    }
}