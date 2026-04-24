package com.alhaq.deenshield.data

/**
 * Data class representing a video item in the attention span tracking system.
 * Used to track user's viewing behavior for reels and short-form videos.
 */
data class AttentionSpanVideoItem(
    val videoId: String,
    val timestamp: Long,
    val elapsedTime: Long,
    val packageName: String
)
