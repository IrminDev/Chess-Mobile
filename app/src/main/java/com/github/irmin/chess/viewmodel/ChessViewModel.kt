package com.github.irmin.chess.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.irmin.chess.model.*
import com.github.irmin.chess.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChessUiState(
    val board: ChessBoard = ChessBoard(),
    val selectedPosition: Position? = null,
    val validMoves: List<Position> = emptyList(),
    val isGameActive: Boolean = false,
    val hasSavedGame: Boolean = false,
    val statistics: GameStatistics? = null,
    val showStatistics: Boolean = false
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()
    
    private val xmlManager = GameStateXmlManager(application)
    private val database = ChessDatabase.getDatabase(application)
    private val repository = GameRepository(database.gameStatisticsDao())
    
    private var gameStartTime: Long = 0L
    
    init {
        checkForSavedGame()
        loadStatistics()
    }
    
    private fun checkForSavedGame() {
        viewModelScope.launch {
            val hasSaved = xmlManager.hasSavedGame()
            _uiState.value = _uiState.value.copy(hasSavedGame = hasSaved)
        }
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            repository.statistics.collect { stats ->
                _uiState.value = _uiState.value.copy(statistics = stats)
            }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            gameStartTime = System.currentTimeMillis()
            _uiState.value = ChessUiState(
                board = ChessBoard(),
                isGameActive = true,
                statistics = _uiState.value.statistics
            )
        }
    }
    
    fun continueGame() {
        viewModelScope.launch {
            val savedState = xmlManager.loadGameState()
            if (savedState != null) {
                val (board, startTime) = savedState
                gameStartTime = startTime
                _uiState.value = _uiState.value.copy(
                    board = board,
                    isGameActive = true,
                    hasSavedGame = false
                )
            }
        }
    }
    
    fun saveGame() {
        viewModelScope.launch {
            val success = xmlManager.saveGameState(_uiState.value.board, gameStartTime)
            if (success) {
                _uiState.value = _uiState.value.copy(hasSavedGame = true)
            }
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
                
                // Check if game ended
                if (board.gameState == GameState.CHECKMATE) {
                    endGame()
                } else if (board.gameState == GameState.STALEMATE) {
                    endGame()
                }
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
    
    private fun endGame() {
        viewModelScope.launch {
            val board = _uiState.value.board
            val gameTime = System.currentTimeMillis() - gameStartTime
            val movesCount = board.getMoveHistorySize()
            
            when (board.gameState) {
                GameState.CHECKMATE -> {
                    // El ganador es el turno opuesto (porque el turno cambió después del jaque mate)
                    val winner = if (board.currentTurn == PieceColor.WHITE) "black" else "white"
                    repository.updateGameWon(winner, gameTime, movesCount)
                }
                GameState.STALEMATE -> {
                    repository.updateGameWon("draw", gameTime, movesCount)
                }
                else -> {}
            }
            
            // Eliminar el juego guardado si existe
            xmlManager.deleteSavedGame()
            _uiState.value = _uiState.value.copy(hasSavedGame = false)
        }
    }

    fun resetGame() {
        viewModelScope.launch {
            val board = _uiState.value.board
            board.reset()
            gameStartTime = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(
                board = board,
                isGameActive = true,
                selectedPosition = null,
                validMoves = emptyList()
            )
        }
    }

    fun backToMenu() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGameActive = false,
                showStatistics = false
            )
            checkForSavedGame()
        }
    }
    
    fun showStatistics() {
        _uiState.value = _uiState.value.copy(showStatistics = true)
    }
    
    fun hideStatistics() {
        _uiState.value = _uiState.value.copy(showStatistics = false)
    }
    
    fun resetStatistics() {
        viewModelScope.launch {
            repository.resetStatistics()
        }
    }
    
    fun deleteSavedGame() {
        viewModelScope.launch {
            xmlManager.deleteSavedGame()
            checkForSavedGame()
        }
    }
}
