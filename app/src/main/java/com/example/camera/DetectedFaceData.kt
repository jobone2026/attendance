package com.example.camera

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark

enum class LandmarkType(val label: String) {
    LEFT_EYE("Left Eye"),
    RIGHT_EYE("Right Eye"),
    NOSE_BASE("Nose"),
    MOUTH_LEFT("Mouth (L)"),
    MOUTH_RIGHT("Mouth (R)"),
    MOUTH_BOTTOM("Mouth (B)"),
    LEFT_CHEEK("Left Cheek"),
    RIGHT_CHEEK("Right Cheek"),
    LEFT_EAR("Left Ear"),
    RIGHT_EAR("Right Ear")
}

data class FaceLandmarkPoint(
    val type: LandmarkType,
    val point: PointF,
    val positionX: Float,
    val positionY: Float
)

data class FaceDetectionResult(
    val isDetected: Boolean = false,
    val rawFace: Face? = null,
    val boundingBox: Rect = Rect(),
    val normalizedBoundingBox: RectF = RectF(),
    val landmarks: List<FaceLandmarkPoint> = emptyList(),
    val leftEyePosition: PointF? = null,
    val rightEyePosition: PointF? = null,
    val noseBasePosition: PointF? = null,
    val mouthLeftPosition: PointF? = null,
    val mouthRightPosition: PointF? = null,
    val mouthBottomPosition: PointF? = null,
    val leftEyeOpenProbability: Float? = null,
    val rightEyeOpenProbability: Float? = null,
    val smilingProbability: Float? = null,
    val headEulerAngleX: Float = 0f,
    val headEulerAngleY: Float = 0f,
    val headEulerAngleZ: Float = 0f,
    val trackingId: Int? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val rotationDegrees: Int = 0,
    val isFrontCamera: Boolean = true
)
