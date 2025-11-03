package com.github.irmin.chess.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    
    /**
     * Verifica si Bluetooth está disponible y habilitado
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Obtiene la lista de dispositivos emparejados
     */
    fun getPairedDevices() {
        if (!checkBluetoothPermission()) {
            _connectionState.value = ConnectionState.Error("Permisos Bluetooth no concedidos")
            return
        }
        
        try {
            val devices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            _pairedDevices.value = devices
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.Error("Error de permisos: ${e.message}")
        }
    }
    
    /**
     * Inicia el servidor Bluetooth para aceptar conexiones
     */
    fun startServer() {
        if (!checkBluetoothPermission()) {
            _connectionState.value = ConnectionState.Error("Permisos Bluetooth no concedidos")
            return
        }
        
        stopAllThreads()
        serverThread = AcceptThread()
        serverThread?.start()
        _connectionState.value = ConnectionState.Listening
    }
    
    /**
     * Conecta a un dispositivo remoto como cliente
     */
    fun connectToDevice(device: BluetoothDevice) {
        if (!checkBluetoothPermission()) {
            _connectionState.value = ConnectionState.Error("Permisos Bluetooth no concedidos")
            return
        }
        
        stopAllThreads()
        clientThread = ConnectThread(device)
        clientThread?.start()
        _connectionState.value = ConnectionState.Connecting
    }
    
    /**
     * Envía un mensaje por Bluetooth
     */
    fun sendMessage(message: String) {
        connectedThread?.write(message.toByteArray())
    }
    
    /**
     * Desconecta y limpia recursos
     */
    fun disconnect() {
        stopAllThreads()
        _connectionState.value = ConnectionState.Disconnected
    }
    
    private fun stopAllThreads() {
        serverThread?.cancel()
        serverThread = null
        clientThread?.cancel()
        clientThread = null
        connectedThread?.cancel()
        connectedThread = null
    }
    
    private fun connected(socket: BluetoothSocket) {
        stopAllThreads()
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()
        _connectionState.value = ConnectionState.Connected
    }
    
    private fun checkBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
    
    /**
     * Thread para aceptar conexiones entrantes (Servidor)
     */
    private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                if (checkBluetoothPermission()) {
                    bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
                } else {
                    null
                }
            } catch (e: SecurityException) {
                null
            } catch (e: IOException) {
                null
            }
        }
        
        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
                
                socket?.also {
                    connected(it)
                    serverSocket?.close()
                    shouldLoop = false
                }
            }
        }
        
        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {
                // Ignorar
            }
        }
    }
    
    /**
     * Thread para conectar a un dispositivo (Cliente)
     */
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                if (checkBluetoothPermission()) {
                    device.createRfcommSocketToServiceRecord(APP_UUID)
                } else {
                    null
                }
            } catch (e: SecurityException) {
                null
            } catch (e: IOException) {
                null
            }
        }
        
        override fun run() {
            try {
                if (checkBluetoothPermission()) {
                    bluetoothAdapter?.cancelDiscovery()
                }
            } catch (e: SecurityException) {
                // Ignorar
            }
            
            socket?.let { socket ->
                try {
                    socket.connect()
                    connected(socket)
                } catch (e: IOException) {
                    try {
                        socket.close()
                    } catch (closeException: IOException) {
                        // Ignorar
                    }
                    _connectionState.value = ConnectionState.Error("Fallo al conectar")
                }
            }
        }
        
        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                // Ignorar
            }
        }
    }
    
    /**
     * Thread para manejar la conexión establecida
     */
    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private val buffer: ByteArray = ByteArray(1024)
        
        override fun run() {
            var numBytes: Int
            val messageBuffer = StringBuilder()
            
            while (true) {
                try {
                    numBytes = inputStream.read(buffer)
                    val readMessage = String(buffer, 0, numBytes)
                    
                    // Acumular el mensaje hasta encontrar el delimitador
                    messageBuffer.append(readMessage)
                    
                    // Procesar mensajes completos (asumiendo que cada XML termina con ?>)
                    val message = messageBuffer.toString()
                    if (message.contains("</ChessMove>")) {
                        val startIndex = message.indexOf("<?xml")
                        val endIndex = message.indexOf("</ChessMove>") + "</ChessMove>".length
                        
                        if (startIndex != -1 && endIndex > startIndex) {
                            val completeMessage = message.substring(startIndex, endIndex)
                            _receivedMessage.value = completeMessage
                            
                            // Limpiar el buffer con lo que queda después del mensaje
                            messageBuffer.clear()
                            if (endIndex < message.length) {
                                messageBuffer.append(message.substring(endIndex))
                            }
                        }
                    }
                } catch (e: IOException) {
                    _connectionState.value = ConnectionState.Disconnected
                    break
                }
            }
        }
        
        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
                outputStream.flush()
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.Error("Error al enviar: ${e.message}")
            }
        }
        
        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                // Ignorar
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
