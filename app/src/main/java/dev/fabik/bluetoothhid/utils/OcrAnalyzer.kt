package dev.fabik.bluetoothhid.utils

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.nio.ByteBuffer

class OcrAnalyzer(
    private val onTextDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            processImage(inputImage, imageProxy)
        } else {
            imageProxy.close()
        }
    }

    private fun processImage(image: InputImage, imageProxy: ImageProxy) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onTextDetected(visionText.text)
                imageProxy.close()
            }
            .addOnFailureListener { e ->
                Log.e("OcrAnalyzer", "Text recognition error: ${e.message}")
                imageProxy.close()
            }
    }
}
