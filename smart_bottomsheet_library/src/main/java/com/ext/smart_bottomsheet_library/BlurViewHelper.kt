package com.ext.smart_bottomsheet_library

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View

/**
 * BlurViewHelper - Applies blur effect to background views
 * Works on Android 12+ (API 31+)
 */
class BlurViewHelper(
    private val blurRadius: Float
) {

    /**
     * Apply blur effect to view
     * This should be applied to the dim/background view, NOT the sheet itself
     */
    fun applyBlur(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val effect = RenderEffect.createBlurEffect(
                    blurRadius,
                    blurRadius,
                    Shader.TileMode.CLAMP
                )
                view.setRenderEffect(effect)
            } catch (e: Exception) {
                // Blur not supported on this device
                e.printStackTrace()
            }
        }
    }

    /**
     * Remove blur effect
     */
    fun removeBlur(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(null)
        }
    }

    companion object {
        /**
         * Check if blur is supported on this device
         */
        fun isBlurSupported(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }
    }
}