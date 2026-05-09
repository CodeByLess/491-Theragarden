package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // each line the user draws (path + its color)
    data class Stroke(val path: Path, val paint: Paint)

    private val strokes = mutableListOf<Stroke>() // all finished strokes

    private var currentPath = Path() // line being drawn right now
    private var currentPaint = createPaint(Color.BLACK) // current color

    // create a paint object with a color
    private fun createPaint(color: Int): Paint {
        return Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        // draw all previous strokes
        for (stroke in strokes) {
            canvas.drawPath(stroke.path, stroke.paint)
        }

        // draw the current stroke
        canvas.drawPath(currentPath, currentPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path() // start new line
                currentPath.moveTo(x, y)
            }

            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y) // keep drawing
            }

            MotionEvent.ACTION_UP -> {
                // save the stroke with its color
                strokes.add(Stroke(currentPath, Paint(currentPaint)))
            }
        }

        invalidate() // redraw screen
        return true
    }

    // change color for future strokes
    fun setColor(newColor: Int) {
        currentPaint = createPaint(newColor)
    }

    // remove last stroke
    fun undo() {
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.size - 1)
            invalidate()
        }
    }

    // clear everything
    fun clearCanvas() {
        strokes.clear()
        currentPath.reset()
        invalidate()
    }

    // turn drawing into image for saving
    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }
}