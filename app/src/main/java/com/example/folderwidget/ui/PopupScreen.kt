package com.example.folderwidget.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.folderwidget.R

class PopupApp(val pkg: String, val label: String, val icon: ImageBitmap?)

@Composable
fun PopupScreen(
    apps: List<PopupApp>,
    color: Int,
    onClose: () -> Unit,
    onLaunch: (PopupApp) -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(if (shown) 1f else 0f, label = "popup")

    val textColor = if (ColorUtils.calculateLuminance(color) > 0.5) Color(0xFF1B1B1F) else Color.White
    val noRipple = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * progress))
            .clickable(interactionSource = noRipple, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    alpha = progress
                    scaleX = 0.8f + 0.2f * progress
                    scaleY = 0.8f + 0.2f * progress
                }
                .clip(RoundedCornerShape(32.dp))
                .background(Color(color))
                // swallow taps so touching the panel does not close it
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (apps.isEmpty()) {
                Text(
                    stringResource(R.string.popup_empty),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(220.dp),
                )
            } else {
                val cols = when {
                    apps.size <= 4 -> apps.size
                    apps.size <= 6 -> 3
                    else -> 4
                }
                apps.chunked(cols).forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) Spacer(Modifier.size(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { app -> AppCell(app, textColor) { onLaunch(app) } }
                        repeat(cols - row.size) { Spacer(Modifier.width(76.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCell(app: PopupApp, textColor: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        } else {
            Spacer(Modifier.size(56.dp))
        }
        Spacer(Modifier.size(6.dp))
        Text(
            app.label,
            color = textColor,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
