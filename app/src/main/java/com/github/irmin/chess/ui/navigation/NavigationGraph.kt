package com.github.irmin.chess.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.irmin.chess.ui.screens.GameScreen
import com.github.irmin.chess.ui.screens.MenuScreen
import com.github.irmin.chess.ui.screens.StatisticsScreen
import com.github.irmin.chess.viewmodel.ChessViewModel
import androidx.compose.runtime.collectAsState

enum class Screen {
    MENU,
    GAME,
    STATISTICS
}

@Composable
fun ChessApp(viewModel: ChessViewModel = viewModel()) {
    val uiState = viewModel.uiState.collectAsState().value
    
    val currentScreen = when {
        uiState.showStatistics -> Screen.STATISTICS
        uiState.isGameActive -> Screen.GAME
        else -> Screen.MENU
    }

    when (currentScreen) {
        Screen.MENU -> {
            MenuScreen(
                onSinglePlayerClick = { /* Not implemented yet */ },
                onMultiplayerLocalClick = { viewModel.startGame() },
                onMultiplayerRemoteClick = { /* Not implemented yet */ },
                onContinueGameClick = { viewModel.continueGame() },
                onStatisticsClick = { viewModel.showStatistics() },
                hasSavedGame = uiState.hasSavedGame
            )
        }
        Screen.GAME -> {
            GameScreen(
                viewModel = viewModel,
                onBackToMenu = { viewModel.backToMenu() }
            )
        }
        Screen.STATISTICS -> {
            StatisticsScreen(
                statistics = uiState.statistics,
                onBackClick = { viewModel.hideStatistics() },
                onResetClick = { viewModel.resetStatistics() }
            )
        }
    }
}
