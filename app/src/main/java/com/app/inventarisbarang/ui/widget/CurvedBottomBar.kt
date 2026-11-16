package com.app.inventarisbarang.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.app.inventarisbarang.R

class CurvedBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    data class NavItem(
        val id: Int,
        val title: String,
        val iconRes: Int,
    )

    private val items = mutableListOf<NavItem>()
    private val slotViews = mutableListOf<View>()
    private var selectedIndex = 0

    private val path = Path()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(dp(8f), 0f, dp(3f), Color.parseColor("#22000000"))
    }

    private var currentWaveCenterX = 0f
    private var targetWaveCenterX = 0f
    private var isMeasuredReady = false

    private lateinit var itemsContainer: LinearLayout
    private lateinit var activeCircle: FrameLayout
    private lateinit var ivActiveIcon: ImageView
    private lateinit var bottomGestureBar: View

    var onItemSelectedListener: ((itemId: Int) -> Unit)? = null

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        clipChildren = false
        clipToPadding = false

        initViews()
    }

    private fun initViews() {
        // Horizontal Items Container (occupies bottom 62dp)
        itemsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        val itemsParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            dp(62f).toInt(),
        ).apply {
            gravity = Gravity.BOTTOM
        }
        addView(itemsContainer, itemsParams)

        // Bottom Gesture / Home Indicator Pill
        bottomGestureBar = View(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.nav_gesture_indicator)
            alpha = 0.35f
        }
        val gestureParams = LayoutParams(
            dp(100f).toInt(),
            dp(3.5f).toInt(),
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(4f).toInt()
        }
        addView(bottomGestureBar, gestureParams)

        // Floating Active Circle (the raised circular bubble)
        val circleSize = dp(48f).toInt()
        activeCircle = FrameLayout(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.circle_active_nav_bg)
            elevation = dp(8f)
            clipChildren = false
            clipToPadding = false

            ivActiveIcon = ImageView(context).apply {
                val iconSize = dp(22f).toInt()
                val iconParams = LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER
                }
                layoutParams = iconParams
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
            }
            addView(ivActiveIcon)
        }

        val circleParams = LayoutParams(circleSize, circleSize).apply {
            topMargin = dp(2f).toInt()
        }
        addView(activeCircle, circleParams)
    }

    fun setItems(navItems: List<NavItem>, defaultSelectedId: Int = 0) {
        items.clear()
        items.addAll(navItems)
        slotViews.clear()
        itemsContainer.removeAllViews()

        val inflater = LayoutInflater.from(context)
        items.forEachIndexed { index, item ->
            val slotView = inflater.inflate(R.layout.item_curved_nav, itemsContainer, false)
            val ivIcon = slotView.findViewById<ImageView>(R.id.ivNavIcon)
            val tvTitle = slotView.findViewById<TextView>(R.id.tvNavTitle)

            ivIcon.setImageResource(item.iconRes)
            tvTitle.text = item.title

            slotView.setOnClickListener {
                if (index != selectedIndex) {
                    setSelectedItemId(item.id, animate = true)
                    onItemSelectedListener?.invoke(item.id)
                }
            }

            itemsContainer.addView(slotView)
            slotViews.add(slotView)
        }

        val defaultIndex = items.indexOfFirst { it.id == defaultSelectedId }.coerceAtLeast(0)
        selectedIndex = defaultIndex

        if (isMeasuredReady && width > 0) {
            updatePosition(selectedIndex, animate = false)
        }
    }

    fun setSelectedItemId(itemId: Int, animate: Boolean = true) {
        val index = items.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            selectedIndex = index
            if (isMeasuredReady && width > 0) {
                updatePosition(index, animate)
            }
        }
    }

    private var currentAnimator: ValueAnimator? = null

    private fun updatePosition(index: Int, animate: Boolean) {
        if (items.isEmpty() || width <= 0) return

        val slotWidth = width.toFloat() / items.size
        targetWaveCenterX = (index + 0.5f) * slotWidth
        val circleHalf = activeCircle.width.toFloat() / 2f

        if (!animate) {
            currentAnimator?.cancel()
            currentWaveCenterX = targetWaveCenterX
            activeCircle.translationX = currentWaveCenterX - circleHalf
            activeCircle.translationY = 0f
            activeCircle.scaleX = 1f
            activeCircle.scaleY = 1f
            activeCircle.alpha = 1f
            ivActiveIcon.setImageResource(items[index].iconRes)
            updateSlotVisuals(index)
            invalidate()
            return
        }

        currentAnimator?.cancel()
        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

        val startX = currentWaveCenterX
        val distanceX = targetWaveCenterX - startX
        val dipDepth = dp(20f)
        var iconSwapped = false

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 340
            interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
            addUpdateListener { va ->
                val progress = va.animatedFraction

                // 1. Fluid horizontal glide tracking the wave cutout
                currentWaveCenterX = startX + distanceX * progress
                activeCircle.translationX = currentWaveCenterX - circleHalf

                // 2. Fluid vertical dip into cradle scoop & spring-rise out
                if (progress < 0.45f) {
                    val sinkProgress = progress / 0.45f
                    activeCircle.translationY = dipDepth * sinkProgress
                    val scale = 1.0f - 0.35f * sinkProgress
                    activeCircle.scaleX = scale
                    activeCircle.scaleY = scale
                } else {
                    if (!iconSwapped) {
                        iconSwapped = true
                        ivActiveIcon.setImageResource(items[index].iconRes)
                        updateSlotVisuals(index)
                    }
                    val riseProgress = (progress - 0.45f) / 0.55f
                    // Smooth elastic overshoot on rise
                    val bounceFactor = (1.0f - riseProgress) * kotlin.math.sin(riseProgress * kotlin.math.PI).toFloat() * 0.35f
                    activeCircle.translationY = dipDepth * (1.0f - riseProgress) - dp(4f) * bounceFactor
                    val scale = 0.65f + 0.35f * riseProgress + bounceFactor * 0.2f
                    activeCircle.scaleX = scale
                    activeCircle.scaleY = scale
                }

                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    activeCircle.translationY = 0f
                    activeCircle.scaleX = 1f
                    activeCircle.scaleY = 1f
                    updateSlotVisuals(index)
                    invalidate()
                }
            })
        }

        currentAnimator = animator
        animator.start()
    }

    private fun updateSlotVisuals(activeIndex: Int) {
        slotViews.forEachIndexed { i, view ->
            val ivIcon = view.findViewById<ImageView>(R.id.ivNavIcon)
            val tvTitle = view.findViewById<TextView>(R.id.tvNavTitle)

            if (i == activeIndex) {
                ivIcon.visibility = View.INVISIBLE
                tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                tvTitle.setTypeface(null, Typeface.BOLD)
                tvTitle.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(120)
                    .withEndAction {
                        tvTitle.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    }
                    .start()
            } else {
                ivIcon.visibility = View.VISIBLE
                ivIcon.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.nav_inactive),
                )
                tvTitle.setTextColor(ContextCompat.getColor(context, R.color.nav_inactive))
                tvTitle.setTypeface(null, Typeface.NORMAL)
                tvTitle.scaleX = 1.0f
                tvTitle.scaleY = 1.0f
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // On wide tablets or landscape screens, cap the max width to 520dp for optimal ergonomic reach
        val maxWidth = dp(520f).toInt()
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val finalWidthMeasureSpec = if (widthMode != MeasureSpec.UNSPECIFIED && widthSize > maxWidth) {
            MeasureSpec.makeMeasureSpec(maxWidth, widthMode)
        } else {
            widthMeasureSpec
        }
        super.onMeasure(finalWidthMeasureSpec, heightMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        isMeasuredReady = true
        if (items.isNotEmpty()) {
            post {
                updatePosition(selectedIndex, animate = false)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val baseTop = dp(22f)
        val cradleDepth = dp(28f)
        val slotWidth = if (items.isNotEmpty()) w / items.size else w / 5f
        val halfWave = (slotWidth * 0.44f).coerceIn(dp(26f), dp(36f))
        val cornerRadius = dp(20f).coerceAtMost(w * 0.08f)

        val cx = currentWaveCenterX
        val cradleStart = (cx - halfWave).coerceAtLeast(0f)
        val cradleEnd = (cx + halfWave).coerceAtMost(w)

        path.reset()

        // 1. Top-Left rounded corner
        path.moveTo(0f, baseTop + cornerRadius)
        path.quadTo(0f, baseTop, cornerRadius.coerceAtMost(cradleStart), baseTop)

        // 2. Line to start of cradle
        if (cradleStart > cornerRadius) {
            path.lineTo(cradleStart, baseTop)
        }

        // 3. Left half of cradle (curves DOWN into cradle bottom)
        val cradleLeftDist = (cx - cradleStart).coerceAtLeast(1f)
        path.cubicTo(
            cx - cradleLeftDist * 0.58f, baseTop,
            cx - cradleLeftDist * 0.35f, baseTop + cradleDepth,
            cx, baseTop + cradleDepth,
        )

        // 4. Right half of cradle (curves UP back to baseTop)
        val cradleRightDist = (cradleEnd - cx).coerceAtLeast(1f)
        path.cubicTo(
            cx + cradleRightDist * 0.35f, baseTop + cradleDepth,
            cx + cradleRightDist * 0.58f, baseTop,
            cradleEnd, baseTop,
        )

        // 5. Line to top-right corner
        val rightCornerStart = (w - cornerRadius).coerceAtLeast(cradleEnd)
        if (rightCornerStart > cradleEnd) {
            path.lineTo(rightCornerStart, baseTop)
        }
        path.quadTo(w, baseTop, w, baseTop + cornerRadius)

        // 6. Right edge down to bottom-right corner
        path.lineTo(w, h - cornerRadius)
        path.quadTo(w, h, w - cornerRadius, h)

        // 7. Bottom edge to bottom-left corner
        path.lineTo(cornerRadius, h)
        path.quadTo(0f, h, 0f, h - cornerRadius)

        // 8. Close back to top-left
        path.close()

        canvas.drawPath(path, backgroundPaint)
    }

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics,
        )
    }
}
