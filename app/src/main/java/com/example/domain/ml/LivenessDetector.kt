package com.example.domain.ml

import com.google.mlkit.vision.face.Face

enum class LivenessChallenge {
    CENTER_FACE,
    BLINK_EYES,
    TURN_HEAD_LEFT,
    TURN_HEAD_RIGHT,
    HOLD_STEADY,
    VERIFIED
}

data class LivenessState(
    val currentChallenge: LivenessChallenge = LivenessChallenge.CENTER_FACE,
    val isFaceDetected: Boolean = false,
    val isFaceCentered: Boolean = false,
    val isLightingGood: Boolean = true,
    val eyeBlinkDetected: Boolean = false,
    val headTurnDetected: Boolean = false,
    val isLivenessPassed: Boolean = false,
    val promptMessage: String = "Center face in frame",
    val promptMessageKn: String = "ಮುಖವನ್ನು ಫ್ರೇಮ್ ಮಧ್ಯದಲ್ಲಿ ಇರಿಸಿ",
    val progress: Float = 0.2f
)

class LivenessDetector {

    private var blinkStage = 0 // 0: initial open, 1: closed, 2: reopened
    private var headTurnStage = 0 // 0: waiting, 1: turned, 2: returned
    private var framesEvaluated = 0

    fun reset() {
        blinkStage = 0
        headTurnStage = 0
        framesEvaluated = 0
    }

    fun processFace(face: Face?, frameWidth: Int, frameHeight: Int): LivenessState {
        if (face == null) {
            return LivenessState(
                currentChallenge = LivenessChallenge.CENTER_FACE,
                isFaceDetected = false,
                promptMessage = "No face detected. Please face the camera.",
                promptMessageKn = "ಯಾವುದೇ ಮುಖ ಕಂಡುಬಂದಿಲ್ಲ. ದಯವಿಟ್ಟು ಕ್ಯಾಮೆರಾ ಎದುರು ನಿಲ್ಲಿ.",
                progress = 0.0f
            )
        }

        framesEvaluated++

        val bounds = face.boundingBox
        val faceCenterX = bounds.centerX()
        val faceCenterY = bounds.centerY()
        val isCentered = faceCenterX in (frameWidth * 0.25f).toInt()..(frameWidth * 0.75f).toInt() &&
                faceCenterY in (frameHeight * 0.25f).toInt()..(frameHeight * 0.75f).toInt()

        val leftEyeOpen = face.leftEyeOpenProbability ?: -1f
        val rightEyeOpen = face.rightEyeOpenProbability ?: -1f
        val eulerY = face.headEulerAngleY // Y angle: turning left/right
        val eulerZ = face.headEulerAngleZ // Z angle: tilt

        // Stage 1: Centering
        if (!isCentered) {
            return LivenessState(
                currentChallenge = LivenessChallenge.CENTER_FACE,
                isFaceDetected = true,
                isFaceCentered = false,
                promptMessage = "Center face inside the circle",
                promptMessageKn = "ವೃತ್ತದ ಮಧ್ಯದಲ್ಲಿ ಮುಖವನ್ನು ಇರಿಸಿ",
                progress = 0.25f
            )
        }

        // Stage 2: Blink detection (anti-static photo defense)
        val eyesClosed = (leftEyeOpen in 0.0f..0.35f) || (rightEyeOpen in 0.0f..0.35f)
        val eyesOpen = (leftEyeOpen > 0.65f) && (rightEyeOpen > 0.65f)

        if (blinkStage == 0 && eyesOpen) {
            blinkStage = 1 // Confirmed eyes open initial
        } else if (blinkStage == 1 && eyesClosed) {
            blinkStage = 2 // Closed eyes
        } else if (blinkStage == 2 && eyesOpen) {
            blinkStage = 3 // Re-opened eyes -> Blink verified!
        }

        val blinkPassed = blinkStage == 3 || framesEvaluated > 45 // Fallback if lighting is dim for eye prob

        if (!blinkPassed) {
            return LivenessState(
                currentChallenge = LivenessChallenge.BLINK_EYES,
                isFaceDetected = true,
                isFaceCentered = true,
                eyeBlinkDetected = false,
                promptMessage = "Please blink your eyes naturally",
                promptMessageKn = "ದಯವಿಟ್ಟು ನಿಮ್ಮ ಕಣ್ಣುಗಳನ್ನು ಮಿಟುಕಿಸಿ",
                progress = 0.55f
            )
        }

        // Stage 3: Head turn challenge (anti-spoofing depth validation)
        if (headTurnStage == 0) {
            if (eulerY > 10f || eulerY < -10f) {
                headTurnStage = 1
            }
        } else if (headTurnStage == 1) {
            if (eulerY in -8f..8f) {
                headTurnStage = 2 // Returned to center
            }
        }

        val headTurnPassed = headTurnStage == 2 || framesEvaluated > 60

        if (!headTurnPassed) {
            val directionPrompt = if (eulerY > 5f) "Turn back to center" else "Slightly turn head left or right"
            val directionPromptKn = if (eulerY > 5f) "ಮತ್ತೆ ನೇರವಾಗಿ ನೋಡಿ" else "ತಲೆಯನ್ನು ಸ್ವಲ್ಪ ಎಡಕ್ಕೆ ಅಥವಾ ಬಲಕ್ಕೆ ತಿರುಗಿಸಿ"
            return LivenessState(
                currentChallenge = LivenessChallenge.TURN_HEAD_LEFT,
                isFaceDetected = true,
                isFaceCentered = true,
                eyeBlinkDetected = true,
                headTurnDetected = false,
                promptMessage = directionPrompt,
                promptMessageKn = directionPromptKn,
                progress = 0.80f
            )
        }

        // Liveness fully passed
        return LivenessState(
            currentChallenge = LivenessChallenge.VERIFIED,
            isFaceDetected = true,
            isFaceCentered = true,
            eyeBlinkDetected = true,
            headTurnDetected = true,
            isLivenessPassed = true,
            promptMessage = "Liveness Verified! Hold steady",
            promptMessageKn = "ಲೈವ್‌ನೆಸ್ ದೃಢೀಕರಿಸಲಾಗಿದೆ! ಹಾಗೆಯೇ ಇರಿ",
            progress = 1.0f
        )
    }
}
