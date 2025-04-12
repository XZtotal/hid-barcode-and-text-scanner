package dev.fabik.bluetoothhid.utils

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.nio.ByteBuffer
import java.util.Queue

class OcrAnalyzer(
    private val onTextDetected: (String) -> Unit,
    private val ocrBuffer: Queue<String>, // Add buffer parameter
    private val bufferLock: Any, // Add buffer lock parameter
    private val delimitedFrame: Rect // Add delimited frame parameter
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
        val croppedBitmap = cropToDelimitedFrame(image.bitmapInternal)
        val croppedImage = InputImage.fromBitmap(croppedBitmap, image.rotationDegrees)

        recognizer.process(croppedImage)
            .addOnSuccessListener { visionText ->
                onTextDetected(visionText.text)
                // Add detected OCR value to the buffer
                synchronized(bufferLock) {
                    ocrBuffer.add(visionText.text)
                }
                imageProxy.close()
            }
            .addOnFailureListener { e ->
                Log.e("OcrAnalyzer", "Text recognition error: ${e.message}")
                imageProxy.close()
            }
    }

    private fun cropToDelimitedFrame(bitmap: Bitmap): Bitmap {
        return Bitmap.createBitmap(
            bitmap,
            delimitedFrame.left,
            delimitedFrame.top,
            delimitedFrame.width(),
            delimitedFrame.height()
        )
    }
}
