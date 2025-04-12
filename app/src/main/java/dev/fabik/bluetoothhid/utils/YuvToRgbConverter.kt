import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type


class YuvToRgbConverter(context: Context) {
    private val rs: RenderScript = RenderScript.create(context)
    private val script: ScriptIntrinsicYuvToRGB =
        ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    private var yuvType: Type? = null
    private var inputAllocation: Allocation? = null
    private var outputAllocation: Allocation? = null

    fun yuvToRgb(image: Image, output: Bitmap) {
        val width = image.width
        val height = image.height

        val yuvBytes = yuv420ToNv21(image)

        if (yuvType == null || inputAllocation == null) {
            yuvType = Type.Builder(rs, Element.U8(rs)).setX(yuvBytes.size).create()
            inputAllocation = Allocation.createTyped(rs, yuvType, Allocation.USAGE_SCRIPT)
        }

        if (outputAllocation == null) {
            val outType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height).create()
            outputAllocation = Allocation.createTyped(rs, outType, Allocation.USAGE_SCRIPT)
        }

        inputAllocation?.copyFrom(yuvBytes)
        script.setInput(inputAllocation)
        script.forEach(outputAllocation)
        outputAllocation?.copyTo(output)
    }

    private fun yuv420ToNv21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)

        image.planes[0].buffer.get(nv21, 0, ySize)

        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val rowStride = image.planes[1].rowStride
        val pixelStride = image.planes[1].pixelStride

        val uBytes = ByteArray(uBuffer.remaining())
        uBuffer.get(uBytes)
        val vBytes = ByteArray(vBuffer.remaining())
        vBuffer.get(vBytes)

        var pos = ySize
        val uvWidth = width / 2
        val uvHeight = height / 2
        for (row in 0 until uvHeight) {
            val rowStart = row * rowStride
            for (col in 0 until uvWidth) {
                nv21[pos++] = vBytes[rowStart + col * pixelStride]
                nv21[pos++] = uBytes[rowStart + col * pixelStride]
            }
        }
        return nv21
    }

    fun destroy() {
        rs.destroy()
    }
}
