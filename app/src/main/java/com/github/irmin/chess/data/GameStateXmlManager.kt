package com.github.irmin.chess.data

import android.content.Context
import com.github.irmin.chess.model.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.util.Xml

class GameStateXmlManager(private val context: Context) {
    
    private val fileName = "chess_game_state.xml"
    
    /**
     * Guarda el estado actual del juego en un archivo XML
     */
    fun saveGameState(board: ChessBoard, gameStartTime: Long): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            val xmlSerializer: XmlSerializer = Xml.newSerializer()
            
            xmlSerializer.setOutput(outputStream, "UTF-8")
            xmlSerializer.startDocument("UTF-8", true)
            xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            
            // Elemento raíz
            xmlSerializer.startTag(null, "ChessGame")
            
            // Metadatos del juego
            xmlSerializer.startTag(null, "Metadata")
            xmlSerializer.startTag(null, "SaveTime")
            xmlSerializer.text(System.currentTimeMillis().toString())
            xmlSerializer.endTag(null, "SaveTime")
            xmlSerializer.startTag(null, "GameStartTime")
            xmlSerializer.text(gameStartTime.toString())
            xmlSerializer.endTag(null, "GameStartTime")
            xmlSerializer.startTag(null, "CurrentTurn")
            xmlSerializer.text(board.currentTurn.name)
            xmlSerializer.endTag(null, "CurrentTurn")
            xmlSerializer.startTag(null, "GameState")
            xmlSerializer.text(board.gameState.name)
            xmlSerializer.endTag(null, "GameState")
            xmlSerializer.endTag(null, "Metadata")
            
            // Estado del tablero
            xmlSerializer.startTag(null, "Board")
            for (row in 0..7) {
                for (col in 0..7) {
                    val piece = board.getPiece(row, col)
                    if (piece != null) {
                        xmlSerializer.startTag(null, "Piece")
                        xmlSerializer.attribute(null, "row", row.toString())
                        xmlSerializer.attribute(null, "col", col.toString())
                        xmlSerializer.attribute(null, "type", piece.type.name)
                        xmlSerializer.attribute(null, "color", piece.color.name)
                        xmlSerializer.attribute(null, "hasMoved", piece.hasMoved.toString())
                        xmlSerializer.endTag(null, "Piece")
                    }
                }
            }
            xmlSerializer.endTag(null, "Board")
            
            // Último movimiento (para en passant)
            board.lastMove?.let { move ->
                xmlSerializer.startTag(null, "LastMove")
                xmlSerializer.attribute(null, "fromRow", move.from.row.toString())
                xmlSerializer.attribute(null, "fromCol", move.from.col.toString())
                xmlSerializer.attribute(null, "toRow", move.to.row.toString())
                xmlSerializer.attribute(null, "toCol", move.to.col.toString())
                xmlSerializer.endTag(null, "LastMove")
            }
            
            xmlSerializer.endTag(null, "ChessGame")
            xmlSerializer.endDocument()
            xmlSerializer.flush()
            outputStream.close()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Carga el estado del juego desde el archivo XML
     * Retorna una tupla con el tablero restaurado y el tiempo de inicio del juego
     */
    fun loadGameState(): Pair<ChessBoard, Long>? {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return null
            
            val inputStream = FileInputStream(file)
            val parserFactory = XmlPullParserFactory.newInstance()
            val parser = parserFactory.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            
            val board = ChessBoard()
            // Limpiar el tablero
            for (row in 0..7) {
                for (col in 0..7) {
                    board.setPieceForLoading(Position(row, col), null)
                }
            }
            
            var gameStartTime = System.currentTimeMillis()
            var currentTurn = PieceColor.WHITE
            var gameState = GameState.PLAYING
            val pieces = mutableMapOf<Position, ChessPiece>()
            var lastMove: Move? = null
            
            var eventType = parser.eventType
            var currentTag = ""
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        when (currentTag) {
                            "Piece" -> {
                                val row = parser.getAttributeValue(null, "row").toInt()
                                val col = parser.getAttributeValue(null, "col").toInt()
                                val type = PieceType.valueOf(parser.getAttributeValue(null, "type"))
                                val color = PieceColor.valueOf(parser.getAttributeValue(null, "color"))
                                val hasMoved = parser.getAttributeValue(null, "hasMoved").toBoolean()
                                
                                pieces[Position(row, col)] = ChessPiece(type, color, hasMoved)
                            }
                            "LastMove" -> {
                                val fromRow = parser.getAttributeValue(null, "fromRow").toInt()
                                val fromCol = parser.getAttributeValue(null, "fromCol").toInt()
                                val toRow = parser.getAttributeValue(null, "toRow").toInt()
                                val toCol = parser.getAttributeValue(null, "toCol").toInt()
                                
                                lastMove = Move(
                                    from = Position(fromRow, fromCol),
                                    to = Position(toRow, toCol)
                                )
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "GameStartTime" -> gameStartTime = text.toLong()
                                "CurrentTurn" -> currentTurn = PieceColor.valueOf(text)
                                "GameState" -> gameState = GameState.valueOf(text)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            
            inputStream.close()
            
            // Restaurar el tablero
            pieces.forEach { (position, piece) ->
                board.setPieceForLoading(position, piece)
            }
            
            // Restaurar el estado del juego
            board.setCurrentTurnForLoading(currentTurn)
            board.setGameStateForLoading(gameState)
            lastMove?.let { board.setLastMoveForLoading(it) }
            
            Pair(board, gameStartTime)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Verifica si existe un juego guardado
     */
    fun hasSavedGame(): Boolean {
        val file = File(context.filesDir, fileName)
        return file.exists()
    }
    
    /**
     * Elimina el juego guardado
     */
    fun deleteSavedGame(): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
