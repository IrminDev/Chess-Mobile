package com.github.irmin.chess.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.irmin.chess.model.*
import com.github.irmin.chess.data.*
import com.github.irmin.chess.ai.ChessAI
import com.github.irmin.chess.ai.AIDifficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class GameMode {
    LOCAL_MULTIPLAYER,
    SINGLE_PLAYER
}

data class ChessUiState(
    val board: ChessBoard = ChessBoard(),
    val selectedPosition: Position? = null,
    val validMoves: List<Position> = emptyList(),
    val isGameActive: Boolean = false,
    val hasSavedGame: Boolean = false,
    val statistics: GameStatistics? = null,
    val showStatistics: Boolean = false,
    val gameMode: GameMode = GameMode.LOCAL_MULTIPLAYER,
    val isAIThinking: Boolean = false,
    val aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()
    
    private val xmlManager = GameStateXmlManager(application)
    private val database = ChessDatabase.getDatabase(application)
    private val repository = GameRepository(database.gameStatisticsDao())
    
    private var gameStartTime: Long = 0L
    private var chessAI: ChessAI? = null

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

    fun startGame(gameMode: GameMode = GameMode.LOCAL_MULTIPLAYER, difficulty: AIDifficulty = AIDifficulty.MEDIUM) {
        viewModelScope.launch {
            gameStartTime = System.currentTimeMillis()

            // Inicializar IA si es modo Single Player
            chessAI = if (gameMode == GameMode.SINGLE_PLAYER) {
                ChessAI(difficulty)
            } else {
                null
            }

            _uiState.value = ChessUiState(
                board = ChessBoard(),
                isGameActive = true,
                statistics = _uiState.value.statistics,
                gameMode = gameMode,
                aiDifficulty = difficulty
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
        if (!currentState.isGameActive || currentState.isAIThinking) return

        // En modo Single Player, solo permitir al jugador humano (blancas) hacer movimientos
        if (currentState.gameMode == GameMode.SINGLE_PLAYER &&
            currentState.board.currentTurn == PieceColor.BLACK) {
            return
        }

        val board = currentState.board
        val selectedPos = currentState.selectedPosition

        if (selectedPos == null) {
            val piece = board.getPiece(position)
            if (piece != null && piece.color == board.currentTurn) {
                _uiState.value = currentState.copy(
                    selectedPosition = position,
                    validMoves = board.getValidMoves(position)
                )
            }
        } else {
            if (position == selectedPos) {
                _uiState.value = currentState.copy(
                    selectedPosition = null,
                    validMoves = emptyList()
                )
            } else if (board.makeMove(selectedPos, position)) {
                _uiState.value = currentState.copy(
                    selectedPosition = null,
                    validMoves = emptyList()
                )
                
                // Check if game ended
                if (board.gameState == GameState.CHECKMATE) {
                    endGame()
                } else if (board.gameState == GameState.STALEMATE) {
                    endGame()
                } else if (currentState.gameMode == GameMode.SINGLE_PLAYER) {
                    // Turno de la IA
                    makeAIMove()
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
    
    /**
     * Hace que la IA realice un movimiento
     */
    private fun makeAIMove() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val ai = chessAI ?: return@launch

            // Indicar que la IA está pensando
            _uiState.value = currentState.copy(isAIThinking = true)

            // Pequeño delay para que la UI se actualice y se vea más natural
            delay(500)

            // Calcular el mejor movimiento
            val bestMove = ai.getBestMove(currentState.board)

            if (bestMove != null) {
                // Realizar el movimiento
                currentState.board.makeMove(bestMove.from, bestMove.to)

                // Actualizar el estado
                _uiState.value = currentState.copy(isAIThinking = false)

                // Verificar si el juego terminó
                if (currentState.board.gameState == GameState.CHECKMATE) {
                    endGame()
                } else if (currentState.board.gameState == GameState.STALEMATE) {
                    endGame()
                }
            } else {
                // Si no hay movimientos válidos (no debería pasar)
                _uiState.value = currentState.copy(isAIThinking = false)
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
                   val winner = if (board.currentTurn == PieceColor.WHITE) "black" else "white"
                    repository.updateGameWon(winner, gameTime, movesCount)
                }
                GameState.STALEMATE -> {
                    repository.updateGameWon("draw", gameTime, movesCount)
                }
                else -> {}
            }
            
            xmlManager.deleteSavedGame()
            _uiState.value = _uiState.value.copy(hasSavedGame = false)
        }
    }

    fun resetGame() {
        viewModelScope.launch {
            val currentMode = _uiState.value.gameMode
            val currentDifficulty = _uiState.value.aiDifficulty
            val board = _uiState.value.board
            board.reset()
            gameStartTime = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(
                board = board,
                isGameActive = true,
                selectedPosition = null,
                validMoves = emptyList(),
                gameMode = currentMode,
                aiDifficulty = currentDifficulty
            )
        }
    }

    fun backToMenu() {
        viewModelScope.launch {
            chessAI = null
            _uiState.value = _uiState.value.copy(
                isGameActive = false,
                showStatistics = false,
                gameMode = GameMode.LOCAL_MULTIPLAYER,
                isAIThinking = false
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
