package ir.mehdivijeh.scanner.re

import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.core.CvType.CV_32F
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc.*
import kotlin.math.ceil


private const val TAG = "Dewarper"

class Dewarper(val imagePath: String) {

    fun dewarping() {
        var srcImage = loadImage()
        Log.d(TAG, "PythonConvertedDewarper: ori = ${srcImage.size()}")
        srcImage = resizeToScreen(srcImage)
        Log.d(TAG, "PythonConvertedDewarper: scaled = ${srcImage.size()}")
        val pair = getPageExtents(srcImage)
        val pageMask = pair.first
        val ouline = pair.second
        getContours(pageMask, ouline, isMaskType = true)
    }

    private fun loadImage(): Mat {
        OpenCVLoader.initDebug()
        return Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_UNCHANGED)
    }

    private fun resizeToScreen(img: Mat, maxW: Int = 1280, maxH: Int = 700): Mat {
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

        rectangle(img, Point(xMin, yMin), Point(xMax, yMax), Scalar(255.0, 255.0, 255.0), -1)

        val outline = Mat(4, 2, CV_32F)
        outline.put(0, 0, xMin)
        outline.put(0, 1, yMin)

        outline.put(1, 0, xMin)
        outline.put(1, 1, yMax)

        outline.put(2, 0, xMax)
        outline.put(2, 1, yMax)

        outline.put(3, 0, xMax)
        outline.put(3, 1, yMin)

        return Pair(img, outline)
    }

    private fun getContours(img: Mat, pageMask: Mat, isMaskType: Boolean) {
        val img = getMask(img, pageMask, isMaskType)

        val contours: List<MatOfPoint> = ArrayList()
        findContours(img, contours, null, RETR_EXTERNAL, CHAIN_APPROX_NONE)

        for (i in contours) {
            val rect = boundingRect(i)
            val xMin = rect.x
            val yMin = rect.y
            val width = rect.width
            val height = rect.height

            if (width < TEXT_MIN_WIDTH ||
                    height < TEXT_MIN_HEIGHT ||
                    width < (TEXT_MIN_ASPECT * height)) {
                continue
            }


        }
    }

    private fun makeTightMask(contour: MatOfPoint, xMin: Int, yMin: Int, width: Int, height: Int) {
        val tightMask = Mat(height, width, CvType.CV_8U, Scalar.all(0.0))
        val tightContour = contour.toList() - Mat(xMin, yMin, CvType.CV_8U).reshape(0 , 2)
    }

    private fun getMask(img: Mat, pageMask: Mat, isMaskType: Boolean): Mat {
        cvtColor(img, img, COLOR_RGB2GRAY)

        if (isMaskType) {
            adaptiveThreshold(img, img, 255.0, ADAPTIVE_THRESH_MEAN_C,
                    THRESH_BINARY_INV, ADAPTIVE_WIN_SZ, 25.0)

            dilate(img, img,
                    getStructuringElement(THRESH_BINARY_INV, Size(9.0, 1.0)))

            erode(img, img,
                    getStructuringElement(THRESH_BINARY_INV, Size(1.0, 3.0)))
        } else {
            adaptiveThreshold(img, img, 255.0, ADAPTIVE_THRESH_MEAN_C,
                    THRESH_BINARY_INV, ADAPTIVE_WIN_SZ, 7.0)

            erode(img, img,
                    getStructuringElement(THRESH_BINARY_INV, Size(3.0, 1.0))
                    , null, 3)

            dilate(img, img,
                    getStructuringElement(THRESH_BINARY_INV, Size(8.0, 2.0)))

        }
        Core.min(img, pageMask, img)
        return img
    }

}