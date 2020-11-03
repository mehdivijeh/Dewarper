package ir.mehdivijeh.scanner.re

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.atan2


private const val TAG = "ContourInfo"

class ContourInfo(val contour: MatOfPoint, val rect: Rect, val tightMask: Mat) {


    init {
        val blobAndTangent = blobMeanAndTangent(contour)
        val center = blobAndTangent.first
        val tangent = blobAndTangent.second
        val angle = atan2(tangent[0, 1][0], tangent[0, 0][0])

        //TODO : fix performance
        val clx = mutableListOf<Double>()
        for (i in 0 until contour.rows()) {
            for (j in 0 until contour.cols()) {
                val point = doubleArrayOf(contour[i, j][0], contour[i, j][1])
                clx.add(projX(tangent, center, point))
            }
        }
        val lXMin = clx.minOrNull() ?: 0.0
        val lXMax = clx.maxOrNull() ?: 0.0
        val localXRng = listOf(lXMin, lXMax)

        val scalarMin = Scalar(lXMin)
        val scalarMax = Scalar(lXMin)

        val tangentMin = Mat()
        val tangentMax = Mat()

        Core.multiply(tangent, scalarMin, tangentMin)
        Core.multiply(tangent, scalarMax, tangentMax)

        val point0 = Mat()
        val point1 = Mat()
        Core.add(center, tangentMin, point0)
        Core.add(center, tangentMax, point1)
    }


    private fun blobMeanAndTangent(contour: MatOfPoint): Pair<Mat, Mat> {
        val moments = Imgproc.moments(contour)
        val area = moments._m00
        val meanX = moments.m10 / area
        val meanY = moments.m01 / area

        //Logger.log(TAG, "$area   $meanX   $meanY", LoggerType.Debug)

        val momentsMatrix = Mat(2, 2, CvType.CV_32F)
        momentsMatrix.put(0, 0, moments.mu20 / area)
        momentsMatrix.put(0, 1, moments.mu11 / area)
        momentsMatrix.put(1, 0, moments.mu11 / area)
        momentsMatrix.put(1, 1, moments.mu02)

        val svdU = Mat()
        Core.SVDecomp(momentsMatrix, Mat(), svdU, Mat())

        val center = Mat(1, 1, CvType.CV_32F)
        center.put(0, 0, meanX, meanY)

        val tangent = Mat(1, svdU.cols(), CvType.CV_32F)
        tangent.put(0, 0, svdU[0, 0][0])
        tangent.put(0, 1, svdU[0, 1][0])

        return Pair(center, tangent)
    }

    private fun projX(tangent: Mat, center: Mat, point: DoubleArray): Double {
        val tangentArray = FloatArray(tangent.height() * tangent.width())
        tangent.get(0, 0, tangentArray)

        for (i in point.indices) {
            point.toMutableList()[i] = point[i] - center[i, 0][0]
        }

        return tangentArray.asSequence().zip(point.asSequence()) { a, b -> a * b }.sum()
    }
}