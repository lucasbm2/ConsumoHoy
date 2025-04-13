package com.example.consumohoy.datosprecios

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.composecatalog.navigation.DatosPreciosScreen
import com.example.composecatalog.navigation.ElectricityConsumption
import com.example.consumohoy.R
import com.example.consumohoy.firebase.PrecioWorker


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
            navController.navigate(DatosPreciosScreen)
        }) {
            Text(
                text = stringResource(R.string.opcion_1),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Button(onClick = {
            navController.navigate(ElectricityConsumption)
        }) {
            Text(
                text = stringResource(R.string.opcion_2),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Button(onClick = {
            // Acción opción 3
        }) {
            Text(
                text = stringResource(R.string.opcion_3),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Button(onClick = {
            val workRequest: WorkRequest = OneTimeWorkRequestBuilder<PrecioWorker>().build()
            WorkManager.getInstance(navController.context).enqueue(workRequest)
        }) {
            //Para probar la notificacion
            Text(
                text = "Forzar notificación ahora",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}