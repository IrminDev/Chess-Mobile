package com.github.irmin.chess.bluetooth

import com.github.irmin.chess.model.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Clase para serializar y deserializar el estado del juego en XML
 * para transmitir por Bluetooth
 */
data class BluetoothGameState(
    val board: ChessBoard,
    val moveFrom: Position,
    val moveTo: Position,
    val currentTurn: PieceColor
) {
    companion object {
        /**
         * Serializa un movimiento a XML
         */
        fun serializeMove(from: Position, to: Position, board: ChessBoard): String {
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc: Document = docBuilder.newDocument()
            
            val root = doc.createElement("ChessMove")
            doc.appendChild(root)
            
            // Información del movimiento
            val moveElement = doc.createElement("Move")
            moveElement.setAttribute("fromRow", from.row.toString())
            moveElement.setAttribute("fromCol", from.col.toString())
            moveElement.setAttribute("toRow", to.row.toString())
            moveElement.setAttribute("toCol", to.col.toString())
            root.appendChild(moveElement)
            
            // Turno actual
            val turnElement = doc.createElement("Turn")
            turnElement.textContent = board.currentTurn.name
            root.appendChild(turnElement)
            
            // Estado del juego
            val stateElement = doc.createElement("GameState")
            stateElement.textContent = board.gameState.name
            root.appendChild(stateElement)
            
            // Serializar el tablero completo
            val boardElement = doc.createElement("Board")
            for (row in 0..7) {
                for (col in 0..7) {
                    val piece = board.getPiece(Position(row, col))
                    if (piece != null) {
                        val pieceElement = doc.createElement("Piece")
                        pieceElement.setAttribute("row", row.toString())
                        pieceElement.setAttribute("col", col.toString())
                        pieceElement.setAttribute("type", piece.type.name)
                        pieceElement.setAttribute("color", piece.color.name)
                        pieceElement.setAttribute("hasMoved", piece.hasMoved.toString())
                        boardElement.appendChild(pieceElement)
                    }
                }
            }
            root.appendChild(boardElement)
            
            // Convertir a String
            return documentToString(doc)
        }
        
        /**
         * Deserializa un movimiento desde XML
         */
        fun deserializeMove(xml: String): MoveData? {
            return try {
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                val doc = builder.parse(xml.byteInputStream())
                
                val moveElement = doc.getElementsByTagName("Move").item(0) as Element
                val fromRow = moveElement.getAttribute("fromRow").toInt()
                val fromCol = moveElement.getAttribute("fromCol").toInt()
                val toRow = moveElement.getAttribute("toRow").toInt()
                val toCol = moveElement.getAttribute("toCol").toInt()
                
                val turnElement = doc.getElementsByTagName("Turn").item(0) as Element
                val turn = PieceColor.valueOf(turnElement.textContent)
                
                val stateElement = doc.getElementsByTagName("GameState").item(0) as Element
                val gameState = GameState.valueOf(stateElement.textContent)
                
                // Deserializar el tablero
                val boardPieces = mutableListOf<PieceData>()
                val boardElement = doc.getElementsByTagName("Board").item(0) as Element
                val pieceNodes = boardElement.getElementsByTagName("Piece")
                
                for (i in 0 until pieceNodes.length) {
                    val pieceElement = pieceNodes.item(i) as Element
                    val row = pieceElement.getAttribute("row").toInt()
                    val col = pieceElement.getAttribute("col").toInt()
                    val type = PieceType.valueOf(pieceElement.getAttribute("type"))
                    val color = PieceColor.valueOf(pieceElement.getAttribute("color"))
                    val hasMoved = pieceElement.getAttribute("hasMoved").toBoolean()
                    
                    boardPieces.add(PieceData(row, col, type, color, hasMoved))
                }
                
                MoveData(
                    Position(fromRow, fromCol),
                    Position(toRow, toCol),
                    turn,
                    gameState,
                    boardPieces
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        
        private fun documentToString(doc: Document): String {
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
            
            val writer = StringWriter()
            transformer.transform(DOMSource(doc), StreamResult(writer))
            return writer.toString()
        }
    }
}

/**
 * Datos de un movimiento deserializado
 */
data class MoveData(
    val from: Position,
    val to: Position,
    val turn: PieceColor,
    val gameState: GameState,
    val boardPieces: List<PieceData>
)

/**
 * Datos de una pieza en el tablero
 */
data class PieceData(
    val row: Int,
    val col: Int,
    val type: PieceType,
    val color: PieceColor,
    val hasMoved: Boolean
)
