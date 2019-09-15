package com.google.firebase.codelab.mlkit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark


class FaceContourGraphic : GraphicOverlay.Graphic {
    private val TAG = "FaceContourGraphic"
    private val FACE_POSITION_RADIUS = 10.0f
    private val ID_TEXT_SIZE = 70.0f
    private val ID_Y_OFFSET = 80.0f
    private val ID_X_OFFSET = -70.0f
    private val BOX_STROKE_WIDTH = 5.0f

    private val COLOR_CHOICES = arrayOf(
        Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.RED, Color.WHITE, Color.YELLOW
    )
    private var currentColorIndex = 0
    private lateinit var facePositionPaint: Paint
    private lateinit var idPaint: Paint
    private lateinit var boxPaint: Paint

    @Volatile private lateinit var firebaseVisionFace: FirebaseVisionFace

    constructor(overlay: GraphicOverlay): super(overlay) {
        currentColorIndex = (currentColorIndex+1) % COLOR_CHOICES.size
        val selectedColor = COLOR_CHOICES[currentColorIndex]
        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor
        idPaint = Paint()
        idPaint.color = selectedColor
        idPaint.textSize = ID_TEXT_SIZE
        boxPaint = Paint()
        boxPaint.color = selectedColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH
    }

    fun updateFace(face: FirebaseVisionFace) {
        firebaseVisionFace = face
        postInvalidate()
    }

    override fun draw(canvas: Canvas) {
        val face = firebaseVisionFace
        if(face == null) {
            return
        }
        val x = translateX(face.boundingBox.centerX().toFloat())
        val y = translateY(face.boundingBox.centerY().toFloat())
        canvas.drawCircle(x,y,FACE_POSITION_RADIUS,facePositionPaint)
        canvas.drawText("id: "+face.trackingId,
            x+ID_X_OFFSET, y+ID_Y_OFFSET, idPaint)
//        Draw a bounding box around the face
        val xOffset = scaleX(face.boundingBox.width()/2.0f)
        val yOffset = scaleY(face.boundingBox.height()/2.0f)
        val left = x - xOffset
        val right = x + xOffset
        val top = y - yOffset
        val bottom = y + yOffset
        canvas.drawRect(left,top,right,bottom,boxPaint)

        val contour = face.getContour(FirebaseVisionFaceContour.ALL_POINTS)
        for(point in contour.points) {
            val px = translateX(point.x)
            val py = translateY(point.y)
            canvas.drawCircle(px,py,FACE_POSITION_RADIUS,facePositionPaint)
        }

        if (face.smilingProbability >= 0) {
            canvas.drawText(
                "happiness: "+String.format("%.2f",face.smilingProbability),
                x + ID_X_OFFSET *3,
                y - ID_Y_OFFSET,
                idPaint
            )
        }
        if (face.rightEyeOpenProbability >= 0) {
            canvas.drawText(
                "right eye: " + String.format("%.2f", face.rightEyeOpenProbability),
                x - ID_X_OFFSET,
                y,
                idPaint
            )
        }
        if (face.leftEyeOpenProbability >= 0) {
            canvas.drawText(
                "left eye: " + String.format("%.2f", face.leftEyeOpenProbability),
                x + ID_X_OFFSET * 6,
                y,
                idPaint
            )
        }
        val leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE)
        if (leftEye != null && leftEye.position != null) {
            canvas.drawCircle(
                translateX(leftEye.position.x),
                translateY(leftEye.position.y),
                FACE_POSITION_RADIUS,
                facePositionPaint
            )
        }
        val rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE)
        if (rightEye != null && rightEye.position != null) {
            canvas.drawCircle(
                translateX(rightEye.position.x),
                translateY(rightEye.position.y),
                FACE_POSITION_RADIUS,
                facePositionPaint
            )
        }

        val leftCheek = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_CHEEK)
        if (leftCheek != null && leftCheek.position != null) {
            canvas.drawCircle(
                translateX(leftCheek.position.x),
                translateY(leftCheek.position.y),
                FACE_POSITION_RADIUS,
                facePositionPaint
            )
        }
        val rightCheek = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_CHEEK)
        if (rightCheek != null && rightCheek.position != null) {
            canvas.drawCircle(
                translateX(rightCheek.position.x),
                translateY(rightCheek.position.y),
                FACE_POSITION_RADIUS,
                facePositionPaint
            )
        }
    }

}