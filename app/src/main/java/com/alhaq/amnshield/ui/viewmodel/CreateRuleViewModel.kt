package com.alhaq.amnshield.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.alhaq.amnshield.ui.state.SchedulePeriod

class CreateRuleViewModel : ViewModel() {
    var ruleName by mutableStateOf("")
    var targetType by mutableStateOf("Block Schedule")
    var targetBlockerType by mutableStateOf("App Blocker")

    val selectedApps = mutableStateListOf<String>("com.instagram.android", "com.google.android.youtube")
    val selectedKeywords = mutableStateListOf<String>()
    val selectedWebsites = mutableStateListOf<String>()
    val selectedPlatforms = mutableStateListOf<String>("Instagram", "TikTok")

    var customAppInput by mutableStateOf("")
    var customKeywordInput by mutableStateOf("")
    var customWebsiteInput by mutableStateOf("")

    val periodsList = mutableStateListOf<SchedulePeriod>()

    var newStartTime by mutableStateOf("09:00")
    var newEndTime by mutableStateOf("17:00")
    val newSelectedDays = mutableStateListOf("Mon", "Tue", "Wed", "Thu", "Fri")

    var limitValueStr by mutableStateOf("5")
    
    private var isInitialized = false

    fun initialize(initialType: String, keywords: List<String>, websites: List<String>) {
        if (isInitialized) return
        targetType = initialType
        if (periodsList.isEmpty()) {
            periodsList.add(SchedulePeriod("09:00", "17:00", listOf("Mon", "Tue", "Wed", "Thu", "Fri")))
        }
        if (selectedKeywords.isEmpty() && keywords.isNotEmpty()) {
            selectedKeywords.addAll(keywords.take(2))
        }
        if (selectedWebsites.isEmpty() && websites.isNotEmpty()) {
            selectedWebsites.addAll(websites.take(2))
        }
        isInitialized = true
    }
    
    fun addSelectedApp(pkg: String) {
        if (!selectedApps.contains(pkg)) {
            selectedApps.add(pkg)
        }
    }
    
    fun removeSelectedApp(pkg: String) {
        selectedApps.remove(pkg)
    }

    fun addCustomApp() {
        val app = customAppInput.trim()
        if (app.isNotBlank()) {
            addSelectedApp(app)
            customAppInput = ""
        }
    }

    fun addSelectedKeyword(keyword: String) {
        if (!selectedKeywords.contains(keyword)) {
            selectedKeywords.add(keyword)
        }
    }

    fun removeSelectedKeyword(keyword: String) {
        selectedKeywords.remove(keyword)
    }

    fun addCustomKeyword() {
        val kw = customKeywordInput.trim()
        if (kw.isNotBlank()) {
            addSelectedKeyword(kw)
            customKeywordInput = ""
        }
    }

    fun addSelectedWebsite(domain: String) {
        if (!selectedWebsites.contains(domain)) {
            selectedWebsites.add(domain)
        }
    }

    fun removeSelectedWebsite(domain: String) {
        selectedWebsites.remove(domain)
    }

    fun addCustomWebsite() {
        val site = customWebsiteInput.trim()
        if (site.isNotBlank()) {
            addSelectedWebsite(site)
            customWebsiteInput = ""
        }
    }

    fun togglePlatform(platform: String) {
        if (selectedPlatforms.contains(platform)) {
            selectedPlatforms.remove(platform)
        } else {
            selectedPlatforms.add(platform)
        }
    }

    fun removePeriod(index: Int) {
        if (index in periodsList.indices) {
            periodsList.removeAt(index)
        }
    }

    fun addPeriod() {
        if (newStartTime.isNotBlank() && newEndTime.isNotBlank() && newSelectedDays.isNotEmpty()) {
            val newPeriod = SchedulePeriod(
                startTime = newStartTime,
                endTime = newEndTime,
                days = newSelectedDays.toList()
            )
            periodsList.add(newPeriod)
        }
    }

    fun toggleSelectedDay(day: String) {
        if (newSelectedDays.contains(day)) {
            newSelectedDays.remove(day)
        } else {
            newSelectedDays.add(day)
        }
    }
}
