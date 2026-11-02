package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Script
import com.example.data.repository.ScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

sealed interface Screen {
    object List : Screen
    data class Edit(val scriptId: Long) : Screen
    data class Prompter(val scriptId: Long) : Screen
}

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

class TeleprompterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScriptRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = ScriptRepository(db.scriptDao())
    }

    // Navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.List)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Filter & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val categories = listOf("Todos", "TikTok/Reels", "YouTube", "Podcast", "Discursos", "Importados")

    // Filtered scripts flow
    val scripts: StateFlow<List<Script>> = combine(
        repository.allScripts,
        _searchQuery,
        _selectedCategory
    ) { all, query, category ->
        all.filter { script ->
            val matchesQuery = query.isBlank() ||
                    script.title.contains(query, ignoreCase = true) ||
                    script.content.contains(query, ignoreCase = true)
            val matchesCategory = category == "Todos" || script.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active Script being edited or read
    private val _activeScript = MutableStateFlow<Script?>(null)
    val activeScript: StateFlow<Script?> = _activeScript.asStateFlow()

    // Notification toast / snackbar
    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    fun dismissNotification() {
        _notification.value = null
    }

    fun showNotification(message: String, isError: Boolean = false) {
        _notification.value = UiNotification(message, isError)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun navigateToList() {
        _currentScreen.value = Screen.List
    }

    fun navigateToEdit(scriptId: Long) {
        viewModelScope.launch {
            if (scriptId == 0L) {
                // New script
                _activeScript.value = Script(
                    title = "",
                    content = "",
                    category = if (_selectedCategory.value != "Todos") _selectedCategory.value else "TikTok/Reels",
                    fontSize = 36f,
                    scrollSpeed = 60f,
                    mirrorHorizontal = false,
                    mirrorVertical = false,
                    showCueGuide = true
                )
            } else {
                _activeScript.value = repository.getScriptOnce(scriptId)
            }
            _currentScreen.value = Screen.Edit(scriptId)
        }
    }

    fun navigateToPrompter(scriptId: Long) {
        viewModelScope.launch {
            val script = repository.getScriptOnce(scriptId)
            if (script != null) {
                _activeScript.value = script
                _currentScreen.value = Screen.Prompter(scriptId)
            } else {
                showNotification("No se encontró el guión", isError = true)
            }
        }
    }

    fun saveScript(
        title: String,
        content: String,
        category: String,
        fontSize: Float,
        scrollSpeed: Float,
        mirrorHorizontal: Boolean,
        mirrorVertical: Boolean,
        showCueGuide: Boolean,
        backgroundColorHex: String,
        textColorHex: String,
        onSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val current = _activeScript.value
            val scriptToSave = (current ?: Script(title = "", content = "")).copy(
                title = title.ifBlank { "Sin Título" },
                content = content,
                category = category,
                fontSize = fontSize,
                scrollSpeed = scrollSpeed,
                mirrorHorizontal = mirrorHorizontal,
                mirrorVertical = mirrorVertical,
                showCueGuide = showCueGuide,
                backgroundColorHex = backgroundColorHex,
                textColorHex = textColorHex
            )
            val newId = repository.saveScript(scriptToSave)
            _activeScript.value = scriptToSave.copy(id = newId)
            showNotification("Guión guardado exitosamente")
            onSaved(newId)
        }
    }

    fun deleteScript(script: Script) {
        viewModelScope.launch {
            repository.deleteScript(script)
            showNotification("Guión eliminado")
            if (_activeScript.value?.id == script.id) {
                _activeScript.value = null
                _currentScreen.value = Screen.List
            }
        }
    }

    fun duplicateScript(script: Script) {
        viewModelScope.launch {
            val newId = repository.duplicateScript(script)
            showNotification("Guión duplicado")
        }
    }

    fun importFromClipboard(): Boolean {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0)?.coerceToText(getApplication())?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                val lines = text.lines()
                val inferredTitle = lines.firstOrNull { it.isNotBlank() }?.take(40) ?: "Guión desde Portapapeles"
                viewModelScope.launch {
                    val id = repository.importScript(
                        title = inferredTitle,
                        content = text,
                        category = "Importados"
                    )
                    showNotification("Guión importado desde portapapeles ($inferredTitle)")
                    navigateToEdit(id)
                }
                return true
            }
        }
        showNotification("El portapapeles está vacío o no contiene texto", isError = true)
        return false
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver
                
                // Try resolving file display name
                var fileName = "Archivo Importado"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) {
                                fileName = name.substringBeforeLast(".")
                            }
                        }
                    }
                }

                val stringBuilder = StringBuilder()
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            stringBuilder.appendLine(line)
                            line = reader.readLine()
                        }
                    }
                }

                val fileContent = stringBuilder.toString().trim()
                if (fileContent.isNotBlank()) {
                    val id = repository.importScript(
                        title = fileName,
                        content = fileContent,
                        category = "Importados"
                    )
                    showNotification("Archivo importado: $fileName")
                    navigateToEdit(id)
                } else {
                    showNotification("El archivo seleccionado está vacío", isError = true)
                }
            } catch (e: Exception) {
                showNotification("Error al leer archivo: ${e.localizedMessage ?: "Formato no compatible"}", isError = true)
            }
        }
    }

    fun updatePrompterSettings(
        fontSize: Float,
        scrollSpeed: Float,
        mirrorHorizontal: Boolean,
        mirrorVertical: Boolean,
        showCueGuide: Boolean,
        backgroundColorHex: String,
        textColorHex: String
    ) {
        val current = _activeScript.value ?: return
        val updated = current.copy(
            fontSize = fontSize,
            scrollSpeed = scrollSpeed,
            mirrorHorizontal = mirrorHorizontal,
            mirrorVertical = mirrorVertical,
            showCueGuide = showCueGuide,
            backgroundColorHex = backgroundColorHex,
            textColorHex = textColorHex
        )
        _activeScript.value = updated
        viewModelScope.launch {
            repository.saveScript(updated)
        }
    }
}
