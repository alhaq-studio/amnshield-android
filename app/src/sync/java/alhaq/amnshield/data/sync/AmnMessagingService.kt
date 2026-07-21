package neth.iecal.curbox.data.sync

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CurboxMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("fcm","turning on sync")
        SyncGateway.init(applicationContext)
        val provider = SyncGateway.provider
        if (!provider.isAvailable) return
        val playstore = provider as? PlaystoreSyncProvider
        if (playstore != null) {
            playstore.wake()
        } else {
            CoroutineScope(Dispatchers.IO).launch { runCatching { provider.refresh() } }
        }
    }

    override fun onNewToken(token: String) {
        SyncGateway.init(applicationContext)
        (SyncGateway.provider as? PlaystoreSyncProvider)?.onFcmToken(token)
    }
}
