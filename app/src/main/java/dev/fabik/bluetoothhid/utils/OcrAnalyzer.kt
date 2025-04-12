package dev.fabik.bluetoothhid.utils

import android.graphics.Rect as AndroidRect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Queue

class OcrAnalyzer(
    private val onTextDetected: (String) -> Unit,
    private val delimitedFrame: Rect
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
        val rotation = imageProxy.imageInfo.rotationDegrees
        val imageWidth = image.width
        val imageHeight = image.height

        // Convertir delimitedFrame al sistema de coordenadas original de la imagen
//        val adjustedRect = when (rotation) {
//            90 -> {
//                Rect(
//                    left = (imageWidth - delimitedFrame.bottom).toFloat(),
//                    top = delimitedFrame.left,
//                    right = (imageWidth - delimitedFrame.top).toFloat(),
//                    bottom = delimitedFrame.right
//                )
//            }
//            180 -> {
//                Rect(
//                    left = (imageWidth - delimitedFrame.right).toFloat(),
//                    top = (imageHeight - delimitedFrame.bottom).toFloat(),
//                    right = (imageWidth - delimitedFrame.left).toFloat(),
//                    bottom = (imageHeight - delimitedFrame.top).toFloat()
//                )
//            }
//            270 -> {
//                Rect(
//                    left = delimitedFrame.top,
//                    top = (imageHeight - delimitedFrame.right).toFloat(),
//                    right = delimitedFrame.bottom,
//                    bottom = (imageHeight - delimitedFrame.left).toFloat()
//                )
//            }
//            else -> delimitedFrame // 0 grados

//        }
        val adjustedRect = Rect(
                    left = 0f,
                    top = 580f,
                    right = 1080f,
                    bottom = 860f
                )

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val filteredText = buildString {
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            line.boundingBox?.let { boundingBox ->
                                val lineRect = Rect(
                                    left = boundingBox.left.toFloat(),
                                    top = boundingBox.top.toFloat(),
                                    right = boundingBox.right.toFloat(),
                                    bottom = boundingBox.bottom.toFloat()
                                )
                                if (adjustedRect.left <= lineRect.left &&
                                    adjustedRect.top <= lineRect.top &&
                                    adjustedRect.right >= lineRect.right &&
                                    adjustedRect.bottom >= lineRect.bottom) {
                                    append(line.text).append("\n")
                                }
                            }
                        }
                    }
                }.trim()

                onTextDetected(filteredText)

                imageProxy.close()
            }
            .addOnFailureListener { e ->
                Log.e("OcrAnalyzer", "Error en reconocimiento de texto: ${e.message}")
                imageProxy.close()
            }
    }
}