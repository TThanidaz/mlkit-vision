package com.google.firebase.codelab.mlkit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint

class LabelGraphic : GraphicOverlay.Graphic {
    private val TAG = "LabelGraphic"
    private lateinit var textPaint: Paint
    private lateinit var bgPaint: Paint
    private lateinit var overlay: GraphicOverlay

    private lateinit var labels: List<String>

    constructor(overlay: GraphicOverlay, labels: List<String>): super(overlay) {
        this.overlay = overlay
        this.labels = labels
        textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 60.0f
        bgPaint = Paint()
        bgPaint.color = Color.BLACK
        bgPaint.alpha = 50
    }

    override fun draw(canvas: Canvas) {
        var x = overlay.width/4.0f
        var y = overlay.height/4.0f
        for(label in labels) {
            drawTextWithBackground(label,x,y, TextPaint(textPaint),bgPaint,canvas)
            y -= 62.0f
        }
    }

    private fun drawTextWithBackground(text: String, x: Float, y: Float,
                                       paint: TextPaint, bgPaint: Paint, canvas: Canvas) {
        val fontMetrics = paint.fontMetrics
        canvas.drawRect(
            Rect(x.toInt(),
                (y+fontMetrics.top).toInt(),
                (x+paint.measureText(text)).toInt(),
                (y+fontMetrics.bottom).toInt()),
                bgPaint
        )
        canvas.drawText(text,x,y,textPaint)
    }

}