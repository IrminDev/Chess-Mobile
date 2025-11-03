package com.github.irmin.chess.ai

import com.github.irmin.chess.model.*
import kotlin.math.max
import kotlin.math.min

/**
 * Motor de IA para el juego de ajedrez usando Minimax con poda Alfa-Beta
 */
class ChessAI(
    private val difficulty: AIDifficulty = AIDifficulty.MEDIUM
) {

    companion object {
        // Valores de las piezas para la función de evaluación
        private const val PAWN_VALUE = 100
        private const val KNIGHT_VALUE = 320
        private const val BISHOP_VALUE = 330
        private const val ROOK_VALUE = 500
        private const val QUEEN_VALUE = 900
        private const val KING_VALUE = 20000

        // Bonificación por posición central
        private const val CENTER_BONUS = 10
        private const val EXTENDED_CENTER_BONUS = 5

        // Bonificación por movilidad
        private const val MOBILITY_BONUS = 5
    }

    /**
     * Obtiene la profundidad de búsqueda según la dificultad
     */
    private val searchDepth: Int
        get() = when (difficulty) {
            AIDifficulty.EASY -> 2
            AIDifficulty.MEDIUM -> 3
            AIDifficulty.HARD -> 4
        }

    /**
     * Calcula el mejor movimiento para la IA
     */
    fun getBestMove(board: ChessBoard): Move? {
        val aiColor = board.currentTurn
        var bestMove: Move? = null
        var bestValue = Int.MIN_VALUE
        val alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        val allMoves = getAllPossibleMoves(board, aiColor)

        // Ordenar movimientos para mejorar la poda (capturas primero)
        val sortedMoves = allMoves.sortedByDescending { move ->
            val capturedPiece = board.getPiece(move.to)
            capturedPiece?.let { getPieceValue(it.type) } ?: 0
        }

        for (move in sortedMoves) {
            // Simular el movimiento
            val undoInfo = makeTemporaryMove(board, move)

            // Evaluar la posición resultante
            val value = minimax(board, searchDepth - 1, alpha, beta, false, aiColor)

            // Deshacer el movimiento
            undoTemporaryMove(board, move, undoInfo)

            if (value > bestValue) {
                bestValue = value
                bestMove = move
            }
        }

        return bestMove
    }

    /**
     * Algoritmo Minimax con poda Alfa-Beta
     */
    private fun minimax(
        board: ChessBoard,
        depth: Int,
        alpha: Int,
        beta: Int,
        maximizingPlayer: Boolean,
        aiColor: PieceColor
    ): Int {
        // Condición de parada: profundidad 0 o juego terminado
        if (depth == 0 || board.gameState != GameState.PLAYING) {
            return evaluateBoard(board, aiColor)
        }

        var alphaLocal = alpha
        var betaLocal = beta

        if (maximizingPlayer) {
            var maxEval = Int.MIN_VALUE
            val moves = getAllPossibleMoves(board, aiColor)

            for (move in moves) {
                val undoInfo = makeTemporaryMove(board, move)
                val eval = minimax(board, depth - 1, alphaLocal, betaLocal, false, aiColor)
                undoTemporaryMove(board, move, undoInfo)

                maxEval = max(maxEval, eval)
                alphaLocal = max(alphaLocal, eval)

                // Poda Beta
                if (betaLocal <= alphaLocal) {
                    break
                }
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            val opponentColor = aiColor.opposite()
            val moves = getAllPossibleMoves(board, opponentColor)

            for (move in moves) {
                val undoInfo = makeTemporaryMove(board, move)
                val eval = minimax(board, depth - 1, alphaLocal, betaLocal, true, aiColor)
                undoTemporaryMove(board, move, undoInfo)

                minEval = min(minEval, eval)
                betaLocal = min(betaLocal, eval)

                // Poda Alfa
                if (betaLocal <= alphaLocal) {
                    break
                }
            }
            return minEval
        }
    }

    /**
     * Función de evaluación del tablero
     */
    private fun evaluateBoard(board: ChessBoard, aiColor: PieceColor): Int {
        // Si el juego terminó, dar una evaluación extrema
        when (board.gameState) {
            GameState.CHECKMATE -> {
                return if (board.currentTurn == aiColor) {
                    Int.MIN_VALUE + 1000 // La IA perdió
                } else {
                    Int.MAX_VALUE - 1000 // La IA ganó
                }
            }
            GameState.STALEMATE -> return 0 // Empate
            else -> {}
        }

        var score = 0

        // Evaluar material y posición de cada pieza
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board.getPiece(row, col) ?: continue
                val position = Position(row, col)
                val pieceValue = getPieceValue(piece.type)
                val positionBonus = getPositionBonus(position, piece)
                val mobilityBonus = getMobilityBonus(board, position, piece)

                val totalValue = pieceValue + positionBonus + mobilityBonus

                if (piece.color == aiColor) {
                    score += totalValue
                } else {
                    score -= totalValue
                }
            }
        }

        // Bonificación si la IA tiene el turno
        if (board.currentTurn == aiColor) {
            score += 50
        }

        return score
    }

    /**
     * Obtiene el valor base de una pieza
     */
    private fun getPieceValue(type: PieceType): Int {
        return when (type) {
            PieceType.PAWN -> PAWN_VALUE
            PieceType.KNIGHT -> KNIGHT_VALUE
            PieceType.BISHOP -> BISHOP_VALUE
            PieceType.ROOK -> ROOK_VALUE
            PieceType.QUEEN -> QUEEN_VALUE
            PieceType.KING -> KING_VALUE
        }
    }

    /**
     * Obtiene bonificación por posición de la pieza
     */
    private fun getPositionBonus(position: Position, piece: ChessPiece): Int {
        var bonus = 0

        // Bonificación por casillas centrales
        if (position.row in 3..4 && position.col in 3..4) {
            bonus += CENTER_BONUS
        } else if (position.row in 2..5 && position.col in 2..5) {
            bonus += EXTENDED_CENTER_BONUS
        }

        // Bonificación especial para peones avanzados
        if (piece.type == PieceType.PAWN) {
            val advancement = if (piece.color == PieceColor.WHITE) {
                7 - position.row
            } else {
                position.row
            }
            bonus += advancement * 10
        }

        // Penalización por rey expuesto en el centro del tablero en el medio juego
        if (piece.type == PieceType.KING) {
            if (position.row in 2..5 && position.col in 2..5) {
                bonus -= 50
            }
        }

        return bonus
    }

    /**
     * Obtiene bonificación por movilidad de la pieza
     */
    private fun getMobilityBonus(board: ChessBoard, position: Position, piece: ChessPiece): Int {
        val validMoves = board.getValidMoves(position)
        return validMoves.size * MOBILITY_BONUS
    }

    /**
     * Obtiene todos los movimientos posibles para un color
     */
    private fun getAllPossibleMoves(board: ChessBoard, color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()

        for (fromRow in 0..7) {
            for (fromCol in 0..7) {
                val piece = board.getPiece(fromRow, fromCol) ?: continue
                if (piece.color != color) continue

                val fromPosition = Position(fromRow, fromCol)
                val validMoves = board.getValidMoves(fromPosition)

                for (toPosition in validMoves) {
                    moves.add(Move(fromPosition, toPosition))
                }
            }
        }

        return moves
    }

    /**
     * Realiza un movimiento temporal en el tablero
     */
    private fun makeTemporaryMove(board: ChessBoard, move: Move): UndoInfo {
        val fromPiece = board.getPiece(move.from)!!
        val toPiece = board.getPiece(move.to)
        val currentTurn = board.currentTurn
        val gameState = board.gameState
        val lastMove = board.lastMove

        // Realizar el movimiento usando el método público
        board.makeMove(move.from, move.to)

        return UndoInfo(
            move = move,
            capturedPiece = toPiece,
            previousTurn = currentTurn,
            previousGameState = gameState,
            previousLastMove = lastMove,
            movedPiece = fromPiece
        )
    }

    /**
     * Deshace un movimiento temporal
     */
    private fun undoTemporaryMove(board: ChessBoard, move: Move, undoInfo: UndoInfo) {
        // Restaurar la pieza en la posición original
        board.setPieceForLoading(move.from, undoInfo.movedPiece)
        board.setPieceForLoading(move.to, undoInfo.capturedPiece)

        // Si fue un enroque, deshacer el movimiento de la torre
        if (undoInfo.movedPiece.type == PieceType.KING && kotlin.math.abs(move.to.col - move.from.col) == 2) {
            val kingSide = move.to.col > move.from.col
            val rookFromCol = if (kingSide) 7 else 0
            val rookToCol = if (kingSide) move.to.col - 1 else move.to.col + 1

            val rook = board.getPiece(Position(move.from.row, rookToCol))
            board.setPieceForLoading(Position(move.from.row, rookFromCol), rook?.copy(hasMoved = false))
            board.setPieceForLoading(Position(move.from.row, rookToCol), null)
        }

        // Restaurar el estado del juego
        board.setCurrentTurnForLoading(undoInfo.previousTurn)
        board.setGameStateForLoading(undoInfo.previousGameState)
        if (undoInfo.previousLastMove != null) {
            board.setLastMoveForLoading(undoInfo.previousLastMove)
        }
    }

    /**
     * Clase para almacenar información necesaria para deshacer un movimiento
     */
    private data class UndoInfo(
        val move: Move,
        val capturedPiece: ChessPiece?,
        val previousTurn: PieceColor,
        val previousGameState: GameState,
        val previousLastMove: Move?,
        val movedPiece: ChessPiece
    )
}

/**
 * Niveles de dificultad de la IA
 */
enum class AIDifficulty {
    EASY,    // Profundidad 2
    MEDIUM,  // Profundidad 3
    HARD     // Profundidad 4
}

