package com.github.irmin.chess.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Manager para conexiones Bluetooth entre dispositivos
 */
class BluetoothManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val APP_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    private val APP_NAME = "ChessGame"
    
    private var serverThread: AcceptThread? = null
    private var clientThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _receivedMessage = MutableStateFlow<String?>(null)
    val receivedMessage: StateFlow<String?> = _receivedMessage.asStateFlow()
    
    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()
    
    companion object {
        private const val TAG = "BluetoothManager"
    }

    init {
        Log.d(TAG, "BluetoothManager initialized")
    }

    /**
     * Verifica si Bluetooth está disponible y habilitado
     */
    fun isBluetoothAvailable(): Boolean {
        val available = bluetoothAdapter != null
        Log.d(TAG, "isBluetoothAvailable: $available")
        return available
    }
    
    fun isBluetoothEnabled(): Boolean {
        val enabled = bluetoothAdapter?.isEnabled == true
        Log.d(TAG, "isBluetoothEnabled: $enabled")
        return enabled
    }
    
    /**
     * Obtiene la lista de dispositivos emparejados
     */
    fun getPairedDevices() {
        Log.d(TAG, "getPairedDevices() called")

        if (!checkBluetoothPermission()) {
            Log.e(TAG, "Bluetooth permission not granted")
            _connectionState.value = ConnectionState.Error("Permisos Bluetooth no concedidos")
            return
        }
        
        try {
            val devices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            Log.d(TAG, "Found ${devices.size} paired devices")
            devices.forEachIndexed { index, device ->
                try {
                    Log.d(TAG, "Device $index: ${device.name} (${device.address})")
                } catch (e: SecurityException) {
                    Log.e(TAG, "Cannot read device info due to permission", e)
                }
            }
            _pairedDevices.value = devices
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting paired devices", e)
            _connectionState.value = ConnectionState.Error("Error de permisos: ${e.message}")
        }
    }
    
    /**
     * Inicia el servidor Bluetooth para aceptar conexiones
     */
    fun startServer() {
        Log.d(TAG, "startServer() called")

        if (!checkBluetoothPermission()) {
            Log.e(TAG, "Bluetooth permission not granted")
            _connectionState.value = ConnectionState.Error("Permisos Bluetooth no concedidos")
            return
        }
        
        stopAllThreads()
        Log.d(TAG, "Starting AcceptThread...")
        serverThread = AcceptThread()
        serverThread?.start()
        _connectionState.value = ConnectionState.Listening
        Log.d(TAG, "Server started, now listening for connections")
    }
    
    /**
     * Conecta a un dispositivo remoto como cliente
     */
    fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectToDevice() called")

        if (!checkBluetoothPermission()) {
            Log.e(TAG, "Bluetooth permission not granted")
            _connectionState.value = ConnectionState.Error("Permisos Bluetooth no concedidos")
            return
        }
        
        try {
            val deviceName = device.name ?: "Unknown"
            val deviceAddress = device.address
            Log.d(TAG, "Connecting to device: $deviceName ($deviceAddress)")

            stopAllThreads()
            clientThread = ConnectThread(device)
            clientThread?.start()
            _connectionState.value = ConnectionState.Connecting
            Log.d(TAG, "ConnectThread started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception connecting to device", e)
            _connectionState.value = ConnectionState.Error("Error de permisos al conectar")
        }
    }
    
    /**
     * Envía un mensaje por Bluetooth
     */
    fun sendMessage(message: String) {
        Log.d(TAG, "sendMessage() called, message length: ${message.length}")
        connectedThread?.write(message.toByteArray())
    }
    
    /**
     * Limpia el mensaje recibido después de procesarlo
     */
    fun clearReceivedMessage() {
        Log.d(TAG, "clearReceivedMessage() called")
        _receivedMessage.value = null
    }

    /**
     * Desconecta y limpia recursos
     */
    fun disconnect() {
        Log.d(TAG, "disconnect() called")
        stopAllThreads()
        _connectionState.value = ConnectionState.Disconnected
        Log.d(TAG, "Disconnected")
    }
    
    private fun stopAllThreads() {
        Log.d(TAG, "Stopping all threads...")
        serverThread?.cancel()
        serverThread = null
        clientThread?.cancel()
        clientThread = null
        connectedThread?.cancel()
        connectedThread = null
        Log.d(TAG, "All threads stopped")
    }
    
    private fun connected(socket: BluetoothSocket) {
        Log.d(TAG, "connected() called with socket: ${socket.isConnected}")

        // Importante: primero crear el ConnectedThread con el socket
        // ANTES de cancelar los otros threads
        val newConnectedThread = ConnectedThread(socket)

        // Ahora limpiar los threads antiguos (pero no el nuevo)
        Log.d(TAG, "Cleaning up old connection threads...")

        // Cancelar server thread si existe
        serverThread?.cancel()
        serverThread = null

        // Cancelar client thread si existe (el socket ya está en uso, no debe cerrarlo)
        clientThread?.let {
            it.transferSocket() // Marcar que el socket ya fue transferido
            it.cancel()
        }
        clientThread = null

        // Cancelar connected thread antiguo si existe
        connectedThread?.cancel()

        // Asignar el nuevo connected thread
        connectedThread = newConnectedThread

        // Iniciar el thread de comunicación
        connectedThread?.start()
        _connectionState.value = ConnectionState.Connected
        Log.d(TAG, "Connection established, ConnectedThread started")
    }
    
    private fun checkBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Bluetooth CONNECT permission granted: $hasPermission")
            return hasPermission
        }
        Log.d(TAG, "Android < 12, Bluetooth permissions OK")
        return true
    }
    
    /**
     * Thread para aceptar conexiones entrantes (Servidor)
     */
    private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                if (checkBluetoothPermission()) {
                    Log.d(TAG, "Creating server socket...")
                    bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
                } else {
                    Log.e(TAG, "Cannot create server socket, no permission")
                    null
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception creating server socket", e)
                null
            } catch (e: IOException) {
                Log.e(TAG, "IO exception creating server socket", e)
                null
            }
        }
        
        override fun run() {
            Log.d(TAG, "AcceptThread.run() started")
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    Log.d(TAG, "Waiting for incoming connection...")
                    serverSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Server socket accept failed", e)
                    shouldLoop = false
                    null
                }
                
                socket?.also {
                    Log.d(TAG, "Incoming connection accepted!")

                    // Cerrar el server socket ANTES de llamar a connected()
                    // para que stopAllThreads() no intente cerrarlo de nuevo
                    try {
                        Log.d(TAG, "Closing server socket before establishing connection...")
                        serverSocket?.close()
                        Log.d(TAG, "Server socket closed")
                    } catch (e: IOException) {
                        Log.e(TAG, "Error closing server socket", e)
                    }

                    // Ahora establecer la conexión con el socket aceptado
                    connected(it)
                    shouldLoop = false
                }
            }
            Log.d(TAG, "AcceptThread.run() finished")
        }
        
        fun cancel() {
            Log.d(TAG, "AcceptThread.cancel() called")
            try {
                serverSocket?.close()
                Log.d(TAG, "Server socket closed in cancel()")
            } catch (e: IOException) {
                Log.e(TAG, "Error closing server socket in cancel", e)
            }
        }
    }
    
    /**
     * Thread para conectar a un dispositivo (Cliente)
     */
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        @Volatile
        private var shouldClose = true
        private var socket: BluetoothSocket? = null

        override fun run() {
            Log.d(TAG, "ConnectThread.run() started")
            try {
                if (checkBluetoothPermission()) {
                    Log.d(TAG, "Cancelling discovery...")
                    bluetoothAdapter?.cancelDiscovery()
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception cancelling discovery", e)
            }
            
            try {
                if (checkBluetoothPermission()) {
                    Log.d(TAG, "Creating client socket...")
                    socket = device.createRfcommSocketToServiceRecord(APP_UUID)
                    Log.d(TAG, "Attempting to connect...")
                    socket?.connect()
                    Log.d(TAG, "Connected successfully!")

                    // Marcar que NO debemos cerrar este socket en cancel()
                    shouldClose = false

                    // Pasar el socket a connected()
                    socket?.let { connected(it) }
                } else {
                    Log.e(TAG, "Cannot create client socket, no permission")
                    _connectionState.value = ConnectionState.Error("No hay permisos Bluetooth")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception creating/connecting socket", e)
                _connectionState.value = ConnectionState.Error("Error de permisos")
            } catch (e: IOException) {
                Log.e(TAG, "Connection failed", e)
                try {
                    socket?.close()
                } catch (closeException: IOException) {
                    Log.e(TAG, "Error closing socket after failed connection", closeException)
                }
                _connectionState.value = ConnectionState.Error("Fallo al conectar: ${e.message}")
            }

            Log.d(TAG, "ConnectThread.run() finished")
        }
        
        /**
         * Marca que el socket fue transferido al ConnectedThread
         */
        fun transferSocket() {
            Log.d(TAG, "ConnectThread.transferSocket() called - socket ownership transferred")
            shouldClose = false
        }

        fun cancel() {
            Log.d(TAG, "ConnectThread.cancel() called, shouldClose=$shouldClose")
            if (shouldClose) {
                try {
                    socket?.close()
                    Log.d(TAG, "Socket closed in ConnectThread.cancel()")
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing socket in cancel", e)
                }
            } else {
                Log.d(TAG, "NOT closing socket - it's being used by ConnectedThread")
            }
        }
    }
    
    /**
     * Thread para manejar la conexión establecida
     */
    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream
        private val outputStream: OutputStream
        private val buffer: ByteArray = ByteArray(4096) // Aumentar tamaño del buffer
        @Volatile
        private var isRunning = true

        init {
            Log.d(TAG, "ConnectedThread initializing...")
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
                Log.d(TAG, "Input and output streams obtained successfully")
            } catch (e: IOException) {
                Log.e(TAG, "Error getting streams from socket", e)
            }

            inputStream = tmpIn!!
            outputStream = tmpOut!!

            Log.d(TAG, "ConnectedThread initialized successfully, socket state: connected=${socket.isConnected}")
        }

        override fun run() {
            Log.d(TAG, "ConnectedThread.run() started, isRunning=$isRunning, socket.isConnected=${socket.isConnected}")
            val messageBuffer = StringBuilder()

            // Pequeña espera inicial para asegurar que ambos lados están listos
            try {
                Log.d(TAG, "Waiting 100ms before starting read loop...")
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                Log.w(TAG, "Sleep interrupted", e)
            }

            Log.d(TAG, "Starting read loop...")

            while (isRunning) {
                try {
                    // Verificar estado del socket
                    if (!socket.isConnected) {
                        Log.e(TAG, "Socket is not connected anymore, stopping read loop")
                        break
                    }

                    // Verificar si hay datos disponibles
                    val available = inputStream.available()

                    if (available > 0) {
                        Log.d(TAG, "Data available: $available bytes")

                        // Leer los datos disponibles
                        val numBytes = inputStream.read(buffer, 0, minOf(available, buffer.size))

                        if (numBytes == -1) {
                            Log.e(TAG, "End of stream reached (read returned -1)")
                            _connectionState.value = ConnectionState.Disconnected
                            break
                        }

                        if (numBytes > 0) {
                            val readMessage = String(buffer, 0, numBytes, Charsets.UTF_8)
                            Log.d(TAG, "Received $numBytes bytes: ${readMessage.take(150)}...")

                            // Acumular el mensaje
                            messageBuffer.append(readMessage)

                            // Procesar mensajes completos
                            processMessages(messageBuffer)
                        }
                    } else {
                        // No hay datos disponibles, esperar antes de verificar de nuevo
                        Thread.sleep(50)
                    }
                } catch (e: IOException) {
                    if (isRunning) {
                        Log.e(TAG, "Connection lost during read", e)
                        _connectionState.value = ConnectionState.Disconnected
                        break
                    } else {
                        Log.d(TAG, "Socket closed intentionally")
                    }
                } catch (e: InterruptedException) {
                    Log.d(TAG, "Thread interrupted", e)
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error in read loop", e)
                    if (isRunning) {
                        _connectionState.value = ConnectionState.Disconnected
                        break
                    }
                }
            }
            Log.d(TAG, "ConnectedThread.run() finished")
        }

        /**
         * Procesa los mensajes completos del buffer
         */
        private fun processMessages(messageBuffer: StringBuilder) {
            val message = messageBuffer.toString()

            // Buscar todos los mensajes completos
            var searchIndex = 0
            while (true) {
                val startIndex = message.indexOf("<?xml", searchIndex)
                if (startIndex == -1) break

                val endIndex = message.indexOf("</ChessMove>", startIndex)
                if (endIndex == -1) break

                val completeEndIndex = endIndex + "</ChessMove>".length
                val completeMessage = message.substring(startIndex, completeEndIndex)

                Log.d(TAG, "Complete message found, length: ${completeMessage.length}")
                _receivedMessage.value = completeMessage

                searchIndex = completeEndIndex
            }

            // Limpiar los mensajes procesados del buffer
            if (searchIndex > 0) {
                val remaining = if (searchIndex < message.length) {
                    message.substring(searchIndex)
                } else {
                    ""
                }
                messageBuffer.clear()
                messageBuffer.append(remaining)
                Log.d(TAG, "Buffer cleared, remaining bytes: ${remaining.length}")
            }
        }

        fun write(bytes: ByteArray) {
            try {
                if (!socket.isConnected) {
                    Log.e(TAG, "Cannot write: socket is not connected")
                    _connectionState.value = ConnectionState.Error("Socket desconectado")
                    return
                }

                Log.d(TAG, "Writing ${bytes.size} bytes...")
                outputStream.write(bytes)
                outputStream.flush()
                Log.d(TAG, "Data written and flushed successfully")
            } catch (e: IOException) {
                Log.e(TAG, "Error writing data", e)
                _connectionState.value = ConnectionState.Error("Error al enviar: ${e.message}")
            }
        }
        
        fun cancel() {
            Log.d(TAG, "ConnectedThread.cancel() called")
            isRunning = false
            try {
                socket.close()
                Log.d(TAG, "Socket closed successfully")
            } catch (e: IOException) {
                Log.e(TAG, "Error closing socket in cancel", e)
            }
        }
    }
}

/**
 * Estados de la conexión Bluetooth
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Listening : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
