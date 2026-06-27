package com.example.drawcast.ui.call

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CallScreen(
    roomId: String,
    isExpert: Boolean,
    viewModel: CallViewModel = viewModel()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // TODO: SurfaceViewRenderer for remote video (Phone A)
        // TODO: TextureView for local camera preview (Phone B)
        // TODO: AnnotationOverlayView layered on top

        Text(
            text = "Room: $roomId • ${if (isExpert) "Expert" else "Field User"}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
