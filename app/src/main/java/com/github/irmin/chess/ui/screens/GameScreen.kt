package com.github.irmin.chess.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.irmin.chess.R
import com.github.irmin.chess.model.*
import com.github.irmin.chess.viewmodel.ChessViewModel

@Composable
fun GameScreen(
    viewModel: ChessViewModel,
    onBackToMenu: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val board = uiState.board

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with game info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBackToMenu) {
                Text("Menu")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Current Turn:",
                    fontSize = 16.sp
                )
                Text(
                    text = if (board.currentTurn == PieceColor.WHITE) "White" else "Black",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(onClick = { viewModel.resetGame() }) {
                Text("Reset")
            }
        }

        // Game state message
        when (board.gameState) {
            GameState.CHECK -> {
                Text(
                    text = "CHECK!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp)
                )
            }
            GameState.CHECKMATE -> {
                Text(
                    text = "CHECKMATE! ${if (board.currentTurn == PieceColor.WHITE) "Black" else "White"} wins!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp)
                )
            }
            GameState.STALEMATE -> {
                Text(
                    text = "STALEMATE! It's a draw!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Blue,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp)
                )
            }
            else -> Spacer(modifier = Modifier.height(40.dp))
        }

        // Chess board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            ChessBoard(
                board = board,
                selectedPosition = uiState.selectedPosition,
                validMoves = uiState.validMoves,
                onSquareClick = { position -> viewModel.onSquareClicked(position) }
            )
        }
    }
}

@Composable
fun ChessBoard(
    board: ChessBoard,
    selectedPosition: Position?,
    validMoves: List<Position>,
    onSquareClick: (Position) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        for (row in 0..7) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0..7) {
                    val position = Position(row, col)
                    ChessSquare(
                        position = position,
                        piece = board.getPiece(position),
                        isSelected = position == selectedPosition,
                        isValidMove = validMoves.contains(position),
                        onClick = { onSquareClick(position) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChessSquare(
    position: Position,
    piece: ChessPiece?,
    isSelected: Boolean,
    isValidMove: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightSquare = (position.row + position.col) % 2 == 0
    val backgroundColor = when {
        isSelected -> Color(0xFF7B9FFF)
        isLightSquare -> Color(0xFFEEEED2)
        else -> Color(0xFF769656)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = Color.Yellow
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isValidMove && piece == null) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0x80000000), shape = CircleShape)
            )
        } else if (isValidMove && piece != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(3.dp, Color.Red)
            )
        }

        piece?.let {
            Image(
                painter = painterResource(id = getPieceDrawable(it)),
                contentDescription = "${it.color} ${it.type}",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }
    }
}

fun getPieceDrawable(piece: ChessPiece): Int {
    return when (piece.type) {
        PieceType.PAWN -> if (piece.color == PieceColor.WHITE) R.drawable.pawn_w else R.drawable.pawn_b
        PieceType.ROOK -> if (piece.color == PieceColor.WHITE) R.drawable.rook_w else R.drawable.rook_b
        PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) R.drawable.knight_w else R.drawable.knight_b
        PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) R.drawable.bishop_w else R.drawable.bishop_b
        PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) R.drawable.queen_w else R.drawable.queen_b
        PieceType.KING -> if (piece.color == PieceColor.WHITE) R.drawable.king_w else R.drawable.king_b
    }
}

