package com.ext.smart_bottomsheet_library

import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

/**
 * DragHelper - Enhanced version with velocity tracking and smooth animations
 * Fixed to work with parent height calculations for proper positioning
 */
class DragHelper(
    private val targetView: View,
    private val collapsedHeight: Int,
    private val parentHeight: Int,
    private val dragSensitivity: Float,
    private val settleDuration: Long = 350,
    private val onDrag: ((Float) -> Unit)? = null,
    private val onStateChange: ((Boolean) -> Unit)? = null
) {

    private var lastTouchY = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var velocityTracker: VelocityTracker? = null

    // Fling velocity threshold (px/sec)
    private val minFlingVelocity = 800f

    // Current state
    private var isExpanded = false

    // Calculate Y positions based on parent height
    private val expandedY = 0f
    private val collapsedY = (parentHeight - collapsedHeight).toFloat()

    /**
     * Check if we should intercept touch
     */
    fun shouldInterceptTouch(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchY = event.y
                // Only intercept if touching the top drag area (handle)
                initialTouchY < 150 // Top 150px is draggable
            }
            else -> isDragging
        }
    }

    /**
     * Handle touch events
     */
    fun onTouch(event: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.rawY
                isDragging = true

                // Cancel any ongoing animations
                targetView.animate().cancel()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) return false

                val dy = (event.rawY - lastTouchY) * dragSensitivity

                // Calculate new position with proper bounds
                val newY = (targetView.translationY + dy).coerceIn(expandedY, collapsedY)

                targetView.translationY = newY

                // Calculate progress (0 = collapsed, 1 = expanded)
                val progress = 1f - ((newY - expandedY) / (collapsedY - expandedY))
                onDrag?.invoke(progress.coerceIn(0f, 1f))

                lastTouchY = event.rawY
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (!isDragging) return false

                // Calculate velocity
                velocityTracker?.computeCurrentVelocity(1000)
                val velocityY = velocityTracker?.yVelocity ?: 0f

                // Decide based on velocity or position
                if (abs(velocityY) > minFlingVelocity) {
                    // Fling detected
                    if (velocityY < 0) {
                        // Fling up = expand
                        expand()
                    } else {
                        // Fling down = collapse
                        collapse()
                    }
                } else {
                    // Settle to nearest state
                    settleToNearestState()
                }

                isDragging = false
                velocityTracker?.recycle()
                velocityTracker = null
                return true
            }
        }
        return false
    }

    /**
     * Settle to nearest state based on current position
     */
    private fun settleToNearestState() {
        val currentY = targetView.translationY
        val midPoint = (expandedY + collapsedY) / 2f

        if (currentY > midPoint) {
            collapse()
        } else {
            expand()
        }
    }

    /**
     * Expand to full height
     */
    fun expand() {
        animateToPosition(expandedY, true)
    }

    /**
     * Collapse to minimum height
     */
    fun collapse() {
        animateToPosition(collapsedY, false)
    }

    /**
     * Animate to specific position
     */
    private fun animateToPosition(targetY: Float, expanded: Boolean) {
        targetView.animate()
            .translationY(targetY)
            .setDuration(settleDuration)
            .setInterpolator(DecelerateInterpolator())
            .setUpdateListener { animation ->
                val currentY = targetView.translationY
                val progress = 1f - ((currentY - expandedY) / (collapsedY - expandedY))
                onDrag?.invoke(progress.coerceIn(0f, 1f))
            }
            .withEndAction {
                isExpanded = expanded
                onStateChange?.invoke(expanded)
            }
            .start()
    }
}