package com.example

import android.os.Bundle
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ScriptEditorScreen
import com.example.ui.screens.ScriptListScreen
import com.example.ui.screens.TeleprompterScreen
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
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val scripts by viewModel.scripts.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeScript by viewModel.activeScript.collectAsStateWithLifecycle()
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
                    // Prompter screen is full bleed immersive (0 padding); others respect system bars
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
                            onImportUri = { uri -> viewModel.importFromUri(uri) }
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
                                }
                            )
                        } ?: run {
                            viewModel.navigateToList()
                        }
                    }
                }
            }
        }
    }
}
