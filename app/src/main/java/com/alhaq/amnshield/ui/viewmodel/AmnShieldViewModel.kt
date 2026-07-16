package com.alhaq.amnshield.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.alhaq.amnshield.ui.state.AmnShieldState
import com.alhaq.amnshield.ui.state.AppTheme

class AmnShieldViewModel : ViewModel() {
    private val _state = MutableStateFlow(AmnShieldState())
    val state: StateFlow<AmnShieldState> = _state.asStateFlow()

    fun loadState(newState: AmnShieldState) {
        _state.update { current ->
            newState.copy(
                userName = if (newState.userName == "Alhaq DST") current.userName else newState.userName,
                userEmail = if (newState.userEmail == "alhaq.dst@gmail.com") current.userEmail else newState.userEmail,
                userBio = if (newState.userBio == "Digital Wellbeing Guardian • Staying mindful & focused.") current.userBio else newState.userBio,
                userGoalMinutes = if (newState.userGoalMinutes == 120) current.userGoalMinutes else newState.userGoalMinutes,
                focusProfileType = if (newState.focusProfileType == "Deep Focus") current.focusProfileType else newState.focusProfileType
            )
        }
    }

    fun updatePinSettings(appLock: Boolean, bypassLock: Boolean) {
        _state.update {
            it.copy(
                isAppLockEnabled = appLock,
                isBypassPinLockEnabled = bypassLock
            )
        }
    }

    fun toggleAppBlocker() {
        _state.update { it.copy(isAppBlockerEnabled = !it.isAppBlockerEnabled) }
    }

    fun toggleReelsBlocker() {
        _state.update { it.copy(isReelsBlockerEnabled = !it.isReelsBlockerEnabled) }
    }

    fun toggleKeywordBlocker() {
        _state.update { it.copy(isKeywordBlockerEnabled = !it.isKeywordBlockerEnabled) }
    }

    fun toggleWebFilter() {
        _state.update { it.copy(isWebFilterEnabled = !it.isWebFilterEnabled) }
    }

    fun toggleSchedule() {
        _state.update { it.copy(isScheduleEnabled = !it.isScheduleEnabled) }
    }

    fun toggleUsageLimit() {
        _state.update { it.copy(isUsageLimitEnabled = !it.isUsageLimitEnabled) }
    }

    fun toggleFocusMode() {
        _state.update { 
            val newActive = !it.isFocusModeActive
            it.copy(
                isFocusModeActive = newActive,
                focusTimeMinutes = if (newActive) it.focusTimeMinutes + 1 else it.focusTimeMinutes
            ) 
        }
    }

    fun updateTheme(theme: AppTheme) {
        _state.update { it.copy(currentTheme = theme) }
    }

    fun completeSetup() {
        _state.update { it.copy(isSetupComplete = true) }
    }

    fun toggleAccessibilityPermission() {
        _state.update { 
            val newValue = !it.permissionAccessibilityDone
            it.copy(permissionAccessibilityDone = newValue) 
        }
    }

    fun toggleBackgroundPermission() {
        _state.update { 
            val newValue = !it.permissionBackgroundDone
            it.copy(permissionBackgroundDone = newValue) 
        }
    }

    fun toggleNotificationsPermission() {
        _state.update { 
            val newValue = !it.permissionNotificationsDone
            it.copy(permissionNotificationsDone = newValue) 
        }
    }

    fun addKeyword(keyword: String) {
        if (keyword.isBlank()) return
        _state.update { it.copy(keywords = it.keywords + keyword.trim()) }
    }

    fun removeKeyword(keyword: String) {
        _state.update { it.copy(keywords = it.keywords.filter { kw -> kw != keyword }) }
    }

    fun addBlockedWebsite(website: String) {
        if (website.isBlank()) return
        _state.update { it.copy(customBlockedWebsites = it.customBlockedWebsites + website.trim().lowercase()) }
    }

    fun removeBlockedWebsite(website: String) {
        _state.update { it.copy(customBlockedWebsites = it.customBlockedWebsites.filter { s -> s != website }) }
    }

    fun dismissNotification(id: String) {
        _state.update { it.copy(notifications = it.notifications.filter { item -> item.id != id }) }
    }

    fun clearAllNotifications() {
        _state.update { it.copy(notifications = emptyList()) }
    }

    // Modal toggles
    fun setShowAddKeywordModal(show: Boolean) {
        _state.update { it.copy(isShowingAddKeywordModal = show) }
    }

    fun setShowAddAppModal(show: Boolean) {
        _state.update { it.copy(isShowingAddAppModal = show) }
    }

    fun addScheduleRule(rule: com.alhaq.amnshield.ui.state.ScheduleRule) {
        _state.update { it.copy(scheduleRules = it.scheduleRules + rule) }
    }

    fun deleteScheduleRule(id: String) {
        _state.update { it.copy(scheduleRules = it.scheduleRules.filter { rule -> rule.id != id }) }
    }

    fun toggleScheduleRuleActive(id: String) {
        _state.update {
            it.copy(
                scheduleRules = it.scheduleRules.map { rule ->
                    if (rule.id == id) rule.copy(isActive = !rule.isActive) else rule
                }
            )
        }
    }

    fun updateProfile(
        name: String,
        email: String,
        bio: String,
        goalMinutes: Int,
        profileType: String,
        pinEnabled: Boolean,
        pin: String
    ) {
        _state.update {
            it.copy(
                userName = name,
                userEmail = email,
                userBio = bio,
                userGoalMinutes = goalMinutes,
                focusProfileType = profileType,
                isPinProtectionEnabled = pinEnabled,
                profilePin = pin
            )
        }
    }

    fun setAdvancedMode(enabled: Boolean) {
        _state.update { it.copy(isAdvancedMode = enabled) }
    }
}
