package com.github.irmin.chess.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.irmin.chess.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChessUiState(
    val board: ChessBoard = ChessBoard(),
    val selectedPosition: Position? = null,
    val validMoves: List<Position> = emptyList(),
    val isGameActive: Boolean = false
)

class ChessViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    fun startGame() {
        viewModelScope.launch {
            _uiState.value = ChessUiState(
                board = ChessBoard(),
                isGameActive = true
            )
        }
    }

    fun onSquareClicked(position: Position) {
        val currentState = _uiState.value
        if (!currentState.isGameActive) return

        val board = currentState.board
        val selectedPos = currentState.selectedPosition

        if (selectedPos == null) {
            // Select a piece
            val piece = board.getPiece(position)
            if (piece != null && piece.color == board.currentTurn) {
                _uiState.value = currentState.copy(
                    selectedPosition = position,
                    validMoves = board.getValidMoves(position)
                )
            }
        } else {
            // Try to move the piece
            if (position == selectedPos) {
                // Deselect
                _uiState.value = currentState.copy(
                    selectedPosition = null,
                    validMoves = emptyList()
                )
            } else if (board.makeMove(selectedPos, position)) {
                // Move successful
                _uiState.value = currentState.copy(
                    selectedPosition = null,
                    validMoves = emptyList()
                )
            } else {
                // Invalid move, try to select different piece
                val piece = board.getPiece(position)
                if (piece != null && piece.color == board.currentTurn) {
                    _uiState.value = currentState.copy(
                        selectedPosition = position,
                        validMoves = board.getValidMoves(position)
                    )
                } else {
                    _uiState.value = currentState.copy(
                        selectedPosition = null,
                        validMoves = emptyList()
                    )
                }
            }
        }
    }

    fun resetGame() {
        viewModelScope.launch {
            val board = _uiState.value.board
            board.reset()
            _uiState.value = ChessUiState(
                board = board,
                isGameActive = true
            )
        }
    }

    fun backToMenu() {
        viewModelScope.launch {
            _uiState.value = ChessUiState(isGameActive = false)
        }
    }
}
