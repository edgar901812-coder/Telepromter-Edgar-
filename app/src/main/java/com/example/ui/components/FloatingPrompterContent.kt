package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun FloatingPrompterContent(
    title: String,
    content: String,
    initialFontSize: Float = 26f,
    initialSpeed: Float = 55f,
    initialMirror: Boolean = false,
    onClose: () -> Unit,
    onDragDelta: ((Float, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableFloatStateOf(initialSpeed) }
    var fontSize by remember { mutableFloatStateOf(initialFontSize) }
    var isMirrored by remember { mutableStateOf(initialMirror) }
    var isCollapsed by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current.density
    val coroutineScope = rememberCoroutineScope()

    val progress by remember {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }
    }

    // Auto-scroll engine
    LaunchedEffect(isPlaying, scrollSpeed) {
        if (!isPlaying) return@LaunchedEffect
        var lastNano = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { nowNano ->
                val dtSec = (nowNano - lastNano) / 1_000_000_000f
                lastNano = nowNano

                val deltaPx = scrollSpeed * dtSec * density
                val current = scrollState.value
                val max = scrollState.maxValue

                if (current + deltaPx >= max && max > 0) {
                    scrollState.dispatchRawDelta((max - current).toFloat())
                    isPlaying = false
                } else {
                    scrollState.dispatchRawDelta(deltaPx)
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(ElectricCyan.copy(alpha = 0.8f), StudioAmber.copy(alpha = 0.6f))),
                RoundedCornerShape(18.dp)
            )
            .testTag("floating_prompter_widget"),
        color = Color(0xF2090D16), // Premium semi-transparent dark studio glass
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Drag Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131B2E))
                    .then(
                        if (onDragDelta != null) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDragDelta(dragAmount.x, dragAmount.y)
                                }
                            }
                        } else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Mover ventana flotante",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = title.ifBlank { "Teleprompter Flotante" },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mirror toggle
                    IconButton(
                        onClick = { isMirrored = !isMirrored },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Modo espejo",
                            tint = if (isMirrored) StudioAmber else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Replay to top
                    IconButton(
                        onClick = {
                            isPlaying = false
                            coroutineScope.launch { scrollState.scrollTo(0) }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Reiniciar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Collapse / Expand toggle
                    IconButton(
                        onClick = { isCollapsed = !isCollapsed },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Minimizar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar flotante",
                            tint = StudioCoral,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = ElectricCyan,
                trackColor = Color.DarkGray
            )

            // Main Content: Script Text Area (Visible when not collapsed)
            AnimatedVisibility(visible = !isCollapsed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xE6000000))
                        .graphicsLayer {
                            scaleX = if (isMirrored) -1f else 1f
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .verticalScroll(scrollState),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = content.ifBlank { "Sin texto en el guión." },
                            color = Color.White,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.35f).sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }

            // Floating Controls Bar: ONLY Script controls, Play/Pause, and Speed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Speed Controls: [-] 55px/s [+]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { scrollSpeed = (scrollSpeed - 5f).coerceAtLeast(15f) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text("-", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    Text(
                        text = "⚡ ${scrollSpeed.toInt()} px/s",
                        color = StudioAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { scrollSpeed = (scrollSpeed + 5f).coerceAtMost(200f) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text("+", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }

                // Font Size Micro Controls: A- / A+
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { fontSize = (fontSize - 2f).coerceAtLeast(16f) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text("A-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Text(
                        text = "${fontSize.toInt()}sp",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    IconButton(
                        onClick = { fontSize = (fontSize + 2f).coerceAtMost(56f) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text("A+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                // Main Play / Pause Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isPlaying) listOf(StudioCoral, Color(0xFFBE123C))
                                else listOf(ElectricCyan, Color(0xFF0284C7))
                            )
                        )
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
