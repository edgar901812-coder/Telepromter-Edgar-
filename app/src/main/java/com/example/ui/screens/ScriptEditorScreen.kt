package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Script
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    initialScript: Script?,
    categories: List<String>,
    onBack: () -> Unit,
    onSaveAndExit: (
        title: String,
        content: String,
        category: String,
        fontSize: Float,
        scrollSpeed: Float,
        mirrorHorizontal: Boolean,
        mirrorVertical: Boolean,
        showCueGuide: Boolean,
        backgroundColorHex: String,
        textColorHex: String
    ) -> Unit,
    onStartPrompter: (
        title: String,
        content: String,
        category: String,
        fontSize: Float,
        scrollSpeed: Float,
        mirrorHorizontal: Boolean,
        mirrorVertical: Boolean,
        showCueGuide: Boolean,
        backgroundColorHex: String,
        textColorHex: String
    ) -> Unit,
    onStartFloating: (
        title: String,
        content: String,
        category: String,
        fontSize: Float,
        scrollSpeed: Float,
        mirrorHorizontal: Boolean,
        mirrorVertical: Boolean,
        showCueGuide: Boolean,
        backgroundColorHex: String,
        textColorHex: String
    ) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var title by remember { mutableStateOf(initialScript?.title ?: "") }
    var content by remember { mutableStateOf(initialScript?.content ?: "") }
    var category by remember { mutableStateOf(initialScript?.category ?: "TikTok/Reels") }
    var fontSize by remember { mutableFloatStateOf(initialScript?.fontSize ?: 36f) }
    var scrollSpeed by remember { mutableFloatStateOf(initialScript?.scrollSpeed ?: 60f) }
    var mirrorHorizontal by remember { mutableStateOf(initialScript?.mirrorHorizontal ?: false) }
    var mirrorVertical by remember { mutableStateOf(initialScript?.mirrorVertical ?: false) }
    var showCueGuide by remember { mutableStateOf(initialScript?.showCueGuide ?: true) }
    var backgroundColorHex by remember { mutableStateOf(initialScript?.backgroundColorHex ?: "#000000") }
    var textColorHex by remember { mutableStateOf(initialScript?.textColorHex ?: "#FFFFFF") }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    // File picker to replace/insert content into editor
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(it)?.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            stringBuilder.appendLine(line)
                            line = reader.readLine()
                        }
                    }
                }
                val importedText = stringBuilder.toString().trim()
                if (importedText.isNotEmpty()) {
                    content = if (content.isBlank()) importedText else "$content\n\n$importedText"
                }
            } catch (_: Exception) {}
        }
    }

    // Calculations
    val wordCount = remember(content) {
        if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    }
    val estimatedDurationSec = remember(wordCount) {
        if (wordCount == 0) 0 else ((wordCount.toFloat() / 140f) * 60).toInt().coerceAtLeast(1)
    }
    val formattedDuration = remember(estimatedDurationSec) {
        val min = estimatedDurationSec / 60
        val sec = estimatedDurationSec % 60
        if (min > 0) "${min}m ${sec}s" else "${sec}s"
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("script_editor_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialScript == null || initialScript.id == 0L) "Nuevo Guión" else "Editar Guión",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                actions = {
                    // Quick Save Button
                    IconButton(
                        onClick = {
                            onSaveAndExit(
                                title,
                                content,
                                category,
                                fontSize,
                                scrollSpeed,
                                mirrorHorizontal,
                                mirrorVertical,
                                showCueGuide,
                                backgroundColorHex,
                                textColorHex
                            )
                        },
                        modifier = Modifier.testTag("save_script_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Guardar",
                            tint = ElectricCyan
                        )
                    }

                    // Start Prompter Button
                    Button(
                        onClick = {
                            onStartPrompter(
                                title,
                                content,
                                category,
                                fontSize,
                                scrollSpeed,
                                mirrorHorizontal,
                                mirrorVertical,
                                showCueGuide,
                                backgroundColorHex,
                                textColorHex
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF00354E)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("start_prompter_from_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Iniciar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Floating Prompter Button
                    IconButton(
                        onClick = {
                            onStartFloating(
                                title,
                                content,
                                category,
                                fontSize,
                                scrollSpeed,
                                mirrorHorizontal,
                                mirrorVertical,
                                showCueGuide,
                                backgroundColorHex,
                                textColorHex
                            )
                        },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("start_floating_from_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Teleprompter Flotante",
                            tint = StudioAmber
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título del Guión") },
                placeholder = { Text("Ej: Hook TikTok, Video YouTube...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("script_title_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = StudioCardBorder
                )
            )

            // Category selector dropdown
            ExposedDropdownMenuBox(
                expanded = isCategoryDropdownExpanded,
                onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría / Formato") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = StudioCardBorder
                    )
                )
                ExposedDropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false }
                ) {
                    categories.filter { it != "Todos" }.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                isCategoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Quick Actions & Metrics Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📝 $wordCount palabras",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "⏱️ ~$formattedDuration",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioAmber
                    )
                }

                Row {
                    // Paste from clipboard
                    IconButton(
                        onClick = {
                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = clipMgr?.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
                                if (!text.isNullOrBlank()) {
                                    content = if (content.isBlank()) text else "$content\n\n$text"
                                }
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("paste_into_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Pegar portapapeles",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Open file
                    IconButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("text/*", "text/plain"))
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("open_file_into_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Cargar archivo",
                            tint = StudioAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Clear
                    if (content.isNotEmpty()) {
                        IconButton(
                            onClick = { content = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar texto",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Script Content Editor
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Texto del Guión") },
                placeholder = {
                    Text(
                        "Escribe aquí o pega tu guión...\n\nConsejo: Separa las ideas en párrafos cortos para que sea más fácil leer mientras miras a la cámara."
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .testTag("script_content_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = StudioCardBorder
                )
            )

            // Prompter Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(StudioCardBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedSettings = !showAdvancedSettings },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ajustes de Lectura y Teleprompter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (showAdvancedSettings) "Ocultar" else "Personalizar",
                            fontSize = 12.sp,
                            color = ElectricCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Always show Speed & Font size sliders
                    Spacer(modifier = Modifier.height(14.dp))

                    // Font Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tamaño del Texto",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${fontSize.toInt()} sp",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 20f..72f,
                        steps = 25,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("font_size_slider")
                    )

                    // Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Velocidad de Desplazamiento",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${scrollSpeed.toInt()} px/s",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioAmber
                        )
                    }
                    Slider(
                        value = scrollSpeed,
                        onValueChange = { scrollSpeed = it },
                        valueRange = 15f..180f,
                        steps = 32,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioAmber,
                            activeTrackColor = StudioAmber
                        ),
                        modifier = Modifier.testTag("scroll_speed_slider")
                    )

                    // Mirror Mode Switches & Guide Line
                    if (showAdvancedSettings) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Horizontal Mirror Mode (Beam Splitter Glass)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🪞 Modo Espejo Horizontal",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Invierte el texto horizontalmente para cristales divisores o beam-splitter.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = mirrorHorizontal,
                                onCheckedChange = { mirrorHorizontal = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan),
                                modifier = Modifier.testTag("mirror_horizontal_switch")
                            )
                        }

                        // Vertical Mirror Mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🔃 Modo Espejo Vertical",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Invierte el texto si el teléfono se monta de cabeza en el rig.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = mirrorVertical,
                                onCheckedChange = { mirrorVertical = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan),
                                modifier = Modifier.testTag("mirror_vertical_switch")
                            )
                        }

                        // Focus guide line toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎯 Línea Guía de Mirada",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Muestra la franja de enfoque para mantener la mirada fija en el lente de la cámara.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = showCueGuide,
                                onCheckedChange = { showCueGuide = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan),
                                modifier = Modifier.testTag("cue_guide_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // High contrast palette options
                        Text(
                            text = "Paleta de Contraste del Prompter",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val themes = listOf(
                                Triple("#000000", "#FFFFFF", "OLED"),
                                Triple("#000000", "#FBBF24", "Ámbar"),
                                Triple("#131B2E", "#38BDF8", "Estudio"),
                                Triple("#FFFFFF", "#0F172A", "Claro")
                            )
                            themes.forEach { (bg, fg, label) ->
                                val isSelected = backgroundColorHex == bg && textColorHex == fg
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ElectricCyan.copy(alpha = 0.2f) else StudioSurfaceVariant)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) ElectricCyan else StudioCardBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            backgroundColorHex = bg
                                            textColorHex = fg
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(bg)))
                                            .border(1.dp, Color.Gray, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("A", color = Color(android.graphics.Color.parseColor(fg)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
