package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.Script
import kotlin.math.roundToInt

@Composable
fun InAppFloatingPrompter(
    script: Script,
    onClose: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(24f) }
    var offsetY by remember { mutableFloatStateOf(160f) }

    Box(modifier = Modifier.fillMaxSize()) {
        FloatingPrompterContent(
            title = script.title,
            content = script.content,
            initialFontSize = script.fontSize * 0.75f,
            initialSpeed = script.scrollSpeed,
            initialMirror = script.mirrorHorizontal,
            onClose = onClose,
            onDragDelta = { dx, dy ->
                offsetX += dx
                offsetY += dy
            },
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .width(320.dp)
        )
    }
}
