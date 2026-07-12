package com.alhaq.deenshield.blockers

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Legacy short-form video blocker. Detection of YouTube Shorts, Instagram
 * Reels and TikTok has been consolidated into [ReelBlocker], which now also
 * handles per-app toggles and browser short-form URLs.
 *
 * This class is retained because [findElementById] is reused by other
 * blockers and because existing user preferences and warning-config code
 * paths still reference the `view_blocker` namespace. [doesViewNeedToBeBlocked]
 * intentionally returns null so no surface is double-blocked.
 */
class ViewBlocker : BaseBlocker() {
    companion object {
        fun findElementById(node: AccessibilityNodeInfo?, id: String?): AccessibilityNodeInfo? {
            if (node == null || id.isNullOrEmpty()) return null
            return try {
                val matches = node.findAccessibilityNodeInfosByViewId(id)
                if (matches.isNullOrEmpty()) {
                    null
                } else {
                    for (i in 1 until matches.size) {
                        matches[i].recycle()
                    }
                    matches[0]
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private val cooldownViewIdsList = mutableMapOf<String, Long>()

    @Suppress("UNUSED_PARAMETER")
    fun doesViewNeedToBeBlocked(
        node: AccessibilityNodeInfo,
        packageName: String
    ): ViewBlockerResult? = null

    fun applyCooldown(viewId: String, endTime: Long) {
        cooldownViewIdsList[viewId] = endTime
    }

    fun restoreCooldowns(cooldowns: Map<String, Long>) {
        cooldownViewIdsList.clear()
        val now = System.currentTimeMillis()
        cooldownViewIdsList.putAll(cooldowns.filterValues { it > now })
    }

    fun getCooldownSnapshot(): Map<String, Long> = HashMap(cooldownViewIdsList)

    data class ViewBlockerResult(
        val isBlocked: Boolean = false,
        val requestHomePressInstead: Boolean = false,
        val isReelFoundInCooldownState: Boolean = false,
        val viewId: String = ""
    )
}
