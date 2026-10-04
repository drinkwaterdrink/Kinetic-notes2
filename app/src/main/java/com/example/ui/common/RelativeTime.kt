package com.example.ui.common

/**
 * Compact relative timestamp for cards and list rows ("now", "5m", "2h ago", "3d ago").
 *
 * Pure function so it can be unit tested without a Clock or Android framework.
 */
fun relativeTimeLabel(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val deltaMs = nowMs - timestampMs
    if (deltaMs < 0) return "just now"

    val seconds = deltaMs / 1_000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 45 -> "just now"
        minutes < 60 -> "${minutes.coerceAtLeast(1)}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        days < 365 -> "${days / 7}w ago"
        else -> "${days / 365}y ago"
    }
}
