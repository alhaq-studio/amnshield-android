package com.alhaq.amnshield.api

/**
 * The public contract for the AmnShield API.
 *
 * Other apps bind to [AmnShieldApiService] over AIDL ([IAmnShieldApi]) and drive AmnShield with the command
 * and state names below. Everything here is part of the public surface.
 */
object AmnShieldApiContract {

    const val API_VERSION = 2

    // Argument keys placed in the Bundle passed to execute().
    const val ARG_TARGET = "target"
    const val ARG_ENABLE = "enable"
    const val ARG_MINUTES = "minutes"

    // Keys used in the JSON returned by query().
    const val VAR_FOCUS_GROUP = "focus_group"
    const val VAR_FOCUS_REMAINING = "focus_remaining"
    const val VAR_SCREENTIME = "screentime"
    const val VAR_REELS = "reels"

    // Status strings returned by execute().
    const val STATUS_OK = "OK"
    const val STATUS_DENIED = "DENIED"
    const val STATUS_UNKNOWN_COMMAND = "UNKNOWN_COMMAND"
    const val STATUS_FAILED = "FAILED"
}

/** Actions a client can run through execute(). */
enum class ApiCommand {
    START_FOCUS,
    STOP_FOCUS,
    SET_APP_BLOCKER_GROUP,
    SET_KEYWORD_BLOCKER,
    SET_KEYWORD_GROUP,
    SET_REEL_BLOCKER,
    SET_GRAYSCALE_GROUP,
    SET_REEL_COUNTER,
    SET_DND;

    companion object {
        fun fromNameOrNull(name: String?): ApiCommand? = entries.firstOrNull { it.name == name }
    }
}

/** States a client can read through query(). */
enum class ApiState {
    FOCUS_ACTIVE,
    SCREENTIME_TODAY,
    REELS_TODAY;

    companion object {
        fun fromNameOrNull(name: String?): ApiState? = entries.firstOrNull { it.name == name }
    }
}

/** Kinds a client can enumerate through list(). */
enum class ApiList {
    FOCUS_GROUPS,
    APP_BLOCKER_GROUPS,
    KEYWORD_GROUPS,
    GRAYSCALE_GROUPS,
    AUTO_DND_GROUPS,
    STATUS;

    companion object {
        fun fromNameOrNull(name: String?): ApiList? = entries.firstOrNull { it.name == name }
    }
}
