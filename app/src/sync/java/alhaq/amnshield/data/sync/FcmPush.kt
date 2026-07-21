package neth.iecal.curbox.data.sync

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging

object FcmPush {

    @Volatile private var initialised = false

    @Synchronized
    fun ensureInit(context: Context): Boolean {
        if (initialised) return true
        if (!FcmConfig.isConfigured) return false
        return try {
            val app = context.applicationContext
            if (FirebaseApp.getApps(app).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(FcmConfig.PROJECT_ID)
                    .setApplicationId(FcmConfig.APP_ID)
                    .setApiKey(FcmConfig.API_KEY)
                    .setGcmSenderId(FcmConfig.SENDER_ID)
                    .build()
                FirebaseApp.initializeApp(app, options)
            }
            initialised = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun token(context: Context): String? {
        if (!ensureInit(context)) return null
        return try {
            Tasks.await(FirebaseMessaging.getInstance().token)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
