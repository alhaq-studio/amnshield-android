package com.alhaq.amnshield.blockers

import android.view.accessibility.AccessibilityNodeInfo
import com.alhaq.amnshield.utils.ScheduleUtils
import com.alhaq.amnshield.utils.TimeTools

class ReelBlocker : BaseBlocker() {

    companion object {
        const val MODE_BLOCK_ALL = 0
        const val MODE_BLOCK_AFTER_DAILY_COUNT = 1

        // Platform identifiers, used as viewId prefixes for cooldown keys / warning dialog.
        const val PLATFORM_YOUTUBE = "youtube"
        const val PLATFORM_INSTAGRAM = "instagram"
        const val PLATFORM_TIKTOK = "tiktok"

        val TIKTOK_PACKAGE_NAMES = hashSetOf(
            "com.ss.android.ugc.trill",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.aweme"
        )

        // Map of native (non-browser) reel surface view IDs to their platform.
        private val NATIVE_SURFACE_PLATFORM = linkedMapOf(
            "com.instagram.android:id/root_clips_layout" to PLATFORM_INSTAGRAM,
            "com.google.android.youtube:id/reel_recycler" to PLATFORM_YOUTUBE,
            "app.revanced.android.youtube:id/reel_recycler" to PLATFORM_YOUTUBE
        )

        // Backwards-compat alias kept for older code paths.
        @JvmStatic
        val REEL_VIEW_ID_LIST: List<String> = NATIVE_SURFACE_PLATFORM.keys.toList()

        // Browsers whose URL bar we inspect for short-form video URLs.
        // (id_resource_name -> displayed URL/text view in the toolbar)
        private val BROWSER_URL_BAR_IDS = mapOf(
            "com.android.chrome" to "url_bar",
            "com.chrome.beta" to "url_bar",
            "com.chrome.dev" to "url_bar",
            "com.chrome.canary" to "url_bar",
            "com.brave.browser" to "url_bar",
            "com.microsoft.emmx" to "url_bar",
            "com.sec.android.app.sbrowser" to "location_bar_edit_text",
            "org.mozilla.firefox" to "mozac_browser_toolbar_url_view",
            "org.mozilla.focus" to "mozac_browser_toolbar_url_view",
            "com.opera.browser" to "url_field",
            "com.opera.mini.native" to "url_field",
            "com.duckduckgo.mobile.android" to "omnibarTextInput",
            "com.vivaldi.browser" to "url_bar",
            "com.kiwibrowser.browser" to "url_bar"
        )

        // URL fragments that identify short-form video surfaces, mapped to platform.
        // Order matters - first match wins.
        private val BROWSER_URL_PATTERNS: List<Pair<Regex, String>> = listOf(
            Regex("""youtube\.com/shorts/""", RegexOption.IGNORE_CASE) to PLATFORM_YOUTUBE,
            Regex("""youtu\.be/shorts/""", RegexOption.IGNORE_CASE) to PLATFORM_YOUTUBE,
            Regex("""m\.youtube\.com/shorts/""", RegexOption.IGNORE_CASE) to PLATFORM_YOUTUBE,
            Regex("""instagram\.com/reels?(/|$|\?)""", RegexOption.IGNORE_CASE) to PLATFORM_INSTAGRAM,
            Regex("""instagram\.com/[^/?#]+/reel/""", RegexOption.IGNORE_CASE) to PLATFORM_INSTAGRAM,
            Regex("""tiktok\.com""", RegexOption.IGNORE_CASE) to PLATFORM_TIKTOK
        )

        fun isBrowserPackage(packageName: String): Boolean =
            BROWSER_URL_BAR_IDS.containsKey(packageName)
    }

    private val cooldownViewIdsList = mutableMapOf<String, Long>()

    var isEnabled = false
    var isIGInboxReelAllowed = false
    var isFirstReelInFeedAllowed = false
    var modeType = MODE_BLOCK_ALL
    var dailyReelLimit = 200
    var reelsScrolledToday = 0

    // Per-platform toggles (default true so existing users keep current behavior).
    var isYoutubeEnabled = true
    var isInstagramEnabled = true
    var isTiktokEnabled = true

    // Browser shorts detection (off by default to avoid surprising existing users).
    var isBrowserShortsEnabled = false

    private var allowanceDate = TimeTools.getCurrentDate()
    private val firstReelAllowanceConsumed = hashSetOf<String>()

    private fun isPlatformEnabled(platform: String): Boolean = when (platform) {
        PLATFORM_YOUTUBE -> isYoutubeEnabled
        PLATFORM_INSTAGRAM -> isInstagramEnabled
        PLATFORM_TIKTOK -> isTiktokEnabled
        else -> true
    }

    fun doesReelNeedToBeBlocked(
        rootNode: AccessibilityNodeInfo,
        packageName: String
    ): ReelBlockerResult? {
        if (!isEnabled) {
            return null
        }

        clearAllowanceIfNeeded()

        if (TIKTOK_PACKAGE_NAMES.contains(packageName)) {
            if (!isTiktokEnabled) return null
            if (isBlockingDeferredByCountMode()) {
                return null
            }

            if (shouldAllowFirstReel(packageName)) {
                return null
            }

            if (isCooldownActive(packageName)) {
                return ReelBlockerResult(
                    isBlocked = false,
                    isReelFoundInCooldownState = true,
                    viewId = packageName,
                    requestHomePressInstead = true
                )
            }
            return ReelBlockerResult(
                isBlocked = true,
                viewId = packageName,
                requestHomePressInstead = true
            )
        }

        if (isIGInboxReelAllowed && isViewOpened(rootNode, "com.instagram.android:id/reply_bar_container")) {
            return null
        }

        for ((viewId, platform) in NATIVE_SURFACE_PLATFORM) {
            if (!isPlatformEnabled(platform)) continue
            if (isViewOpened(rootNode, viewId)) {
                if (isBlockingDeferredByCountMode()) {
                    return null
                }

                if (shouldAllowFirstReel(packageName)) {
                    return null
                }

                if (isCooldownActive(viewId)) {
                    return ReelBlockerResult(
                        isBlocked = false,
                        isReelFoundInCooldownState = true,
                        viewId = viewId
                    )
                }
                return ReelBlockerResult(isBlocked = true, viewId = viewId)
            }
        }

        // Browser short-form URL detection (Chrome, Firefox, etc.).
        if (isBrowserShortsEnabled) {
            val browserSurface = detectBrowserShortsSurface(rootNode, packageName)
            if (browserSurface != null) {
                val (cooldownKey, platform) = browserSurface
                if (!isPlatformEnabled(platform)) return null
                if (isBlockingDeferredByCountMode()) return null
                if (shouldAllowFirstReel(cooldownKey)) return null

                if (isCooldownActive(cooldownKey)) {
                    return ReelBlockerResult(
                        isBlocked = false,
                        isReelFoundInCooldownState = true,
                        viewId = cooldownKey
                    )
                }
                return ReelBlockerResult(isBlocked = true, viewId = cooldownKey)
            }
        }

        return null
    }

    fun detectReelSurfaceId(rootNode: AccessibilityNodeInfo, packageName: String): String? {
        if (!isEnabled) {
            return null
        }

        if (TIKTOK_PACKAGE_NAMES.contains(packageName)) {
            return if (isTiktokEnabled) packageName else null
        }

        if (isIGInboxReelAllowed && isViewOpened(rootNode, "com.instagram.android:id/reply_bar_container")) {
            return null
        }

        for ((viewId, platform) in NATIVE_SURFACE_PLATFORM) {
            if (!isPlatformEnabled(platform)) continue
            if (isViewOpened(rootNode, viewId)) {
                return viewId
            }
        }

        if (isBrowserShortsEnabled) {
            val browserSurface = detectBrowserShortsSurface(rootNode, packageName)
            if (browserSurface != null && isPlatformEnabled(browserSurface.second)) {
                return browserSurface.first
            }
        }

        return null
    }

    /**
     * Detects whether the active browser tab is currently displaying a short-form video URL.
     * Returns Pair(cooldownKey, platform) when matched, or null otherwise.
     */
    private fun detectBrowserShortsSurface(
        rootNode: AccessibilityNodeInfo,
        packageName: String
    ): Pair<String, String>? {
        val urlBarId = BROWSER_URL_BAR_IDS[packageName] ?: return null
        val fullId = "$packageName:id/$urlBarId"
        val urlText = readNodeText(rootNode, fullId) ?: return null
        if (urlText.isBlank()) return null

        for ((regex, platform) in BROWSER_URL_PATTERNS) {
            if (regex.containsMatchIn(urlText)) {
                return "browser:$packageName:$platform" to platform
            }
        }
        return null
    }

    private fun readNodeText(rootNode: AccessibilityNodeInfo, viewId: String): String? {
        val node = ViewBlocker.findElementById(rootNode, viewId) ?: return null
        return try {
            val text = node.text?.toString().orEmpty()
            if (text.isNotBlank()) return text
            val description = node.contentDescription?.toString().orEmpty()
            description.ifBlank { null }
        } finally {
            node.recycle()
        }
    }

    fun applyCooldown(viewId: String, endTime: Long) {
        cooldownViewIdsList[viewId] = endTime
    }

    fun restoreCooldowns(cooldowns: Map<String, Long>) {
        cooldownViewIdsList.clear()
        val now = System.currentTimeMillis()
        cooldownViewIdsList.putAll(cooldowns.filterValues { it > now })
    }

    fun getCooldownSnapshot(): Map<String, Long> {
        return HashMap(cooldownViewIdsList)
    }

    private fun isCooldownActive(viewId: String): Boolean {
        val cooldownEnd = cooldownViewIdsList[viewId] ?: return false
        if (System.currentTimeMillis() > cooldownEnd) {
            cooldownViewIdsList.remove(viewId)
            return false
        }
        return true
    }

    private fun isViewOpened(rootNode: AccessibilityNodeInfo, viewId: String): Boolean {
        val node = ViewBlocker.findElementById(rootNode, viewId)
        val opened = node != null
        node?.recycle()
        return opened
    }

    private fun isBlockingDeferredByCountMode(): Boolean {
        if (modeType != MODE_BLOCK_AFTER_DAILY_COUNT) return false
        if (dailyReelLimit <= 0) return false
        return reelsScrolledToday < dailyReelLimit
    }

    private fun shouldAllowFirstReel(packageName: String): Boolean {
        if (!isFirstReelInFeedAllowed) return false
        if (firstReelAllowanceConsumed.contains(packageName)) return false
        firstReelAllowanceConsumed.add(packageName)
        return true
    }

    private fun clearAllowanceIfNeeded() {
        val today = TimeTools.getCurrentDate()
        if (allowanceDate != today) {
            allowanceDate = today
            firstReelAllowanceConsumed.clear()
        }
    }



    data class ReelBlockerResult(
        val isBlocked: Boolean = false,
        val requestHomePressInstead: Boolean = false,
        val isReelFoundInCooldownState: Boolean = false,
        val viewId: String = ""
    )
}
