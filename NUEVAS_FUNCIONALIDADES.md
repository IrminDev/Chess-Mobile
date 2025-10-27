# Nuevas Funcionalidades - Chess Mobile

## Resumen de Cambios

Se han implementado dos funcionalidades principales para mejorar la experiencia del usuario:

1. **Guardar y Cargar Partidas en XML**
2. **Sistema de Estadísticas con Room Database**

---

## 1. Sistema de Guardado de Partidas (XML)

### Descripción
El juego ahora permite guardar el estado completo de una partida activa en un archivo XML. Cuando el usuario sale de la aplicación o presiona el botón "Save Game", el estado se guarda automáticamente.

### Archivos Creados
- `app/src/main/java/com/github/irmin/chess/data/GameStateXmlManager.kt`

### Funcionalidades

#### Guardar Partida
- **Ubicación**: Botón "Save Game" en la pantalla de juego
- **Qué se guarda**:
  - Posición de todas las piezas en el tablero
  - Turno actual (Blancas o Negras)
  - Estado del juego (Jugando, Jaque, Jaque Mate, Tablas)
  - Último movimiento (necesario para En Passant)
  - Tiempo de inicio de la partida
  - Si cada pieza se ha movido (necesario para Enroque)

#### Cargar Partida
- **Ubicación**: Botón "Continue Game" en el menú principal
- **Comportamiento**:
  - El botón solo está habilitado si existe una partida guardada
  - Al cargar, restaura completamente el estado de la partida
  - El tiempo de juego continúa desde donde se dejó

#### Formato del Archivo XML
```xml
<ChessGame>
  <Metadata>
    <SaveTime>timestamp</SaveTime>
    <GameStartTime>timestamp</GameStartTime>
    <CurrentTurn>WHITE/BLACK</CurrentTurn>
    <GameState>PLAYING/CHECK/CHECKMATE/STALEMATE</GameState>
  </Metadata>
  <Board>
    <Piece row="0" col="0" type="ROOK" color="BLACK" hasMoved="false"/>
    <!-- Más piezas... -->
  </Board>
  <LastMove fromRow="6" fromCol="4" toRow="4" toCol="4"/>
</ChessGame>
```

### Métodos Principales

```kotlin
// Guardar el estado del juego
fun saveGameState(board: ChessBoard, gameStartTime: Long): Boolean

// Cargar el estado del juego
fun loadGameState(): Pair<ChessBoard, Long>?

// Verificar si existe un juego guardado
fun hasSavedGame(): Boolean

// Eliminar el juego guardado
fun deleteSavedGame(): Boolean
```

---

## 2. Sistema de Estadísticas (Room Database)

### Descripción
Se implementó una base de datos local usando Room para rastrear y almacenar estadísticas de las partidas jugadas.

### Archivos Creados
- `app/src/main/java/com/github/irmin/chess/data/GameStatistics.kt` - Entidad de Room
- `app/src/main/java/com/github/irmin/chess/data/GameStatisticsDao.kt` - DAO de Room
- `app/src/main/java/com/github/irmin/chess/data/ChessDatabase.kt` - Base de datos Room
- `app/src/main/java/com/github/irmin/chess/data/GameRepository.kt` - Repositorio para manejar datos
- `app/src/main/java/com/github/irmin/chess/ui/screens/StatisticsScreen.kt` - Pantalla de estadísticas

### Estadísticas Rastreadas

#### Generales
- Total de partidas jugadas
- Partidas ganadas con piezas blancas
- Partidas ganadas con piezas negras
- Partidas en tablas
- Total de movimientos realizados

#### Tasas de Victoria
- Porcentaje de victorias con blancas
- Porcentaje de victorias con negras
- Barras de progreso visuales para cada color

#### Estadísticas de Tiempo
- Tiempo total de juego
- Tiempo promedio por partida
- Partida más larga
- Partida más corta
- Fecha y hora del último juego

### Acceso a Estadísticas
- **Ubicación**: Botón "Statistics" en el menú principal
- **Funcionalidades**:
  - Ver todas las estadísticas
  - Botón "Reset" para borrar todas las estadísticas
  - Actualización automática después de cada partida

### Persistencia de Datos
- Los datos se guardan automáticamente en una base de datos SQLite local
- Las estadísticas persisten entre sesiones de la aplicación
- Se actualizan automáticamente al finalizar cada partida

---

## 3. Cambios en el ViewModel

### ChessViewModel (Actualizado)
- Ahora extiende `AndroidViewModel` en lugar de `ViewModel` para acceder al contexto
- Integra `GameStateXmlManager` para guardar/cargar partidas
- Integra `GameRepository` para manejar estadísticas
- Rastrea el tiempo de inicio de cada partida
- Actualiza automáticamente las estadísticas al finalizar una partida

### Nuevos Métodos
```kotlin
fun continueGame()          // Carga una partida guardada
fun saveGame()              // Guarda la partida actual
fun showStatistics()        // Muestra la pantalla de estadísticas
fun hideStatistics()        // Oculta la pantalla de estadísticas
fun resetStatistics()       // Reinicia todas las estadísticas
fun deleteSavedGame()       // Elimina la partida guardada
```

---

## 4. Actualizaciones de UI

### MenuScreen (Actualizado)
- Nuevo botón "Continue Game" (habilitado solo si hay partida guardada)
- Nuevo botón "Statistics"
- Indicador visual de partida guardada

### GameScreen (Actualizado)
- Nuevo botón "Save Game" para guardar en cualquier momento
- Layout reorganizado para mejor usabilidad

### StatisticsScreen (Nueva)
- Interfaz completa para ver estadísticas
- Cards organizadas por categoría
- Barras de progreso para tasas de victoria
- Formateo de tiempo legible (horas, minutos, segundos)
- Botón para reiniciar estadísticas

---

## 5. Dependencias Agregadas

### En `gradle/libs.versions.toml`:
```toml
[versions]
room = "2.6.1"
ksp = "2.0.21-1.0.28"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### En `app/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
```

---

## 6. Flujo de Usuario

### Iniciar Nueva Partida
1. Usuario abre la aplicación
2. Presiona "Multiplayer Local"
3. Juega la partida
4. Puede presionar "Save Game" en cualquier momento
5. Al terminar, las estadísticas se actualizan automáticamente

### Continuar Partida Guardada
1. Usuario abre la aplicación
2. Ve el mensaje "You have a saved game!"
3. Presiona "Continue Game"
4. La partida se restaura exactamente donde se quedó
5. Puede seguir jugando normalmente

### Ver Estadísticas
1. Usuario presiona "Statistics" en el menú
2. Ve todas sus estadísticas acumuladas
3. Puede presionar "Reset" para borrar todo
4. Presiona "Back" para volver al menú

---

## 7. Consideraciones Técnicas

### Persistencia de Datos
- **XML**: Almacenado en `context.filesDir` (privado de la app)
- **Room**: Base de datos SQLite en almacenamiento interno

### Manejo de Errores
- Validación de datos al cargar XML
- Try-catch en todas las operaciones de I/O
- Manejo de casos donde no hay datos

### Optimización
- Uso de Kotlin Coroutines para operaciones de I/O
- Flow de Room para actualizaciones reactivas
- Carga lazy de la base de datos

### Seguridad
- Los datos solo son accesibles por la aplicación
- No se requieren permisos especiales
- Almacenamiento local seguro

---

## 8. Próximas Mejoras Posibles

1. **Historial de Partidas**: Guardar múltiples partidas con nombres
2. **Exportar/Importar**: Compartir partidas guardadas
3. **Gráficos**: Mostrar tendencias de estadísticas
4. **Notaciones**: Guardar movimientos en notación algebraica
5. **Replay**: Ver partidas anteriores
6. **Cloud Sync**: Sincronizar con la nube

---

## 9. Cómo Compilar y Ejecutar

1. Sincronizar Gradle:
   ```
   ./gradlew sync
   ```

2. Limpiar y construir:
   ```
   ./gradlew clean build
   ```

3. Instalar en dispositivo:
   ```
   ./gradlew installDebug
   ```

---

## 10. Testing

### Probar Guardar/Cargar
1. Iniciar una partida
2. Hacer varios movimientos
3. Presionar "Save Game"
4. Volver al menú
5. Presionar "Continue Game"
6. Verificar que el estado se restauró correctamente

### Probar Estadísticas
1. Completar varias partidas (ganar con blancas, negras, tablas)
2. Ir a "Statistics"
3. Verificar que los números sean correctos
4. Presionar "Reset"
5. Verificar que las estadísticas se borraron

---

¡Disfruta las nuevas funcionalidades de Chess Mobile! 🎮♟️
