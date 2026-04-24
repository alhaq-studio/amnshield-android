package com.alhaq.deenshield.utils

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
fun captureNodeBitmap(service: AccessibilityService, node: AccessibilityNodeInfo, callback: (Bitmap?) -> Unit) {
    // 1. Get the screen bounds of the node that we want to capture.
    val nodeBounds = Rect()
    node.getBoundsInScreen(nodeBounds)

    // 2. Take a screenshot of the entire display.
    service.takeScreenshot(0, service.mainExecutor, object : AccessibilityService.TakeScreenshotCallback {
        override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
            // 3. Convert the resulting HardwareBuffer to a software Bitmap.
            val hardwareBitmap = screenshotResult.hardwareBuffer
            val fullBitmap = Bitmap.wrapHardwareBuffer(hardwareBitmap, screenshotResult.colorSpace)

            if (fullBitmap != null) {
                // 4. Crop the full screenshot to the specific bounds of the node.
                try {
                    // Check if the crop area is valid and within the bitmap's dimensions.
                    if (nodeBounds.left >= 0 && nodeBounds.top >= 0 &&
                        nodeBounds.right <= fullBitmap.width && nodeBounds.bottom <= fullBitmap.height &&
                        nodeBounds.width() > 0 && nodeBounds.height() > 0) {

                        val croppedBitmap = Bitmap.createBitmap(
                            fullBitmap,
                            nodeBounds.left,
                            nodeBounds.top,
                            nodeBounds.width(),
                            nodeBounds.height()
                        )
                        
                        // Convert to software bitmap (ARGB_8888) to allow pixel access
                        // Hardware bitmaps cannot be read by getPixel() which is used in skin detection
                        val softwareBitmap = croppedBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        
                        callback(softwareBitmap)
                    } else {
                        // The node's bounds are outside the screen, so we can't create a bitmap.
                        callback(null)
                    }
                } finally {
                    // 5. IMPORTANT: Always close the hardware buffer to avoid memory leaks.
                    hardwareBitmap.close()
                }
            } else {
                callback(null)
            }
        }

        override fun onFailure(i: Int) {
            // The screenshot attempt failed.
            callback(null)
        }
    })
}
