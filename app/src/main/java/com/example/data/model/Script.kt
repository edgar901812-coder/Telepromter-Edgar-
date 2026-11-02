package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class Script(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val fontSize: Float = 36f, // sp
    val scrollSpeed: Float = 60f, // dp/sec or speed unit
    val mirrorHorizontal: Boolean = false,
    val mirrorVertical: Boolean = false,
    val cueLinePositionPercent: Float = 0.30f, // Focus guide at 30% from top (near camera)
    val showCueGuide: Boolean = true,
    val countdownSeconds: Int = 3,
    val backgroundColorHex: String = "#000000",
    val textColorHex: String = "#FFFFFF",
    val category: String = "TikTok/Reels",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val wordCount: Int
        get() {
            if (content.isBlank()) return 0
            return content.trim().split("\\s+".toRegex()).size
        }

    val estimatedDurationSeconds: Int
        get() {
            val words = wordCount
            if (words == 0) return 0
            // Standard speaking rate for influencers/creators: approx 140 words per minute
            val wordsPerMinute = 140
            return ((words.toFloat() / wordsPerMinute) * 60).toInt().coerceAtLeast(1)
        }

    val formattedDuration: String
        get() {
            val totalSec = estimatedDurationSeconds
            val min = totalSec / 60
            val sec = totalSec % 60
            return if (min > 0) {
                "${min}m ${sec}s"
            } else {
                "${sec}s"
            }
        }
}
