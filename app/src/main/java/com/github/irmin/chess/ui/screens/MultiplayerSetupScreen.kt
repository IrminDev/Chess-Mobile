package com.github.irmin.chess.ui.screens

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.irmin.chess.bluetooth.ConnectionState
import com.github.irmin.chess.viewmodel.MultiplayerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerSetupScreen(
    uiState: MultiplayerUiState,
    onStartAsHost: () -> Unit,
    onConnectToDevice: (BluetoothDevice) -> Unit,
    onRefreshDevices: () -> Unit,
    onNavigateToGame: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Launcher para permisos Bluetooth
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            showPermissionDialog = true
        }
    }
    
    // Solicitar permisos al inicio
    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)
    }
    
    // Navegar al juego cuando se conecte
    LaunchedEffect(uiState.connectionState) {
        if (uiState.connectionState is ConnectionState.Connected && uiState.gameStarted) {
            onNavigateToGame()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multijugador Bluetooth") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshDevices) {
                        Icon(Icons.Default.Refresh, "Actualizar")
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
            ConnectionStatusCard(uiState.connectionState)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Opciones de juego
            when (uiState.connectionState) {
                is ConnectionState.Disconnected -> {
                    DisconnectedOptions(
                        onStartAsHost = onStartAsHost,
                        onRefreshDevices = onRefreshDevices
                    )
                }
                is ConnectionState.Listening -> {
                    ListeningInfo()
                }
                is ConnectionState.Connecting -> {
                    ConnectingInfo()
                }
                is ConnectionState.Connected -> {
                    ConnectedInfo(uiState.isHost)
                }
                is ConnectionState.Error -> {
                    ErrorInfo((uiState.connectionState as ConnectionState.Error).message)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Lista de dispositivos disponibles
            if (uiState.connectionState is ConnectionState.Disconnected) {
                DeviceList(
                    devices = uiState.availableDevices,
                    onDeviceClick = onConnectToDevice
                )
            }
        }
    }
    
    // Diálogo de permisos
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permisos Necesarios") },
            text = { 
                Text("Esta aplicación necesita permisos de Bluetooth para el modo multijugador. Por favor, habilítalos en la configuración.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Ir a Configuración")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPermissionDialog = false
                    onBack()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ConnectionStatusCard(connectionState: ConnectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Estado:",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (connectionState) {
                    is ConnectionState.Disconnected -> "Desconectado"
                    is ConnectionState.Listening -> "Esperando conexión..."
                    is ConnectionState.Connecting -> "Conectando..."
                    is ConnectionState.Connected -> "Conectado"
                    is ConnectionState.Error -> "Error"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (connectionState is ConnectionState.Connected) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Iniciando partida...",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DisconnectedOptions(
    onStartAsHost: () -> Unit,
    onRefreshDevices: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Selecciona un modo:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onStartAsHost,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text("Crear Partida (Host)", fontSize = 18.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "O conecta a una partida existente:",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onRefreshDevices,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buscar Dispositivos")
        }
    }
}

@Composable
fun ListeningInfo() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Esperando que otro jugador se conecte...",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Asegúrate de que este dispositivo sea visible",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ConnectingInfo() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Conectando al dispositivo...",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ConnectedInfo(isHost: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¡Conectado!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isHost) "Jugarás con blancas" else "Jugarás con negras",
            fontSize = 16.sp
        )
    }
}

@Composable
fun ErrorInfo(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error de conexión",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DeviceList(
    devices: List<BluetoothDevice>,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Dispositivos Emparejados:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (devices.isEmpty()) {
            Text(
                text = "No hay dispositivos emparejados",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(devices) { device ->
                    DeviceItem(
                        device = device,
                        onClick = { onDeviceClick(device) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceItem(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            try {
                Text(
                    text = device.name ?: "Dispositivo desconocido",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = device.address,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } catch (e: SecurityException) {
                Text(
                    text = "Dispositivo (permisos requeridos)",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
