# Guía Rápida - Nuevas Funcionalidades

## 🎮 Cómo Usar las Nuevas Funcionalidades

### 💾 Guardar y Cargar Partidas

#### Guardar una Partida en Progreso
1. Estás jugando una partida
2. Presiona el botón **"Save Game"** (en la parte superior del tablero)
3. ✅ Tu partida se ha guardado automáticamente
4. Puedes salir del juego sin perder el progreso

#### Continuar una Partida Guardada
1. Abre la aplicación
2. En el menú principal, verás el mensaje: **"You have a saved game!"**
3. Presiona el botón **"Continue Game"**
4. 🎉 ¡Tu partida se carga exactamente donde la dejaste!
5. Sigue jugando normalmente

#### Notas Importantes
- Solo puedes tener 1 partida guardada a la vez
- Guardar una nueva partida sobrescribirá la anterior
- La partida se elimina automáticamente cuando termina
- Se guarda: posición de piezas, turno, movimientos especiales, tiempo

---

### 📊 Ver y Gestionar Estadísticas

#### Ver tus Estadísticas
1. En el menú principal, presiona **"Statistics"**
2. Verás 3 categorías de estadísticas:
   - **General Statistics**: Partidas ganadas, empates, movimientos
   - **Win Rates**: Porcentajes de victoria con blancas y negras
   - **Time Statistics**: Tiempos de juego, promedio, récords

#### Interpretar las Estadísticas

**Estadísticas Generales:**
- `Total Games`: Total de partidas completadas
- `Games Won as White`: Victorias jugando con piezas blancas
- `Games Won as Black`: Victorias jugando con piezas negras
- `Games Drawn`: Partidas terminadas en tablas
- `Total Moves`: Suma de todos los movimientos realizados

**Tasas de Victoria:**
- `White Win Rate`: Porcentaje de victorias con blancas
- `Black Win Rate`: Porcentaje de victorias con negras
- Barras de progreso visuales para cada color

**Estadísticas de Tiempo:**
- `Total Play Time`: Tiempo total jugado (acumulado)
- `Average Game Time`: Duración promedio por partida
- `Longest Game`: Tu partida más larga
- `Shortest Game`: Tu partida más corta
- `Last Played`: Fecha y hora de tu última partida

#### Reiniciar Estadísticas
1. Estando en la pantalla de estadísticas
2. Presiona el botón rojo **"Reset"**
3. Confirma la acción
4. ✅ Todas las estadísticas se borran y vuelven a 0

---

## 🎯 Escenarios de Uso Comunes

### Escenario 1: Partido Interrumpido
```
Estás jugando → Necesitas salir urgentemente
→ Presiona "Save Game"
→ Cierra la app
→ Más tarde: Abre la app → "Continue Game"
→ ✅ Sigues jugando desde donde lo dejaste
```

### Escenario 2: Seguimiento de Progreso
```
Juega varias partidas durante la semana
→ Vas al menú → "Statistics"
→ Ves cuántas has ganado, tiempo jugado, etc.
→ Observas tu mejora con el tiempo
```

### Escenario 3: Empezar de Nuevo
```
Quieres borrar tu historial
→ Ve a "Statistics"
→ Presiona "Reset"
→ ✅ Comienza con estadísticas limpias
```

---

## 📱 Interfaz de Usuario

### Menú Principal
```
┌─────────────────────────────────┐
│        Chess Game               │
│                                 │
│  [Single Player]  (Disabled)    │
│  [Multiplayer Local]            │
│  [Continue Game]  ⭐ NUEVO      │
│  [Multiplayer Remote] (Disabled)│
│  [Statistics]     ⭐ NUEVO      │
│                                 │
│  "You have a saved game!"       │
└─────────────────────────────────┘
```

### Pantalla de Juego
```
┌─────────────────────────────────┐
│ [Menu]  Turn: White  [Reset]    │
│         [Save Game] ⭐ NUEVO    │
│                                 │
│     ♜ ♞ ♝ ♛ ♚ ♝ ♞ ♜           │
│     ♟ ♟ ♟ ♟ ♟ ♟ ♟ ♟           │
│     · · · · · · · ·           │
│     · · · · · · · ·           │
│     · · · · · · · ·           │
│     · · · · · · · ·           │
│     ♙ ♙ ♙ ♙ ♙ ♙ ♙ ♙           │
│     ♖ ♘ ♗ ♕ ♔ ♗ ♘ ♖           │
└─────────────────────────────────┘
```

### Pantalla de Estadísticas
```
┌─────────────────────────────────┐
│ [Back]   Statistics    [Reset]  │
│─────────────────────────────────│
│                                 │
│  General Statistics             │
│  ├─ Total Games: 10             │
│  ├─ Games Won as White: 6       │
│  ├─ Games Won as Black: 3       │
│  └─ Games Drawn: 1              │
│                                 │
│  Win Rates                      │
│  ├─ White: 60% ████████░░       │
│  └─ Black: 30% ██████░░░░       │
│                                 │
│  Time Statistics                │
│  ├─ Total Play Time: 2h 30m     │
│  ├─ Average Game: 15m           │
│  ├─ Longest: 28m 15s            │
│  └─ Shortest: 8m 42s            │
└─────────────────────────────────┘
```

---

## 🔧 Solución de Problemas

### No puedo continuar mi partida
- **Problema**: El botón "Continue Game" está deshabilitado
- **Solución**: No hay partida guardada. Inicia una nueva y guárdala primero.

### Las estadísticas no se actualizan
- **Problema**: Los números no cambian después de jugar
- **Solución**: Las estadísticas solo se actualizan cuando la partida termina (Jaque Mate o Tablas)

### Perdí mi partida guardada
- **Problema**: La partida guardada desapareció
- **Solución**: Las partidas se eliminan automáticamente cuando terminan. Si quieres guardarla, presiona "Save Game" durante el juego.

### Quiero múltiples partidas guardadas
- **Limitación actual**: Solo se puede guardar 1 partida a la vez
- **Solución temporal**: Completa la partida actual antes de guardar otra

---

## 💡 Consejos y Trucos

### Para Guardar Efectivamente
- 🎯 Guarda antes de movimientos críticos
- 🎯 Guarda si necesitas pensar una jugada compleja
- 🎯 Guarda si vas a cerrar la app temporalmente

### Para Mejorar tus Estadísticas
- 📈 Juega regularmente para ver tendencias
- 📈 Observa si ganas más con blancas o negras
- 📈 Intenta reducir el tiempo promedio de partida
- 📈 Compara tus récords con amigos

### Gestión de Datos
- 💾 Las estadísticas se guardan automáticamente
- 💾 No necesitas conexión a internet
- 💾 Los datos persisten entre reinicios
- 💾 Solo tú puedes ver tus estadísticas

---

## 📋 Resumen de Botones y Acciones

| Pantalla | Botón | Acción |
|----------|-------|--------|
| Menú | Continue Game | Carga partida guardada |
| Menú | Statistics | Muestra estadísticas |
| Juego | Save Game | Guarda partida actual |
| Juego | Menu | Vuelve al menú (sin guardar) |
| Juego | Reset | Reinicia el tablero |
| Estadísticas | Back | Vuelve al menú |
| Estadísticas | Reset | Borra todas las estadísticas |

---

## 🆘 Ayuda Adicional

Para más información detallada:
- Ver `NUEVAS_FUNCIONALIDADES.md` - Documentación técnica completa
- Ver `ESTRUCTURA_PROYECTO.md` - Arquitectura del sistema
- Ver `README.md` - Guía general del proyecto

---

**¡Disfruta jugando ajedrez con las nuevas funcionalidades! ♟️🎮**
