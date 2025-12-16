package com.ext.smart_bottomsheet_library

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi

/**
 * BlurViewHelper - Creates real glassmorphism blur effect
 * Supports both modern (Android 12+) and legacy (Android 4.3+) blur methods
 */
class BlurViewHelper(
    private val blurRadius: Float
) {

    private var renderScript: RenderScript? = null
    private var blurScript: ScriptIntrinsicBlur? = null
    private var blurredBitmap: Bitmap? = null

    /**
     * Apply real background blur effect
     * This blurs the content BEHIND the view, not the view itself
     */
    fun applyBackgroundBlur(targetView: View, dimOverlay: View?) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Modern approach: Use RenderEffect (Android 12+)
                applyModernBlur(dimOverlay)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> {
                // Legacy approach: Use RenderScript (Android 4.3+)
                applyLegacyBlur(targetView, dimOverlay)
            }
            else -> {
                // Fallback: Just show semi-transparent overlay
                // No blur support on this device
            }
        }
    }

    /**
     * Modern blur using RenderEffect (Android 12+)
     * This creates a real-time blur of background content
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyModernBlur(dimOverlay: View?) {
        try {
            dimOverlay?.let { overlay ->
                // Create blur effect that will blur everything behind this view
                val blurEffect = RenderEffect.createBlurEffect(
                    blurRadius,
                    blurRadius,
                    Shader.TileMode.CLAMP
                )

                // CRITICAL: Apply blur to the overlay which sits BEHIND the sheet
                // This creates the glassmorphism effect
                overlay.setRenderEffect(blurEffect)

                // Make overlay semi-transparent so blur is visible
                overlay.alpha = 0.95f
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Legacy blur using RenderScript (Android 4.3 - 11)
     * Captures background and applies blur effect
     */
    private fun applyLegacyBlur(targetView: View, dimOverlay: View?) {
        try {
            val parent = targetView.parent as? ViewGroup ?: return

            // Capture the background content
            val backgroundBitmap = captureBackground(parent, targetView)

            // Apply blur to captured bitmap
            val blurred = blurBitmap(targetView.context, backgroundBitmap)

            // Set as background of dim overlay
            dimOverlay?.let { overlay ->
                overlay.background = android.graphics.drawable.BitmapDrawable(
                    overlay.resources,
                    blurred
                )
                overlay.alpha = 0.95f
            }

            // Store for cleanup
            blurredBitmap = blurred

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Capture background content (everything except the target view)
     */
    private fun captureBackground(parent: ViewGroup, excludeView: View): Bitmap {
        val width = parent.width.coerceAtLeast(1)
        val height = parent.height.coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Temporarily hide the target view
        val originalVisibility = excludeView.visibility
        excludeView.visibility = View.INVISIBLE

        // Draw parent with all children except target
        parent.draw(canvas)

        // Restore visibility
        excludeView.visibility = originalVisibility

        return bitmap
    }

    /**
     * Apply RenderScript blur to bitmap
     */
    private fun blurBitmap(context: android.content.Context, bitmap: Bitmap): Bitmap {
        // Create output bitmap
        val outputBitmap = Bitmap.createBitmap(bitmap)

        try {
            // Initialize RenderScript
            if (renderScript == null) {
                renderScript = RenderScript.create(context)
            }

            // Create blur script
            if (blurScript == null) {
                blurScript = ScriptIntrinsicBlur.create(
                    renderScript,
                    Element.U8_4(renderScript)
                )
            }

            // Set blur radius (1-25)
            blurScript?.setRadius(blurRadius.coerceIn(1f, 25f))

            // Create allocations
            val inputAllocation = Allocation.createFromBitmap(renderScript, bitmap)
            val outputAllocation = Allocation.createFromBitmap(renderScript, outputBitmap)

            // Apply blur
            blurScript?.setInput(inputAllocation)
            blurScript?.forEach(outputAllocation)

            // Copy result to output bitmap
            outputAllocation.copyTo(outputBitmap)

            // Cleanup allocations
            inputAllocation.destroy()
            outputAllocation.destroy()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return outputBitmap
    }

    /**
     * Remove blur effect and cleanup resources
     */
    fun removeBlur(dimOverlay: View?) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                dimOverlay?.setRenderEffect(null)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> {
                dimOverlay?.background = null
                blurredBitmap?.recycle()
                blurredBitmap = null
            }
        }
    }

    /**
     * Update blur radius dynamically
     */
    fun updateBlurRadius(newRadius: Float, targetView: View, dimOverlay: View?) {
        // For modern devices, we need to recreate the effect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyModernBlur(dimOverlay)
        } else {
            // For legacy, would need to recapture and reblur
            applyLegacyBlur(targetView, dimOverlay)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        blurScript?.destroy()
        blurScript = null
        renderScript?.destroy()
        renderScript = null
        blurredBitmap?.recycle()
        blurredBitmap = null
    }

    companion object {
        /**
         * Check if blur is supported on this device
         */
        fun isBlurSupported(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
        }

        /**
         * Check if modern (real-time) blur is supported
         */
        fun isModernBlurSupported(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

        /**
         * Get recommended blur radius based on device capabilities
         */
        fun getRecommendedBlurRadius(): Float {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> 25f // Modern blur
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> 15f // RenderScript
                else -> 0f // No blur
            }
        }
    }
}


//package com.ext.smart_bottomsheet_library
//
//import android.graphics.RenderEffect
//import android.graphics.Shader
//import android.os.Build
//import android.view.View
//
///**
// * BlurViewHelper - Applies blur effect to background views
// * Works on Android 12+ (API 31+)
// */
//class BlurViewHelper(
//    private val blurRadius: Float
//) {
//
//    /**
//     * Apply blur effect to view
//     * This should be applied to the dim/background view, NOT the sheet itself
//     */
//    fun applyBlur(view: View) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            try {
//                val effect = RenderEffect.createBlurEffect(
//                    blurRadius,
//                    blurRadius,
//                    Shader.TileMode.CLAMP
//                )
//                view.setRenderEffect(effect)
//            } catch (e: Exception) {
//                // Blur not supported on this device
//                e.printStackTrace()
//            }
//        }
//    }
//
//    /**
//     * Remove blur effect
//     */
//    fun removeBlur(view: View) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            view.setRenderEffect(null)
//        }
//    }
//
//    companion object {
//        /**
//         * Check if blur is supported on this device
//         */
//        fun isBlurSupported(): Boolean {
//            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
//        }
//    }
//}