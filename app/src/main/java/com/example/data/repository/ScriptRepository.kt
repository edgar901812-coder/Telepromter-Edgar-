package com.example.data.repository

import com.example.data.local.ScriptDao
import com.example.data.model.Script
import kotlinx.coroutines.flow.Flow

class ScriptRepository(private val scriptDao: ScriptDao) {
    val allScripts: Flow<List<Script>> = scriptDao.getAllScripts()

    fun getScript(id: Long): Flow<Script?> = scriptDao.getScriptById(id)

    suspend fun getScriptOnce(id: Long): Script? = scriptDao.getScriptByIdOnce(id)

    suspend fun saveScript(script: Script): Long {
        val updatedScript = script.copy(updatedAt = System.currentTimeMillis())
        return if (script.id == 0L) {
            scriptDao.insertScript(updatedScript)
        } else {
            scriptDao.updateScript(updatedScript)
            script.id
        }
    }

    suspend fun deleteScript(script: Script) {
        scriptDao.deleteScript(script)
    }

    suspend fun deleteScriptById(id: Long) {
        scriptDao.deleteScriptById(id)
    }

    suspend fun duplicateScript(script: Script): Long {
        val duplicate = script.copy(
            id = 0L,
            title = "${script.title} (Copia)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return scriptDao.insertScript(duplicate)
    }

    suspend fun importScript(title: String, content: String, category: String = "Importado"): Long {
        val newScript = Script(
            title = title.ifBlank { "Guión Importado" },
            content = content,
            category = category,
            fontSize = 36f,
            scrollSpeed = 60f,
            mirrorHorizontal = false,
            mirrorVertical = false,
            showCueGuide = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return scriptDao.insertScript(newScript)
    }
}
