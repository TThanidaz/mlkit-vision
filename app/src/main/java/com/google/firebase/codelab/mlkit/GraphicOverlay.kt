package com.google.firebase.codelab.mlkit

import android.content.Context
import android.graphics.Canvas
import android.hardware.camera2.CameraCharacteristics
import android.util.AttributeSet
import android.view.View

class GraphicOverlay(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val lock = Object()
    private var previewWidth = 0
    private var previewHeight = 0
    private var widthScaleFactor = 1.0f
    private var heightScaleFactor = 1.0f
    private var facing = CameraCharacteristics.LENS_FACING_BACK
    private var graphics: HashSet<Graphic> = HashSet()

    abstract class Graphic(private var overlay: GraphicOverlay) {
        fun getApplicationContext(): Context? {
            return overlay.context.applicationContext
        }

        abstract fun draw(canvas: Canvas)
        fun scaleX(horizontal: Float): Float {
            return horizontal * overlay.widthScaleFactor
        }
        fun scaleY(verical: Float): Float {
            return verical * overlay.heightScaleFactor
        }
        fun translateX(x: Float): Float {
            if(overlay.facing == CameraCharacteristics.LENS_FACING_FRONT){
                return overlay.width - scaleX(x)
            } else {
                return scaleX(x)
            }
        }
        fun translateY(y: Float): Float {
            return scaleY(y)
        }

        fun postInvalidate() {
            overlay.invalidate()
        }
    }

    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }
    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
        postInvalidate()
    }
    fun remove(graphic: Graphic) {
        synchronized(lock) {
            graphics.remove(graphic)
        }
        postInvalidate()
    }

    fun setCameraInfo(previewWidth: Int, previewHeight: Int, facing: Int) {
        synchronized(lock) {
            this.previewWidth = previewWidth
            this.previewHeight = previewHeight
            this.facing = facing
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            if((this.previewWidth != 0) && (this.previewHeight != 0)) {
                this.widthScaleFactor = canvas.width.toFloat() / this.previewWidth.toFloat()
                this.heightScaleFactor = canvas.height.toFloat() / this.previewHeight.toFloat()
            }
            for(graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}