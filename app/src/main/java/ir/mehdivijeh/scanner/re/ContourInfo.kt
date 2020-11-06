package ir.mehdivijeh.scanner.re

import org.opencv.core.*
import org.opencv.core.Core.SVDecomp
import org.opencv.imgproc.Imgproc
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min


class ContourInfo(val contour: MatOfPoint, val rect: Rect, val tightMask: Mat) {

    private val tangent: Mat
    val localXRng: Mat
    val center: Mat
    val point0: Mat
    val point1: Mat
    val angle : Double
    var pred : ContourInfo? = null
    var succ : ContourInfo? = null

    init {
        val blobAndTangent = blobMeanAndTangent(contour)
        center = blobAndTangent.first
        tangent = blobAndTangent.second
        angle = atan2(tangent[0, 1][0], tangent[0, 0][0])

        val clx = mutableListOf<Double>()
        for (i in 0 until contour.rows()) {
            for (j in 0 until contour.cols()) {
                val point = doubleArrayOf(contour[i, j][0], contour[i, j][1])
                clx.add(projectX(tangent, center, point))
            }
        }
        val lXMin = clx.minOrNull() ?: 0.0
        val lXMax = clx.maxOrNull() ?: 0.0

        localXRng = Mat(1, 2, CvType.CV_64FC1)
        localXRng.put(0, 0, lXMin)
        localXRng.put(0, 1, lXMax)

        val scalarMin = Scalar(lXMin)
        val scalarMax = Scalar(lXMax)

        val tangentMin = Mat()
        val tangentMax = Mat()

        Core.multiply(tangent, scalarMin, tangentMin)
        Core.multiply(tangent, scalarMax, tangentMax)

        point0 = Mat()
        point1 = Mat()

        Core.add(center, tangentMin, point0)
        Core.add(center, tangentMax, point1)
    }


    private fun blobMeanAndTangent(contour: MatOfPoint): Pair<Mat, Mat> {
        val moments = Imgproc.moments(contour)
        val area = moments.m00
        val meanX = moments.m10 / area
        val meanY = moments.m01 / area

        //Logger.log(TAG, "$area   $meanX   $meanY", LoggerType.Debug)

        val momentsMatrix = Mat(2, 2, CvType.CV_64FC1)
        momentsMatrix.put(0, 0, moments.mu20 / area)
        momentsMatrix.put(0, 1, moments.mu11 / area)
        momentsMatrix.put(1, 0, moments.mu11 / area)
        momentsMatrix.put(1, 1, moments.mu02 / area)

        val svdU = Mat()
        SVDecomp(momentsMatrix, Mat(), svdU, Mat())

        val center = Mat(1, 2, CvType.CV_64FC1)
        center.put(0, 0, meanX)
        center.put(0, 1, meanY)

        val tangent = Mat(1, 2, CvType.CV_64FC1)
        tangent.put(0, 0, svdU[0, 0][0])
        tangent.put(0, 1, svdU[1, 0][0])

        return Pair(center, tangent)
    }

    private fun projectX(tangent: Mat, center: Mat, point: DoubleArray): Double {
        val tangentArray = doubleArrayOf(tangent[0, 0][0], tangent[0, 1][0])

        val pointC = mutableListOf<Double>()
        for (i in point.indices) {
            pointC.add(point[i] - center[0, i][0])
        }

        return pointC.asSequence().zip(tangentArray.asSequence()) { a, b -> a * b }.sum()
    }

    fun localOverlap(other: ContourInfo): Double {
        val xMin = projectX(tangent, center, doubleArrayOf(other.point0[0, 0][0], other.point0[0, 1][0]))
        val xMax = projectX(tangent, center, doubleArrayOf(other.point1[0, 0][0], other.point1[0, 1][0]))
        val minMax = Mat(1, 2, CvType.CV_64FC1)
        minMax.put(0, 0, xMin)
        minMax.put(0, 1, xMax)
        return intervalMeasureOverlap(localXRng, minMax)
    }

    private fun intervalMeasureOverlap(a: Mat, b: Mat): Double {
        return min(a[0, 1][0], b[0, 1][0]) - max(a[0, 0][0], b[0, 0][0])
    }

}