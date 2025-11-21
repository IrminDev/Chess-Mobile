package com.github.irmin.chess.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.irmin.chess.ai.AIDifficulty
import com.github.irmin.chess.ui.theme.AppTheme

@Composable
fun MenuScreen(
    onSinglePlayerClick: (AIDifficulty) -> Unit,
    onMultiplayerLocalClick: () -> Unit,
    onMultiplayerRemoteClick: () -> Unit,
    onContinueGameClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    hasSavedGame: Boolean,
    currentTheme: AppTheme = AppTheme.MAROON,
    onThemeChange: () -> Unit = {}
) {
    var showDifficultyDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Botón de configuración de tema en la esquina superior derecha
        IconButton(
            onClick = onThemeChange,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Cambiar tema",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Chess Game",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Indicador del tema actual
            Text(
                text = when (currentTheme) {
                    AppTheme.MAROON -> "Tema: Guinda"
                    AppTheme.BLUE -> "Tema: Azul"
                },
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = { showDifficultyDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Single Player",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onMultiplayerLocalClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Multiplayer Local",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onContinueGameClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 4.dp),
                enabled = hasSavedGame
            ) {
                Text(
                    text = "Continue Game",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onMultiplayerRemoteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Multiplayer Bluetooth",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onStatisticsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Statistics",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (hasSavedGame) "You have a saved game!" else "Start a new game",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Diálogo para seleccionar dificultad
    if (showDifficultyDialog) {
        DifficultySelectionDialog(
            onDismiss = { showDifficultyDialog = false },
            onDifficultySelected = { difficulty ->
                showDifficultyDialog = false
                onSinglePlayerClick(difficulty)
            }
        )
    }
}

@Composable
fun DifficultySelectionDialog(
    onDismiss: () -> Unit,
    onDifficultySelected: (AIDifficulty) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Difficulty",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose the AI difficulty level:",
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onDifficultySelected(AIDifficulty.EASY) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Easy", fontSize = 18.sp)
                }

                Button(
                    onClick = { onDifficultySelected(AIDifficulty.MEDIUM) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Medium", fontSize = 18.sp)
                }

                Button(
                    onClick = { onDifficultySelected(AIDifficulty.HARD) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hard", fontSize = 18.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
