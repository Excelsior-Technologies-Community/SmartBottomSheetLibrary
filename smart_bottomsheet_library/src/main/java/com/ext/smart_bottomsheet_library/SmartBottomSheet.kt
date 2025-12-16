package com.ext.smart_bottomsheet_library

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.widget.NestedScrollView
import kotlin.math.abs

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
    private var isFullScreen = false

    // Helpers
    private lateinit var dragHelper: DragHelper
    private var dimOverlay: View? = null
    private var blurHelper: BlurViewHelper? = null

    // Containers
    private lateinit var sheetContent: FrameLayout

    // Nested ScrollView reference
    private var nestedScrollView: NestedScrollView? = null

    // Touch handling
    private var initialX = 0f
    private var initialY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var isScrollingContent = false

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

            if (autoExpand) {
                updateDimOverlay(1f)
            }

            // Setup scroll listener
            postDelayed({
                setupNestedScrollListener()
            }, 100)
        }
    }

    private fun readAttributes(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SmartBottomSheet)

        collapsedHeight = ta.getDimensionPixelSize(
            R.styleable.SmartBottomSheet_collapsedHeight,
            dpToPx(120)
        )

        expandedHeight = ta.getDimensionPixelSize(
            R.styleable.SmartBottomSheet_expandedHeight,
            dpToPx(500)
        )

        dragSensitivity = ta.getFloat(
            R.styleable.SmartBottomSheet_dragSensitivity,
            1f
        )

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

        enableBlur = ta.getBoolean(
            R.styleable.SmartBottomSheet_enableBlur,
            false
        )

        blurRadius = ta.getInt(
            R.styleable.SmartBottomSheet_blurRadius,
            25
        )

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

        val sheetIndex = parentView.indexOfChild(this@SmartBottomSheet)
        if (sheetIndex > 0) {
            parentView.addView(dimOverlay, sheetIndex)
        } else {
            parentView.addView(dimOverlay, 0)
        }

        // Initialize blur helper if enabled
        if (enableBlur && BlurViewHelper.isBlurSupported()) {
            blurHelper = BlurViewHelper(blurRadius.toFloat())
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

    private fun setupNestedScrollListener() {
        nestedScrollView = findNestedScrollView(this)

        nestedScrollView?.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener {
                v, scrollX, scrollY, oldScrollX, oldScrollY ->

            val scrollDelta = scrollY - oldScrollY

            if (scrollDelta > 15 && v.canScrollVertically(1) && !isFullScreen) {
                expandToFullScreen()
            } else if (scrollY == 0 && isFullScreen && !v.canScrollVertically(-1)) {
                collapseFromFullScreen()
            }
        })
    }

    private fun findNestedScrollView(view: View): NestedScrollView? {
        if (view is NestedScrollView) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findNestedScrollView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun expandToFullScreen() {
        if (isFullScreen) return

        isFullScreen = true
        val parentView = parent as? View ?: return
        val parentHeight = parentView.height

        ValueAnimator.ofFloat(translationY, 0f).apply {
            duration = animationDuration
            addUpdateListener { animation ->
                translationY = animation.animatedValue as Float
                updateDimOverlay(1f)
            }
            start()
        }

        val currentParams = layoutParams
        ValueAnimator.ofInt(currentParams.height, parentHeight).apply {
            duration = animationDuration
            addUpdateListener { animation ->
                currentParams.height = animation.animatedValue as Int
                layoutParams = currentParams
            }
            start()
        }
    }

    private fun collapseFromFullScreen() {
        if (!isFullScreen) return

        isFullScreen = false
        val parentView = parent as? View ?: return
        val parentHeight = parentView.height

        val targetY = if (isExpanded) 0f else (parentHeight - collapsedHeight).toFloat()

        ValueAnimator.ofFloat(translationY, targetY).apply {
            duration = animationDuration
            addUpdateListener { animation ->
                translationY = animation.animatedValue as Float
                if (isExpanded) {
                    updateDimOverlay(1f)
                } else {
                    val progress = 1f - (translationY / parentHeight)
                    updateDimOverlay(progress)
                }
            }
            start()
        }

        val currentParams = layoutParams
        ValueAnimator.ofInt(currentParams.height, expandedHeight).apply {
            duration = animationDuration
            addUpdateListener { animation ->
                currentParams.height = animation.animatedValue as Int
                layoutParams = currentParams
            }
            start()
        }
    }

    private fun updateDimOverlay(progress: Float) {
        dimOverlay?.let { overlay ->
            if (isExpanded || isFullScreen || progress > 0.5f) {
                if (overlay.visibility != View.VISIBLE) {
                    overlay.visibility = View.VISIBLE

                    // Apply blur when overlay becomes visible
                    if (enableBlur) {
                        blurHelper?.applyBackgroundBlur(this, overlay)
                    }
                }
                overlay.alpha = (progress * dimOverlayOpacity).coerceIn(0f, dimOverlayOpacity)
            } else if (progress <= 0.01f) {
                overlay.alpha = 0f
                overlay.visibility = View.GONE

                // Remove blur when overlay is hidden
                if (enableBlur) {
                    blurHelper?.removeBlur(overlay)
                }
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!::dragHelper.isInitialized) return super.onInterceptTouchEvent(ev)

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                isScrollingContent = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = abs(ev.x - initialX)
                val deltaY = abs(ev.y - initialY)

                if (deltaY > touchSlop && deltaY > deltaX) {
                    val scrollView = nestedScrollView
                    if (scrollView != null && isTouchInsideView(scrollView, ev)) {
                        isScrollingContent = true
                        return false
                    }
                }
            }
        }

        if (isFullScreen && isScrollingContent) {
            return false
        }

        if (ev.y < 150) {
            return dragHelper.shouldInterceptTouch(ev)
        }

        return super.onInterceptTouchEvent(ev)
    }

    private fun isTouchInsideView(view: View, event: MotionEvent): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = event.rawX.toInt()
        val y = event.rawY.toInt()
        return (x >= location[0] && x <= location[0] + view.width &&
                y >= location[1] && y <= location[1] + view.height)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!::dragHelper.isInitialized) return super.onTouchEvent(event)

        if (isScrollingContent && isFullScreen) {
            return false
        }

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
        if (isFullScreen) {
            collapseFromFullScreen()
        } else if (!::dragHelper.isInitialized) {
            post { dragHelper.collapse() }
        } else {
            dragHelper.collapse()
        }
    }

    fun toggle() {
        if (isExpanded) collapse() else expand()
    }

    fun isExpanded(): Boolean = isExpanded

    fun isFullScreen(): Boolean = isFullScreen

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

    fun setBlurEnabled(enabled: Boolean) {
        enableBlur = enabled
        if (enabled && BlurViewHelper.isBlurSupported()) {
            blurHelper = BlurViewHelper(blurRadius.toFloat())
            // Apply if already visible
            if (dimOverlay?.visibility == View.VISIBLE) {
                blurHelper?.applyBackgroundBlur(this, dimOverlay)
            }
        } else {
            blurHelper?.removeBlur(dimOverlay)
            blurHelper?.cleanup()
            blurHelper = null
        }
    }

    fun setBlurRadius(radius: Int) {
        blurRadius = radius
        if (enableBlur) {
            blurHelper?.cleanup()
            blurHelper = BlurViewHelper(radius.toFloat())
            if (dimOverlay?.visibility == View.VISIBLE) {
                blurHelper?.applyBackgroundBlur(this, dimOverlay)
            }
        }
    }

    // ========== Utilities ==========

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // Cleanup blur resources
        blurHelper?.removeBlur(dimOverlay)
        blurHelper?.cleanup()
        blurHelper = null

        // Remove overlay
        dimOverlay?.let {
            (parent as? ViewGroup)?.removeView(it)
        }
        dimOverlay = null
    }
}



//package com.ext.smart_bottomsheet_library
//
//import android.animation.ValueAnimator
//import android.content.Context
//import android.graphics.RenderEffect
//import android.graphics.Shader
//import android.graphics.drawable.GradientDrawable
//import android.os.Build
//import android.util.AttributeSet
//import android.view.MotionEvent
//import android.view.View
//import android.view.ViewConfiguration
//import android.view.ViewGroup
//import android.widget.FrameLayout
//import androidx.core.widget.NestedScrollView
//import kotlin.math.abs
//
//class SmartBottomSheet @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0
//) : FrameLayout(context, attrs, defStyleAttr) {
//
//    // Attributes - Heights
//    private var collapsedHeight = 300
//    private var expandedHeight = 1000
//
//    // Attributes - Drag
//    private var dragSensitivity = 1f
//
//    // Attributes - Visual Styling
//    private var cornerRadius = 32f
//    private var sheetBackgroundColor = 0xFFFFFFFF.toInt()
//    private var sheetElevation = 16f
//    private var dimOverlayColor = 0x99000000.toInt()
//    private var dimOverlayOpacity = 0.85f
//
//    // Attributes - Blur
//    private var enableBlur = false
//    private var blurRadius = 20
//
//    // Attributes - Behavior
//    private var autoExpand = false
//    private var animationDuration = 350L
//    private var closeOnTapOutside = true
//
//    // State
//    private var isExpanded = false
//    private var isFullScreen = false
//
//    // Helpers
//    private lateinit var dragHelper: DragHelper
//    private var dimOverlay: View? = null
//
//    // Containers
//    private lateinit var sheetContent: FrameLayout
//
//    // Nested ScrollView reference
//    private var nestedScrollView: NestedScrollView? = null
//
//    // Touch handling
//    private var initialX = 0f
//    private var initialY = 0f
//    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
//    private var isScrollingContent = false
//
//    // Callbacks
//    private var onStateChangeListener: ((Boolean) -> Unit)? = null
//
//    init {
//        inflate(context, R.layout.layout_smart_bottomsheet, this)
//        sheetContent = findViewById(R.id.sheetContent)
//
//        readAttributes(attrs)
//        setupBackground()
//
//        post {
//            setupDimOverlay()
//            setupHelpers()
//
//            // Calculate collapsed position
//            val collapsedY = (parent as? View)?.height?.toFloat()?.minus(collapsedHeight) ?: 0f
//
//            // Set initial position based on autoExpand
//            translationY = if (autoExpand) 0f else collapsedY
//            isExpanded = autoExpand
//
//            if (autoExpand) {
//                updateDimOverlay(1f)
//            }
//
//            // Setup scroll listener
//            postDelayed({
//                setupNestedScrollListener()
//            }, 100)
//        }
//    }
//
//    private fun readAttributes(attrs: AttributeSet?) {
//        val ta = context.obtainStyledAttributes(attrs, R.styleable.SmartBottomSheet)
//
//        collapsedHeight = ta.getDimensionPixelSize(
//            R.styleable.SmartBottomSheet_collapsedHeight,
//            dpToPx(120)
//        )
//
//        expandedHeight = ta.getDimensionPixelSize(
//            R.styleable.SmartBottomSheet_expandedHeight,
//            dpToPx(500)
//        )
//
//        dragSensitivity = ta.getFloat(
//            R.styleable.SmartBottomSheet_dragSensitivity,
//            1f
//        )
//
//        cornerRadius = ta.getDimension(
//            R.styleable.SmartBottomSheet_cornerRadius,
//            dpToPx(28).toFloat()
//        )
//
//        sheetBackgroundColor = ta.getColor(
//            R.styleable.SmartBottomSheet_sheetBackgroundColor,
//            0xFFFFFFFF.toInt()
//        )
//
//        sheetElevation = ta.getDimension(
//            R.styleable.SmartBottomSheet_sheetElevation,
//            dpToPx(16).toFloat()
//        )
//
//        dimOverlayColor = ta.getColor(
//            R.styleable.SmartBottomSheet_dimOverlayColor,
//            0x99000000.toInt()
//        )
//
//        dimOverlayOpacity = ta.getFloat(
//            R.styleable.SmartBottomSheet_dimOverlayOpacity,
//            0.85f
//        )
//
//        enableBlur = ta.getBoolean(
//            R.styleable.SmartBottomSheet_enableBlur,
//            false
//        )
//
//        blurRadius = ta.getInt(
//            R.styleable.SmartBottomSheet_blurRadius,
//            25
//        )
//
//        autoExpand = ta.getBoolean(
//            R.styleable.SmartBottomSheet_autoExpand,
//            false
//        )
//
//        animationDuration = ta.getInt(
//            R.styleable.SmartBottomSheet_animationDuration,
//            350
//        ).toLong()
//
//        closeOnTapOutside = ta.getBoolean(
//            R.styleable.SmartBottomSheet_closeOnTapOutside,
//            true
//        )
//
//        ta.recycle()
//    }
//
//    private fun setupBackground() {
//        val drawable = GradientDrawable()
//        drawable.cornerRadii = floatArrayOf(
//            cornerRadius, cornerRadius,
//            cornerRadius, cornerRadius,
//            0f, 0f,
//            0f, 0f
//        )
//        drawable.setColor(sheetBackgroundColor)
//        background = drawable
//        elevation = sheetElevation
//    }
//
//    private fun setupDimOverlay() {
//        val parentView = parent as? ViewGroup ?: return
//
//        dimOverlay = View(context).apply {
//            layoutParams = ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT
//            )
//            setBackgroundColor(dimOverlayColor)
//            alpha = 0f
//            visibility = View.GONE
//            isClickable = closeOnTapOutside
//
//            if (closeOnTapOutside) {
//                setOnClickListener {
//                    collapse()
//                }
//            }
//        }
//
//        val sheetIndex = parentView.indexOfChild(this@SmartBottomSheet)
//        if (sheetIndex > 0) {
//            parentView.addView(dimOverlay, sheetIndex)
//        } else {
//            parentView.addView(dimOverlay, 0)
//        }
//
//        if (enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            try {
//                val blurEffect = RenderEffect.createBlurEffect(
//                    blurRadius.toFloat(),
//                    blurRadius.toFloat(),
//                    Shader.TileMode.CLAMP
//                )
//                dimOverlay?.setRenderEffect(blurEffect)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    private fun setupHelpers() {
//        val parentHeight = (parent as? View)?.height ?: 0
//
//        dragHelper = DragHelper(
//            targetView = this,
//            collapsedHeight = collapsedHeight,
//            parentHeight = parentHeight,
//            dragSensitivity = dragSensitivity,
//            settleDuration = animationDuration,
//            onDrag = { progress ->
//                updateDimOverlay(progress)
//            },
//            onStateChange = { expanded ->
//                isExpanded = expanded
//                onStateChangeListener?.invoke(expanded)
//            }
//        )
//    }
//
//    private fun setupNestedScrollListener() {
//        nestedScrollView = findNestedScrollView(this)
//
//        nestedScrollView?.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener {
//                v, scrollX, scrollY, oldScrollX, oldScrollY ->
//
//            // Only trigger on significant scroll changes
//            val scrollDelta = scrollY - oldScrollY
//
//            if (scrollDelta > 15 && v.canScrollVertically(1) && !isFullScreen) {
//                // Scrolling up with more content below -> go full screen
//                expandToFullScreen()
//            } else if (scrollY == 0 && isFullScreen && !v.canScrollVertically(-1)) {
//                // ONLY collapse when EXACTLY at top (scrollY = 0) and can't scroll up anymore
//                collapseFromFullScreen()
//            }
//        })
//    }
//
//    private fun findNestedScrollView(view: View): NestedScrollView? {
//        if (view is NestedScrollView) {
//            return view
//        }
//        if (view is ViewGroup) {
//            for (i in 0 until view.childCount) {
//                val found = findNestedScrollView(view.getChildAt(i))
//                if (found != null) return found
//            }
//        }
//        return null
//    }
//
//    private fun expandToFullScreen() {
//        if (isFullScreen) return
//
//        isFullScreen = true
//        val parentView = parent as? View ?: return
//        val parentHeight = parentView.height
//
//        // Animate to top
//        ValueAnimator.ofFloat(translationY, 0f).apply {
//            duration = animationDuration
//            addUpdateListener { animation ->
//                translationY = animation.animatedValue as Float
//                updateDimOverlay(1f)
//            }
//            start()
//        }
//
//        // Animate height to match parent
//        val currentParams = layoutParams
//        ValueAnimator.ofInt(currentParams.height, parentHeight).apply {
//            duration = animationDuration
//            addUpdateListener { animation ->
//                currentParams.height = animation.animatedValue as Int
//                layoutParams = currentParams
//            }
//            start()
//        }
//    }
//
//    private fun collapseFromFullScreen() {
//        if (!isFullScreen) return
//
//        isFullScreen = false
//        val parentView = parent as? View ?: return
//        val parentHeight = parentView.height
//
//        // Calculate target position based on current expanded state
//        val targetY = if (isExpanded) 0f else (parentHeight - collapsedHeight).toFloat()
//
//        // Animate back to expanded position (stay at top, translationY = 0)
//        ValueAnimator.ofFloat(translationY, targetY).apply {
//            duration = animationDuration
//            addUpdateListener { animation ->
//                translationY = animation.animatedValue as Float
//                // Keep dim overlay at full if still expanded
//                if (isExpanded) {
//                    updateDimOverlay(1f)
//                } else {
//                    val progress = 1f - (translationY / parentHeight)
//                    updateDimOverlay(progress)
//                }
//            }
//            start()
//        }
//
//        // Animate height back to expanded height
//        val currentParams = layoutParams
//        ValueAnimator.ofInt(currentParams.height, expandedHeight).apply {
//            duration = animationDuration
//            addUpdateListener { animation ->
//                currentParams.height = animation.animatedValue as Int
//                layoutParams = currentParams
//            }
//            start()
//        }
//    }
//
//    private fun updateDimOverlay(progress: Float) {
//        dimOverlay?.let { overlay ->
//            // Always show overlay if expanded or full screen, only hide when collapsed
//            if (isExpanded || isFullScreen || progress > 0.5f) {
//                if (overlay.visibility != View.VISIBLE) {
//                    overlay.visibility = View.VISIBLE
//                }
//                overlay.alpha = (progress * dimOverlayOpacity).coerceIn(0f, dimOverlayOpacity)
//            } else if (progress <= 0.01f) {
//                // Only hide when truly collapsed
//                overlay.alpha = 0f
//                overlay.visibility = View.GONE
//            }
//        }
//    }
//
//    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
//        if (!::dragHelper.isInitialized) return super.onInterceptTouchEvent(ev)
//
//        when (ev.action) {
//            MotionEvent.ACTION_DOWN -> {
//                initialX = ev.x
//                initialY = ev.y
//                isScrollingContent = false
//            }
//            MotionEvent.ACTION_MOVE -> {
//                val deltaX = abs(ev.x - initialX)
//                val deltaY = abs(ev.y - initialY)
//
//                // If user is scrolling vertically in the content area
//                if (deltaY > touchSlop && deltaY > deltaX) {
//                    val scrollView = nestedScrollView
//                    // Check if touch is within scroll view bounds
//                    if (scrollView != null && isTouchInsideView(scrollView, ev)) {
//                        isScrollingContent = true
//                        return false // Let ScrollView handle it
//                    }
//                }
//            }
//        }
//
//        // If in full screen mode and scrolling content, don't intercept
//        if (isFullScreen && isScrollingContent) {
//            return false
//        }
//
//        // Only intercept for drag handle (top 150px)
//        if (ev.y < 150) {
//            return dragHelper.shouldInterceptTouch(ev)
//        }
//
//        return super.onInterceptTouchEvent(ev)
//    }
//
//    private fun isTouchInsideView(view: View, event: MotionEvent): Boolean {
//        val location = IntArray(2)
//        view.getLocationOnScreen(location)
//        val x = event.rawX.toInt()
//        val y = event.rawY.toInt()
//        return (x >= location[0] && x <= location[0] + view.width &&
//                y >= location[1] && y <= location[1] + view.height)
//    }
//
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        if (!::dragHelper.isInitialized) return super.onTouchEvent(event)
//
//        // If scrolling content, don't handle drag
//        if (isScrollingContent && isFullScreen) {
//            return false
//        }
//
//        return dragHelper.onTouch(event) || super.onTouchEvent(event)
//    }
//
//    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
//        if (::sheetContent.isInitialized && child?.id != R.id.sheetRoot) {
//            sheetContent.addView(child, index, params)
//        } else {
//            super.addView(child, index, params)
//        }
//    }
//
//    // ========== Public API ==========
//
//    fun expand() {
//        if (!::dragHelper.isInitialized) {
//            post { dragHelper.expand() }
//        } else {
//            dragHelper.expand()
//        }
//    }
//
//    fun collapse() {
//        if (isFullScreen) {
//            collapseFromFullScreen()
//        } else if (!::dragHelper.isInitialized) {
//            post { dragHelper.collapse() }
//        } else {
//            dragHelper.collapse()
//        }
//    }
//
//    fun toggle() {
//        if (isExpanded) collapse() else expand()
//    }
//
//    fun isExpanded(): Boolean = isExpanded
//
//    fun isFullScreen(): Boolean = isFullScreen
//
//    fun setOnStateChangeListener(listener: (Boolean) -> Unit) {
//        onStateChangeListener = listener
//    }
//
//    // ========== Customization Methods ==========
//
//    fun setSheetBackgroundColor(color: Int) {
//        sheetBackgroundColor = color
//        (background as? GradientDrawable)?.setColor(color)
//    }
//
//    fun setCornerRadius(radius: Float) {
//        cornerRadius = radius
//        setupBackground()
//    }
//
//    fun setDragSensitivity(sensitivity: Float) {
//        dragSensitivity = sensitivity
//        if (::dragHelper.isInitialized) {
//            try {
//                val field = dragHelper.javaClass.getDeclaredField("dragSensitivity")
//                field.isAccessible = true
//                field.set(dragHelper, sensitivity)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    fun setDimOverlayColor(color: Int) {
//        dimOverlayColor = color
//        dimOverlay?.setBackgroundColor(color)
//    }
//
//    fun setDimOverlayOpacity(opacity: Float) {
//        dimOverlayOpacity = opacity.coerceIn(0f, 1f)
//    }
//
//    fun setCloseOnTapOutside(enabled: Boolean) {
//        closeOnTapOutside = enabled
//        dimOverlay?.isClickable = enabled
//        if (enabled) {
//            dimOverlay?.setOnClickListener { collapse() }
//        } else {
//            dimOverlay?.setOnClickListener(null)
//        }
//    }
//
//    // ========== Utilities ==========
//
//    private fun dpToPx(dp: Int): Int {
//        return (dp * resources.displayMetrics.density).toInt()
//    }
//
//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//        dimOverlay?.let {
//            (parent as? ViewGroup)?.removeView(it)
//        }
//        dimOverlay = null
//    }
//}
//
//
//
////package com.ext.smart_bottomsheet_library
////
////import android.animation.ValueAnimator
////import android.content.Context
////import android.graphics.RenderEffect
////import android.graphics.Shader
////import android.graphics.drawable.GradientDrawable
////import android.os.Build
////import android.util.AttributeSet
////import android.view.MotionEvent
////import android.view.View
////import android.view.ViewConfiguration
////import android.view.ViewGroup
////import android.widget.FrameLayout
////import androidx.core.widget.NestedScrollView
////import kotlin.math.abs
////
////class SmartBottomSheet @JvmOverloads constructor(
////    context: Context,
////    attrs: AttributeSet? = null,
////    defStyleAttr: Int = 0
////) : FrameLayout(context, attrs, defStyleAttr) {
////
////    // Attributes - Heights
////    private var collapsedHeight = 300
////    private var expandedHeight = 1000
////
////    // Attributes - Drag
////    private var dragSensitivity = 1f
////
////    // Attributes - Visual Styling
////    private var cornerRadius = 32f
////    private var sheetBackgroundColor = 0xFFFFFFFF.toInt()
////    private var sheetElevation = 16f
////    private var dimOverlayColor = 0x99000000.toInt()
////    private var dimOverlayOpacity = 0.85f
////
////    // Attributes - Blur
////    private var enableBlur = false
////    private var blurRadius = 20
////
////    // Attributes - Behavior
////    private var autoExpand = false
////    private var animationDuration = 350L
////    private var closeOnTapOutside = true
////
////    // State
////    private var isExpanded = false
////    private var isFullScreen = false
////
////    // Helpers
////    private lateinit var dragHelper: DragHelper
////    private var dimOverlay: View? = null
////
////    // Containers
////    private lateinit var sheetContent: FrameLayout
////
////    // Nested ScrollView reference
////    private var nestedScrollView: NestedScrollView? = null
////
////    // Touch handling
////    private var initialX = 0f
////    private var initialY = 0f
////    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
////    private var isScrollingContent = false
////
////    // Callbacks
////    private var onStateChangeListener: ((Boolean) -> Unit)? = null
////
////    init {
////        inflate(context, R.layout.layout_smart_bottomsheet, this)
////        sheetContent = findViewById(R.id.sheetContent)
////
////        readAttributes(attrs)
////        setupBackground()
////
////        post {
////            setupDimOverlay()
////            setupHelpers()
////
////            // Calculate collapsed position
////            val collapsedY = (parent as? View)?.height?.toFloat()?.minus(collapsedHeight) ?: 0f
////
////            // Set initial position based on autoExpand
////            translationY = if (autoExpand) 0f else collapsedY
////            isExpanded = autoExpand
////
////            if (autoExpand) {
////                updateDimOverlay(1f)
////            }
////
////            // Setup scroll listener
////            postDelayed({
////                setupNestedScrollListener()
////            }, 100)
////        }
////    }
////
////    private fun readAttributes(attrs: AttributeSet?) {
////        val ta = context.obtainStyledAttributes(attrs, R.styleable.SmartBottomSheet)
////
////        collapsedHeight = ta.getDimensionPixelSize(
////            R.styleable.SmartBottomSheet_collapsedHeight,
////            dpToPx(120)
////        )
////
////        expandedHeight = ta.getDimensionPixelSize(
////            R.styleable.SmartBottomSheet_expandedHeight,
////            dpToPx(500)
////        )
////
////        dragSensitivity = ta.getFloat(
////            R.styleable.SmartBottomSheet_dragSensitivity,
////            1f
////        )
////
////        cornerRadius = ta.getDimension(
////            R.styleable.SmartBottomSheet_cornerRadius,
////            dpToPx(28).toFloat()
////        )
////
////        sheetBackgroundColor = ta.getColor(
////            R.styleable.SmartBottomSheet_sheetBackgroundColor,
////            0xFFFFFFFF.toInt()
////        )
////
////        sheetElevation = ta.getDimension(
////            R.styleable.SmartBottomSheet_sheetElevation,
////            dpToPx(16).toFloat()
////        )
////
////        dimOverlayColor = ta.getColor(
////            R.styleable.SmartBottomSheet_dimOverlayColor,
////            0x99000000.toInt()
////        )
////
////        dimOverlayOpacity = ta.getFloat(
////            R.styleable.SmartBottomSheet_dimOverlayOpacity,
////            0.85f
////        )
////
////        enableBlur = ta.getBoolean(
////            R.styleable.SmartBottomSheet_enableBlur,
////            false
////        )
////
////        blurRadius = ta.getInt(
////            R.styleable.SmartBottomSheet_blurRadius,
////            25
////        )
////
////        autoExpand = ta.getBoolean(
////            R.styleable.SmartBottomSheet_autoExpand,
////            false
////        )
////
////        animationDuration = ta.getInt(
////            R.styleable.SmartBottomSheet_animationDuration,
////            350
////        ).toLong()
////
////        closeOnTapOutside = ta.getBoolean(
////            R.styleable.SmartBottomSheet_closeOnTapOutside,
////            true
////        )
////
////        ta.recycle()
////    }
////
////    private fun setupBackground() {
////        val drawable = GradientDrawable()
////        drawable.cornerRadii = floatArrayOf(
////            cornerRadius, cornerRadius,
////            cornerRadius, cornerRadius,
////            0f, 0f,
////            0f, 0f
////        )
////        drawable.setColor(sheetBackgroundColor)
////        background = drawable
////        elevation = sheetElevation
////    }
////
////    private fun setupDimOverlay() {
////        val parentView = parent as? ViewGroup ?: return
////
////        dimOverlay = View(context).apply {
////            layoutParams = ViewGroup.LayoutParams(
////                ViewGroup.LayoutParams.MATCH_PARENT,
////                ViewGroup.LayoutParams.MATCH_PARENT
////            )
////            setBackgroundColor(dimOverlayColor)
////            alpha = 0f
////            visibility = View.GONE
////            isClickable = closeOnTapOutside
////
////            if (closeOnTapOutside) {
////                setOnClickListener {
////                    collapse()
////                }
////            }
////        }
////
////        val sheetIndex = parentView.indexOfChild(this@SmartBottomSheet)
////        if (sheetIndex > 0) {
////            parentView.addView(dimOverlay, sheetIndex)
////        } else {
////            parentView.addView(dimOverlay, 0)
////        }
////
////        if (enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
////            try {
////                val blurEffect = RenderEffect.createBlurEffect(
////                    blurRadius.toFloat(),
////                    blurRadius.toFloat(),
////                    Shader.TileMode.CLAMP
////                )
////                dimOverlay?.setRenderEffect(blurEffect)
////            } catch (e: Exception) {
////                e.printStackTrace()
////            }
////        }
////    }
////
////    private fun setupHelpers() {
////        val parentHeight = (parent as? View)?.height ?: 0
////
////        dragHelper = DragHelper(
////            targetView = this,
////            collapsedHeight = collapsedHeight,
////            parentHeight = parentHeight,
////            dragSensitivity = dragSensitivity,
////            settleDuration = animationDuration,
////            onDrag = { progress ->
////                updateDimOverlay(progress)
////            },
////            onStateChange = { expanded ->
////                isExpanded = expanded
////                onStateChangeListener?.invoke(expanded)
////            }
////        )
////    }
////
////    private fun setupNestedScrollListener() {
////        nestedScrollView = findNestedScrollView(this)
////
////        nestedScrollView?.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener {
////                v, scrollX, scrollY, oldScrollX, oldScrollY ->
////
////            // Only trigger on significant scroll changes
////            val scrollDelta = scrollY - oldScrollY
////
////            if (scrollDelta > 15 && v.canScrollVertically(1) && !isFullScreen) {
////                // Scrolling up with more content below -> go full screen
////                expandToFullScreen()
////            } else if (scrollDelta < -15 && scrollY <= 0 && isFullScreen) {
////                // Scrolled to top while in full screen -> collapse back
////                collapseFromFullScreen()
////            }
////        })
////    }
////
////    private fun findNestedScrollView(view: View): NestedScrollView? {
////        if (view is NestedScrollView) {
////            return view
////        }
////        if (view is ViewGroup) {
////            for (i in 0 until view.childCount) {
////                val found = findNestedScrollView(view.getChildAt(i))
////                if (found != null) return found
////            }
////        }
////        return null
////    }
////
////    private fun expandToFullScreen() {
////        if (isFullScreen) return
////
////        isFullScreen = true
////        val parentView = parent as? View ?: return
////        val parentHeight = parentView.height
////
////        // Animate to top
////        ValueAnimator.ofFloat(translationY, 0f).apply {
////            duration = animationDuration
////            addUpdateListener { animation ->
////                translationY = animation.animatedValue as Float
////                updateDimOverlay(1f)
////            }
////            start()
////        }
////
////        // Animate height to match parent
////        val currentParams = layoutParams
////        ValueAnimator.ofInt(currentParams.height, parentHeight).apply {
////            duration = animationDuration
////            addUpdateListener { animation ->
////                currentParams.height = animation.animatedValue as Int
////                layoutParams = currentParams
////            }
////            start()
////        }
////    }
////
////    private fun collapseFromFullScreen() {
////        if (!isFullScreen) return
////
////        isFullScreen = false
////
////        // Animate back to expanded position
////        ValueAnimator.ofFloat(translationY, 0f).apply {
////            duration = animationDuration
////            addUpdateListener { animation ->
////                translationY = animation.animatedValue as Float
////                updateDimOverlay(1f)
////            }
////            start()
////        }
////
////        // Animate height back to expanded height
////        val currentParams = layoutParams
////        ValueAnimator.ofInt(currentParams.height, expandedHeight).apply {
////            duration = animationDuration
////            addUpdateListener { animation ->
////                currentParams.height = animation.animatedValue as Int
////                layoutParams = currentParams
////            }
////            start()
////        }
////    }
////
////    private fun updateDimOverlay(progress: Float) {
////        dimOverlay?.let { overlay ->
////            if (progress > 0.01f) {
////                if (overlay.visibility != View.VISIBLE) {
////                    overlay.visibility = View.VISIBLE
////                }
////                overlay.alpha = (progress * dimOverlayOpacity).coerceIn(0f, dimOverlayOpacity)
////            } else {
////                overlay.alpha = 0f
////                overlay.visibility = View.GONE
////            }
////        }
////    }
////
////    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
////        if (!::dragHelper.isInitialized) return super.onInterceptTouchEvent(ev)
////
////        when (ev.action) {
////            MotionEvent.ACTION_DOWN -> {
////                initialX = ev.x
////                initialY = ev.y
////                isScrollingContent = false
////            }
////            MotionEvent.ACTION_MOVE -> {
////                val deltaX = abs(ev.x - initialX)
////                val deltaY = abs(ev.y - initialY)
////
////                // If user is scrolling vertically in the content area
////                if (deltaY > touchSlop && deltaY > deltaX) {
////                    val scrollView = nestedScrollView
////                    // Check if touch is within scroll view bounds
////                    if (scrollView != null && isTouchInsideView(scrollView, ev)) {
////                        isScrollingContent = true
////                        return false // Let ScrollView handle it
////                    }
////                }
////            }
////        }
////
////        // If in full screen mode and scrolling content, don't intercept
////        if (isFullScreen && isScrollingContent) {
////            return false
////        }
////
////        // Only intercept for drag handle (top 150px)
////        if (ev.y < 150) {
////            return dragHelper.shouldInterceptTouch(ev)
////        }
////
////        return super.onInterceptTouchEvent(ev)
////    }
////
////    private fun isTouchInsideView(view: View, event: MotionEvent): Boolean {
////        val location = IntArray(2)
////        view.getLocationOnScreen(location)
////        val x = event.rawX.toInt()
////        val y = event.rawY.toInt()
////        return (x >= location[0] && x <= location[0] + view.width &&
////                y >= location[1] && y <= location[1] + view.height)
////    }
////
////    override fun onTouchEvent(event: MotionEvent): Boolean {
////        if (!::dragHelper.isInitialized) return super.onTouchEvent(event)
////
////        // If scrolling content, don't handle drag
////        if (isScrollingContent && isFullScreen) {
////            return false
////        }
////
////        return dragHelper.onTouch(event) || super.onTouchEvent(event)
////    }
////
////    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
////        if (::sheetContent.isInitialized && child?.id != R.id.sheetRoot) {
////            sheetContent.addView(child, index, params)
////        } else {
////            super.addView(child, index, params)
////        }
////    }
////
////    // ========== Public API ==========
////
////    fun expand() {
////        if (!::dragHelper.isInitialized) {
////            post { dragHelper.expand() }
////        } else {
////            dragHelper.expand()
////        }
////    }
////
////    fun collapse() {
////        if (isFullScreen) {
////            collapseFromFullScreen()
////        } else if (!::dragHelper.isInitialized) {
////            post { dragHelper.collapse() }
////        } else {
////            dragHelper.collapse()
////        }
////    }
////
////    fun toggle() {
////        if (isExpanded) collapse() else expand()
////    }
////
////    fun isExpanded(): Boolean = isExpanded
////
////    fun isFullScreen(): Boolean = isFullScreen
////
////    fun setOnStateChangeListener(listener: (Boolean) -> Unit) {
////        onStateChangeListener = listener
////    }
////
////    // ========== Customization Methods ==========
////
////    fun setSheetBackgroundColor(color: Int) {
////        sheetBackgroundColor = color
////        (background as? GradientDrawable)?.setColor(color)
////    }
////
////    fun setCornerRadius(radius: Float) {
////        cornerRadius = radius
////        setupBackground()
////    }
////
////    fun setDragSensitivity(sensitivity: Float) {
////        dragSensitivity = sensitivity
////        if (::dragHelper.isInitialized) {
////            try {
////                val field = dragHelper.javaClass.getDeclaredField("dragSensitivity")
////                field.isAccessible = true
////                field.set(dragHelper, sensitivity)
////            } catch (e: Exception) {
////                e.printStackTrace()
////            }
////        }
////    }
////
////    fun setDimOverlayColor(color: Int) {
////        dimOverlayColor = color
////        dimOverlay?.setBackgroundColor(color)
////    }
////
////    fun setDimOverlayOpacity(opacity: Float) {
////        dimOverlayOpacity = opacity.coerceIn(0f, 1f)
////    }
////
////    fun setCloseOnTapOutside(enabled: Boolean) {
////        closeOnTapOutside = enabled
////        dimOverlay?.isClickable = enabled
////        if (enabled) {
////            dimOverlay?.setOnClickListener { collapse() }
////        } else {
////            dimOverlay?.setOnClickListener(null)
////        }
////    }
////
////    // ========== Utilities ==========
////
////    private fun dpToPx(dp: Int): Int {
////        return (dp * resources.displayMetrics.density).toInt()
////    }
////
////    override fun onDetachedFromWindow() {
////        super.onDetachedFromWindow()
////        dimOverlay?.let {
////            (parent as? ViewGroup)?.removeView(it)
////        }
////        dimOverlay = null
////    }
////}