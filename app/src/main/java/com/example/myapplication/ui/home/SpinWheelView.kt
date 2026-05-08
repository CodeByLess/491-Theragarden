package com.example.myapplication.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SpinWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Colors for each wheel segment
    private val segmentColors = listOf(
        Color.parseColor("#F4B400"),
        Color.parseColor("#4285F4"),
        Color.parseColor("#AB47BC"),
        Color.parseColor("#EF6C00")
    )

    // List of reward names shown on the wheel
    private var segments: List<String> = emptyList()

    // Current wheel rotation used during animation
    private var currentRotation = 0f

    // Rectangle used to draw the circular wheel
    private val wheelRect = RectF()

    // Paint for colored wheel sections
    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Paint for white borders between sections
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.WHITE
    }

    // Paint for segment text labels
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 38f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }

    // Paint for center circle
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    // Paint for top pointer arrow
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#5D4037")
    }

    // Set wheel labels and refresh view
    fun setSegments(items: List<String>) {
        segments = items
        invalidate()
    }

    // Animate wheel spin and stop at the selected reward index
    fun spinToIndex(targetIndex: Int, onFinished: () -> Unit) {
        if (segments.isEmpty()) {
            onFinished()
            return
        }

        val sweepAngle = 360f / segments.size
        val targetCenterAngle = targetIndex * sweepAngle + sweepAngle / 2f
        val pointerAngle = 270f
        val normalizedCurrent = ((currentRotation % 360f) + 360f) % 360f
        val extraSpins = 5 * 360f

        val finalRotation = currentRotation + extraSpins +
                (pointerAngle - targetCenterAngle - normalizedCurrent + 360f) % 360f

        // Animate the custom rotation property
        val animator = ObjectAnimator.ofFloat(
            this,
            "rotationAngleProperty",
            currentRotation,
            finalRotation
        )

        animator.duration = 3500
        animator.interpolator = DecelerateInterpolator()

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                currentRotation = finalRotation % 360f
                onFinished()
            }
        })

        animator.start()
    }

    // Setter used by ObjectAnimator
    @Suppress("unused")
    fun setRotationAngleProperty(value: Float) {
        currentRotation = value
        invalidate()
    }

    // Getter used by ObjectAnimator
    @Suppress("unused")
    fun getRotationAngleProperty(): Float {
        return currentRotation
    }

    // Draw the wheel and pointer
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (segments.isEmpty()) return

        val size = min(width, height).toFloat()
        val radius = size * 0.38f
        val cx = width / 2f
        val cy = height / 2f + 30f

        wheelRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Rotate the wheel while keeping pointer fixed
        canvas.save()
        canvas.rotate(currentRotation, cx, cy)

        val sweepAngle = 360f / segments.size

        for (i in segments.indices) {
            segmentPaint.color = segmentColors[i % segmentColors.size]
            val startAngle = i * sweepAngle

            canvas.drawArc(wheelRect, startAngle, sweepAngle, true, segmentPaint)
            canvas.drawArc(wheelRect, startAngle, sweepAngle, true, borderPaint)

            drawSegmentText(canvas, segments[i], cx, cy, radius, startAngle, sweepAngle)
        }

        // Draw white center circle
        canvas.drawCircle(cx, cy, radius * 0.12f, centerPaint)
        canvas.restore()

        // Draw fixed top pointer
        drawPointer(canvas, cx, cy, radius)
    }

    // Draw each reward label inside its segment
    private fun drawSegmentText(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        radius: Float,
        startAngle: Float,
        sweepAngle: Float
    ) {
        val angle = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
        val textRadius = radius * 0.62f

        val x = (cx + cos(angle) * textRadius).toFloat()
        val y = (cy + sin(angle) * textRadius).toFloat()

        canvas.save()
        canvas.rotate(startAngle + sweepAngle / 2f + 90f, x, y)

        val lines = splitText(text)
        val lineHeight = textPaint.textSize + 4f
        val totalHeight = lineHeight * lines.size

        for ((index, line) in lines.withIndex()) {
            val lineY = y - totalHeight / 2f + (index + 1) * lineHeight
            canvas.drawText(line, x, lineY, textPaint)
        }

        canvas.restore()
    }

    // Split longer labels into two lines
    private fun splitText(text: String): List<String> {
        val parts = text.split(" ")
        return if (parts.size >= 2) {
            listOf(parts[0], parts.drop(1).joinToString(" "))
        } else {
            listOf(text)
        }
    }

    // Draw the pointer arrow above the wheel
    private fun drawPointer(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val topX = cx
        val topY = cy - radius - 28f

        val path = Path().apply {
            moveTo(topX, topY + 36f)
            lineTo(topX - 28f, topY)
            lineTo(topX + 28f, topY)
            close()
        }

        canvas.drawPath(path, pointerPaint)
    }
}