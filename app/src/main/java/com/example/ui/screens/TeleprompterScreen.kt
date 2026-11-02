package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Script
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class ColumnWidthOption(val label: String, val fraction: Float) {
    FULL("100%", 1.0f),
    MEDIUM("75%", 0.75f),
    FOCUSED("55%", 0.55f)
}

@Composable
fun TeleprompterScreen(
    script: Script,
    onClose: () -> Unit,
    onSaveSettings: (
        fontSize: Float,
        scrollSpeed: Float,
        mirrorH: Boolean,
        mirrorV: Boolean,
        showCue: Boolean,
        bgHex: String,
        fgHex: String
    ) -> Unit,
    onSwitchToFloating: () -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val density = LocalDensity.current.density
    val coroutineScope = rememberCoroutineScope()

    // Keep screen active while teleprompter is open
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Prompter Live State
    var isPlaying by remember { mutableStateOf(false) }
    var fontSize by remember { mutableFloatStateOf(script.fontSize) }
    var scrollSpeed by remember { mutableFloatStateOf(script.scrollSpeed) }
    var mirrorHorizontal by remember { mutableStateOf(script.mirrorHorizontal) }
    var mirrorVertical by remember { mutableStateOf(script.mirrorVertical) }
    var showCueGuide by remember { mutableStateOf(script.showCueGuide) }
    var cueGuidePercent by remember { mutableFloatStateOf(script.cueLinePositionPercent) }
    var columnWidth by remember { mutableStateOf(ColumnWidthOption.MEDIUM) }
    var bgHex by remember { mutableStateOf(script.backgroundColorHex) }
    var fgHex by remember { mutableStateOf(script.textColorHex) }

    // Controls visibility
    var showControls by remember { mutableStateOf(true) }
    var activePanel by remember { mutableStateOf<String?>(null) } // "speed", "font", "display", null

    // Countdown state (3, 2, 1)
    var countdownValue by remember { mutableIntStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Reading progress percentage
    val scrollProgress by remember {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }
    }

    // Auto-save settings on exit
    DisposableEffect(Unit) {
        onDispose {
            onSaveSettings(
                fontSize,
                scrollSpeed,
                mirrorHorizontal,
                mirrorVertical,
                showCueGuide,
                bgHex,
                fgHex
            )
        }
    }

    // Auto-hide controls after 3.5s of playing
    LaunchedEffect(isPlaying, showControls, activePanel) {
        if (isPlaying && showControls && activePanel == null) {
            delay(3500)
            showControls = false
        }
    }

    // Auto-scroll loop engine running smoothly at display refresh rate
    LaunchedEffect(isPlaying, scrollSpeed) {
        if (!isPlaying) return@LaunchedEffect
        var lastNano = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { nowNano ->
                val dtSec = (nowNano - lastNano) / 1_000_000_000f
                lastNano = nowNano

                // Speed in pixels per second: base scrollSpeed multiplied by density
                val deltaPx = scrollSpeed * dtSec * density
                val current = scrollState.value
                val max = scrollState.maxValue

                if (current + deltaPx >= max && max > 0) {
                    scrollState.dispatchRawDelta((max - current).toFloat())
                    isPlaying = false
                    showControls = true
                } else {
                    scrollState.dispatchRawDelta(deltaPx)
                }
            }
        }
    }

    // Countdown animation
    fun startWithCountdown() {
        if (scrollState.value >= scrollState.maxValue && scrollState.maxValue > 0) {
            coroutineScope.launch { scrollState.scrollTo(0) }
        }
        countdownValue = 3
        isCountingDown = true
    }

    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            while (countdownValue > 0) {
                delay(1000)
                countdownValue -= 1
            }
            isCountingDown = false
            isPlaying = true
        }
    }

    val backgroundColor = remember(bgHex) {
        try {
            Color(android.graphics.Color.parseColor(bgHex))
        } catch (_: Exception) {
            Color.Black
        }
    }

    val textColor = remember(fgHex) {
        try {
            Color(android.graphics.Color.parseColor(fgHex))
        } catch (_: Exception) {
            Color.White
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Tapping anywhere reveals controls or toggles play/pause
                if (!showControls) {
                    showControls = true
                } else {
                    if (activePanel != null) {
                        activePanel = null
                    } else {
                        isPlaying = !isPlaying
                    }
                }
            }
            .testTag("teleprompter_screen")
    ) {
        val totalHeight = maxHeight
        val totalWidth = maxWidth

        // 1. Text Container with Mirror Mode Transformation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = if (mirrorHorizontal) -1f else 1f
                    scaleY = if (mirrorVertical) -1f else 1f
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(columnWidth.fraction)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Generous top padding so initial text starts aligned with the cue guide
                Spacer(modifier = Modifier.height(totalHeight * cueGuidePercent))

                Text(
                    text = script.content.ifBlank { "El guión no contiene texto." },
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.45f).sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("teleprompter_text_content")
                )

                // Extra bottom space so presenter can read all the way to the last line comfortably
                Spacer(modifier = Modifier.height(totalHeight * 0.75f))
            }
        }

        // 2. Reading Cue / Eye-Level Guide Line
        if (showCueGuide) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = totalHeight * cueGuidePercent - 24.dp)
                    .height(48.dp)
            ) {
                // Transparent focus highlight band
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ElectricCyan.copy(alpha = 0.04f),
                                    ElectricCyan.copy(alpha = 0.16f),
                                    ElectricCyan.copy(alpha = 0.04f)
                                )
                            )
                        )
                )

                // Left Neon Guide Arrow
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan)
                )

                // Right Neon Guide Arrow
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan)
                )
            }
        }

        // 3. Countdown Overlay (3, 2, 1, ¡Acción!)
        if (isCountingDown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable {
                        // Tap skips countdown directly to play
                        isCountingDown = false
                        isPlaying = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val countText = if (countdownValue > 0) "$countdownValue" else "¡ACCIÓN!"
                    Text(
                        text = countText,
                        fontSize = if (countdownValue > 0) 110.sp else 64.sp,
                        fontWeight = FontWeight.Black,
                        color = if (countdownValue > 0) StudioAmber else ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Mira a la cámara • Toca para iniciar ya",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 4. On-Screen HUD Controls (Animated Visibility)
        AnimatedVisibility(
            visible = showControls && !isCountingDown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top HUD Bar: Close Button, Script Title, Mirror status, Progress
                Surface(
                    color = StudioSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                        .border(1.dp, StudioSurfaceVariant, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("prompter_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar teleprompter",
                                tint = Color.White
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = script.title.ifBlank { "Teleprompter" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${(scrollProgress * 100).toInt()}% leído",
                                    color = ElectricCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (mirrorHorizontal) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• 🪞 Espejo Activo",
                                        color = StudioAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Switch to floating prompter mode
                        IconButton(
                            onClick = onSwitchToFloating,
                            modifier = Modifier.testTag("prompter_switch_to_floating")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Modo Flotante",
                                tint = StudioAmber
                            )
                        }

                        // Replay to beginning button
                        IconButton(
                            onClick = {
                                isPlaying = false
                                coroutineScope.launch { scrollState.scrollTo(0) }
                            },
                            modifier = Modifier.testTag("prompter_rewind_to_start")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Reiniciar al principio",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Subpanel overlay for Speed, Font, or Display settings
                activePanel?.let { panel ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .align(Alignment.Center),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioSurface.copy(alpha = 0.96f)),
                        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(ElectricCyan, StudioAmber)))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            when (panel) {
                                "speed" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚡ Velocidad de Desplazamiento",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "${scrollSpeed.toInt()} px/s",
                                            fontWeight = FontWeight.Black,
                                            color = StudioAmber,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { scrollSpeed = (scrollSpeed - 5f).coerceAtLeast(15f) }
                                        ) {
                                            Text("-5", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = scrollSpeed,
                                            onValueChange = { scrollSpeed = it },
                                            valueRange = 15f..200f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = StudioAmber,
                                                activeTrackColor = StudioAmber
                                            )
                                        )
                                        IconButton(
                                            onClick = { scrollSpeed = (scrollSpeed + 5f).coerceAtMost(200f) }
                                        ) {
                                            Text("+5", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Lenta (15 px/s)", fontSize = 11.sp, color = Color.Gray)
                                        Text("Normal (60 px/s)", fontSize = 11.sp, color = Color.Gray)
                                        Text("Rápida (180 px/s)", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                "font" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🔤 Tamaño del Texto",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "${fontSize.toInt()} sp",
                                            fontWeight = FontWeight.Black,
                                            color = ElectricCyan,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { fontSize = (fontSize - 2f).coerceAtLeast(20f) }
                                        ) {
                                            Text("A-", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = fontSize,
                                            onValueChange = { fontSize = it },
                                            valueRange = 20f..76f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = ElectricCyan,
                                                activeTrackColor = ElectricCyan
                                            )
                                        )
                                        IconButton(
                                            onClick = { fontSize = (fontSize + 2f).coerceAtMost(76f) }
                                        ) {
                                            Text("A+", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                "display" -> {
                                    Text(
                                        text = "⚙️ Opciones de Pantalla y Espejo",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Mirror mode horizontal toggle
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (mirrorHorizontal) ElectricCyan.copy(alpha = 0.2f) else StudioSurfaceVariant)
                                            .clickable { mirrorHorizontal = !mirrorHorizontal }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("🪞 Modo Espejo Horizontal (Beam Splitter)", color = Color.White, fontSize = 13.sp)
                                        Text(if (mirrorHorizontal) "ACTIVADO" else "DESACTIVADO", color = if (mirrorHorizontal) ElectricCyan else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Cue line toggle
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (showCueGuide) ElectricCyan.copy(alpha = 0.2f) else StudioSurfaceVariant)
                                            .clickable { showCueGuide = !showCueGuide }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("🎯 Línea Guía de Enfoque", color = Color.White, fontSize = 13.sp)
                                        Text(if (showCueGuide) "VISIBLE" else "OCULTA", color = if (showCueGuide) ElectricCyan else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Column Width
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Ancho de Columna (Evita mover ojos)", color = Color.White, fontSize = 13.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            ColumnWidthOption.values().forEach { opt ->
                                                val isSel = columnWidth == opt
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSel) ElectricCyan else StudioSurfaceVariant)
                                                        .clickable { columnWidth = opt }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        opt.label,
                                                        color = if (isSel) Color(0xFF00354E) else Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { activePanel = null },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                            ) {
                                Text("Listo", color = Color(0xFF00354E), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Bottom HUD Control Bar
                Surface(
                    color = StudioSurface.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                        .border(1.dp, StudioSurfaceVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Quick Toggles Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speed tool
                            IconButton(
                                onClick = { activePanel = if (activePanel == "speed") null else "speed" },
                                modifier = Modifier.testTag("toggle_speed_panel")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "Ajustar velocidad",
                                        tint = if (activePanel == "speed") StudioAmber else Color.White
                                    )
                                    Text(
                                        "${scrollSpeed.toInt()}",
                                        fontSize = 10.sp,
                                        color = if (activePanel == "speed") StudioAmber else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Font size tool
                            IconButton(
                                onClick = { activePanel = if (activePanel == "font") null else "font" },
                                modifier = Modifier.testTag("toggle_font_panel")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = "Ajustar tamaño de texto",
                                        tint = if (activePanel == "font") ElectricCyan else Color.White
                                    )
                                    Text(
                                        "${fontSize.toInt()}sp",
                                        fontSize = 10.sp,
                                        color = if (activePanel == "font") ElectricCyan else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 1-Tap Mirror mode toggle!
                            IconButton(
                                onClick = { mirrorHorizontal = !mirrorHorizontal },
                                modifier = Modifier.testTag("toggle_mirror_mode")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Flip,
                                        contentDescription = "Modo espejo",
                                        tint = if (mirrorHorizontal) StudioAmber else Color.White
                                    )
                                    Text(
                                        if (mirrorHorizontal) "Espejo ON" else "Espejo",
                                        fontSize = 10.sp,
                                        color = if (mirrorHorizontal) StudioAmber else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Display settings tool
                            IconButton(
                                onClick = { activePanel = if (activePanel == "display") null else "display" },
                                modifier = Modifier.testTag("toggle_display_panel")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ViewWeek,
                                        contentDescription = "Ajustes de pantalla",
                                        tint = if (activePanel == "display") ElectricCyan else Color.White
                                    )
                                    Text(
                                        columnWidth.label,
                                        fontSize = 10.sp,
                                        color = if (activePanel == "display") ElectricCyan else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Primary Playback Controls: Rewind, Big Play/Pause, Forward
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rewind 5s
                            IconButton(
                                onClick = {
                                    scrollState.dispatchRawDelta(-scrollSpeed * 5f * density)
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .testTag("prompter_rewind_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Retroceder 5 segundos",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            // Main Play/Pause Button
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            if (isPlaying) listOf(StudioCoral, Color(0xFFBE123C))
                                            else listOf(ElectricCyan, Color(0xFF0284C7))
                                        )
                                    )
                                    .clickable {
                                        if (!isPlaying && scrollState.value == 0) {
                                            startWithCountdown()
                                        } else {
                                            isPlaying = !isPlaying
                                        }
                                    }
                                    .testTag("prompter_play_pause_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pausar lectura" else "Iniciar lectura",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            // Fast Forward 5s
                            IconButton(
                                onClick = {
                                    scrollState.dispatchRawDelta(scrollSpeed * 5f * density)
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .testTag("prompter_forward_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Adelantar 5 segundos",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
