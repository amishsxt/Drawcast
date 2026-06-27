package com.example.drawcast.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onNavigateToCall: (roomId: String, isExpert: Boolean) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = viewModel()
) {
    var roomInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Drawcast", style = MaterialTheme.typography.displaySmall)

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { viewModel.createRoom() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Room (Expert)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = roomInput,
            onValueChange = { roomInput = it },
            label = { Text("Room Code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.joinRoom(roomInput) },
            modifier = Modifier.fillMaxWidth(),
            enabled = roomInput.length == 6
        ) {
            Text("Join Room (Field User)")
        }
    }
}
