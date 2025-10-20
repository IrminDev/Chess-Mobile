package com.github.irmin.chess.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.irmin.chess.ui.screens.GameScreen
import com.github.irmin.chess.ui.screens.MenuScreen
import com.github.irmin.chess.viewmodel.ChessViewModel
import androidx.compose.runtime.collectAsState

enum class Screen {
    MENU,
    GAME
}

@Composable
fun ChessApp() {
    val chessViewModel: ChessViewModel = viewModel()
    val currentScreen = if (chessViewModel.uiState.collectAsState().value.isGameActive) Screen.GAME else Screen.MENU

    when (currentScreen) {
        Screen.MENU -> {
            MenuScreen(
                onSinglePlayerClick = { /* Not implemented yet */ },
                onMultiplayerLocalClick = { chessViewModel.startGame() },
                onMultiplayerRemoteClick = { /* Not implemented yet */ }
            )
        }
        Screen.GAME -> {
            GameScreen(
                viewModel = chessViewModel,
                onBackToMenu = { chessViewModel.backToMenu() }
            )
        }
    }
}
