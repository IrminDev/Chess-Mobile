package com.github.irmin.chess.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.irmin.chess.ui.screens.*
import com.github.irmin.chess.viewmodel.ChessViewModel
import com.github.irmin.chess.viewmodel.GameMode
import com.github.irmin.chess.viewmodel.MultiplayerViewModel
import com.github.irmin.chess.viewmodel.ThemeViewModel

enum class Screen {
    MENU,
    GAME,
    STATISTICS,
    MULTIPLAYER_SETUP,
    MULTIPLAYER_GAME
}

private const val TAG = "NavigationGraph"

@Composable
fun ChessApp(
    chessViewModel: ChessViewModel = viewModel(),
    multiplayerViewModel: MultiplayerViewModel = viewModel(),
    themeViewModel: ThemeViewModel
) {
    val chessUiState = chessViewModel.uiState.collectAsState().value
    val multiplayerUiState = multiplayerViewModel.uiState.collectAsState().value

    val currentScreen = when {
        chessUiState.showStatistics -> {
            Log.d(TAG, "Current screen: STATISTICS")
            Screen.STATISTICS
        }
        chessUiState.isGameActive -> {
            Log.d(TAG, "Current screen: GAME (local/AI)")
            Screen.GAME
        }
        multiplayerUiState.gameStarted -> {
            Log.d(TAG, "Current screen: MULTIPLAYER_GAME")
            Screen.MULTIPLAYER_GAME
        }
        multiplayerUiState.showSetupScreen -> {
            Log.d(TAG, "Current screen: MULTIPLAYER_SETUP")
            Screen.MULTIPLAYER_SETUP
        }
        else -> {
            Log.d(TAG, "Current screen: MENU")
            Screen.MENU
        }
    }

    when (currentScreen) {
        Screen.MENU -> {
            val currentTheme = themeViewModel.currentTheme.collectAsState().value
            MenuScreen(
                onSinglePlayerClick = { difficulty ->
                    Log.d(TAG, "Menu: Single player clicked with difficulty $difficulty")
                    chessViewModel.startGame(GameMode.SINGLE_PLAYER, difficulty)
                },
                onMultiplayerLocalClick = {
                    Log.d(TAG, "Menu: Multiplayer local clicked")
                    chessViewModel.startGame(GameMode.LOCAL_MULTIPLAYER)
                },
                onMultiplayerRemoteClick = {
                    Log.d(TAG, "Menu: Multiplayer Bluetooth clicked")
                    multiplayerViewModel.startMultiplayerMode()
                },
                onContinueGameClick = {
                    Log.d(TAG, "Menu: Continue game clicked")
                    chessViewModel.continueGame()
                },
                onStatisticsClick = {
                    Log.d(TAG, "Menu: Statistics clicked")
                    chessViewModel.showStatistics()
                },
                hasSavedGame = chessUiState.hasSavedGame,
                currentTheme = currentTheme,
                onThemeChange = {
                    Log.d(TAG, "Menu: Theme change clicked")
                    themeViewModel.toggleTheme()
                }
            )
        }
        Screen.GAME -> {
            GameScreen(
                viewModel = chessViewModel,
                onBackToMenu = {
                    Log.d(TAG, "Game: Back to menu clicked")
                    chessViewModel.backToMenu()
                }
            )
        }
        Screen.STATISTICS -> {
            StatisticsScreen(
                statistics = chessUiState.statistics,
                onBackClick = {
                    Log.d(TAG, "Statistics: Back clicked")
                    chessViewModel.hideStatistics()
                },
                onResetClick = {
                    Log.d(TAG, "Statistics: Reset clicked")
                    chessViewModel.resetStatistics()
                }
            )
        }
        Screen.MULTIPLAYER_SETUP -> {
            Log.d(TAG, "Rendering MULTIPLAYER_SETUP screen")
            MultiplayerSetupScreen(
                uiState = multiplayerUiState,
                onStartAsHost = {
                    Log.d(TAG, "Setup: Start as host clicked")
                    multiplayerViewModel.startAsHost()
                },
                onConnectToDevice = { device ->
                    Log.d(TAG, "Setup: Connect to device clicked")
                    multiplayerViewModel.connectToDevice(device)
                },
                onRefreshDevices = {
                    Log.d(TAG, "Setup: Refresh devices clicked")
                    multiplayerViewModel.getAvailableDevices()
                },
                onNavigateToGame = { /* La navegación se maneja automáticamente */ },
                onBack = {
                    Log.d(TAG, "Setup: Back clicked")
                    multiplayerViewModel.disconnect()
                }
            )
        }
        Screen.MULTIPLAYER_GAME -> {
            Log.d(TAG, "Rendering MULTIPLAYER_GAME screen")
            MultiplayerGameScreen(
                uiState = multiplayerUiState,
                onSquareClick = { position ->
                    multiplayerViewModel.onSquareClicked(position)
                },
                onDisconnect = {
                    Log.d(TAG, "Game: Disconnect clicked")
                    multiplayerViewModel.disconnect()
                },
                onBack = {
                    Log.d(TAG, "Game: Back clicked")
                    multiplayerViewModel.disconnect()
                }
            )
        }
    }
}
