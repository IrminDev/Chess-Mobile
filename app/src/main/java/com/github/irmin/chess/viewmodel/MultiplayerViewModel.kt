package com.github.irmin.chess.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.irmin.chess.bluetooth.*
import com.github.irmin.chess.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para manejar el juego multijugador por Bluetooth
 */
class MultiplayerViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothManager = BluetoothManager(application)
    
    private val _uiState = MutableStateFlow(MultiplayerUiState())
    val uiState: StateFlow<MultiplayerUiState> = _uiState.asStateFlow()
    
    private var myColor: PieceColor? = null
    
    init {
        // Observar estado de conexión
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
                
                when (state) {
                    is ConnectionState.Connected -> {
                        // Si somos el servidor (Host), jugamos con blancas
                        if (_uiState.value.isHost) {
                            myColor = PieceColor.WHITE
                            _uiState.update { it.copy(
                                gameStarted = true,
                                myTurn = true
                            )}
                        } else {
                            // Si somos el cliente, jugamos con negras
                            myColor = PieceColor.BLACK
                            _uiState.update { it.copy(
                                gameStarted = true,
                                myTurn = false
                            )}
                        }
                    }
                    is ConnectionState.Disconnected -> {
                        _uiState.update { it.copy(
                            gameStarted = false,
                            errorMessage = "Desconectado"
                        )}
                    }
                    is ConnectionState.Error -> {
                        _uiState.update { it.copy(
                            errorMessage = state.message
                        )}
                    }
                    else -> {}
                }
            }
        }
        
        // Observar mensajes recibidos
        viewModelScope.launch {
            bluetoothManager.receivedMessage.collect { message ->
                message?.let {
                    handleReceivedMove(it)
                }
            }
        }
        
        // Observar dispositivos emparejados
        viewModelScope.launch {
            bluetoothManager.pairedDevices.collect { devices ->
                _uiState.update { it.copy(availableDevices = devices) }
            }
        }
    }
    
    /**
     * Inicia el modo host (servidor)
     */
    fun startAsHost() {
        if (!bluetoothManager.isBluetoothAvailable()) {
            _uiState.update { it.copy(errorMessage = "Bluetooth no disponible") }
            return
        }
        
        if (!bluetoothManager.isBluetoothEnabled()) {
            _uiState.update { it.copy(errorMessage = "Por favor, habilita Bluetooth") }
            return
        }
        
        _uiState.update { it.copy(isHost = true) }
        bluetoothManager.startServer()
    }
    
    /**
     * Obtiene la lista de dispositivos emparejados
     */
    fun getAvailableDevices() {
        bluetoothManager.getPairedDevices()
    }
    
    /**
     * Conecta a un dispositivo como cliente
     */
    fun connectToDevice(device: BluetoothDevice) {
        _uiState.update { it.copy(isHost = false) }
        bluetoothManager.connectToDevice(device)
    }
    
    /**
     * Inicia una nueva partida
     */
    fun startNewGame() {
        _uiState.update { it.copy(
            board = ChessBoard(),
            selectedPosition = null,
            validMoves = emptyList(),
            gameStarted = true,
            myTurn = myColor == PieceColor.WHITE
        )}
    }
    
    /**
     * Maneja el clic en una casilla del tablero
     */
    fun onSquareClicked(position: Position) {
        val currentState = _uiState.value
        
        // Solo permitir movimientos si es nuestro turno
        if (!currentState.myTurn || !currentState.gameStarted) return
        
        val board = currentState.board
        val selectedPos = currentState.selectedPosition
        
        if (selectedPos == null) {
            // Seleccionar pieza
            val piece = board.getPiece(position)
            if (piece != null && piece.color == myColor) {
                _uiState.update { it.copy(
                    selectedPosition = position,
                    validMoves = board.getValidMoves(position)
                )}
            }
        } else {
            // Intentar hacer el movimiento
            if (position == selectedPos) {
                // Deseleccionar
                _uiState.update { it.copy(
                    selectedPosition = null,
                    validMoves = emptyList()
                )}
            } else if (board.makeMove(selectedPos, position)) {
                // Movimiento exitoso
                _uiState.update { it.copy(
                    selectedPosition = null,
                    validMoves = emptyList(),
                    myTurn = false
                )}
                
                // Enviar movimiento por Bluetooth
                sendMove(selectedPos, position, board)
                
                // Verificar si el juego terminó
                checkGameEnd(board)
            }
        }
    }
    
    /**
     * Envía un movimiento por Bluetooth
     */
    private fun sendMove(from: Position, to: Position, board: ChessBoard) {
        viewModelScope.launch {
            try {
                val xml = BluetoothGameState.serializeMove(from, to, board)
                bluetoothManager.sendMessage(xml)
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    errorMessage = "Error al enviar movimiento: ${e.message}"
                )}
            }
        }
    }
    
    /**
     * Maneja un movimiento recibido por Bluetooth
     */
    private fun handleReceivedMove(xml: String) {
        viewModelScope.launch {
            try {
                val moveData = BluetoothGameState.deserializeMove(xml)
                if (moveData != null) {
                    // Aplicar el movimiento al tablero
                    val board = _uiState.value.board
                    
                    // Reconstruir el tablero desde los datos recibidos
                    val newBoard = ChessBoard()
                    // Limpiar el tablero
                    for (row in 0..7) {
                        for (col in 0..7) {
                            newBoard.setPieceForMultiplayer(Position(row, col), null)
                        }
                    }
                    
                    // Colocar las piezas recibidas
                    for (pieceData in moveData.boardPieces) {
                        val piece = ChessPiece(pieceData.type, pieceData.color)
                        piece.setHasMoved(pieceData.hasMoved)
                        newBoard.setPieceForMultiplayer(
                            Position(pieceData.row, pieceData.col),
                            piece
                        )
                    }
                    
                    // Actualizar el turno y estado del juego
                    newBoard.setCurrentTurn(moveData.turn)
                    newBoard.setGameState(moveData.gameState)
                    
                    _uiState.update { it.copy(
                        board = newBoard,
                        myTurn = true,
                        selectedPosition = null,
                        validMoves = emptyList()
                    )}
                    
                    // Verificar si el juego terminó
                    checkGameEnd(newBoard)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    errorMessage = "Error al procesar movimiento: ${e.message}"
                )}
            }
        }
    }
    
    /**
     * Verifica si el juego ha terminado
     */
    private fun checkGameEnd(board: ChessBoard) {
        when (board.gameState) {
            GameState.CHECKMATE -> {
                val winner = if (board.currentTurn == PieceColor.WHITE) "Negras" else "Blancas"
                _uiState.update { it.copy(
                    gameStarted = false,
                    gameEndMessage = "¡Jaque mate! Ganan las $winner"
                )}
            }
            GameState.STALEMATE -> {
                _uiState.update { it.copy(
                    gameStarted = false,
                    gameEndMessage = "¡Empate por ahogado!"
                )}
            }
            else -> {}
        }
    }
    
    /**
     * Desconecta del juego
     */
    fun disconnect() {
        bluetoothManager.disconnect()
        _uiState.update { it.copy(
            gameStarted = false,
            selectedPosition = null,
            validMoves = emptyList(),
            connectionState = ConnectionState.Disconnected
        )}
    }
    
    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Limpia el mensaje de fin de juego
     */
    fun clearGameEndMessage() {
        _uiState.update { it.copy(gameEndMessage = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothManager.disconnect()
    }
}

/**
 * Estado de la UI para el modo multijugador
 */
data class MultiplayerUiState(
    val board: ChessBoard = ChessBoard(),
    val selectedPosition: Position? = null,
    val validMoves: List<Position> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val availableDevices: List<BluetoothDevice> = emptyList(),
    val isHost: Boolean = false,
    val gameStarted: Boolean = false,
    val myTurn: Boolean = false,
    val errorMessage: String? = null,
    val gameEndMessage: String? = null
)
