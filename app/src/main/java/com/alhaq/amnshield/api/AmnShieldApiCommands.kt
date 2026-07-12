package com.alhaq.amnshield.api

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.alhaq.amnshield.blockers.FocusModeBlocker
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.google.gson.Gson
import java.util.Locale

/**
 * Executes API commands for AmnShield.
 */
object AmnShieldApiCommands {
    private const val TAG = "AmnShieldApiCommands"
    private val gson = Gson()

    suspend fun runCommand(context: Context, commandName: String?, args: Bundle): Boolean {
        val command = ApiCommand.fromNameOrNull(commandName) ?: return false
        val target = args.getString(AmnShieldApiContract.ARG_TARGET).orEmpty().trim()
        val enable = args.getBoolean(AmnShieldApiContract.ARG_ENABLE, true)
        val minutes = args.getInt(AmnShieldApiContract.ARG_MINUTES, 25)

        val loader = SavedPreferencesLoader(context)

        return try {
            when (command) {
                ApiCommand.START_FOCUS -> {
                    val focusData = loader.getFocusModeData()
                    focusData.isTurnedOn = true
                    // Update endTime
                    val endTime = System.currentTimeMillis() + (minutes * 60_000L)
                    val updatedFocusData = FocusModeBlocker.FocusModeData(
                        isTurnedOn = true,
                        endTime = endTime,
                        modeType = focusData.modeType,
                        selectedApps = focusData.selectedApps
                    )
                    loader.saveFocusModeData(updatedFocusData)
                    loader.saveFocusSessionStartTime(System.currentTimeMillis(), endTime)
                    broadcast(context, "amnshield.refresh.focusmode")
                    true
                }

                ApiCommand.STOP_FOCUS -> {
                    val focusData = loader.getFocusModeData()
                    focusData.isTurnedOn = false
                    val updatedFocusData = FocusModeBlocker.FocusModeData(
                        isTurnedOn = false,
                        endTime = -1,
                        modeType = focusData.modeType,
                        selectedApps = focusData.selectedApps
                    )
                    loader.saveFocusModeData(updatedFocusData)
                    loader.completeFocusSession()
                    broadcast(context, "amnshield.refresh.focusmode")
                    true
                }

                ApiCommand.SET_APP_BLOCKER_GROUP -> {
                    if (target.isNotEmpty()) {
                        // Treat target as package name to block/unblock
                        val blockedApps = loader.loadBlockedApps().toMutableSet()
                        if (enable) {
                            blockedApps.add(target)
                        } else {
                            blockedApps.remove(target)
                        }
                        loader.saveBlockedApps(blockedApps)
                    } else {
                        // Toggle app blocker globally
                        loader.setAppBlockerFeatureEnabled(enable)
                    }
                    broadcast(context, "amnshield.refresh.appblocker")
                    true
                }

                ApiCommand.SET_KEYWORD_BLOCKER -> {
                    loader.setKeywordBlockerFeatureEnabled(enable)
                    broadcast(context, "amnshield.refresh.keywords")
                    true
                }

                ApiCommand.SET_KEYWORD_GROUP -> {
                    if (target.isNotEmpty()) {
                        // Treat target as custom keyword to add/remove
                        val kw = target.lowercase(Locale.ROOT)
                        val keywords = loader.loadBlockedKeywords().toMutableSet()
                        if (enable) {
                            keywords.add(kw)
                        } else {
                            keywords.remove(kw)
                        }
                        loader.saveBlockedKeywords(keywords)
                    }
                    broadcast(context, "amnshield.refresh.keywords")
                    true
                }

                ApiCommand.SET_REEL_BLOCKER -> {
                    loader.setReelBlockerEnabled(enable)
                    broadcast(context, "amnshield.refresh.reelblocker")
                    true
                }

                ApiCommand.SET_GRAYSCALE_GROUP -> {
                    if (target.isNotEmpty()) {
                        val apps = loader.loadGrayScaleApps().toMutableSet()
                        if (enable) {
                            apps.add(target)
                        } else {
                            apps.remove(target)
                        }
                        loader.saveGrayScaleApps(apps)
                    }
                    true
                }

                ApiCommand.SET_REEL_COUNTER -> {
                    // Reel tracking control
                    loader.setReelBlockerEnabled(enable)
                    broadcast(context, "amnshield.refresh.reelblocker")
                    true
                }

                ApiCommand.SET_DND -> {
                    // Do Not Disturb control placeholder
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run command $commandName", e)
            false
        }
    }

    suspend fun queryState(context: Context, stateName: String?): Map<String, Any>? {
        val state = ApiState.fromNameOrNull(stateName) ?: return null
        val loader = SavedPreferencesLoader(context)

        return try {
            when (state) {
                ApiState.FOCUS_ACTIVE -> {
                    val focusData = loader.getFocusModeData()
                    val active = focusData.isTurnedOn && focusData.endTime > System.currentTimeMillis()
                    val remaining = if (active) (focusData.endTime - System.currentTimeMillis()) / 60_000L else 0L
                    mapOf(
                        "is_active" to active,
                        "remaining_minutes" to remaining
                    )
                }

                ApiState.SCREENTIME_TODAY -> {
                    // Mock screen time or read from usage helper if available
                    mapOf("screentime_ms" to 0L)
                }

                ApiState.REELS_TODAY -> {
                    mapOf("blocked_count" to 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query state $stateName", e)
            null
        }
    }

    suspend fun list(context: Context, kindName: String?): Any? {
        val kind = ApiList.fromNameOrNull(kindName) ?: return null
        val loader = SavedPreferencesLoader(context)

        return try {
            when (kind) {
                ApiList.APP_BLOCKER_GROUPS -> {
                    loader.loadBlockedApps().toList()
                }

                ApiList.KEYWORD_GROUPS -> {
                    loader.loadBlockedKeywords().toList()
                }

                ApiList.FOCUS_GROUPS -> {
                    loader.loadPinnedApps().toList()
                }

                ApiList.GRAYSCALE_GROUPS -> {
                    emptyList<String>()
                }

                ApiList.AUTO_DND_GROUPS -> {
                    emptyList<String>()
                }

                ApiList.STATUS -> {
                    val focusData = loader.getFocusModeData()
                    mapOf(
                        "app_blocker_enabled" to loader.isAppBlockerFeatureEnabled(),
                        "keyword_blocker_enabled" to loader.isKeywordBlockerFeatureEnabled(),
                        "reel_blocker_enabled" to loader.isReelBlockerEnabled(),
                        "focus_active" to (focusData.isTurnedOn && focusData.endTime > System.currentTimeMillis())
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list $kindName", e)
            null
        }
    }

    private fun broadcast(context: Context, action: String) {
        val intent = Intent(action).setPackage(context.packageName)
        context.sendBroadcast(intent)
    }
}
