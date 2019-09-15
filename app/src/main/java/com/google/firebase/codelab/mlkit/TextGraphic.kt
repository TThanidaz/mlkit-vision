package com.google.firebase.codelab.mlkit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.lang.IllegalStateException

class TextGraphic(overlay: GraphicOverlay) : GraphicOverlay.Graphic(overlay) {
    private val TAG = "TextGraphic"
    private val TEXT_COLOR = Color.RED
    private val TEXT_SIZE = 54.0f
    private val STROKE_WIDTH = 4.0f

    private lateinit var rectPaint: Paint
    private lateinit var textPaint: Paint
    private lateinit var element: FirebaseVisionText.Element

    constructor(overlay: GraphicOverlay, element: FirebaseVisionText.Element) : this(overlay) {
        this.element = element
        rectPaint = Paint()
        rectPaint.color = TEXT_COLOR
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = STROKE_WIDTH

        textPaint = Paint()
        textPaint.color = TEXT_COLOR
        textPaint.textSize = TEXT_SIZE
//        Redraw the overlay, as this graphic has been added
        this.postInvalidate()
    }

    override fun draw(canvas: Canvas) {
        Log.d(TAG, "on draw text graphic")
        if(this.element == null) {
            throw IllegalStateException("Attempting to draw a null text")
        }
//        Draws the bounding box around the textbox
        val rect = RectF(this.element.boundingBox)
        canvas.drawRect(rect, rectPaint)
//        Renders the text at the bottom of the box
        canvas.drawText(element.text, rect.left, rect.bottom, textPaint)
    }

}