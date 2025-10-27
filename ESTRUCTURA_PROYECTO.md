# Estructura del Proyecto Chess Mobile - Actualizada

## 📁 Estructura de Directorios

```
Chess-Mobile/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/github/irmin/chess/
│   │   │   │   ├── data/                          ⭐ NUEVO
│   │   │   │   │   ├── ChessDatabase.kt          - Base de datos Room
│   │   │   │   │   ├── GameRepository.kt         - Repositorio de datos
│   │   │   │   │   ├── GameStatistics.kt         - Entidad de estadísticas
│   │   │   │   │   ├── GameStatisticsDao.kt      - DAO de Room
│   │   │   │   │   └── GameStateXmlManager.kt    - Gestor de guardado XML
│   │   │   │   ├── model/
│   │   │   │   │   ├── ChessBoard.kt             - Lógica del juego (actualizado)
│   │   │   │   │   ├── ChessPiece.kt
│   │   │   │   │   ├── Position.kt
│   │   │   │   │   ├── Move.kt
│   │   │   │   │   ├── PieceType.kt
│   │   │   │   │   ├── PieceColor.kt
│   │   │   │   │   └── GameState.kt
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── ChessViewModel.kt         - ViewModel (actualizado)
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── GameScreen.kt         - Pantalla de juego (actualizado)
│   │   │   │   │   │   ├── MenuScreen.kt         - Menú principal (actualizado)
│   │   │   │   │   │   └── StatisticsScreen.kt   ⭐ NUEVO - Pantalla de estadísticas
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── NavigationGraph.kt    - Navegación (actualizado)
│   │   │   │   │   └── theme/
│   │   │   │   └── MainActivity.kt                - Actividad principal (actualizado)
│   │   │   └── res/
│   │   │       └── drawable/                      - Imágenes de piezas
│   │   └── test/
│   └── build.gradle.kts                          ⭐ ACTUALIZADO - Nuevas dependencias
├── gradle/
│   └── libs.versions.toml                        ⭐ ACTUALIZADO - Room y KSP
├── NUEVAS_FUNCIONALIDADES.md                      ⭐ NUEVO - Documentación
└── README.md
```

## 🎯 Funcionalidades por Componente

### 📦 Capa de Datos (Nueva)

#### ChessDatabase.kt
```kotlin
@Database(entities = [GameStatistics::class])
abstract class ChessDatabase : RoomDatabase()
```
- Base de datos SQLite usando Room
- Singleton pattern para única instancia
- Contiene DAO para acceso a datos

#### GameStatistics.kt
```kotlin
@Entity(tableName = "game_statistics")
data class GameStatistics(
    val gamesWonWhite: Int,
    val gamesWonBlack: Int,
    val totalPlayTime: Long,
    ...
)
```
- Entidad de Room para estadísticas
- Propiedades computadas (winRate, averageTime)
- Timestamps de última partida

#### GameStatisticsDao.kt
```kotlin
@Dao
interface GameStatisticsDao {
    @Query("SELECT * FROM game_statistics")
    fun getStatistics(): Flow<GameStatistics?>
    
    @Insert
    suspend fun insertStatistics(...)
}
```
- Operaciones CRUD para estadísticas
- Flow para actualizaciones reactivas
- Coroutines para operaciones asíncronas

#### GameRepository.kt
```kotlin
class GameRepository(private val dao: GameStatisticsDao) {
    suspend fun updateGameWon(winner: String, gameTime: Long)
    suspend fun resetStatistics()
}
```
- Capa de abstracción sobre el DAO
- Lógica de negocio para estadísticas
- Manejo de actualizaciones complejas

#### GameStateXmlManager.kt
```kotlin
class GameStateXmlManager(private val context: Context) {
    fun saveGameState(board: ChessBoard): Boolean
    fun loadGameState(): Pair<ChessBoard, Long>?
    fun hasSavedGame(): Boolean
}
```
- Serialización/deserialización XML
- Guarda estado completo del tablero
- Manejo de archivos local

### 🎮 Capa de ViewModel

#### ChessViewModel.kt (Actualizado)
```kotlin
class ChessViewModel(application: Application) : AndroidViewModel() {
    // Nuevas funciones
    fun continueGame()
    fun saveGame()
    fun showStatistics()
    fun resetStatistics()
    
    // Estado extendido
    data class ChessUiState(
        val hasSavedGame: Boolean,
        val statistics: GameStatistics?,
        val showStatistics: Boolean
    )
}
```
- Integración con XML Manager y Repository
- Rastreo de tiempo de juego
- Actualización automática de estadísticas

### 🎨 Capa de UI

#### MenuScreen.kt (Actualizado)
- ✅ Botón "Continue Game" (condicional)
- ✅ Botón "Statistics"
- ✅ Indicador de partida guardada

#### GameScreen.kt (Actualizado)
- ✅ Botón "Save Game"
- ✅ Layout mejorado
- ✅ Guardado automático al finalizar

#### StatisticsScreen.kt (Nueva)
```kotlin
@Composable
fun StatisticsScreen(
    statistics: GameStatistics?,
    onBackClick: () -> Unit,
    onResetClick: () -> Unit
)
```
- Cards con categorías de estadísticas
- Barras de progreso visuales
- Formateo de tiempo legible
- Opción de reset

## 🔄 Flujo de Datos

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│  MenuScreen │ GameScreen │ StatisticsScreen     │
└─────────────────┬───────────────────────────────┘
                  │
                  │ Observes StateFlow
                  ▼
┌─────────────────────────────────────────────────┐
│              ChessViewModel                      │
│  - Maneja estado UI                             │
│  - Coordina guardado/carga                      │
│  - Actualiza estadísticas                       │
└──────────┬──────────────────────┬────────────────┘
           │                      │
           │                      │
           ▼                      ▼
┌──────────────────┐    ┌────────────────────────┐
│ XML Manager      │    │  GameRepository        │
│ - Guardar/Cargar │    │  - CRUD estadísticas   │
│ - XML File I/O   │    │  - Lógica negocio      │
└──────────────────┘    └───────────┬────────────┘
                                    │
                                    ▼
                        ┌────────────────────────┐
                        │  Room Database         │
                        │  - SQLite              │
                        │  - DAO                 │
                        └────────────────────────┘
```

## 📊 Modelo de Datos

### GameStatistics Entity
```
┌─────────────────────────────────────────┐
│         GameStatistics                  │
├─────────────────────────────────────────┤
│ id: Long (PK, Auto)                     │
│ gamesWonWhite: Int                      │
│ gamesWonBlack: Int                      │
│ gamesDrawn: Int                         │
│ totalPlayTime: Long (ms)                │
│ longestGame: Long (ms)                  │
│ shortestGame: Long (ms)                 │
│ totalMoves: Int                         │
│ lastPlayed: Long (timestamp)            │
├─────────────────────────────────────────┤
│ Computed Properties:                    │
│ - totalGames: Int                       │
│ - winRateWhite: Float                   │
│ - winRateBlack: Float                   │
│ - averageGameTime: Long                 │
└─────────────────────────────────────────┘
```

### XML Structure
```xml
<ChessGame>
  ├── <Metadata>
  │   ├── <SaveTime>
  │   ├── <GameStartTime>
  │   ├── <CurrentTurn>
  │   └── <GameState>
  ├── <Board>
  │   └── <Piece> (multiple)
  │       ├── @row
  │       ├── @col
  │       ├── @type
  │       ├── @color
  │       └── @hasMoved
  └── <LastMove>
      ├── @fromRow
      ├── @fromCol
      ├── @toRow
      └── @toCol
</ChessGame>
```

## 🔧 Configuración de Dependencias

### build.gradle.kts (app)
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")  // ⭐ NUEVO
}

dependencies {
    // Room ⭐ NUEVO
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Existing dependencies...
}
```

## 🎯 Casos de Uso

### 1. Guardar Partida
```
Usuario juega → Presiona "Save Game" 
→ ViewModel.saveGame()
→ GameStateXmlManager.saveGameState()
→ Serializa board a XML
→ Guarda en filesDir
→ Actualiza UI (hasSavedGame = true)
```

### 2. Cargar Partida
```
Usuario abre app → Ve "Continue Game"
→ Presiona botón
→ ViewModel.continueGame()
→ GameStateXmlManager.loadGameState()
→ Lee XML y deserializa
→ Restaura ChessBoard
→ Inicia juego desde estado guardado
```

### 3. Actualizar Estadísticas
```
Partida termina (Checkmate/Stalemate)
→ ViewModel.endGame()
→ Calcula tiempo de juego
→ Repository.updateGameWon(winner, time, moves)
→ Lee estadísticas actuales
→ Actualiza contadores
→ Guarda en Room DB
→ Flow emite nueva data
→ UI se actualiza automáticamente
```

### 4. Ver Estadísticas
```
Usuario en menú → Presiona "Statistics"
→ ViewModel.showStatistics()
→ Navega a StatisticsScreen
→ Observa Flow de estadísticas
→ Muestra datos formateados
→ Usuario presiona "Reset" (opcional)
→ Repository.resetStatistics()
→ Borra todos los datos
```

## 🚀 Puntos de Extensión Futuros

1. **Multiple Save Slots**: Guardar múltiples partidas con nombres
2. **Export/Import**: Compartir partidas via archivos
3. **Cloud Backup**: Firebase o similar para backup
4. **Advanced Stats**: Gráficos, tendencias, análisis
5. **Replay System**: Reproducir partidas anteriores
6. **PGN Support**: Exportar en formato estándar de ajedrez

---

**Estado del Proyecto**: ✅ Completamente funcional con guardado XML y estadísticas Room
