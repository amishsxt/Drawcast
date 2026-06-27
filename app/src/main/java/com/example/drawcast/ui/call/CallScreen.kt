package com.amishsxt.drawcast.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val BgColor = Color(0xFF121212)
private val ToolbarColor = Color(0xFF1E1E1E)
private val ChipColor = Color(0xFF2C2C2C)
private val StatusGreen = Color(0xFF4CAF50)
private val AnnotationOrange = Color(0xFFE64A19)

@Composable
fun CallScreen(
    roomId: String,
    isExpert: Boolean,
    onClose: () -> Unit,
    viewModel: CallViewModel = viewModel()
) {
    var selectedTool by remember { mutableStateOf("pencil") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // ── Center placeholder ────────────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF555555)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Live Video Feed",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF64B5F6)
            )
            Text(
                text = "Remote camera stream",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF888888)
            )
        }

        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // X close button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ChipColor)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Connected status chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ChipColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Connected · 24ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }

        // ── Room code chip (top left, below X) ───────────────────────────
        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 72.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(10.dp))
                .background(ChipColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Room Code",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF888888)
            )
            Text(
                text = roomId,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // ── Bottom toolbar ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(ToolbarColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Pencil (selected)
            ToolButton(
                icon = Icons.Default.Edit,
                label = "pencil",
                selectedTool = selectedTool,
                onSelect = { selectedTool = it }
            )

            // Arrow
            ToolButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "arrow",
                selectedTool = selectedTool,
                onSelect = { selectedTool = it }
            )

            // Circle
            ToolButton(
                icon = Icons.Default.RadioButtonUnchecked,
                label = "circle",
                selectedTool = selectedTool,
                onSelect = { selectedTool = it }
            )

            // Color indicator dot
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AnnotationOrange)
                    .clickable { /* TODO: open color picker */ }
            )

            // Laser pointer dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
                    .clickable { selectedTool = "laser" }
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )

            // Undo
            IconButton(onClick = { viewModel.undoLast() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = Color.White
                )
            }

            // Clear all
            IconButton(onClick = { viewModel.clearAll() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    selectedTool: String,
    onSelect: (String) -> Unit
) {
    val isSelected = selectedTool == label
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) AnnotationOrange else Color.Transparent)
            .clickable { onSelect(label) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}
