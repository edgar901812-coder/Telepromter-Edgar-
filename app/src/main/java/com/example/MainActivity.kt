package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.InAppFloatingPrompter
import com.example.ui.screens.ScriptEditorScreen
import com.example.ui.screens.ScriptListScreen
import com.example.ui.screens.TeleprompterScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TeleprompterTheme
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TeleprompterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeleprompterTheme(darkTheme = true) {
                TeleprompterApp()
            }
        }
    }
}

@Composable
fun TeleprompterApp(
    viewModel: TeleprompterViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val scripts by viewModel.scripts.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeScript by viewModel.activeScript.collectAsStateWithLifecycle()
    val floatingScript by viewModel.floatingScript.collectAsStateWithLifecycle()
    val pendingOverlayPermission by viewModel.pendingOverlayPermission.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Display notifications
    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.dismissNotification()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    if (currentScreen is Screen.Prompter) androidx.compose.foundation.layout.PaddingValues()
                    else innerPadding
                )
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.List -> {
                        ScriptListScreen(
                            scripts = scripts,
                            categories = viewModel.categories,
                            selectedCategory = selectedCategory,
                            searchQuery = searchQuery,
                            onCategorySelected = { viewModel.setSelectedCategory(it) },
                            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                            onNewScript = { viewModel.navigateToEdit(0L) },
                            onEditScript = { id -> viewModel.navigateToEdit(id) },
                            onPlayScript = { id -> viewModel.navigateToPrompter(id) },
                            onDuplicateScript = { script -> viewModel.duplicateScript(script) },
                            onDeleteScript = { script -> viewModel.deleteScript(script) },
                            onImportClipboard = { viewModel.importFromClipboard() },
                            onImportUri = { uri -> viewModel.importFromUri(uri) },
                            onLaunchFloating = { script -> viewModel.startFloatingPrompter(context, script) }
                        )
                    }

                    is Screen.Edit -> {
                        ScriptEditorScreen(
                            initialScript = activeScript,
                            categories = viewModel.categories,
                            onBack = { viewModel.navigateToList() },
                            onSaveAndExit = { title, content, category, fontSize, speed, mirrorH, mirrorV, cue, bg, fg ->
                                viewModel.saveScript(
                                    title,
                                    content,
                                    category,
                                    fontSize,
                                    speed,
                                    mirrorH,
                                    mirrorV,
                                    cue,
                                    bg,
                                    fg
                                ) {
                                    viewModel.navigateToList()
                                }
                            },
                            onStartPrompter = { title, content, category, fontSize, speed, mirrorH, mirrorV, cue, bg, fg ->
                                viewModel.saveScript(
                                    title,
                                    content,
                                    category,
                                    fontSize,
                                    speed,
                                    mirrorH,
                                    mirrorV,
                                    cue,
                                    bg,
                                    fg
                                ) { savedId ->
                                    viewModel.navigateToPrompter(savedId)
                                }
                            },
                            onStartFloating = { title, content, category, fontSize, speed, mirrorH, mirrorV, cue, bg, fg ->
                                viewModel.saveScript(
                                    title,
                                    content,
                                    category,
                                    fontSize,
                                    speed,
                                    mirrorH,
                                    mirrorV,
                                    cue,
                                    bg,
                                    fg
                                ) {
                                    viewModel.activeScript.value?.let { script ->
                                        viewModel.startFloatingPrompter(context, script)
                                    }
                                }
                            }
                        )
                    }

                    is Screen.Prompter -> {
                        activeScript?.let { script ->
                            TeleprompterScreen(
                                script = script,
                                onClose = { viewModel.navigateToList() },
                                onSaveSettings = { fontSize, speed, mirrorH, mirrorV, cue, bg, fg ->
                                    viewModel.updatePrompterSettings(
                                        fontSize,
                                        speed,
                                        mirrorH,
                                        mirrorV,
                                        cue,
                                        bg,
                                        fg
                                    )
                                },
                                onSwitchToFloating = {
                                    viewModel.startFloatingPrompter(context, script)
                                    viewModel.navigateToList()
                                }
                            )
                        } ?: run {
                            viewModel.navigateToList()
                        }
                    }
                }
            }

            // In-app draggable floating prompter overlay
            floatingScript?.let { script ->
                InAppFloatingPrompter(
                    script = script,
                    onClose = { viewModel.closeInAppFloating() }
                )
            }
        }
    }

    // Overlay Permission Explanation Dialog
    pendingOverlayPermission?.let {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOverlayPermissionDialog() },
            title = {
                Text(
                    text = "Teleprompter Flotante en Pantalla",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Para mantener el teleprompter flotando sobre cualquier aplicación (como tu Cámara, Instagram, TikTok o YouTube), activa el permiso de 'Aparecer encima' en los ajustes de Android.\n\n¡Mientras tanto, ya puedes usar el widget flotante aquí dentro de la aplicación!",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            context.startActivity(fallbackIntent)
                        }
                        viewModel.dismissOverlayPermissionDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Abrir Ajustes", color = Color(0xFF00354E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissOverlayPermissionDialog() }) {
                    Text("Usar en la app", color = Color.White)
                }
            },
            containerColor = StudioSurface
        )
    }
}
