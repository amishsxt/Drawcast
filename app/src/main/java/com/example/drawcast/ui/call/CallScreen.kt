package com.amishsxt.drawcast.ui.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amishsxt.drawcast.webrtc.WebRTCManager
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

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
    val context = LocalContext.current
    var selectedTool by remember { mutableStateOf("pencil") }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionGranted = true
        } else {
            permissionDenied = true
        }
    }

    // Request on first entry if not already granted
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Init WebRTC once permission is confirmed
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            viewModel.init(context, roomId, isExpert)
        }
    }

    // Permission denied wall
    if (permissionDenied) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFF555555),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Camera permission is required to start a call.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
        return
    }

    val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsState()
    val localVideoTrack by viewModel.localVideoTrack.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // ── Video layer ───────────────────────────────────────────────────
        val isDisconnected = connectionState == WebRTCManager.ConnectionState.DISCONNECTED
                || connectionState == WebRTCManager.ConnectionState.FAILED

        when {
            remoteVideoTrack != null -> {
                // Remote full screen
                val remTrack = remoteVideoTrack!!
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            init(viewModel.eglBase.eglBaseContext, null)
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                            setEnableHardwareScaler(true)
                            remTrack.addSink(this)
                        }
                    },
                    onRelease = { renderer ->
                        remTrack.removeSink(renderer)
                        renderer.release()
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Local PiP (bottom-right, above toolbar)
                if (localVideoTrack != null) {
                    val locTrack = localVideoTrack!!
                    AndroidView(
                        factory = { ctx ->
                            SurfaceViewRenderer(ctx).apply {
                                init(viewModel.eglBase.eglBaseContext, null)
                                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                                setEnableHardwareScaler(true)
                                setMirror(true)
                                locTrack.addSink(this)
                            }
                        },
                        onRelease = { renderer ->
                            locTrack.removeSink(renderer)
                            renderer.release()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 80.dp, end = 16.dp)
                            .size(width = 120.dp, height = 160.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            localVideoTrack != null -> {
                // Own camera full screen while waiting for remote to join
                val locTrack = localVideoTrack!!
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            init(viewModel.eglBase.eglBaseContext, null)
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                            setEnableHardwareScaler(true)
                            setMirror(true)
                            locTrack.addSink(this)
                        }
                    },
                    onRelease = { renderer ->
                        locTrack.removeSink(renderer)
                        renderer.release()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // Camera not ready yet
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
                        text = "Starting camera…",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF64B5F6)
                    )
                }
            }
        }

        // ── Call ended overlay ────────────────────────────────────────────
        if (isDisconnected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "Call ended",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFEF5350)
                    )
                    Text(
                        text = "The remote user disconnected",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF888888)
                    )
                }
            }
        }

        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: connection status + room code stacked
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        text = "Connected",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
                // Room code chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ChipColor)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Room ",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF888888)
                    )
                    Text(
                        text = roomId,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Right: hangup button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFD32F2F))
                    .clickable { onClose() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End call",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "End Call",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
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
