package com.alhaq.deenshield.blockers

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.GestureDescription
import android.content.res.Resources
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.util.LruCache
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque
import java.util.Locale

class KeywordBlocker(val service: AccessibilityService) : BaseBlocker() {
    companion object {
        const val DEFAULT_REDIRECT_URL = "https://www.youtube.com/watch?v=x31tDT-4fQw&t=1s"
        private const val SAFE_STRING_TOKEN = "||SAFE||"

        private val TEXT_NODE_CLASS_NAMES = setOf(
            "android.widget.TextView",
            "android.widget.EditText",
            "android.widget.Button"
        )

        private val SEARCH_FIELD_CLASS_NAMES = setOf(
            "android.widget.EditText",
            "android.widget.AutoCompleteTextView",
            "android.widget.SearchView"
        )

        private val SEARCH_KEYWORD_HINTS = listOf(
            "search",
            "find",
            "discover",
            "query",
            "omnibox",
            "url",
            "address",
            "browser",
            "look",
            "explore"
        )

        val URL_BAR_ID_LIST = mapOf(
            "com.android.chrome" to BrowserUrlBarInfo(
                displayUrlBarId = "url_bar",
                browserSugggestionBoxId = "omnibox_suggestions_dropdown"
            ),
            "com.brave.browser" to BrowserUrlBarInfo(
                displayUrlBarId = "url_bar",
                browserSugggestionBoxId = "omnibox_suggestions_dropdown"
            ),
            "org.mozilla.firefox" to BrowserUrlBarInfo(
                displayUrlBarId = "mozac_browser_toolbar_url_view",
                browserSugggestionBoxId = "sfcnt",
            ),
            "com.opera.browser" to BrowserUrlBarInfo(
                displayUrlBarId = "url_field",
                browserSugggestionBoxId = "right_state_button",
                isSuggestionEqualToGo = true
            ),
        )
    }

    lateinit var blockedKeyword: HashSet<String>

    var redirectUrl: String = DEFAULT_REDIRECT_URL
    var isSearchAllTextFields = false
    var isUnsupportedBrowserBlockingOn = false
    var ignoredPackages: Set<String> = emptySet()

    private val browserBlocker = BrowserBlocker(service)
    private val detectionCache = LruCache<String, String>(200)
    private val wordSplitRegex = Regex("[^a-zA-Z0-9]+")

    private val recursionResultNodes: MutableList<AccessibilityNodeInfo> = mutableListOf()

    private fun containsBlockedKeyword(text: CharSequence?): String? {
        if (text.isNullOrBlank()) return null
        if (blockedKeyword.isEmpty()) return null

        val rawText = text.toString()
        val cachedResult = detectionCache.get(rawText)
        if (cachedResult != null) {
            return if (cachedResult == SAFE_STRING_TOKEN) null else cachedResult
        }

        val keywords = parseTextForKeywords(rawText)
        keywords.forEach { word ->
            if (blockedKeyword.contains(word)) {
                detectionCache.put(rawText, word)
                return word
            }
        }

        detectionCache.put(rawText, SAFE_STRING_TOKEN)
        return null
    }

    fun resetDetectionCache() {
        detectionCache.evictAll()
    }

    private fun parseTextForKeywords(input: String): Set<String> {
        fun extractWords(text: String): Set<String> {
            return text.split(wordSplitRegex)
                .filter { it.isNotEmpty() }
                .map { it.lowercase(Locale.ROOT) }
                .toSet()
        }

        val urlPattern = "([\\w-]+\\.)+[\\w-]+(/[^?#]*)?\\??([^#]*)?"
        val regex = Regex(urlPattern)
        val words = mutableSetOf<String>()

        if (regex.find(input) != null) {
            words.addAll(extractWords(input))
            regex.find(input)?.groups?.get(3)?.value?.let { queryParams ->
                queryParams.split('&').forEach { param ->
                    if ('=' in param) {
                        val (key, value) = param.split('=', limit = 2)
                        words.addAll(extractWords(key))
                        words.addAll(extractWords(value))
                    }
                }
            }
        } else {
            words.addAll(extractWords(input))
        }

        return words
    }

    fun checkIfUserGettingFreaky(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent
    ): KeywordBlockerResult {
        rootNode ?: return KeywordBlockerResult()
        if (blockedKeyword.isEmpty()) return KeywordBlockerResult()

        val packageName = event.packageName?.toString()
        val rootPackage = rootNode.packageName?.toString()
        
        // Never let keyword blocker act on DeenShield itself
        if ((packageName != null && packageName.equals("com.alhaq.deenshield", ignoreCase = true)) ||
            (rootPackage != null && rootPackage.equals("com.alhaq.deenshield", ignoreCase = true))) {
            return KeywordBlockerResult()
        }
        
        // CRITICAL: Check ignored packages BEFORE any text scanning
        // This prevents blocking system apps like Settings, launchers, etc.
        if (!packageName.isNullOrEmpty() && ignoredPackages.contains(packageName)) {
            return KeywordBlockerResult()
        }

        browserBlocker.isTurnedOn = isUnsupportedBrowserBlockingOn
        if (isUnsupportedBrowserBlockingOn && browserBlocker.isAppBrowser(event)) {
            return KeywordBlockerResult(isHomePressRequested = true, resultDetectWord = "/ unsupported browser")
        }

        var detectedAdultKeyword: String? = null

        if (isSearchAllTextFields) {
            recursionResultNodes.clear()
            for (className in TEXT_NODE_CLASS_NAMES) {
                findNodesByClassName(rootNode, className, false)
            }

            try {
                for (node in recursionResultNodes) {
                    val textKeyword = containsBlockedKeyword(node.text)
                    if (textKeyword != null) {
                        detectedAdultKeyword = textKeyword
                        break
                    }
                    val descKeyword = containsBlockedKeyword(node.contentDescription)
                    if (descKeyword != null) {
                        detectedAdultKeyword = descKeyword
                        break
                    }
                }
            } catch (_: Exception) {
            } finally {
                recursionResultNodes.clear()
            }
        } else if (!packageName.isNullOrEmpty()) {
            recursionResultNodes.clear()
            for (className in SEARCH_FIELD_CLASS_NAMES) {
                findNodesByClassName(rootNode, className, false)
            }

            try {
                for (node in recursionResultNodes) {
                    if (!isLikelySearchField(node, packageName)) continue

                    val textKeyword = containsBlockedKeyword(node.text)
                    if (textKeyword != null) {
                        detectedAdultKeyword = textKeyword
                        break
                    }
                    val descKeyword = containsBlockedKeyword(node.contentDescription)
                    if (descKeyword != null) {
                        detectedAdultKeyword = descKeyword
                        break
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val hintKeyword = containsBlockedKeyword(node.hintText)
                        if (hintKeyword != null) {
                            detectedAdultKeyword = hintKeyword
                            break
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                recursionResultNodes.clear()
            }
        }

        if (detectedAdultKeyword == null) {
            event.text.forEach { entry ->
                containsBlockedKeyword(entry)?.let {
                    detectedAdultKeyword = it
                    return@forEach
                }
            }
        }

        val urlBarInfo = packageName?.let { URL_BAR_ID_LIST[it] }
        if (urlBarInfo == null) {
            return if (detectedAdultKeyword != null) {
                KeywordBlockerResult(isHomePressRequested = true, resultDetectWord = detectedAdultKeyword)
            } else {
                KeywordBlockerResult()
            }
        }

        val idPrefixPart = "$packageName:id/"
        val displayUrlTextNode =
            ViewBlocker.findElementById(rootNode, idPrefixPart + urlBarInfo.displayUrlBarId)

        if (detectedAdultKeyword == null) {
            detectedAdultKeyword = searchKeywordsInWebViewTitle(rootNode)
                ?: containsBlockedKeyword(displayUrlTextNode?.text)
                ?: return KeywordBlockerResult()
        }

        performSmallUpwardScroll()
        Thread.sleep(200)
        displayUrlTextNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Thread.sleep(200)

        val editUrlBarId = urlBarInfo.editUrlBarId ?: urlBarInfo.displayUrlBarId
        val editUrlBar = ViewBlocker.findElementById(rootNode, idPrefixPart + editUrlBarId)
            ?: findFirstEditableField(rootNode, packageName)
            ?: return KeywordBlockerResult(isHomePressRequested = false, resultDetectWord = detectedAdultKeyword)

        editUrlBar.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                redirectUrl
            )
        })
        Thread.sleep(300)
        val goBtnNode =
            ViewBlocker.findElementById(rootNode, idPrefixPart + urlBarInfo.browserSugggestionBoxId)
                ?: return KeywordBlockerResult(resultDetectWord = detectedAdultKeyword)
        if (urlBarInfo.isSuggestionEqualToGo) {
            goBtnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            val goNode = goBtnNode.getChild(urlBarInfo.suggestionBoxIndexOfGoBtn)
                ?: return KeywordBlockerResult(resultDetectWord = detectedAdultKeyword)
            goNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        return KeywordBlockerResult(resultDetectWord = detectedAdultKeyword)
    }

    private fun searchKeywordsInWebViewTitle(rootNode: AccessibilityNodeInfo): String? {
        recursionResultNodes.clear()
        return try {
            findNodesByClassName(rootNode, "android.webkit.WebView")
            val webView = recursionResultNodes.getOrNull(0)
            containsBlockedKeyword(webView?.text)
        } catch (_: Exception) {
            null
        } finally {
            recursionResultNodes.clear()
        }
    }

    private fun findFirstEditableField(
        rootNode: AccessibilityNodeInfo,
        packageName: String?
    ): AccessibilityNodeInfo? {
        packageName ?: return null
        val nodesToVisit: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        nodesToVisit.add(rootNode)
        while (nodesToVisit.isNotEmpty()) {
            val node = nodesToVisit.removeFirst()
            if (node.packageName == packageName && node.className == "android.widget.EditText") {
                return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let(nodesToVisit::addLast)
            }
        }
        return null
    }

    private fun findNodesByClassName(
        node: AccessibilityNodeInfo?,
        targetClassName: String,
        returnOnFirstResult: Boolean = true
    ) {
        node ?: return

        if (node.className == targetClassName) {
            recursionResultNodes.add(node)
            if (returnOnFirstResult) return
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            findNodesByClassName(child, targetClassName, returnOnFirstResult)
            if (returnOnFirstResult && recursionResultNodes.isNotEmpty()) return
        }
    }

    private fun isLikelySearchField(node: AccessibilityNodeInfo, packageName: String): Boolean {
        val className = node.className?.toString()
        val isEditableField = node.isEditable || (className != null && SEARCH_FIELD_CLASS_NAMES.contains(className))
        if (!isEditableField) return false

        val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT)
        if (!viewId.isNullOrEmpty()) {
            val strippedId = if (viewId.startsWith("$packageName:")) {
                viewId.removePrefix("$packageName:")
            } else {
                viewId
            }
            if (SEARCH_KEYWORD_HINTS.any { strippedId.contains(it) }) return true
        }

        val description = node.contentDescription?.toString()?.lowercase(Locale.ROOT)
        if (!description.isNullOrEmpty() && SEARCH_KEYWORD_HINTS.any { description.contains(it) }) {
            return true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hint = node.hintText?.toString()?.lowercase(Locale.ROOT)
            if (!hint.isNullOrEmpty() && SEARCH_KEYWORD_HINTS.any { hint.contains(it) }) {
                return true
            }
        }

        val parentNode = node.parent
        val parentId = parentNode?.viewIdResourceName?.lowercase(Locale.ROOT)
        parentNode?.recycle()
        if (!parentId.isNullOrEmpty() && SEARCH_KEYWORD_HINTS.any { parentId.contains(it) }) {
            return true
        }

        return false
    }

    fun performSmallUpwardScroll() {
        val path = Path()
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val startY = (screenHeight * 0.75).toFloat()
        val endY = startY - (screenHeight * 0.1).toFloat()
        val centerX = Resources.getSystem().displayMetrics.widthPixels / 2f

        path.moveTo(centerX, startY)
        path.lineTo(centerX, endY)

        val gestureBuilder = GestureDescription.Builder()
        val gestureStroke = GestureDescription.StrokeDescription(
            path,
            0,
            200
        )

        val gesture = gestureBuilder
            .addStroke(gestureStroke)
            .build()

        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                service.performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }, null)
    }

    data class BrowserUrlBarInfo(
        val displayUrlBarId: String,
        val editUrlBarId: String? = null,
        val browserSugggestionBoxId: String,
        val suggestionBoxIndexOfGoBtn: Int = 0,
        val isSuggestionEqualToGo: Boolean = false
    )

    data class KeywordBlockerResult(
        val isHomePressRequested: Boolean = false,
        val resultDetectWord: String? = null
    )
}
