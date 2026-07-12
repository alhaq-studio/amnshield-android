package com.alhaq.amnshield.api

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.amnshield.R

/**
 * The Allow or Deny screen a client opens to ask the user for access, like the Shizuku permission
 * prompt. On allow we remember the requesting package in [ApiAuthStore].
 */
class ApiPermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requester = requestingPackage()
        if (requester == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        if (ApiAuthStore.isApiEnabled(this) && requester in ApiAuthStore.grantedPackages(this)) {
            setResult(Activity.RESULT_OK)
            finish()
            return
        }

        val appLabel = labelFor(requester)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.api_permission_title))
            .setMessage(getString(R.string.api_permission_message, appLabel))
            .setCancelable(false)
            .setPositiveButton(R.string.api_permission_allow) { _, _ ->
                // Allowing also turns the API on so the grant takes effect right away.
                ApiAuthStore.setApiEnabled(this, true)
                ApiAuthStore.grant(this, requester)
                setResult(Activity.RESULT_OK)
                finish()
            }
            .setNegativeButton(R.string.api_permission_deny) { _, _ ->
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            .setOnCancelListener {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            .show()
    }

    /** callingPackage is reliable when launched for result; referrer is the fallback. */
    private fun requestingPackage(): String? = callingPackage ?: referrer?.host

    private fun labelFor(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
