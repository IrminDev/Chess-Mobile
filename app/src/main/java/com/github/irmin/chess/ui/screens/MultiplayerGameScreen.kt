package com.github.irmin.chess.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.irmin.chess.bluetooth.ConnectionState
import com.github.irmin.chess.model.*
import com.github.irmin.chess.viewmodel.MultiplayerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerGameScreen(
    uiState: MultiplayerUiState,
    onSquareClick: (Position) -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit
) {
    var showDisconnectDialog by remember { mutableStateOf(false) }
    
    // Mostrar mensaje de fin de juego
    uiState.gameEndMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Juego Terminado") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = onBack) {
                    Text("Volver al Menú")
                }
            }
        )
    }
    
    // Mostrar errores
    uiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = onBack) {
                    Text("Volver al Menú")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Multijugador Bluetooth")
                        Text(
                            text = if (uiState.isHost) "Jugando con Blancas" else "Jugando con Negras",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showDisconnectDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Salir")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Estado de conexión
            ConnectionStatusBanner(uiState.connectionState)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Información de turno
            TurnIndicator(
                currentTurn = uiState.board.currentTurn,
                isMyTurn = uiState.myTurn,
                gameState = uiState.board.gameState
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tablero de ajedrez
            ChessBoard(
                board = uiState.board,
                selectedPosition = uiState.selectedPosition,
                validMoves = uiState.validMoves,
                onSquareClick = onSquareClick,
                enabled = uiState.myTurn && uiState.gameStarted
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botón de desconexión
            OutlinedButton(
                onClick = { showDisconnectDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Desconectar")
            }
        }
    }
    
    // Diálogo de confirmación de desconexión
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Desconectar") },
            text = { Text("¿Estás seguro de que quieres desconectarte? Esto terminará la partida.") },
            confirmButton = {
                Button(onClick = {
                    showDisconnectDialog = false
                    onDisconnect()
                    onBack()
                }) {
                    Text("Sí, Desconectar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ConnectionStatusBanner(connectionState: ConnectionState) {
    if (connectionState !is ConnectionState.Connected) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Conexión perdida...",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TurnIndicator(
    currentTurn: PieceColor,
    isMyTurn: Boolean,
    gameState: GameState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMyTurn) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isMyTurn) "Tu turno" else "Turno del oponente",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Juegan ${if (currentTurn == PieceColor.WHITE) "Blancas" else "Negras"}",
                fontSize = 14.sp
            )
            
            // Estado del juego
            when (gameState) {
                GameState.CHECK -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¡JAQUE!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ChessBoard(
    board: com.github.irmin.chess.model.ChessBoard,
    selectedPosition: Position?,
    validMoves: List<Position>,
    onSquareClick: (Position) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        for (row in 0..7) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                for (col in 0..7) {
                    val position = Position(row, col)
                    Square(
                        position = position,
                        piece = board.getPiece(position),
                        isSelected = position == selectedPosition,
                        isValidMove = validMoves.contains(position),
                        onClick = { if (enabled) onSquareClick(position) }
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.Square(
    position: Position,
    piece: ChessPiece?,
    isSelected: Boolean,
    isValidMove: Boolean,
    onClick: () -> Unit
) {
    val isLightSquare = (position.row + position.col) % 2 == 0
    val backgroundColor = when {
        isSelected -> Color(0xFF7B9B6C)
        isLightSquare -> Color(0xFFEEEED2)
        else -> Color(0xFF769656)
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .background(backgroundColor)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) Color(0xFF4A7C59) else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Highlight para movimientos válidos
        if (isValidMove) {
            Box(
                modifier = Modifier
                    .size(if (piece != null) 40.dp else 20.dp)
                    .background(
                        color = Color(0x80000000),
                        shape = CircleShape
                    )
            )
        }

        // Pieza
        piece?.let {
            Image(
                painter = painterResource(id = getPieceDrawable(it)),
                contentDescription = "${it.color} ${it.type}",
                modifier = Modifier.fillMaxSize(0.85f)
            )
        }
    }
}