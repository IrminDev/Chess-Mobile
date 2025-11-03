# Chess Mobile

A fully functional chess game application for Android, built with Kotlin and Jetpack Compose. This application implements all standard chess rules including special moves, check detection, and game state management.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Screenshots](#screenshots)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Requirements](#requirements)
- [Installation](#installation)
- [Project Structure](#project-structure)
- [Game Rules Implementation](#game-rules-implementation)
- [Contributing](#contributing)
- [License](#license)

## Overview

Chess Mobile is a native Android application that provides a complete chess gaming experience. The application allows two players to play chess locally on the same device, with a clean and intuitive interface built using Jetpack Compose's modern UI toolkit.

## Features

- **Complete Chess Rules Implementation**
  - All standard piece movements (Pawn, Rook, Knight, Bishop, Queen, King)
  - Special moves: Castling, En Passant, Pawn Promotion
  - Move validation and legal move highlighting
  - Check and checkmate detection
  - Stalemate detection

- **Game Save/Load System** ⭐ NEW
  - Save game state to XML file
  - Continue games from where you left off
  - Automatic save on game end
  - Manual save during gameplay
  - Preserves complete board state, turn, and move history

- **Statistics Tracking** ⭐ NEW
  - Track wins as White and Black
  - Record total play time
  - Monitor game durations (longest, shortest, average)
  - View win rates and game statistics
  - Persistent data storage using Room database
  - Reset statistics option

- **User Interface**
  - Modern Material Design 3 interface
  - Interactive chessboard with visual feedback
  - Move highlighting for selected pieces
  - Current turn indicator
  - Game state notifications (Check, Checkmate, Stalemate)
  - Game reset functionality
  - Statistics viewer with detailed insights

- **Game Management**
  - Local multiplayer (hot-seat mode)
  - Move history tracking
  - Complete game state management
  - Turn-based gameplay
  - Save/Load functionality
  - Comprehensive statistics

## Screenshots

### Main Menu
The main menu screen showing game mode options.

![Main Menu](screenshots/menu.png)

### Game Board
The chess board during gameplay with pieces in their initial positions.

![Game Board](screenshots/game_board.png)

### Active Game
An active game with a piece selected and valid moves highlighted.

![Active Game](screenshots/active_game.png)

### Check State
The check notification when a king is in check.

![Check State](screenshots/check_state.png)

### Checkmate
The checkmate notification and game end state.

![Checkmate](screenshots/checkmate.png)

## Architecture

The application follows the **MVVM (Model-View-ViewModel)** architecture pattern with clean separation of concerns:

### Layers

```
┌─────────────────────────────────────────┐
│           UI Layer (Compose)            │
│   - GameScreen, MenuScreen              │
│   - StatisticsScreen                    │
│   - Composable components               │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         ViewModel Layer                 │
│   - ChessViewModel                      │
│   - UI State Management                 │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│       Data & Model Layer                │
│   - ChessBoard (Game Logic)             │
│   - GameStateXmlManager (Save/Load)     │
│   - GameRepository (Statistics)         │
│   - Room Database                       │
└─────────────────────────────────────────┘
```

### Key Components

- **Data Layer**: ⭐ NEW - Persistence and statistics
  - `GameStateXmlManager`: XML serialization for game state
  - `ChessDatabase`: Room database for statistics
  - `GameRepository`: Data access abstraction
  - `GameStatistics`: Statistics entity

- **Model Layer**: Contains the core game logic and data structures
  - `ChessBoard`: Main game logic, move validation, check detection
  - `ChessPiece`: Piece representation with type, color, and state
  - `Position`: Board position utilities
  - `GameState`, `PieceType`, `PieceColor`: Enums for game state

- **ViewModel Layer**: Manages UI state and user interactions
  - `ChessViewModel`: Handles game state, move processing, save/load, statistics
  - `ChessUiState`: Immutable state representation for the UI

- **UI Layer**: Jetpack Compose-based interface
  - `GameScreen`: Main game board and controls
  - `MenuScreen`: Game mode selection, continue game, statistics
  - `StatisticsScreen`: ⭐ NEW - View game statistics
  - `NavigationGraph`: App navigation management

## Technologies

### Core Technologies

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Kotlin Coroutines and Flow
- **Build System**: Gradle with Kotlin DSL

### Dependencies

```kotlin
- androidx.core:core-ktx:1.17.0
- androidx.lifecycle:lifecycle-runtime-ktx:2.9.4
- androidx.activity:activity-compose:1.11.0
- androidx.compose:compose-bom:2024.09.00
- androidx.compose.material3:material3
- androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4
- androidx.room:room-runtime:2.6.1                 ⭐ NEW - androidx.room:room-ktx:2.6.1                     ⭐ NEW
```

### Development Tools

- Android Gradle Plugin: 8.13.0
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 36
- Compile SDK: 36
- Java Version: 11
- KSP: 2.0.21-1.0.28 ⭐ NEW (for Room annotation processing)

## Requirements

- Android Studio Ladybug | 2024.2.1 or newer
- JDK 11 or higher
- Android SDK with minimum API level 24
- Kotlin 2.0.21 or compatible version

## Installation

### Clone the Repository

```bash
git clone https://github.com/IrminDev/Chess-Mobile.git
cd Chess-Mobile
```

### Open in Android Studio

1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned repository folder
4. Wait for Gradle sync to complete

### Build and Run

#### Using Android Studio

1. Connect an Android device or start an emulator
2. Click the "Run" button or press `Shift + F10`
3. Select your target device

#### Using Command Line

```bash
# Build the project
./gradlew build

# Install on connected device
./gradlew installDebug

# Build and install in one step
./gradlew installDebug
```

### Generate APK

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing configuration)
./gradlew assembleRelease
```

The generated APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
Chess-Mobile/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/github/irmin/chess/
│   │   │   │   ├── data/                          ⭐ NEW
│   │   │   │   │   ├── ChessDatabase.kt          # Room database
│   │   │   │   │   ├── GameRepository.kt         # Data repository
│   │   │   │   │   ├── GameStatistics.kt         # Statistics entity
│   │   │   │   │   ├── GameStatisticsDao.kt      # Room DAO
│   │   │   │   │   └── GameStateXmlManager.kt    # XML save/load
│   │   │   │   ├── model/
│   │   │   │   │   ├── ChessBoard.kt             # Core game logic
│   │   │   │   │   ├── ChessPiece.kt             # Piece representation
│   │   │   │   │   ├── Position.kt               # Board positions
│   │   │   │   │   ├── Move.kt                   # Move records
│   │   │   │   │   ├── PieceType.kt              # Piece types enum
│   │   │   │   │   ├── PieceColor.kt             # Color enum
│   │   │   │   │   └── GameState.kt              # Game state enum
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── ChessViewModel.kt         # Game state management
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── GameScreen.kt         # Game UI
│   │   │   │   │   │   ├── MenuScreen.kt         # Menu UI
│   │   │   │   │   │   └── StatisticsScreen.kt   ⭐ NEW
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── NavigationGraph.kt
│   │   │   │   │   └── theme/                    # App theming
│   │   │   │   └── MainActivity.kt               # Entry point
│   │   │   ├── res/
│   │   │   │   ├── drawable/                     # Piece images
│   │   │   │   └── values/                       # Strings, colors, themes
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                                 # Unit tests
│   │   └── androidTest/                          # Instrumented tests
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                        # Dependency versions
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
├── NUEVAS_FUNCIONALIDADES.md                     ⭐ NEW - Detailed documentation
└── ESTRUCTURA_PROYECTO.md                        ⭐ NEW - Project structure guide
```

## New Features ⭐

### Game Save/Load System

The application now supports saving and loading game states using XML serialization:

- **Save Game**: Click "Save Game" button during play to save current state
- **Continue Game**: Resume from last saved position via "Continue Game" button
- **Auto-save**: Games are automatically deleted when completed
- **XML Format**: Human-readable XML file stores complete board state

**What gets saved:**
- All piece positions and states
- Current turn (White/Black)
- Game state (Playing, Check, Checkmate, Stalemate)
- Last move (for En Passant validation)
- Game start time
- Whether pieces have moved (for Castling)

### Statistics System

Comprehensive statistics tracking using Room database:

**Tracked Statistics:**
- Games won as White
- Games won as Black
- Games drawn
- Total play time
- Longest game duration
- Shortest game duration
- Average game time
- Total moves played
- Win rates for each color
- Last played timestamp

**Statistics Features:**
- Persistent storage across app sessions
- Real-time updates after each game
- Visual progress bars for win rates
- Formatted time displays
- Reset option to clear all statistics
- Automatic calculation of derived metrics

### How to Use

1. **Start New Game**: Click "Multiplayer Local" from menu
2. **Play**: Make moves on the board
3. **Save**: Click "Save Game" to save progress
4. **Continue**: Next time, click "Continue Game" to resume
5. **View Stats**: Click "Statistics" to see your performance
6. **Reset Stats**: Use "Reset" button in statistics screen to clear data

For detailed documentation, see [NUEVAS_FUNCIONALIDADES.md](NUEVAS_FUNCIONALIDADES.md) and [ESTRUCTURA_PROYECTO.md](ESTRUCTURA_PROYECTO.md).

## Game Rules Implementation

### Standard Moves

All chess pieces follow their traditional movement patterns:

- **Pawn**: Moves forward one square, two squares on first move, captures diagonally
- **Rook**: Moves horizontally or vertically any number of squares
- **Knight**: Moves in an L-shape (2 squares in one direction, 1 in perpendicular)
- **Bishop**: Moves diagonally any number of squares
- **Queen**: Combines rook and bishop movements
- **King**: Moves one square in any direction

### Special Moves

- **Castling**: King and rook swap positions under specific conditions
- **En Passant**: Special pawn capture move
- **Pawn Promotion**: Pawns reaching the opposite end can promote to Queen, Rook, Bishop, or Knight

### Game State Detection

- **Check**: Detects when a king is under attack
- **Checkmate**: Identifies when a player has no legal moves to escape check
- **Stalemate**: Recognizes when a player has no legal moves but is not in check

### Move Validation

The application validates all moves to ensure:
- Pieces move according to their rules
- Moves don't leave the king in check
- Path is clear for sliding pieces (Rook, Bishop, Queen)
- Special move conditions are met

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Write unit tests for new features

## License

This project is available for educational and personal use. Please check with the repository owner for commercial use permissions.

---

**Authors**: 
- IrminDev
- AngelHernand

**Repository**: [https://github.com/IrminDev/Chess-Mobile](https://github.com/IrminDev/Chess-Mobile)  
**Version**: 1.0
