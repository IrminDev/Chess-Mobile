package com.github.irmin.chess.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.irmin.chess.bluetooth.ConnectionState
import com.github.irmin.chess.viewmodel.MultiplayerViewModel

/**
 * Pantalla de prueba de comunicación Bluetooth
 * Permite enviar y recibir mensajes de texto simples para verificar la conexión
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothTestScreen(
    viewModel: MultiplayerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageToSend by remember { mutableStateOf("") }
    var receivedMessages by remember { mutableStateOf(listOf<String>()) }

    // Observar mensajes recibidos
    LaunchedEffect(Unit) {
        // Aquí capturamos los mensajes de handshake
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test de Comunicación Bluetooth") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
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
            // Estado de la conexión
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (uiState.connectionState) {
                        is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                        is ConnectionState.Listening -> MaterialTheme.colorScheme.secondaryContainer
                        is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer
                        is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Estado de Conexión",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (uiState.connectionState) {
                            is ConnectionState.Connected -> "✓ Conectado"
                            is ConnectionState.Listening -> "⏳ Esperando conexión..."
                            is ConnectionState.Connecting -> "⏳ Conectando..."
                            is ConnectionState.Error -> "✗ Error: ${(uiState.connectionState as ConnectionState.Error).message}"
                            else -> "○ Desconectado"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rol: ${if (uiState.isHost) "HOST (Servidor)" else "CLIENT (Cliente)"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.startAsHost() },
                    enabled = uiState.connectionState is ConnectionState.Disconnected
                ) {
                    Text("Iniciar como HOST")
                }

                Button(
                    onClick = { viewModel.getAvailableDevices() },
                    enabled = uiState.connectionState is ConnectionState.Disconnected
                ) {
                    Text("Ver Dispositivos")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dispositivos disponibles
            if (uiState.availableDevices.isNotEmpty() && uiState.connectionState is ConnectionState.Disconnected) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Dispositivos Disponibles",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.availableDevices.forEach { device ->
                            Button(
                                onClick = { viewModel.connectToDevice(device) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(device.name ?: "Dispositivo desconocido")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Área de envío de mensajes
            if (uiState.connectionState is ConnectionState.Connected) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Enviar Mensaje de Prueba",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = messageToSend,
                            onValueChange = { messageToSend = it },
                            label = { Text("Mensaje") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    if (messageToSend.isNotEmpty()) {
                                        viewModel.sendHandshake(messageToSend)
                                        receivedMessages = receivedMessages + "SENT: $messageToSend"
                                        messageToSend = ""
                                    }
                                }
                            ) {
                                Text("Enviar")
                            }

                            Button(
                                onClick = {
                                    viewModel.sendHandshake("PING_${System.currentTimeMillis()}")
                                    receivedMessages = receivedMessages + "SENT: PING"
                                }
                            ) {
                                Text("Enviar PING")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de mensajes recibidos
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Log de Comunicación",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = { receivedMessages = emptyList() }) {
                                Text("Limpiar")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn {
                            items(receivedMessages) { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de desconectar
            if (uiState.connectionState !is ConnectionState.Disconnected) {
                Button(
                    onClick = { viewModel.disconnect() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Desconectar")
                }
            }
        }
    }
}

