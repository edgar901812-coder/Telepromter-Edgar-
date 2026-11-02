package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Script
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptListScreen(
    scripts: List<Script>,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    onCategorySelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onNewScript: () -> Unit,
    onEditScript: (Long) -> Unit,
    onPlayScript: (Long) -> Unit,
    onDuplicateScript: (Script) -> Unit,
    onDeleteScript: (Script) -> Unit,
    onImportClipboard: () -> Unit,
    onImportUri: (Uri) -> Unit,
    onLaunchFloating: (Script) -> Unit
) {
    var isSearchVisible by remember { mutableStateOf(false) }
    var scriptToDelete by remember { mutableStateOf<Script?>(null) }

    // SAF file picker launcher for local files (.txt, .md, plain documents)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportUri(it) }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("script_list_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(ElectricCyan, Color(0xFF0284C7))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Teleprompter",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(StudioCoral.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = StudioCoral,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "Para Creadores e Influencers",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible },
                        modifier = Modifier.testTag("toggle_search_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Buscar guión",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewScript,
                containerColor = ElectricCyan,
                contentColor = Color(0xFF00354E),
                modifier = Modifier.testTag("fab_new_script")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nuevo Guión")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nuevo Guión",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input Row
            AnimatedVisibility(visible = isSearchVisible) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Buscar en títulos o texto...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = ElectricCyan)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_text_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = StudioCardBorder
                    )
                )
            }

            // Quick Import Banner for Influencers
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StudioSurface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(ElectricCyan.copy(alpha = 0.5f), StudioAmber.copy(alpha = 0.3f))
                    ),
                    width = 1.dp
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚡ Importación Rápida de Guiones",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pega texto copiado o importa archivos de tu dispositivo en segundos:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onImportClipboard,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_clipboard_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioSurfaceVariant,
                                contentColor = ElectricCyan
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pegar Portapapeles",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "text/*",
                                        "text/plain",
                                        "application/octet-stream"
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_file_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = StudioAmber
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Abrir Archivo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Categories Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    SuggestionChip(
                        onClick = { onCategorySelected(category) },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) ElectricCyan.copy(alpha = 0.15f) else StudioSurface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (isSelected) ElectricCyan else StudioCardBorder
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Scripts List or Empty State
            if (scripts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(StudioSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No se encontraron guiones" else "No hay guiones aún",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank())
                                "Intenta con otra palabra clave o limpia el filtro."
                            else
                                "Crea un nuevo guión o importa texto desde el portapapeles o archivos locales.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNewScript,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00354E))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Crear Primer Guión", color = Color(0xFF00354E), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(scripts, key = { it.id }) { script ->
                        ScriptItemCard(
                            script = script,
                            onPlay = { onPlayScript(script.id) },
                            onFloating = { onLaunchFloating(script) },
                            onEdit = { onEditScript(script.id) },
                            onDuplicate = { onDuplicateScript(script) },
                            onDelete = { scriptToDelete = script }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    scriptToDelete?.let { script ->
        AlertDialog(
            onDismissRequest = { scriptToDelete = null },
            title = { Text("¿Eliminar guión?", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar permanentemente \"${script.title}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteScript(script)
                        scriptToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StudioCoral)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { scriptToDelete = null }) {
                    Text("Cancelar")
                }
            },
            containerColor = StudioSurface
        )
    }
}

@Composable
fun ScriptItemCard(
    script: Script,
    onPlay: () -> Unit,
    onFloating: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("script_card_${script.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(StudioCardBorder, StudioSurfaceVariant)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Badge + Mirror mode indicator + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category pill
                    val categoryColor = when (script.category) {
                        "TikTok/Reels" -> StudioCoral
                        "YouTube" -> StudioCoral
                        "Podcast" -> StudioAmber
                        "Discursos" -> StudioEmerald
                        else -> ElectricCyan
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.15f))
                            .border(1.dp, categoryColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = script.category,
                            color = categoryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (script.mirrorHorizontal) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioAmber.copy(alpha = 0.15f))
                                .border(1.dp, StudioAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🪞 Espejo",
                                color = StudioAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Stats: Words & Reading Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏱️ ${script.formattedDuration}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${script.wordCount} pal.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = script.title.ifBlank { "Guión sin título" },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Content snippet
            Text(
                text = script.content.ifBlank { "Sin contenido todavía..." },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Primary Action: Start Prompter Fullscreen & Floating Mode
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.testTag("play_button_${script.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF00354E)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Iniciar Teleprompter",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pantalla",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Floating Mode Button
                    Button(
                        onClick = onFloating,
                        modifier = Modifier.testTag("floating_button_${script.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioSurfaceVariant,
                            contentColor = StudioAmber
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Modo Flotante",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Flotante",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Secondary actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_button_${script.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar guión",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("duplicate_button_${script.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicar guión",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_button_${script.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar guión",
                            tint = StudioCoral.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
