package com.github.irmin.chess.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreen(
    onSinglePlayerClick: () -> Unit,
    onMultiplayerLocalClick: () -> Unit,
    onMultiplayerRemoteClick: () -> Unit
) {
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
            onClick = onSinglePlayerClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(vertical = 4.dp),
            enabled = false
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

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Only Multiplayer Local is available",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

