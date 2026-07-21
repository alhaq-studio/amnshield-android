package neth.iecal.curbox.data.sync

object FcmConfig {
    const val PROJECT_ID = "curbox-e6b94"
    const val APP_ID = "1:473249600577:android:db87484e8af0393c9122c0"
    const val API_KEY = "AIzaSyDQyiAGMpP0JHx8A8CJl42f9wOH59tvyu0"
    const val SENDER_ID = "473249600577"

    val isConfigured: Boolean
        get() = PROJECT_ID.isNotBlank() && APP_ID.isNotBlank() && API_KEY.isNotBlank() && SENDER_ID.isNotBlank()
}
