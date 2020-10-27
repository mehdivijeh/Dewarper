package ir.mehdivijeh.scanner.nwrapper.binarize

import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import ir.mehdivijeh.scanner.util.Logger
import ir.mehdivijeh.scanner.util.LoggerType

private const val TAG = "binarize"

object Binarize {

    @JvmStatic
    fun grayscale(image: Mat): Mat {
        Logger.log(TAG, "channel of image = ${image.channels()}", LoggerType.Debug)
        // colorful image
        if (image.channels() > 2) {
            // color channel have 4 channel itself
            if (image.row(2).channels() == 4) {
                //TODO : complete it
            } else {
                return convertToBGRGray(image)
            }
        }
        return image
    }

    private fun convertToBGRGray(image: Mat): Mat {
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2Lab)
        return image.row(0)// return first channel
    }

    private fun convertToGrayScale(image: Mat): Mat {
        return image.row(0)// return first channel
    }

}

