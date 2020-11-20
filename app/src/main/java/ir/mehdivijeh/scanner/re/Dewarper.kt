package ir.mehdivijeh.scanner.re

import android.content.Context
import android.os.Environment
import android.util.Log
import ir.mehdivijeh.scanner.util.Logger
import ir.mehdivijeh.scanner.util.LoggerType
import kotlinx.coroutines.*
import org.apache.commons.math3.analysis.MultivariateFunction
import org.apache.commons.math3.optim.InitialGuess
import org.apache.commons.math3.optim.MaxEval
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer
import org.opencv.android.OpenCVLoader
import org.opencv.calib3d.Calib3d
import org.opencv.calib3d.Calib3d.projectPoints
import org.opencv.core.*
import org.opencv.core.Core.*
import org.opencv.core.CvType.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc.*
import java.io.File
import kotlin.math.*


private const val TAG = "Dewarper"

class Dewarper(val imagePath: String, val context: Context) {

    private val k: Mat

    init {
        OpenCVLoader.initDebug()

        // default intrinsic parameter matrix
        k = Mat(3, 3, CV_64F)
        k.put(0, 0, FOCAL_LENGTH)
        k.put(0, 1, (0.0))
        k.put(0, 2, (0.0))
        k.put(1, 0, (0.0))
        k.put(1, 1, FOCAL_LENGTH)
        k.put(1, 2, (0.0))
        k.put(2, 0, (0.0))
        k.put(2, 1, (0.0))
        k.put(2, 2, (1.0))
    }

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

        val keyPointReturnValues = keyPointsFromSamples(srcImage, pageMask, outline, spanPoints)
        val corners = keyPointReturnValues.first
        val ycoords = keyPointReturnValues.second
        val xcoords = keyPointReturnValues.third

        val getDefaultParamsReturnValues = getDefaultParams(corners, ycoords, xcoords)
        val roughDims = getDefaultParamsReturnValues.first
        val spanCounts = getDefaultParamsReturnValues.second
        val params = getDefaultParamsReturnValues.third

        val cornersPageWidth = Mat(1, 2, CV_64FC1)
        cornersPageWidth.put(0, 0, corners[0, 0][0])
        cornersPageWidth.put(0, 1, corners[0, 1][0])

        spanPoints.add(0, cornersPageWidth)

        val dstPoints = Mat()
        vconcat(spanPoints, dstPoints)

        GlobalScope.launch {
            val optimizeParams = optimizeParams(srcImage, dstPoints, spanCounts, params)
        }

        //getPageDims(corners, roughDims, optimizeParams)

    }


    private fun loadImage(): Mat {
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

        val page = Mat.zeros(img.rows(), img.cols(), img.type())
        rectangle(page, Point(xMin, yMin), Point(xMax, yMax), Scalar(255.0, 255.0, 255.0), -1)

        val outline = Mat(4, 2, CV_64F)
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

            if (width < TEXT_MIN_WIDTH ||
                    height < TEXT_MIN_HEIGHT ||
                    width < (TEXT_MIN_ASPECT * height)) {
                continue
            }

            val tightMask = makeTightMask(contour, xMin, yMin, width, height)

            var max = 0.0
            for (i in 0 until tightMask.cols()) {
                var sum = 0.0
                for (j in 0 until tightMask.rows()) {
                    sum += tightMask[j, i][0]
                }
                if (max < sum) max = sum
            }

            if (max > TEXT_MAX_THICKNESS) {
                continue
            }

            contoursOut.add(ContourInfo(contour, rect, tightMask))
        }

        visualizeContours(small, contoursOut)

        return contoursOut
    }

    private fun makeTightMask(contour: MatOfPoint, xMin: Int, yMin: Int, width: Int, height: Int): Mat {
        val tightMask = Mat.zeros(height, width, CV_64F)
        val tightContour = Mat()
        contour.copyTo(tightContour)

        for (i in 0 until tightContour.rows()) {
            for (j in 0 until tightContour.cols()) {
                tightContour.put(i, j, (contour[i, j][0] - xMin), (contour[i, j][1] - yMin))
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

            debugShow("thresholded", mask)

            erode(mask, mask,
                    getStructuringElement(THRESH_BINARY_INV, Size(3.0, 1.0)), Point(), 3)

            debugShow("eroded", mask)

            dilate(mask, mask,
                    getStructuringElement(THRESH_BINARY_INV, Size(8.0, 2.0)))

            debugShow("dilated", mask)
        }

        /*//TODO : Complete it
        cvtColor(pageMask,pageMask, Imgproc.COLOR_BGR2GRAY);
        Logger.log("debug_output", mask.toString(), LoggerType.Debug)
        Logger.log("debug_output", pageMask.toString(), LoggerType.Debug)
        val rMat = Mat()
        min(mask, pageMask, rMat)*/

        return mask
    }

    private fun debugShow(name: String, display: Mat) {
        val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DCIM), "Scanner")

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

                for (i in 0 until cInfo.tightMask.rows()) {
                    var sum = 0.0
                    for (j in 0 until cInfo.tightMask.cols()) {
                        sum += cInfo.tightMask[i, j][0]
                    }
                }

                val yVals = Mat(cInfo.tightMask.rows(), 1, CV_64FC1)
                for (i in 0 until cInfo.tightMask.rows()) {
                    yVals.put(i, 0, i.toDouble())
                }

                val totals = Mat()
                cInfo.tightMask.copyTo(totals)
                for (i in 0 until cInfo.tightMask.rows()) {
                    for (j in 0 until cInfo.tightMask.cols()) {
                        totals.put(i, j, (cInfo.tightMask[i, j][0] * yVals[i, 0][0]))
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
                    means.put(0, i, (totalsValue[0, i][0] / sum))
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
            contourPoints.put(i, 0, ((contourPoints[i, 0][0] - height) * scl))
            contourPoints.put(i, 1, ((contourPoints[i, 1][0] - width) * scl))
        }
    }

    private fun normToPix(srcImage: Mat, contourPoints: Mat, asInteger: Boolean): Mat {
        val height = srcImage.height().toDouble()
        val width = srcImage.width().toDouble()
        val scl = max(height, width) * 0.5

        val offset = Mat(1, 2, contourPoints.type())
        offset.put(0, 0, (height * 0.5))
        offset.put(0, 1, (width * 0.5))

        val contourPointsMul = Mat()
        multiply(contourPoints, Scalar(scl), contourPointsMul)

        val rval = Mat(contourPointsMul.rows(), 2, CV_64F)
        for (i in 0 until contourPointsMul.rows()) {
            rval.put(i, 0, (contourPointsMul[i, 0][0] + offset[0, 0][0]))
            rval.put(i, 1, (contourPointsMul[i, 1][0] + offset[0, 1][0]))
        }

        if (asInteger) {
            add(rval, Scalar(0.5), rval)
            for (i in 0 until contourPointsMul.rows()) {
                rval.put(i, 0, (rval[i, 0][0].toInt()).toDouble())
                rval.put(i, 1, (rval[i, 1][0].toInt()).toDouble())
            }
        }

        return rval
    }

    private fun keyPointsFromSamples(srcImage: Mat, pageMask: Mat, outline: Mat, spanPoints: MutableList<Mat>): Triple<Mat, MutableList<Double>, MutableList<ArrayList<Double>>> {
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
            allEvecs.put(0, 1, (allEvecs[0, 1][0] + evec[0, 1][0]))
            allWeights += weight
        }
        val evec = Mat()
        divide(allEvecs, Scalar(allWeights), evec)

        val xDir = Mat()
        evec.copyTo(xDir)

        if (xDir[0, 0][0] < 0) {
            xDir.put(0, 0, -xDir[0, 0][0])
            xDir.put(0, 1, -xDir[0, 1][0])
        }

        val yDir = Mat(1, 2, CV_64FC1)
        yDir.put(0, 0, -xDir[0, 1][0])
        yDir.put(0, 1, xDir[0, 0][0])

        val points = Mat.zeros(outline.size(), outline.type())
        findNonZero(outline, points)

        val pageCoords = MatOfInt()
        convexHull(MatOfPoint(points), pageCoords)

        val pageCoordsSort = Mat(4, 2, outline.type())
        for (i in 0 until pageCoords.rows()) {
            pageCoordsSort.put(i, 0, outline[pageCoords[i, 0][0].toInt(), 0][0])
            pageCoordsSort.put(i, 1, outline[pageCoords[i, 0][0].toInt(), 1][0])
        }

        pixToNorm(pageMask, pageCoordsSort)

        val pxCoords = Mat()
        val pyCoords = Mat()

        val xDirShape = Mat(2, 1, xDir.type())
        xDirShape.put(0, 0, xDir[0, 0][0])
        xDirShape.put(1, 0, xDir[0, 1][0])

        val yDirShape = Mat(2, 1, yDir.type())
        yDirShape.put(0, 0, yDir[0, 0][0])
        yDirShape.put(1, 0, yDir[0, 1][0])

        gemm(pageCoordsSort, xDirShape, 1.0, Mat(), 0.0, pxCoords, 0)
        gemm(pageCoordsSort, yDirShape, 1.0, Mat(), 0.0, pyCoords, 0)

        val px0 = minMaxLoc(pxCoords).minVal
        val px1 = minMaxLoc(pxCoords).maxVal

        val py0 = minMaxLoc(pyCoords).minVal
        val py1 = minMaxLoc(pyCoords).maxVal

        val pX00Mul = Mat()
        val pX10Mul = Mat()
        val pY00Mul = Mat()
        val pY10Mul = Mat()

        multiply(xDir, Scalar(px0), pX00Mul)
        multiply(xDir, Scalar(px1), pX10Mul)
        multiply(yDir, Scalar(py0), pY00Mul)
        multiply(yDir, Scalar(py1), pY10Mul)

        val p00 = Mat()
        val p10 = Mat()
        val p11 = Mat()
        val p01 = Mat()

        add(pX00Mul, pY00Mul, p00)
        add(pX10Mul, pY00Mul, p10)
        add(pX10Mul, pY10Mul, p11)
        add(pX00Mul, pY10Mul, p01)

        val corners = Mat(4, 2, CV_64FC1)
        corners.put(0, 0, p00[0, 0][0])
        corners.put(0, 1, p00[0, 1][0])
        corners.put(1, 0, p10[0, 0][0])
        corners.put(1, 1, p10[0, 1][0])
        corners.put(2, 0, p11[0, 0][0])
        corners.put(2, 1, p11[0, 1][0])
        corners.put(3, 0, p01[0, 0][0])
        corners.put(3, 1, p01[0, 1][0])

        val xcoordsArray = mutableListOf<ArrayList<Double>>()
        val ycoordsArray = mutableListOf<Double>()

        spanPoints.forEach { point ->
            val pxCoordsMul = Mat()
            val pyCoordsMul = Mat()

            gemm(point, xDir.reshape(0, 2), 1.0, Mat(), 0.0, pxCoordsMul, 0)
            gemm(point, yDir.reshape(0, 2), 1.0, Mat(), 0.0, pyCoordsMul, 0)

            val xcoordsArrayList = ArrayList<Double>()
            for (i in 0 until pxCoordsMul.rows()) {
                xcoordsArrayList.add(pxCoordsMul[i, 0][0] - px0)
            }
            xcoordsArray.add(xcoordsArrayList)

            val pyCoordsMean = mean(pyCoordsMul)
            ycoordsArray.add(pyCoordsMean.`val`[0] - py0)
        }

        visualizeSpanPoints(srcImage, spanPoints, corners)

        return Triple(corners, ycoordsArray, xcoordsArray)
    }


    private fun getDefaultParams(corners: Mat, yCoords: MutableList<Double>, xCoords: MutableList<ArrayList<Double>>): Triple<Mat, MutableList<Int>, Mat> {
        val cornersPageWidth = Mat(1, 2, CV_64FC1)
        cornersPageWidth.put(0, 0, (corners[1, 0][0] - corners[0, 0][0]))
        cornersPageWidth.put(0, 1, (corners[1, 1][0] - corners[0, 1][0]))

        val cornersPageHeight = Mat(1, 2, CV_64FC1)
        cornersPageHeight.put(0, 0, (corners[corners.rows() - 1, 0][0] - corners[0, 0][0]))
        cornersPageHeight.put(0, 1, (corners[corners.rows() - 1, 1][0] - corners[0, 1][0]))

        val pageWidth = norm(cornersPageWidth)
        val pageHeight = norm(cornersPageHeight)

        val roughDims = Mat(1, 2, CV_64FC1)
        roughDims.put(0, 0, pageWidth)
        roughDims.put(0, 1, pageHeight)

        val cubicSlopes = Mat(1, 2, CV_64FC1)
        cubicSlopes.put(0, 0, 0.0)
        cubicSlopes.put(0, 1, 0.0)

        val cornersObject3dList: MutableList<Point3> = ArrayList()
        cornersObject3dList.add(Point3(0.0, 0.0, 0.0))
        cornersObject3dList.add(Point3(pageWidth, 0.0, 0.0))
        cornersObject3dList.add(Point3(pageWidth, pageHeight, 0.0))
        cornersObject3dList.add(Point3(0.0, pageHeight, 0.0))

        val cornersObject3d = MatOfPoint3f()
        cornersObject3d.fromList(cornersObject3dList)

        val cornersObject2dList: MutableList<Point> = ArrayList()
        for (i in 0 until corners.rows()) {
            cornersObject2dList.add(Point(corners[i, 0][0], corners[i, 1][0]))
        }

        val cornersObject2d = MatOfPoint2f()
        cornersObject2d.fromList(cornersObject2dList)

        val rvec = Mat()
        val tvec = Mat()
        Calib3d.solvePnP(cornersObject3d, cornersObject2d, k, MatOfDouble(0.0, 0.0, 0.0, 0.0, 0.0), rvec, tvec)

        val spanCounts = mutableListOf<Int>()
        for (i in 0 until xCoords.size) {
            spanCounts.add(xCoords[i].size)
        }

        val ycoordsMat = Mat(1, yCoords.size, CV_64FC1)
        for (i in 0 until yCoords.size) {
            ycoordsMat.put(0, i, yCoords[i])
        }

        val mutableListOfMat = mutableListOf<Mat>()
        for (i in 0 until xCoords.size) {
            val xycoordsI = Mat(1, xCoords[i].size, CV_64FC1)
            for (j in 0 until xCoords[i].size) {
                xycoordsI.put(0, j, xCoords[i][j])
            }
            mutableListOfMat.add(xycoordsI)
        }

        val xyCoords = Mat()
        hconcat(mutableListOfMat, xyCoords)

        val params = Mat()
        hconcat(mutableListOf(rvec.reshape(0, 1), tvec.reshape(0, 1), cubicSlopes.reshape(0, 1), ycoordsMat, xyCoords), params)

        return Triple(roughDims, spanCounts, params)
    }

    private suspend fun optimizeParams(srcImage: Mat, dstPoints: Mat, spanCounts: MutableList<Int>, params: Mat) {
        val keyPointIndex = makeKeyPointIndex(spanCounts)

        val rel = objective(params, keyPointIndex!!, dstPoints)
        Logger.log("debug_output", "initial objective is: " + rel.toString(), LoggerType.Debug)

        val project = projectKeyPoints(params, keyPointIndex)
        val display = drawCorrespondences(srcImage, dstPoints, project)
        debugShow("keypoints before", display)

        val paramsArray = mutableListOf<Double>()
        for (i in 0 until params.cols()) {
            paramsArray.add(params[0, i][0])
        }

        //optimize and minimize with powell
        var allMyCodeTake = 0.0
        val multivariateFunction = MultivariateFunction {
            val startFirstLoop = System.nanoTime()

            val toOptimizeParams = Mat(1, it.size, CV_64FC1)
            for (i in it.indices) {
                toOptimizeParams.put(0, i, it[i])
            }

            val ppts = projectKeyPoints(toOptimizeParams, keyPointIndex)

            val returnMat = Mat(dstPoints.rows(), dstPoints.cols(), dstPoints.type())
            for (i in 0 until ppts.rows()) {
                returnMat.put(i, 0, (dstPoints[i, 0][0] - ppts[i, 0][0]).pow(2))
                returnMat.put(i, 1, (dstPoints[i, 1][0] - ppts[i, 0][1]).pow(2))
            }

            val endSecondLoop = System.nanoTime()
            allMyCodeTake += endSecondLoop - startFirstLoop

            sumElems(returnMat).`val`[0]
        }

        GlobalScope.launch(Dispatchers.IO) {
            val startOptimizeTime = System.nanoTime()
            val powellOptimizer = PowellOptimizer(1E-14, 1E-40)
            val value = powellOptimizer.optimize(
                    MaxEval(Int.MAX_VALUE),
                    ObjectiveFunction(multivariateFunction),
                    GoalType.MINIMIZE,
                    InitialGuess(paramsArray.toDoubleArray()))
            val endOptimizeTime = System.nanoTime()

            Logger.log("debug_output", "my code take ${allMyCodeTake / 1_000_000_000} second", LoggerType.Debug)
            Logger.log("debug_output", "all code take ${(endOptimizeTime - startOptimizeTime) / 1_000_000_000} second", LoggerType.Debug)

            val optimizeParams = Mat(1, value.point.size, CV_64FC1)
            for (i in value.point.indices) {
                optimizeParams.put(0, i, value.point[i])
            }

            val projectAfter = projectKeyPoints(optimizeParams, keyPointIndex)
            val displayAfter = drawCorrespondences(srcImage, dstPoints, projectAfter)
            debugShow("keypoints after", displayAfter)
        }

        //return optimizeParams
    }

    private fun objective(params: Mat, keyPointIndex: Mat, dstPoints: Mat): Scalar? {
        val ppts = projectKeyPoints(params, keyPointIndex)

        val returnMat = Mat(dstPoints.rows(), dstPoints.cols(), dstPoints.type())
        for (i in 0 until ppts.rows()) {
            returnMat.put(i, 0, (dstPoints[i, 0][0] - ppts[i, 0][0]).pow(2))
            returnMat.put(i, 1, (dstPoints[i, 1][0] - ppts[i, 0][1]).pow(2))
        }
        return sumElems(returnMat)
    }

    private fun projectKeyPoints(pvec: Mat, keyPointIndex: Mat): MatOfPoint2f {
        val xyCoords = Mat(keyPointIndex.rows(), keyPointIndex.cols(), pvec.type())
        for (i in 0 until keyPointIndex.rows()) {
            val indexX = keyPointIndex[i, 0][0].toInt()
            val indexY = keyPointIndex[i, 1][0].toInt()
            xyCoords.put(i, 0, pvec[0, indexX][0])
            xyCoords.put(i, 1, pvec[0, indexY][0])
        }
        for (i in 0 until xyCoords.cols()) {
            xyCoords.put(0, i, 0.0)
        }

        return projectXy(xyCoords, pvec)
    }

    private fun projectXy(xyCoords: Mat, pvec: Mat): MatOfPoint2f {
        val alpha = pvec[0, CUBIC_IDX.first][0]
        val beta = pvec[0, CUBIC_IDX.second][0]

        val polyIntArray = doubleArrayOf(alpha + beta, -2 * alpha - beta, alpha, 0.0)

        val zCoords = Mat(xyCoords.rows(), 1, xyCoords.type())
        for (i in 0 until xyCoords.rows()) {
            zCoords.put(i, 0, horner(polyIntArray, xyCoords[i, 0][0]))
        }

        val objPointsList: MutableList<Point3> = ArrayList()
        for (i in 0 until xyCoords.rows()) {
            objPointsList.add(Point3(xyCoords[i, 0][0], xyCoords[i, 1][0], zCoords[i, 0][0]))
        }

        val objPoints = MatOfPoint3f()
        objPoints.fromList(objPointsList)

        val rvec = Mat(3, 1, pvec.type())
        rvec.put(0, 0, pvec[0, 0][0])
        rvec.put(1, 0, pvec[0, 1][0])
        rvec.put(2, 0, pvec[0, 2][0])


        val tvec = Mat(3, 1, pvec.type())
        tvec.put(0, 0, pvec[0, 3][0])
        tvec.put(1, 0, pvec[0, 4][0])
        tvec.put(2, 0, pvec[0, 5][0])


        val imagePoint = MatOfPoint2f()
        projectPoints(objPoints, rvec, tvec, k, MatOfDouble(0.0, 0.0, 0.0, 0.0, 0.0), imagePoint, Mat())

        return imagePoint
    }

    fun horner(poly: DoubleArray, x: Double): Double {
        var result = poly[0]
        for (i in 1 until poly.size) result = result * x + poly[i]
        return result
    }

    private fun makeKeyPointIndex(spanCounts: MutableList<Int>): Mat? {
        val nSpans = spanCounts.size
        val nPts = spanCounts.sum()

        val keyPointIndex = Mat.zeros(nPts + 1, 2, CV_64F)

        var start = 1
        spanCounts.forEachIndexed { index, count ->
            val end = start + count

            for (i in start until start + end) {
                keyPointIndex.put(i, 1, (8.0 + index))
            }
            start = end
        }

        keyPointIndex.put(0, 0, 0.0)
        for (i in 1 until keyPointIndex.rows()) {
            keyPointIndex.put(i, 0, ((i - 1) + 8 + nSpans).toDouble())
        }

        return keyPointIndex
    }

    private fun getPageDims(corners: Mat, roughDims: Mat, optimizeParams: Mat) {

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

        cInfoList.forEach { contourInfo ->
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

    private fun visualizeSpanPoints(srcImage: Mat, spanPoints: MutableList<Mat>, corners: Mat) {
        val display = Mat()
        srcImage.copyTo(display)

        spanPoints.forEachIndexed { index, points ->

            val pointsNorm = normToPix(srcImage, points, false)

            val mean = Mat()
            val smallEvec = Mat()
            PCACompute(pointsNorm, mean, smallEvec, 1)

            val dps = Mat()
            val dpm = Mat()
            gemm(pointsNorm, smallEvec.reshape(0, 2), 1.0, Mat(), 0.0, dps, 0)
            gemm(smallEvec, mean.reshape(0, 2), 1.0, Mat(), 0.0, dpm, 0)

            val point0 = Mat()
            val point1 = Mat()

            multiply(smallEvec, Scalar(minMaxLoc(dps).minVal - dpm[0, 0][0]), point0)
            multiply(smallEvec, Scalar(minMaxLoc(dps).maxVal - dpm[0, 0][0]), point1)

            add(mean, point0, point0)
            add(mean, point1, point1)

            for (i in 0 until pointsNorm.rows()) {
                val point = Mat(1, 2, pointsNorm.type())
                point.put(0, 0, pointsNorm[i, 0][0])
                point.put(0, 1, pointsNorm[i, 1][0])

                circle(display, fltp(point), 3, COLORS[index % COLORS.size], -1, LINE_AA)
            }

            line(display, fltp(point0), fltp(point1), Scalar(255.0, 255.0, 255.0), 1, LINE_AA)
        }

        val norm = normToPix(srcImage, corners, true)
        val normVal = Mat(norm.rows(), 1, CV_32SC2)
        for (i in 0 until norm.rows()) {
            normVal.put(i, 0, norm[i, 0][0], norm[i, 1][0])
        }

        polylines(display, mutableListOf(MatOfPoint(normVal)), true, Scalar(255.0, 255.0, 255.0))

        debugShow("span points", display)
    }

    private fun drawCorrespondences(srcImage: Mat, dstPoints: Mat, project: MatOfPoint2f): Mat {
        val display = Mat()
        srcImage.copyTo(display)

        val dstPointsShow = normToPix(srcImage, dstPoints, true)
        val projpts = normToPix(srcImage, project.reshape(1), true)

        for (i in 0 until dstPointsShow.rows()) {
            val pointDst = Mat(1, 2, dstPointsShow.type())
            pointDst.put(0, 0, dstPointsShow[i, 0][0])
            pointDst.put(0, 1, dstPointsShow[i, 1][0])

            circle(display, fltp(pointDst), 3, Scalar(255.0, 0.0, 0.0), -1, LINE_AA)

            val pointPro = Mat(1, 2, projpts.type())
            pointPro.put(0, 0, projpts[i, 0][0])
            pointPro.put(0, 1, projpts[i, 1][0])

            circle(display, fltp(pointPro), 3, Scalar(0.0, 0.0, 255.0), -1, LINE_AA)


            line(display, fltp(pointDst), fltp(pointPro), Scalar(255.0, 255.0, 255.0), 1, LINE_AA)

        }

        return display
    }

}