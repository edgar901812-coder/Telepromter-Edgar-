package com.example

import com.example.data.model.Script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptUnitTest {

    @Test
    fun testWordCountAndDuration() {
        val content = "Hola amigos de YouTube bienvenidos a este nuevo video de teleprompter para influencers."
        val script = Script(
            title = "Test Script",
            content = content
        )

        assertEquals(13, script.wordCount)
        assertTrue(script.estimatedDurationSeconds > 0)
        assertTrue(script.formattedDuration.isNotEmpty())
    }

    @Test
    fun testEmptyScript() {
        val script = Script(
            title = "Empty",
            content = "   "
        )

        assertEquals(0, script.wordCount)
        assertEquals(0, script.estimatedDurationSeconds)
    }
}
