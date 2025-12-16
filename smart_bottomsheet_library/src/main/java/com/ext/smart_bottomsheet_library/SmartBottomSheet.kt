package com.ext.smart_bottomsheet_library

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

class SmartBottomSheet @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Attributes - Heights
    private var collapsedHeight = 300
    private var expandedHeight = 1000

    // Attributes - Drag
    private var dragSensitivity = 1f

    // Attributes - Visual Styling
    private var cornerRadius = 32f
    private var sheetBackgroundColor = 0xFFFFFFFF.toInt()
    private var sheetElevation = 16f
    private var dimOverlayColor = 0x99000000.toInt()
    private var dimOverlayOpacity = 0.85f

    // Attributes - Blur
    private var enableBlur = false
    private var blurRadius = 20

    // Attributes - Behavior
    private var autoExpand = false
    private var animationDuration = 350L
    private var closeOnTapOutside = true

    // State
    private var isExpanded = false

    // Helpers
    private lateinit var dragHelper: DragHelper
    private var dimOverlay: View? = null

    // Containers
    private lateinit var sheetContent: FrameLayout

    // Callbacks
    private var onStateChangeListener: ((Boolean) -> Unit)? = null

    init {
        inflate(context, R.layout.layout_smart_bottomsheet, this)
        sheetContent = findViewById(R.id.sheetContent)

        readAttributes(attrs)
        setupBackground()

        post {
            setupDimOverlay()
            setupHelpers()

            // Calculate collapsed position
            val collapsedY = (parent as? View)?.height?.toFloat()?.minus(collapsedHeight) ?: 0f

            // Set initial position based on autoExpand
            translationY = if (autoExpand) 0f else collapsedY
            isExpanded = autoExpand

            // NEW: If autoExpand is true, immediately show full dim overlay + blur
            if (autoExpand) {
                updateDimOverlay(1f) // progress = 1f → fully expanded
            }
        }
    }

    private fun readAttributes(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SmartBottomSheet)

        // Heights
        collapsedHeight = ta.getDimensionPixelSize(
            R.styleable.SmartBottomSheet_collapsedHeight,
            dpToPx(120)
        )

        expandedHeight = ta.getDimensionPixelSize(
            R.styleable.SmartBottomSheet_expandedHeight,
            dpToPx(500)
        )

        // Drag
        dragSensitivity = ta.getFloat(
            R.styleable.SmartBottomSheet_dragSensitivity,
            1f
        )

        // Visual Styling
        cornerRadius = ta.getDimension(
            R.styleable.SmartBottomSheet_cornerRadius,
            dpToPx(28).toFloat()
        )

        sheetBackgroundColor = ta.getColor(
            R.styleable.SmartBottomSheet_sheetBackgroundColor,
            0xFFFFFFFF.toInt()
        )

        sheetElevation = ta.getDimension(
            R.styleable.SmartBottomSheet_sheetElevation,
            dpToPx(16).toFloat()
        )

        dimOverlayColor = ta.getColor(
            R.styleable.SmartBottomSheet_dimOverlayColor,
            0x99000000.toInt()
        )

        dimOverlayOpacity = ta.getFloat(
            R.styleable.SmartBottomSheet_dimOverlayOpacity,
            0.85f
        )

        // Blur
        enableBlur = ta.getBoolean(
            R.styleable.SmartBottomSheet_enableBlur,
            false
        )

        blurRadius = ta.getInt(
            R.styleable.SmartBottomSheet_blurRadius,
            25
        )

        // Behavior
        autoExpand = ta.getBoolean(
            R.styleable.SmartBottomSheet_autoExpand,
            false
        )

        animationDuration = ta.getInt(
            R.styleable.SmartBottomSheet_animationDuration,
            350
        ).toLong()

        closeOnTapOutside = ta.getBoolean(
            R.styleable.SmartBottomSheet_closeOnTapOutside,
            true
        )

        ta.recycle()
    }

    private fun setupBackground() {
        val drawable = GradientDrawable()
        drawable.cornerRadii = floatArrayOf(
            cornerRadius, cornerRadius,
            cornerRadius, cornerRadius,
            0f, 0f,
            0f, 0f
        )
        drawable.setColor(sheetBackgroundColor)
        background = drawable
        elevation = sheetElevation
    }

    private fun setupDimOverlay() {
        val parentView = parent as? ViewGroup ?: return

        // Create dim overlay
        dimOverlay = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(dimOverlayColor)
            alpha = 0f
            visibility = View.GONE
            isClickable = closeOnTapOutside

            if (closeOnTapOutside) {
                setOnClickListener {
                    collapse()
                }
            }
        }

        // Add dim overlay before the sheet
        val sheetIndex = parentView.indexOfChild(this@SmartBottomSheet)
        if (sheetIndex > 0) {
            parentView.addView(dimOverlay, sheetIndex)
        } else {
            parentView.addView(dimOverlay, 0)
        }

        // Apply blur effect if enabled
        if (enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val blurEffect = RenderEffect.createBlurEffect(
                    blurRadius.toFloat(),
                    blurRadius.toFloat(),
                    Shader.TileMode.CLAMP
                )
                dimOverlay?.setRenderEffect(blurEffect)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupHelpers() {
        val parentHeight = (parent as? View)?.height ?: 0

        dragHelper = DragHelper(
            targetView = this,
            collapsedHeight = collapsedHeight,
            parentHeight = parentHeight,
            dragSensitivity = dragSensitivity,
            settleDuration = animationDuration,
            onDrag = { progress ->
                updateDimOverlay(progress)
            },
            onStateChange = { expanded ->
                isExpanded = expanded
                onStateChangeListener?.invoke(expanded)
            }
        )
    }

    private fun updateDimOverlay(progress: Float) {
        dimOverlay?.let { overlay ->
            if (progress > 0.01f) {
                if (overlay.visibility != View.VISIBLE) {
                    overlay.visibility = View.VISIBLE
                }
                overlay.alpha = (progress * dimOverlayOpacity).coerceIn(0f, dimOverlayOpacity)
            } else {
                overlay.alpha = 0f
                overlay.visibility = View.GONE
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!::dragHelper.isInitialized) return super.onInterceptTouchEvent(ev)
        return dragHelper.shouldInterceptTouch(ev) || super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!::dragHelper.isInitialized) return super.onTouchEvent(event)
        return dragHelper.onTouch(event) || super.onTouchEvent(event)
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (::sheetContent.isInitialized && child?.id != R.id.sheetRoot) {
            sheetContent.addView(child, index, params)
        } else {
            super.addView(child, index, params)
        }
    }

    // ========== Public API ==========

    fun expand() {
        if (!::dragHelper.isInitialized) {
            post { dragHelper.expand() }
        } else {
            dragHelper.expand()
        }
    }

    fun collapse() {
        if (!::dragHelper.isInitialized) {
            post { dragHelper.collapse() }
        } else {
            dragHelper.collapse()
        }
    }

    fun toggle() {
        if (isExpanded) collapse() else expand()
    }

    fun isExpanded(): Boolean = isExpanded

    fun setOnStateChangeListener(listener: (Boolean) -> Unit) {
        onStateChangeListener = listener
    }

    // ========== Customization Methods ==========

    fun setSheetBackgroundColor(color: Int) {
        sheetBackgroundColor = color
        (background as? GradientDrawable)?.setColor(color)
    }

    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        setupBackground()
    }

    fun setDragSensitivity(sensitivity: Float) {
        dragSensitivity = sensitivity
        if (::dragHelper.isInitialized) {
            try {
                val field = dragHelper.javaClass.getDeclaredField("dragSensitivity")
                field.isAccessible = true
                field.set(dragHelper, sensitivity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setDimOverlayColor(color: Int) {
        dimOverlayColor = color
        dimOverlay?.setBackgroundColor(color)
    }

    fun setDimOverlayOpacity(opacity: Float) {
        dimOverlayOpacity = opacity.coerceIn(0f, 1f)
    }

    fun setCloseOnTapOutside(enabled: Boolean) {
        closeOnTapOutside = enabled
        dimOverlay?.isClickable = enabled
        if (enabled) {
            dimOverlay?.setOnClickListener { collapse() }
        } else {
            dimOverlay?.setOnClickListener(null)
        }
    }

    // ========== Utilities ==========

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dimOverlay?.let {
            (parent as? ViewGroup)?.removeView(it)
        }
        dimOverlay = null
    }
}