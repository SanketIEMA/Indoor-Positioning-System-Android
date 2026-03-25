package com.example.indoornavigation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GridOverlayView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var rows = 0
    private var cols = 0

    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2f
    }

    fun setGridSize(width: Int, height: Int) {
        cols = width
        rows = height
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // ❌ No grid initially
        if (rows == 0 || cols == 0) return

        val cellWidth = width.toFloat() / cols
        val cellHeight = height.toFloat() / rows

        for (i in 0..cols) {
            val x = i * cellWidth
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
        }

        for (i in 0..rows) {
            val y = i * cellHeight
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
        }
    }
}