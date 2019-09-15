package com.google.firebase.codelab.mlkit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText
import java.lang.IllegalStateException
import java.lang.StringBuilder

class CloudTextGraphic: GraphicOverlay.Graphic {

    private val TAG = "CloudTextGraphic"
    private val TEXT_COLOR  = Color.GREEN
    private val TEXT_SIZE = 60.0f
    private val STROKE_WIDTH = 5.0f
    private lateinit var rectPaint: Paint
    private lateinit var textPaint: Paint
    private lateinit var word: FirebaseVisionDocumentText.Word
    private lateinit var overlay: GraphicOverlay

    constructor(overlay: GraphicOverlay, word: FirebaseVisionDocumentText.Word): super(overlay) {
        this.word = word
        this.overlay = overlay
        rectPaint = Paint()
        rectPaint.color = TEXT_COLOR
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = STROKE_WIDTH
        textPaint = Paint()
        textPaint.color = TEXT_COLOR
        textPaint.textSize = TEXT_SIZE
        postInvalidate()
    }

    override fun draw(canvas: Canvas) {
        if(word == null) {
            throw IllegalStateException("Attempting to draw a null text")
        }
        val x = overlay.width /4.0f
        val y = overlay.height /4.0f
        val wordStr = StringBuilder()
        val wordRect = word.boundingBox
        canvas.drawRect(wordRect!!,rectPaint)
        val symbols = word.symbols
        for(m in symbols.indices) {
            wordStr.append(symbols.get(m).text)
        }
        canvas.drawText(wordStr.toString(),
            wordRect.left.toFloat(),wordRect.bottom.toFloat(), textPaint)
    }


}