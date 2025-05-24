package com.proyecto.consumohoy.datosprecios

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.proyecto.composecatalog.navigation.DatosPreciosScreen
import com.proyecto.composecatalog.navigation.ConsumptionScreen
import com.proyecto.consumohoy.R
import kotlinx.coroutines.delay

val fuenteEjecutiva = FontFamily(
    Font(resId = R.font.poppins_regular)
)

@Composable
fun MenuScreen(navController: NavHostController) {
    val frases = listOf(
        "Tu asistente diario para el ahorro energético",
        "Toma el control de tu consumo eléctrico",
        "Ahorra sin esfuerzo, cada día",
        "Elige las mejores horas para consumir luz",
        "Tu consumo, bajo control y con sentido",
        "Pequeños cambios, grandes ahorros",
        "Consume cuando es más barato",
        "Sencillo, eficiente, pensado para ti",
        "Haz que cada kilovatio cuente",
        "Más eficiencia, menos factura"
    )

    var fraseActual by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            fraseActual = (fraseActual + 1) % frases.size
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Imagen de fondo
        Image(
            painter = painterResource(id = R.drawable.fondo_bombilla),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Capa blanca translúcida
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCCFFFFFF))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔝 Frase rotativa
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFFBBDEFB), // Azul claro (puedes cambiar)
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = frases[fraseActual],
                    fontFamily = fuenteEjecutiva,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF0D47A1) // Azul oscuro del texto
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // 🔷 Título + Icono
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ConsumoHoy",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = fuenteEjecutiva,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D47A1),
                        fontSize = 40.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = "Icono principal",
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(50.dp))

            Text(
                text = "Visualiza el coste horario de la electricidad cada día.\n\n" +
                        "Planifica tu consumo y reduce tu gasto energético fácilmente."
                ,
                fontFamily = fuenteEjecutiva,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = Color(0xFF0D47A1),
                lineHeight = 24.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(70.dp))

            // 🔘 Botones
            BotonElegante(
                texto = "Ver precios de luz",
                icono = Icons.Filled.FlashOn,
                onClick = { navController.navigate(DatosPreciosScreen) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            BotonElegante(
                texto = "Registrar consumo",
                icono = Icons.Filled.Bolt,
                onClick = { navController.navigate(ConsumptionScreen) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            BotonElegante(
                texto = "Optimizar uso eléctrico",
                icono = Icons.Filled.Analytics,
                onClick = { navController.navigate(com.proyecto.composecatalog.navigation.OptimizationScreen) }
            )

            Spacer(modifier = Modifier.height(32.dp)) // margen inferior
        }
    }
}

@Composable
fun BotonElegante(
    texto: String,
    icono: ImageVector? = null,
    onClick: () -> Unit,
    backgroundColor: Color = Color(0xFF0D47A1),
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(64.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icono?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = texto,
                fontFamily = fuenteEjecutiva,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp
            )
        }
    }
}
