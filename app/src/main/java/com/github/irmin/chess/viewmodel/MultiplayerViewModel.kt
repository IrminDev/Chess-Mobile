package com.github.irmin.chess.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
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
    
    companion object {
        private const val TAG = "MultiplayerViewModel"
    }

    init {
        Log.d(TAG, "MultiplayerViewModel initialized")

        // Observar estado de conexión
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { state ->
                Log.d(TAG, "Connection state changed: $state")
                _uiState.update { it.copy(connectionState = state) }
                
                when (state) {
                    is ConnectionState.Connected -> {
                        Log.d(TAG, "Connected! isHost=${_uiState.value.isHost}")
                        // Si somos el servidor (Host), jugamos con blancas
                        if (_uiState.value.isHost) {
                            myColor = PieceColor.WHITE
                            _uiState.update { it.copy(
                                gameStarted = true,
                                myTurn = true
                            )}
                            Log.d(TAG, "Host: Playing as WHITE, my turn")

                            // Enviar handshake inicial como host
                            viewModelScope.launch {
                                try {
                                    // Esperar más tiempo para asegurar que el cliente esté listo
                                    Log.d(TAG, "Waiting 1 second before sending handshake...")
                                    Thread.sleep(1000)
                                    Log.d(TAG, "Sending initial handshake as HOST")
                                    sendHandshake("HOST_READY")

                                    // Enviar un segundo mensaje de confirmación
                                    Thread.sleep(500)
                                    Log.d(TAG, "Sending confirmation handshake")
                                    sendHandshake("HOST_CONFIRM")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error sending initial handshake", e)
                                }
                            }
                        } else {
                            // Si somos el cliente, jugamos con negras
                            myColor = PieceColor.BLACK
                            _uiState.update { it.copy(
                                gameStarted = true,
                                myTurn = false
                            )}
                            Log.d(TAG, "Client: Playing as BLACK, waiting for opponent")

                            // Enviar handshake inicial como cliente
                            viewModelScope.launch {
                                try {
                                    // El cliente espera menos tiempo
                                    Log.d(TAG, "Waiting 500ms before sending handshake...")
                                    Thread.sleep(500)
                                    Log.d(TAG, "Sending initial handshake as CLIENT")
                                    sendHandshake("CLIENT_READY")

                                    // Enviar un segundo mensaje de confirmación
                                    Thread.sleep(500)
                                    Log.d(TAG, "Sending confirmation handshake")
                                    sendHandshake("CLIENT_CONFIRM")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error sending initial handshake", e)
                                }
                            }
                        }
                    }
                    is ConnectionState.Disconnected -> {
                        Log.d(TAG, "Disconnected - gameStarted: ${_uiState.value.gameStarted}")
                        // Solo mostrar error si estábamos jugando (desconexión inesperada)
                        val wasPlaying = _uiState.value.gameStarted
                        _uiState.update { it.copy(
                            gameStarted = false,
                            errorMessage = if (wasPlaying) "Conexión perdida" else null
                        )}
                    }
                    is ConnectionState.Error -> {
                        Log.e(TAG, "Connection error: ${state.message}")
                        _uiState.update { it.copy(
                            errorMessage = state.message
                        )}
                    }
                    is ConnectionState.Listening -> {
                        Log.d(TAG, "Listening for connections...")
                    }
                    is ConnectionState.Connecting -> {
                        Log.d(TAG, "Connecting to device...")
                    }
                }
            }
        }
        
        // Observar mensajes recibidos
        viewModelScope.launch {
            bluetoothManager.receivedMessage.collect { message ->
                message?.let {
                    Log.d(TAG, "Received message: ${it.take(100)}...")
                    handleReceivedMove(it)
                    // Limpiar el mensaje después de procesarlo
                    bluetoothManager.clearReceivedMessage()
                }
            }
        }
        
        // Observar dispositivos emparejados
        viewModelScope.launch {
            bluetoothManager.pairedDevices.collect { devices ->
                Log.d(TAG, "Paired devices updated: ${devices.size} devices")
                _uiState.update { it.copy(availableDevices = devices) }
            }
        }
    }
    
    /**
     * Inicia el modo multijugador (cambia a la pantalla de setup)
     */
    fun startMultiplayerMode() {
        Log.d(TAG, "startMultiplayerMode() called")

        if (!bluetoothManager.isBluetoothAvailable()) {
            Log.e(TAG, "Bluetooth not available")
            _uiState.update { it.copy(
                errorMessage = "Bluetooth no disponible en este dispositivo",
                showSetupScreen = true
            )}
            return
        }

        if (!bluetoothManager.isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth not enabled")
            _uiState.update { it.copy(
                errorMessage = "Por favor, habilita Bluetooth",
                showSetupScreen = true
            )}
            return
        }

        Log.d(TAG, "Bluetooth available and enabled, showing setup screen")
        _uiState.update { it.copy(showSetupScreen = true) }
        getAvailableDevices()
    }

    /**
     * Inicia el modo host (servidor)
     */
    fun startAsHost() {
        Log.d(TAG, "startAsHost() called")

        if (!bluetoothManager.isBluetoothAvailable()) {
            Log.e(TAG, "Bluetooth not available")
            _uiState.update { it.copy(errorMessage = "Bluetooth no disponible") }
            return
        }
        
        if (!bluetoothManager.isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth not enabled")
            _uiState.update { it.copy(errorMessage = "Por favor, habilita Bluetooth") }
            return
        }
        
        Log.d(TAG, "Starting as host (server)")
        _uiState.update { it.copy(isHost = true) }
        bluetoothManager.startServer()
    }
    
    /**
     * Obtiene la lista de dispositivos emparejados
     */
    fun getAvailableDevices() {
        Log.d(TAG, "getAvailableDevices() called")
        bluetoothManager.getPairedDevices()
    }
    
    /**
     * Conecta a un dispositivo como cliente
     */
    fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectToDevice() called")
        try {
            val deviceName = device.name ?: "Unknown"
            Log.d(TAG, "Connecting to device: $deviceName")
            _uiState.update { it.copy(isHost = false) }
            bluetoothManager.connectToDevice(device)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception connecting to device", e)
            _uiState.update { it.copy(errorMessage = "Error de permisos al conectar") }
        }
    }
    
    /**
     * Inicia una nueva partida
     */
    fun startNewGame() {
        Log.d(TAG, "startNewGame() called")
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
        
        Log.d(TAG, "Square clicked: ($position), myTurn=${currentState.myTurn}, gameStarted=${currentState.gameStarted}")

        // Solo permitir movimientos si es nuestro turno
        if (!currentState.myTurn || !currentState.gameStarted) {
            Log.d(TAG, "Not my turn or game not started, ignoring click")
            return
        }

        val board = currentState.board
        val selectedPos = currentState.selectedPosition
        
        if (selectedPos == null) {
            // Seleccionar pieza
            val piece = board.getPiece(position)
            if (piece != null && piece.color == myColor) {
                Log.d(TAG, "Selected piece: ${piece.type} at $position")
                _uiState.update { it.copy(
                    selectedPosition = position,
                    validMoves = board.getValidMoves(position)
                )}
            } else {
                Log.d(TAG, "Cannot select piece at $position (not my piece)")
            }
        } else {
            // Intentar hacer el movimiento
            if (position == selectedPos) {
                Log.d(TAG, "Deselecting piece")
                // Deseleccionar
                _uiState.update { it.copy(
                    selectedPosition = null,
                    validMoves = emptyList()
                )}
            } else if (board.makeMove(selectedPos, position)) {
                Log.d(TAG, "Move made: $selectedPos -> $position")
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
            } else {
                Log.d(TAG, "Invalid move: $selectedPos -> $position")
            }
        }
    }
    
    /**
     * Envía un movimiento por Bluetooth
     */
    private fun sendMove(from: Position, to: Position, board: ChessBoard) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Sending move: $from -> $to")
                val xml = BluetoothGameState.serializeMove(from, to, board)
                Log.d(TAG, "Serialized move XML: ${xml.take(200)}...")
                bluetoothManager.sendMessage(xml)
                Log.d(TAG, "Move sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending move", e)
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
                Log.d(TAG, "Parsing received move...")
                val moveData = BluetoothGameState.deserializeMove(xml)

                if (moveData == null) {
                    // No es un error, puede ser un handshake u otro tipo de mensaje
                    Log.d(TAG, "Message was not a chess move (possibly a handshake)")
                    return@launch
                }

                Log.d(TAG, "Move parsed: ${moveData.from} -> ${moveData.to}")

                // Aplicar el movimiento al tablero
                val newBoard = ChessBoard()

                // Limpiar el tablero
                for (row in 0..7) {
                    for (col in 0..7) {
                        newBoard.setPieceForMultiplayer(Position(row, col), null)
                    }
                }

                // Colocar las piezas recibidas
                Log.d(TAG, "Reconstructing board with ${moveData.boardPieces.size} pieces")
                for (pieceData in moveData.boardPieces) {
                    val piece = ChessPiece(
                        type = pieceData.type,
                        color = pieceData.color,
                        hasMoved = pieceData.hasMoved
                    )
                    newBoard.setPieceForMultiplayer(
                        Position(pieceData.row, pieceData.col),
                        piece
                    )
                }

                // Actualizar el turno y estado del juego
                newBoard.setCurrentTurn(moveData.turn)
                newBoard.setGameState(moveData.gameState)

                Log.d(TAG, "Board updated, now it's my turn")
                _uiState.update { it.copy(
                    board = newBoard,
                    myTurn = true,
                    selectedPosition = null,
                    validMoves = emptyList()
                )}

                // Verificar si el juego terminó
                checkGameEnd(newBoard)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling received move", e)
                _uiState.update { it.copy(
                    errorMessage = "Error al procesar movimiento: ${e.message}"
                )}
            }
        }
    }
    
    /**
     * Envía un mensaje de handshake para establecer la conexión
     */
     fun sendHandshake(message: String) {
        try {
            Log.d(TAG, "Sending handshake: $message")
            val xml = """<?xml version="1.0" encoding="UTF-8"?>
<ChessMove>
    <Type>HANDSHAKE</Type>
    <Message>$message</Message>
</ChessMove>"""
            bluetoothManager.sendMessage(xml)
            Log.d(TAG, "Handshake sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending handshake", e)
        }
    }

    /**
     * Verifica si el juego ha terminado
     */
    private fun checkGameEnd(board: ChessBoard) {
        when (board.gameState) {
            GameState.CHECKMATE -> {
                val winner = if (board.currentTurn == PieceColor.WHITE) "Negras" else "Blancas"
                Log.d(TAG, "Game ended: CHECKMATE - $winner wins")
                _uiState.update { it.copy(
                    gameStarted = false,
                    gameEndMessage = "¡Jaque mate! Ganan las $winner"
                )}
            }
            GameState.STALEMATE -> {
                Log.d(TAG, "Game ended: STALEMATE")
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
        Log.d(TAG, "disconnect() called")
        bluetoothManager.disconnect()
        _uiState.update { it.copy(
            gameStarted = false,
            selectedPosition = null,
            validMoves = emptyList(),
            connectionState = ConnectionState.Disconnected,
            showSetupScreen = false
        )}
    }
    
    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        Log.d(TAG, "clearError() called")
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Limpia el mensaje de fin de juego
     */
    fun clearGameEndMessage() {
        Log.d(TAG, "clearGameEndMessage() called")
        _uiState.update { it.copy(gameEndMessage = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, disconnecting")
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
    val gameEndMessage: String? = null,
    val showSetupScreen: Boolean = false
)
