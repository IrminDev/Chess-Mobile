package com.github.irmin.chess.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.irmin.chess.ai.AIDifficulty

@Composable
fun MenuScreen(
    onSinglePlayerClick: (AIDifficulty) -> Unit,
    onMultiplayerLocalClick: () -> Unit,
    onMultiplayerRemoteClick: () -> Unit,
    onContinueGameClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    hasSavedGame: Boolean
) {
    var showDifficultyDialog by remember { mutableStateOf(false) }

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
            modifier = Modifier.padding(bottom = 64.dp)
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
                .padding(vertical = 4.dp),
            enabled = false
        ) {
            Text(
                text = "Multiplayer Remote",
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
