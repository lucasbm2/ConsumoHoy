<table>
<tr>
  <td><h1>ConsumoHoy - Ahorro de Electricidad</h1></td>
  <td><img src="images/ic_launcher.png" alt="Icono de ConsumoHoy" width="80"/></td>
</tr>
</table>



**ConsumoHoy - Ahorro de Electricidad** es una aplicación Android desarrollada en Kotlin cuyo objetivo es ayudar a los usuarios a reducir el gasto energético en el hogar. Permite registrar el consumo de distintos aparatos eléctricos, visualizar los precios de la luz por horas (mercado SPOT y PVPC), y ofrece estrategias de optimización personalizadas para ahorrar energía y dinero.

## Motivación

El proyecto nace de una inquietud personal por mejorar la economía doméstica, especialmente en un contexto de creciente preocupación por el coste de la energía. Esta motivación se ve reforzada por mi formación previa en el ámbito eléctrico, que me ha permitido comprender en profundidad el funcionamiento de los sistemas energéticos, los patrones de consumo y las oportunidades reales de optimización.

**ConsumoHoy - Ahorro de Electricidad** no surge únicamente como un ejercicio técnico, sino como una herramienta práctica pensada para usuarios que desean tomar el control de su gasto energético de forma sencilla y eficaz. En lugar de limitarse a mostrar precios, la app interpreta esos datos, los relaciona con los hábitos del usuario y propone estrategias reales de ahorro. En este sentido, busca ayudar al usuario con información comprensible y útil, traducida directamente en decisiones que mejoran su economía diaria.

## Características principales

- Visualización de precios eléctricos por hora (SPOT y PVPC).
- Estimación de precios PVPC cuando los reales aún no están disponibles.
- Registro y gestión de aparatos eléctricos con su consumo en W.
- Cálculo del coste energético estimado por aparato y duración.
- Estrategias de optimización para reducir el gasto:
  - Hora más barata del día.
  - Hora más cercana con mejor precio.
  - Evitar horas punta.
  - Top 3 horas más económicas.
- Estimación de ahorro mensual por estrategia aplicada.
- Interfaz visual coherente, moderna y clara en todas las pantallas.

## Tecnologías utilizadas

- **Kotlin** y **Jetpack Compose** para la interfaz de usuario.
- **Room**: gestión de base de datos local para aparatos eléctricos.
- **Retrofit** + **Gson**: conexión con la API de la Red Eléctrica Española (REE) para obtener precios por hora.
- **StateFlow / ViewModel**: arquitectura moderna y reactiva.
- **Material 3**: diseño visual moderno y accesible.
- **WorkManager**: para enviar notificaciones personalizadas antes de eventos.

## Instalación y ejecución
<p align="center">
  <img src="images/github_ico.png" alt="Icono de GitHub" width="120"/>
</p>
1. Clona el repositorio:
   ```bash
   git clone https://github.com/tu-usuario/consumohoy.git
<p align="center">
  <img src="images/android_ico.png" alt="Icono de Android" width="120"/>
</p>
2. Nota: Próximamente la aplicación estará disponible públicamente en Google Play. 

Actualmente, puedes solicitar acceso como tester interno a través del siguiente enlace:

[Unirse como tester interno](https://play.google.com/apps/internaltest/4701721834206816087)

Posteriormente, puedes [realizar la descarga aquí](https://play.google.com/store/apps/details?id=com.proyecto.consumohoy)
