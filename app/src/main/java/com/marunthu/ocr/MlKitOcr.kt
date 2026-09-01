package com.marunthu.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * On-device, fully OFFLINE Latin-script OCR via ML Kit (bundled model — no download).
 *
 * Medicine strips print brand/generic names + strengths in Latin, so Latin OCR is all we
 * need on the input side. We never attempt on-device Tamil-script OCR (unreliable); Tamil
 * appears only on the OUTPUT side via TTS.
 */
class MlKitOcr {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Recognize text from a Bitmap. Returns the full recognized text (may be multi-line). */
    suspend fun recognize(bitmap: Bitmap): String = suspendCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { cont.resume(it.text) }
            .addOnFailureListener { cont.resume("") }
    }
}
