package ir.mehdivijeh.scanner.re

import android.content.Context
import android.os.Environment
import android.util.Log
import ir.mehdivijeh.scanner.util.Logger
import ir.mehdivijeh.scanner.util.LoggerType
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.core.Core.*
import org.opencv.core.CvType.CV_32SC1
import org.opencv.core.CvType.CV_8U
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil


private const val TAG = "Dewarper"

class Dewarper(val imagePath: String, val context: Context) {

    fun dewarping() {
        var srcImage = loadImage()
        Log.d(TAG, "PythonConvertedDewarper: ori = ${srcImage.size()}")
        srcImage = resizeToScreen(srcImage)
        Log.d(TAG, "PythonConvertedDewarper: scaled = ${srcImage.size()}")
        val pair = getPageExtents(srcImage)
        val pageMask = pair.first
        val ouline = pair.second
        getContours(srcImage, pageMask, isMaskType = true)
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

        val outline = Mat(4, 2, CV_32SC1)
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

    private fun getContours(small: Mat, pageMask: Mat, isMaskType: Boolean) {
        val mask = getMask(small, pageMask, isMaskType)

        val contours: List<MatOfPoint> = ArrayList()
        findContours(mask, contours, Mat(), RETR_EXTERNAL, CHAIN_APPROX_NONE)

        val contoursOut = mutableListOf<ContourInfo>()
        for (i in contours) {
            val rect = boundingRect(i)
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

            val tightMask = makeTightMask(i, xMin, yMin, width, height)

            Logger.log(TAG, (sumElems(tightMask).`val`.maxOrNull()
                    ?: 0.0).toString(), LoggerType.Debug)
            if (sumElems(tightMask).`val`.maxOrNull() ?: 0.0 > TEXT_MAX_THICKNESS) {
                continue
            }

            contoursOut.add(ContourInfo(i, rect, tightMask))
        }


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

    private fun visualizeContours(small: Mat, cInfoList: List<ContourInfo>) {
        val regions = Mat(small.height(), small.width(), CV_8U, Scalar.all(0.0))

        cInfoList.forEachIndexed { index, contourInfo ->
            drawContours(regions, listOf(contourInfo.contour), 0, COLORS[index % COLORS.size], -1)
        }

        val mask = mutableListOf<Boolean>()
        for (i in 0 until regions.rows()) {
            mask.add(minMaxLoc(regions.row(i)).maxVal != 0.0)
        }

        Logger.log(TAG, "is here ****", LoggerType.Debug)
        Logger.log(TAG, mask.size.toString(), LoggerType.Debug)

        val display = Mat()
        small.copyTo(display)
        //display[mask]
    }
}