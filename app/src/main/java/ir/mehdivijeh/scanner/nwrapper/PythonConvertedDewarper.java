package ir.mehdivijeh.scanner.nwrapper;

import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import ir.mehdivijeh.scanner.nwrapper.binarize.Binarize;


public class PythonConvertedDewarper {

    private static final String TAG="PythonConvertedDewarper";
    private String imagePath;

    public PythonConvertedDewarper(String imagePath) {
        this.imagePath = imagePath;

        Mat srcImage = loadImage();
        Binarize.grayscale(srcImage);
    }

    private Mat loadImage() {
        OpenCVLoader.initDebug();
        Log.d(TAG, "loadImage: " + imagePath);
        return Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_UNCHANGED);
    }


}
