package com.alhaq.amnshield.api

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

/**
 * The bound service that Guardian and other authorized clients bind to.
 * Checks calling package permissions using [ApiAuthStore].
 */
class AmnShieldApiService : Service() {

    private val gson = Gson()

    private val binder = object : IAmnShieldApi.Stub() {

        override fun apiVersion(): Int = AmnShieldApiContract.API_VERSION

        override fun isGranted(): Boolean = callerAllowed()

        override fun execute(command: String?, args: Bundle?): String {
            if (!callerAllowed()) return AmnShieldApiContract.STATUS_DENIED
            if (ApiCommand.fromNameOrNull(command) == null) return AmnShieldApiContract.STATUS_UNKNOWN_COMMAND
            return try {
                val applied = runBlocking {
                    AmnShieldApiCommands.runCommand(applicationContext, command, args ?: Bundle())
                }
                if (applied) AmnShieldApiContract.STATUS_OK else AmnShieldApiContract.STATUS_FAILED
            } catch (e: Exception) {
                Log.e(TAG, "execute failed for $command", e)
                AmnShieldApiContract.STATUS_FAILED
            }
        }

        override fun query(state: String?): String? {
            if (!callerAllowed()) return null
            return try {
                val values = runBlocking {
                    AmnShieldApiCommands.queryState(applicationContext, state)
                } ?: return null
                gson.toJson(values)
            } catch (e: Exception) {
                Log.e(TAG, "query failed for $state", e)
                null
            }
        }

        override fun list(kind: String?): String? {
            if (!callerAllowed()) return null
            return try {
                val data = runBlocking {
                    AmnShieldApiCommands.list(applicationContext, kind)
                } ?: return null
                gson.toJson(data)
            } catch (e: Exception) {
                Log.e(TAG, "list failed for $kind", e)
                null
            }
        }
    }

    private fun callerAllowed(): Boolean {
        val packages = packageManager.getPackagesForUid(Binder.getCallingUid())
        // For development, always allow the Guardian/Firewall apps and standard debug applications.
        if (packages != null && (
            packages.contains("com.alhaq.amnshield.guardian") ||
            packages.contains("com.alhaq.amnshield.guardian.debug") ||
            packages.contains("org.alhaq.deenshield.netblock")
        )) {
            return true
        }
        return ApiAuthStore.isAnyGranted(applicationContext, packages)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        private const val TAG = "AmnShieldApiService"
    }
}
