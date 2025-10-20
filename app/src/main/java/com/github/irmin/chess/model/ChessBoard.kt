package com.github.irmin.chess.model

import kotlin.math.abs

class ChessBoard {
    private val board = Array(8) { arrayOfNulls<ChessPiece>(8) }
    var currentTurn = PieceColor.WHITE
        private set
    var gameState = GameState.PLAYING
        private set
    private val moveHistory = mutableListOf<Move>()
    var lastMove: Move? = null
        private set

    init {
        setupInitialPosition()
    }

    private fun setupInitialPosition() {
        // Black pieces
        board[0][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
        board[0][1] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
        board[0][2] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
        board[0][3] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
        board[0][4] = ChessPiece(PieceType.KING, PieceColor.BLACK)
        board[0][5] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
        board[0][6] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
        board[0][7] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
        for (col in 0..7) {
            board[1][col] = ChessPiece(PieceType.PAWN, PieceColor.BLACK)
        }

        // White pieces
        board[7][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
        board[7][1] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
        board[7][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
        board[7][3] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
        board[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE)
        board[7][5] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
        board[7][6] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
        board[7][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
        for (col in 0..7) {
            board[6][col] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
        }
    }

    fun getPiece(position: Position): ChessPiece? {
        if (!position.isValid()) return null
        return board[position.row][position.col]
    }

    fun getPiece(row: Int, col: Int): ChessPiece? = getPiece(Position(row, col))

    private fun setPiece(position: Position, piece: ChessPiece?) {
        if (position.isValid()) {
            board[position.row][position.col] = piece
        }
    }

    fun isValidMove(from: Position, to: Position): Boolean {
        val piece = getPiece(from) ?: return false
        if (piece.color != currentTurn) return false
        if (!to.isValid()) return false

        val targetPiece = getPiece(to)
        if (targetPiece?.color == piece.color) return false

        if (!isLegalMovePattern(from, to, piece)) return false

        return !wouldBeInCheck(from, to, piece.color)
    }

    private fun isLegalMovePattern(from: Position, to: Position, piece: ChessPiece): Boolean {
        return when (piece.type) {
            PieceType.PAWN -> isValidPawnMove(from, to, piece)
            PieceType.ROOK -> isValidRookMove(from, to)
            PieceType.KNIGHT -> isValidKnightMove(from, to)
            PieceType.BISHOP -> isValidBishopMove(from, to)
            PieceType.QUEEN -> isValidQueenMove(from, to)
            PieceType.KING -> isValidKingMove(from, to, piece)
        }
    }

    private fun isValidPawnMove(from: Position, to: Position, piece: ChessPiece): Boolean {
        val direction = if (piece.color == PieceColor.WHITE) -1 else 1
        val rowDiff = to.row - from.row
        val colDiff = abs(to.col - from.col)

        if (colDiff == 0) {
            if (rowDiff == direction && getPiece(to) == null) return true
            if (!piece.hasMoved && rowDiff == 2 * direction &&
                getPiece(to) == null &&
                getPiece(Position(from.row + direction, from.col)) == null) {
                return true
            }
        }

        if (colDiff == 1 && rowDiff == direction) {
            val targetPiece = getPiece(to)
            if (targetPiece != null && targetPiece.color != piece.color) return true
            if (isEnPassantValid(from, to, piece)) return true
        }

        return false
    }

    private fun isEnPassantValid(from: Position, to: Position, piece: ChessPiece): Boolean {
        val lastMv = lastMove ?: return false
        val lastPiece = getPiece(lastMv.to) ?: return false

        if (lastPiece.type != PieceType.PAWN || lastPiece.color == piece.color) return false

        val direction = if (piece.color == PieceColor.WHITE) -1 else 1
        val enPassantRow = if (piece.color == PieceColor.WHITE) 3 else 4

        return from.row == enPassantRow &&
                abs(lastMv.from.row - lastMv.to.row) == 2 &&
                lastMv.to.row == enPassantRow &&
                lastMv.to.col == to.col &&
                abs(from.col - to.col) == 1 &&
                to.row - from.row == direction
    }

    private fun isValidRookMove(from: Position, to: Position): Boolean {
        if (from.row != to.row && from.col != to.col) return false
        return isPathClear(from, to)
    }

    private fun isValidKnightMove(from: Position, to: Position): Boolean {
        val rowDiff = abs(to.row - from.row)
        val colDiff = abs(to.col - from.col)
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)
    }

    private fun isValidBishopMove(from: Position, to: Position): Boolean {
        if (abs(to.row - from.row) != abs(to.col - from.col)) return false
        return isPathClear(from, to)
    }

    private fun isValidQueenMove(from: Position, to: Position): Boolean {
        return isValidRookMove(from, to) || isValidBishopMove(from, to)
    }

    private fun isValidKingMove(from: Position, to: Position, piece: ChessPiece): Boolean {
        val rowDiff = abs(to.row - from.row)
        val colDiff = abs(to.col - from.col)

        if (rowDiff <= 1 && colDiff <= 1) return true

        if (!piece.hasMoved && rowDiff == 0 && colDiff == 2) {
            return isValidCastling(from, to, piece)
        }

        return false
    }

    private fun isValidCastling(from: Position, to: Position, king: ChessPiece): Boolean {
        if (isInCheck(king.color)) return false

        val kingSide = to.col > from.col
        val rookCol = if (kingSide) 7 else 0
        val rook = getPiece(Position(from.row, rookCol))

        if (rook?.type != PieceType.ROOK || rook.hasMoved) return false

        val start = if (kingSide) from.col + 1 else rookCol + 1
        val end = if (kingSide) rookCol else from.col
        for (col in start until end) {
            if (getPiece(Position(from.row, col)) != null) return false
        }

        val step = if (kingSide) 1 else -1
        var col = from.col
        while (col != to.col + step) {
            if (wouldBeInCheck(from, Position(from.row, col), king.color)) return false
            col += step
        }

        return true
    }

    private fun isPathClear(from: Position, to: Position): Boolean {
        val rowStep = when {
            to.row > from.row -> 1
            to.row < from.row -> -1
            else -> 0
        }
        val colStep = when {
            to.col > from.col -> 1
            to.col < from.col -> -1
            else -> 0
        }

        var current = Position(from.row + rowStep, from.col + colStep)
        while (current != to) {
            if (getPiece(current) != null) return false
            current = Position(current.row + rowStep, current.col + colStep)
        }

        return true
    }

    private fun wouldBeInCheck(from: Position, to: Position, color: PieceColor): Boolean {
        val piece = getPiece(from)
        val capturedPiece = getPiece(to)
        setPiece(to, piece)
        setPiece(from, null)

        val inCheck = isInCheck(color)

        setPiece(from, piece)
        setPiece(to, capturedPiece)

        return inCheck
    }

    private fun isInCheck(color: PieceColor): Boolean {
        val kingPos = findKing(color) ?: return false

        for (row in 0..7) {
            for (col in 0..7) {
                val piece = getPiece(row, col) ?: continue
                if (piece.color != color && canAttack(Position(row, col), kingPos, piece)) {
                    return true
                }
            }
        }

        return false
    }

    private fun canAttack(from: Position, to: Position, piece: ChessPiece): Boolean {
        return when (piece.type) {
            PieceType.PAWN -> {
                val direction = if (piece.color == PieceColor.WHITE) -1 else 1
                val rowDiff = to.row - from.row
                val colDiff = abs(to.col - from.col)
                rowDiff == direction && colDiff == 1
            }
            PieceType.KNIGHT -> isValidKnightMove(from, to)
            PieceType.BISHOP -> isValidBishopMove(from, to)
            PieceType.ROOK -> isValidRookMove(from, to)
            PieceType.QUEEN -> isValidQueenMove(from, to)
            PieceType.KING -> {
                val rowDiff = abs(to.row - from.row)
                val colDiff = abs(to.col - from.col)
                rowDiff <= 1 && colDiff <= 1
            }
        }
    }

    private fun findKing(color: PieceColor): Position? {
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = getPiece(row, col)
                if (piece?.type == PieceType.KING && piece.color == color) {
                    return Position(row, col)
                }
            }
        }
        return null
    }

    fun makeMove(from: Position, to: Position): Boolean {
        if (!isValidMove(from, to)) return false

        val piece = getPiece(from) ?: return false
        val capturedPiece = getPiece(to)

        val isEnPassant = piece.type == PieceType.PAWN &&
                          to.col != from.col &&
                          capturedPiece == null

        var enPassantCaptured: ChessPiece? = null
        if (isEnPassant) {
            val captureRow = from.row
            enPassantCaptured = getPiece(Position(captureRow, to.col))
            setPiece(Position(captureRow, to.col), null)
        }

        val isCastling = piece.type == PieceType.KING && abs(to.col - from.col) == 2
        var rookMove: Move? = null
        if (isCastling) {
            val kingSide = to.col > from.col
            val rookFromCol = if (kingSide) 7 else 0
            val rookToCol = if (kingSide) to.col - 1 else to.col + 1
            val rookFrom = Position(from.row, rookFromCol)
            val rookTo = Position(from.row, rookToCol)
            val rook = getPiece(rookFrom)

            setPiece(rookTo, rook?.copy(hasMoved = true))
            setPiece(rookFrom, null)
            rookMove = Move(rookFrom, rookTo)
        }

        setPiece(to, piece.copy(hasMoved = true))
        setPiece(from, null)

        val move = Move(
            from = from,
            to = to,
            capturedPiece = capturedPiece ?: enPassantCaptured,
            isEnPassant = isEnPassant,
            isCastling = isCastling,
            rookMove = rookMove
        )

        moveHistory.add(move)
        lastMove = move

        if (piece.type == PieceType.PAWN && (to.row == 0 || to.row == 7)) {
            setPiece(to, ChessPiece(PieceType.QUEEN, piece.color, true))
        }

        currentTurn = currentTurn.opposite()
        updateGameState()

        return true
    }

    private fun updateGameState() {
        if (isInCheck(currentTurn)) {
            if (hasNoLegalMoves(currentTurn)) {
                gameState = GameState.CHECKMATE
            } else {
                gameState = GameState.CHECK
            }
        } else if (hasNoLegalMoves(currentTurn)) {
            gameState = GameState.STALEMATE
        } else {
            gameState = GameState.PLAYING
        }
    }

    private fun hasNoLegalMoves(color: PieceColor): Boolean {
        for (fromRow in 0..7) {
            for (fromCol in 0..7) {
                val piece = getPiece(fromRow, fromCol) ?: continue
                if (piece.color != color) continue

                for (toRow in 0..7) {
                    for (toCol in 0..7) {
                        if (isValidMove(Position(fromRow, fromCol), Position(toRow, toCol))) {
                            return false
                        }
                    }
                }
            }
        }
        return true
    }

    fun getValidMoves(position: Position): List<Position> {
        val validMoves = mutableListOf<Position>()
        for (row in 0..7) {
            for (col in 0..7) {
                val to = Position(row, col)
                if (isValidMove(position, to)) {
                    validMoves.add(to)
                }
            }
        }
        return validMoves
    }

    fun reset() {
        for (row in 0..7) {
            for (col in 0..7) {
                board[row][col] = null
            }
        }
        setupInitialPosition()
        currentTurn = PieceColor.WHITE
        gameState = GameState.PLAYING
        moveHistory.clear()
        lastMove = null
    }
}
