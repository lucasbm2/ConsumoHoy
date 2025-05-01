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
import com.proyecto.composecatalog.navigation.OptimizationScreen
import com.proyecto.consumohoy.conexion.RetrofitClient.apiService
import com.proyecto.consumohoy.consumption.ConsumptionScreen
import com.proyecto.consumohoy.consumption.ConsumptionViewModel
import com.proyecto.consumohoy.database.AppDatabase
import com.proyecto.consumohoy.datosprecios.*
import com.proyecto.consumohoy.datosprecios.ScreenPriceData
import com.proyecto.consumohoy.optimization.OptimizationScreen
import com.proyecto.consumohoy.optimization.OptimizationViewModel

//Funcion para la navegacion de pantallas
@Composable
fun NavigationWrapper() {
    val navController = rememberNavController()
    val context = LocalContext.current // Obtén el context aquí una vez

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

                ).fallbackToDestructiveMigration()
                    .build()
            }

            val dao = db.consumptionDao()
            val viewModel = remember { ConsumptionViewModel(dao) }

            ConsumptionScreen(viewModel = viewModel)
        }
        composable<OptimizationScreen> {
            // Usa el context obtenido arriba
            val db = remember {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "consumo-db"
                )
                    .fallbackToDestructiveMigration() // Manten esto si no usas migraciones
                    .build()
            }
            val dao = db.consumptionDao()

            // *** Pasa los 3 parámetros al constructor del ViewModel en el orden correcto ***
            val viewModel = remember {
                OptimizationViewModel(
                    dao, // 1. ConsumptionDao
                    apiService, // 2. ApiService
                    context // 3. Context
                )
            }
            // *** Llama a la función Composable de la pantalla, pasándole el ViewModel ***
            OptimizationScreen(viewModel = viewModel) // <-- Llama a la pantalla
        }
    }
}