package neth.iecal.curbox.data.sync

import android.content.Context

fun createSyncProvider(context: Context): SyncProvider = PlaystoreSyncProvider(context)
