package ir.mehdivijeh.scanner.re

import android.content.Context
import android.os.Environment
import android.util.Log
import ir.mehdivijeh.scanner.util.Logger
import ir.mehdivijeh.scanner.util.LoggerType
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.core.Core.*
import org.opencv.core.CvType.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc.*
import java.io.File
import kotlin.math.*


private const val TAG = "Dewarper"

class Dewarper(val imagePath: String, val context: Context) {

    fun dewarping() {
        var srcImage = loadImage()

        Log.d(TAG, "ori = ${srcImage.size()}")
        srcImage = resizeToScreen(srcImage)
        Log.d(TAG, "scaled = ${srcImage.size()}")
        val pair = getPageExtents(srcImage)
        val pageMask = pair.first
        val outline = pair.second
        var cInfoList = getContours(srcImage, pageMask, isMaskType = true)
        var spans = assembleSpans(srcImage, pageMask, cInfoList)

        if (spans.size < 3) {
            Log.d(TAG, "detecting lines because only ${spans.size} text spans")
            cInfoList = getContours(srcImage, pageMask, isMaskType = false)
            val spans2 = assembleSpans(srcImage, pageMask, cInfoList)

            if (spans2.size > spans.size) {
                spans = spans2
            }
        }

        if (spans.size < 1) {
            Log.d(TAG, "skipping because only ${spans.size} spans")
            return
        }

        val spanPoints = sampleSpans(srcImage, spans)

        var sumPoints = 0.0
        for (i in 0 until spanPoints.size) {
            sumPoints += spanPoints[i].rows()
        }
        Log.d(TAG, "got ${spanPoints.size} spans with $sumPoints points")

        keyPointsFromSamples(srcImage, pageMask, outline, spanPoints)
    }

    private fun loadImage(): Mat {
        OpenCVLoader.initDebug()
        return Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_UNCHANGED)
    }

    private fun resizeToScreen(img: Mat, maxW: Double = 1280.0, maxH: Double = 700.0): Mat {
        val width = img.size().width
        val height = img.size().height

        val scaleW = width / maxW
        val scaleH = height / maxH

        val scl = ceil(scaleW.coerceAtLeast(scaleH))

        if (scl > 1.0) {
            val invScl = 1.0 / scl
            val sz = Size(width * invScl, height * invScl)
            resize(img, img, sz)
            return img
        }
        return img
    }

    private fun getPageExtents(img: Mat): Pair<Mat, Mat> {
        val xMin = PAGE_MARGIN_X
        val yMin = PAGE_MARGIN_Y
        val xMax = img.size().width - PAGE_MARGIN_X
        val yMax = img.size().height - PAGE_MARGIN_Y

        val page = Mat(img.rows(), img.cols(), img.type())
        rectangle(page, Point(xMin, yMin), Point(xMax, yMax), Scalar(255.0, 255.0, 255.0), -1)

        val outline = Mat(4, 2, CV_32F)
        outline.put(0, 0, xMin)
        outline.put(0, 1, yMin)

        outline.put(1, 0, xMin)
        outline.put(1, 1, yMax)

        outline.put(2, 0, xMax)
        outline.put(2, 1, yMax)

        outline.put(3, 0, xMax)
        outline.put(3, 1, yMin)

        return Pair(page, outline)
    }

    private fun getContours(small: Mat, pageMask: Mat, isMaskType: Boolean): MutableList<ContourInfo> {
        val mask = getMask(small, pageMask, isMaskType)

        val contours: List<MatOfPoint> = ArrayList()
        findContours(mask, contours, Mat(), RETR_EXTERNAL, CHAIN_APPROX_NONE)

        val contoursOut = mutableListOf<ContourInfo>()
        for (contour in contours) {
            val rect = boundingRect(contour)
            val xMin = rect.x
            val yMin = rect.y
            val width = rect.width
            val height = rect.height

            Logger.log(TAG, "$xMin   $yMin   $width   $height", LoggerType.Debug)

            if (width < TEXT_MIN_WIDTH ||
                    height < TEXT_MIN_HEIGHT ||
                    width < (TEXT_MIN_ASPECT * height)) {
                continue
            }

            val tightMask = makeTightMask(contour, xMin, yMin, width, height)

            Logger.log(TAG, (sumElems(tightMask).`val`.maxOrNull()
                    ?: 0.0).toString(), LoggerType.Debug)
            if (sumElems(tightMask).`val`.maxOrNull() ?: 0.0 > TEXT_MAX_THICKNESS) {
                continue
            }

            contoursOut.add(ContourInfo(contour, rect, tightMask))
        }

        visualizeContours(small, contoursOut)

        return contoursOut
    }

    private fun makeTightMask(contour: MatOfPoint, xMin: Int, yMin: Int, width: Int, height: Int): Mat {
        val tightMask = Mat(height, width, CV_8U, Scalar.all(0.0))
        val tightContour = Mat()
        contour.copyTo(tightContour)

        for (i in 0 until tightContour.rows()) {
            for (j in 0 until tightContour.cols()) {
                tightContour[i, j][0] - xMin
                tightContour[i, j][1] - yMin
            }
        }

        drawContours(tightMask, listOf(MatOfPoint(tightContour)), 0, Scalar(1.0, 1.0, 1.0), -1)
        return tightMask
    }

    private fun getMask(img: Mat, pageMask: Mat, isMaskType: Boolean): Mat {
        val sGray = Mat()
        cvtColor(img, sGray, COLOR_RGB2GRAY)

        val mask = Mat()
        if (isMaskType) {

            adaptiveThreshold(sGray, mask, 255.0, ADAPTIVE_THRESH_MEAN_C,
                    THRESH_BINARY_INV, ADAPTIVE_WIN_SZ, 25.0)

            debugShow("thresholded", mask)

            dilate(mask, mask,
                    getStructuringElement(THRESH_BINARY_INV, Size(9.0, 1.0)))

            debugShow("dilated", mask)

            erode(mask, mask,
                    getStructuringElement(THRESH_BINARY_INV, Size(1.0, 3.0)))

            debugShow("eroded", mask)
        } else {
            adaptiveThreshold(sGray, mask, 255.0, ADAPTIVE_THRESH_MEAN_C,
                    THRESH_BINARY_INV, ADAPTIVE_WIN_SZ, 7.0)

            erode(mask, mask,
                    getStructuringElement(THRESH_BINARY_INV, Size(3.0, 1.0)), null, 3)

            dilate(mask, mask,
                    getStructuringElement(THRESH_BINARY_INV, Size(8.0, 2.0)))
        }

        //TODO : Complete it
        /*val rMat = Mat()
        Core.min(mask, pageMask, rMat)*/

        return mask
    }

    private fun debugShow(name: String, display: Mat) {
        val storageDir = File(context.getExternalFilesDir(
                Environment.DIRECTORY_DCIM), "Scanner")

        if (!storageDir.exists()) {
            val isCreated = storageDir.mkdir()
        }

        val outputImage = File.createTempFile(
                name,
                ".jpg",
                storageDir
        )

        Imgcodecs.imwrite(outputImage.absolutePath, display)
        Logger.log("debug_output", "$name.jpg saved in ${outputImage.absolutePath}", LoggerType.Debug)
    }

    private fun fltp(point: Mat): Point {
        return Point(point[0, 0][0], point[0, 1][0])
    }

    private fun assembleSpans(srcImage: Mat, pageMask: Mat, cInfoList: MutableList<ContourInfo>): MutableList<List<ContourInfo>> {
        cInfoList.sortBy { it.rect.y }

        val candidateEdges = mutableListOf<Triple<Double, ContourInfo, ContourInfo>>()
        cInfoList.forEachIndexed { index, contourInfo ->
            for (j in 0 until index) {
                val edge = generateCandidateEdge(contourInfo, cInfoList[j])
                edge?.let {
                    candidateEdges.add(it)
                }
            }
        }
        //sort by score
        candidateEdges.sortBy { it.first }

        candidateEdges.forEach {
            val contourInfoA = it.second
            val contourInfoB = it.third

            if (contourInfoA.succ == null && contourInfoB.pred == null) {
                contourInfoA.succ = contourInfoB
                contourInfoB.pred = contourInfoA
            }
        }

        val spans = mutableListOf<List<ContourInfo>>()
        while (cInfoList.iterator().hasNext()) {
            var cInfo: ContourInfo? = cInfoList[0]

            while (cInfo?.pred != null) {
                cInfo = cInfo.pred
            }

            var width = 0.0

            val curSpan = mutableListOf<ContourInfo>()
            while (cInfo != null) {
                cInfoList.remove(cInfo)
                curSpan.add(cInfo)
                width += cInfo.localXRng[0, 1][0] - cInfo.localXRng[0, 0][0]
                cInfo = cInfo.succ
            }

            if (width > SPAN_MIN_WIDTH) {
                spans.add(curSpan)
            }
        }

        visualizeSpans(srcImage, pageMask, spans)

        return spans
    }

    private fun generateCandidateEdge(contourInfoA: ContourInfo, contourInfoB: ContourInfo): Triple<Double, ContourInfo, ContourInfo>? {
        var contourInfoATemp = contourInfoA
        var contourInfoBTemp = contourInfoB
        if (contourInfoA.point0[0, 0][0] > contourInfoB.point1[0, 0][0]) {
            contourInfoATemp = contourInfoB
            contourInfoBTemp = contourInfoA
        }
        val xOverlapA = contourInfoATemp.localOverlap(contourInfoBTemp)
        val xOverlapB = contourInfoBTemp.localOverlap(contourInfoATemp)

        val overallTangent = Mat()
        subtract(contourInfoBTemp.center, contourInfoATemp.center, overallTangent)

        val overallAngle = atan2(overallTangent[0, 1][0], overallTangent[0, 0][0])

        val deltaAngle = max(angleDist(contourInfoATemp.angle, overallAngle),
                angleDist(contourInfoBTemp.angle, overallAngle)) * 180 / PI

        val xOverlap = max(xOverlapA, xOverlapB)

        val subtractPoint = Mat()
        subtract(contourInfoBTemp.point0, contourInfoATemp.point1, subtractPoint)
        val dist = norm(subtractPoint)

        return if (dist > EDGE_MAX_LENGTH ||
                xOverlap > EDGE_MAX_OVERLAP ||
                deltaAngle > EDGE_MAX_ANGLE) {
            null
        } else {
            val score = dist + deltaAngle * EDGE_ANGLE_COST
            Triple(score, contourInfoATemp, contourInfoBTemp)
        }

    }

    private fun angleDist(angleB: Double, angleA: Double): Double {
        var diff = angleB - angleA

        while (diff > PI) {
            diff -= 2 * PI
        }

        while (diff < -PI) {
            diff += 2 * PI
        }

        return abs(diff)
    }


    private fun sampleSpans(srcImage: Mat, spans: MutableList<List<ContourInfo>>): MutableList<Mat> {
        val spanPointsList = mutableListOf<Mat>()
        spans.forEach { span ->
            val contourPointsList = mutableListOf<Pair<Int, Double>>()
            span.forEach { cInfo ->
                val yVals = Mat(1, cInfo.tightMask.rows(), CV_64FC1)
                for (i in 0 until cInfo.tightMask.rows())
                    yVals.put(0, i, cInfo.tightMask[i, 0][0])

                val totals = Mat()
                cInfo.tightMask.copyTo(totals)
                for (i in 0 until cInfo.tightMask.rows()) {
                    for (j in 0 until cInfo.tightMask.cols()) {
                        totals.put(i, j, (cInfo.tightMask[i, j][0] * yVals[0, i][0]))
                    }
                }

                val totalsValue = Mat(1, totals.cols(), totals.channels())
                for (i in 0 until totals.cols()) {
                    var sum = 0.0
                    for (j in 0 until totals.rows()) {
                        sum += totals[j, i][0]
                    }
                    totalsValue.put(0, i, sum)
                }
                val means = Mat(1, cInfo.tightMask.cols(), CV_64FC1)
                for (i in 0 until cInfo.tightMask.cols()) {
                    var sum = 0.0
                    for (j in 0 until cInfo.tightMask.rows()) {
                        sum += cInfo.tightMask[j, i][0]
                    }
                    means.put(0, i, (sum / totalsValue[0, i][0]))
                }

                val xMin = cInfo.rect.x
                val yMin = cInfo.rect.y

                val step = SPAN_PX_PER_STEP

                val start = ((means.cols() - 1) % step) / 2

                for (x in start until means.cols() step step) {
                    contourPointsList.add(Pair(x + xMin, means[0, x][0] + yMin))
                }
            }
            val contourPoints = Mat(contourPointsList.size, 2, CV_64FC1)
            for (i in 0 until contourPointsList.size) {
                contourPoints.put(i, 0, contourPointsList[i].first.toDouble())
                contourPoints.put(i, 1, contourPointsList[i].second)
            }

            pixToNorm(srcImage, contourPoints)
            spanPointsList.add(contourPoints)
        }
        return spanPointsList
    }

    private fun pixToNorm(srcImage: Mat, contourPoints: Mat) {
        var height = srcImage.height().toDouble()
        var width = srcImage.width().toDouble()
        val scl = 2.0 / max(height, width)
        height *= 0.5
        width *= 0.5
        for (i in 0 until contourPoints.rows()) {
            contourPoints.put(i, 0, ((contourPoints[i, 0][0] - width) * scl))
            contourPoints.put(i, 1, ((contourPoints[i, 1][0] - height) * scl))
        }
    }

    private fun keyPointsFromSamples(srcImage: Mat, pageMask: Mat, outline: Mat, spanPoints: MutableList<Mat>) {
        val allEvecs = Mat(1, 2, CV_64FC1)
        var allWeights = 0.0

        spanPoints.forEach { points ->
            val evec = Mat()
            PCACompute(points, Mat(), evec, 1)

            val point = Mat(1, 2, points.channels())
            point.put(0, 0, (points[points.rows() - 1, 0][0] - points[0, 0][0]))
            point.put(0, 1, (points[points.rows() - 1, 1][0] - points[0, 1][0]))

            val weight = norm(point)
            val weightScalar = Scalar(norm(point))

            multiply(evec, weightScalar, evec)
            allEvecs.put(0, 0, (allEvecs[0, 0][0] + evec[0, 0][0]))
            allEvecs.put(0, 0, (allEvecs[0, 1][0] + evec[0, 1][0]))
            allWeights += weight
        }
        val evec = Mat()
        divide(allEvecs, Scalar(allWeights), evec)
        val xDir = evec.reshape(1)

        if (xDir[0, 0][0] < 0) {
            xDir.put(0, 0, -xDir[0, 0][0])
        }

        val yDir = Mat(1, 1, CV_8S)
        yDir.put(0, 0, -xDir[0, 1][0])
        yDir.put(0, 1, xDir[0, 0][0])

        val pageCoords = MatOfInt()
        val points: Mat = Mat.zeros(outline.size(), outline.type())
        findNonZero(outline, points)
        convexHull(MatOfPoint(points), pageCoords)

        pixToNorm(pageMask, pageCoords.reshape(1, 1))
        pageCoords.reshape(1, 4)
        //TODO : complete from here
    }

    private fun visualizeContours(small: Mat, cInfoList: List<ContourInfo>) {
        val regions = Mat.zeros(small.height(), small.width(), CV_16SC3)

        cInfoList.forEachIndexed { index, contourInfo ->
            drawContours(regions, listOf(contourInfo.contour), 0, COLORS[index % COLORS.size], -1)
        }

        debugShow("contours1", regions)

        val display = Mat()
        small.copyTo(display)
        for (i in 0 until regions.rows()) {
            for (j in 0 until regions.cols()) {
                val x = regions.get(i, j)[0]
                val y = regions.get(i, j)[1]
                val z = regions.get(i, j)[2]
                if (listOf(x, y, z).maxOf { it } != 0.0) {
                    display.put(i, j, (display[i, j][0] / 2.0) + (regions[i, j][0] / 2.0),
                            (display[i, j][1] / 2.0) + (regions[i, j][1] / 2.0),
                            (display[i, j][2] / 2.0) + (regions[i, j][2] / 2.0))
                }
            }
        }

        cInfoList.forEachIndexed { index, contourInfo ->
            circle(display, fltp(contourInfo.center), 3, Scalar(255.0, 255.0, 255.0), 1, LINE_AA)
            line(display, fltp(contourInfo.point0), fltp(contourInfo.point1), Scalar(255.0, 255.0, 255.0), 1, LINE_AA)
        }

        debugShow("contours", display)
    }

    private fun visualizeSpans(small: Mat, pagemask: Mat, spans: MutableList<List<ContourInfo>>) {
        val regions = Mat.zeros(small.height(), small.width(), CV_16SC3)

        spans.forEachIndexed { index, contourInfo ->
            val contour = contourInfo.map { it.contour }
            drawContours(regions, contour, -1, COLORS[index * 3 % COLORS.size], -1)
        }

        val display = Mat()
        small.copyTo(display)
        for (i in 0 until regions.rows()) {
            for (j in 0 until regions.cols()) {
                val x = regions.get(i, j)[0]
                val y = regions.get(i, j)[1]
                val z = regions.get(i, j)[2]
                if (listOf(x, y, z).maxOf { it } != 0.0) {
                    display.put(i, j, (display[i, j][0] / 2.0) + (regions[i, j][0] / 2.0),
                            (display[i, j][1] / 2.0) + (regions[i, j][1] / 2.0),
                            (display[i, j][2] / 2.0) + (regions[i, j][2] / 2.0))
                }

                val xPageMask = pagemask.get(i, j)[0]
                val yPageMask = pagemask.get(i, j)[1]
                val zPageMask = pagemask.get(i, j)[2]
                if (listOf(xPageMask, yPageMask, zPageMask).equals(0.0)) {
                    display.put(i, j, (display[i, j][0] / 4.0),
                            (display[i, j][1] / 4.0),
                            (display[i, j][2] / 4.0))
                }
            }
        }

        debugShow("spans", display)
    }
}