package ir.mehdivijeh.scanner.wrapper;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImageWrapper {

    private static final String TAG = "ImageWrapper";
    private String imagePath;
    private Context context;
    private List<double[]> pointsPercent;


    public ImageWrapper(Context context, String imagePath, List<double[]> pointsPercent) {
        this.context = context;
        this.imagePath = imagePath;
        this.pointsPercent = pointsPercent;
    }

    public void unwrap() {
        Mat srcImage = loadImage();
        PaperUnWrapper paperUnWrapper = new PaperUnWrapper(srcImage, pointsPercent);
        Mat destination = paperUnWrapper.unwrap(false);

        for (double[] point : paperUnWrapper.getPoints()) {
            /*double[] pointDouble = new double[point.length];
            for (int i = 0 ; i< point.length ; i++){
                pointDouble[i] = point[i];
            }*/

            Imgproc.line(srcImage, new Point(point), new Point(point), new Scalar(0, 255, 255), 10);
        }


        // sharpness image
        Imgproc.cvtColor(destination, destination, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
        //Photo.fastNlMeansDenoising(destination, destination,11,31,9);

        paperUnWrapper.draw_mesh();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_DCIM), "Scanner");

        if (!storageDir.exists()) {
            boolean isCreated = storageDir.mkdir();
        }

        try {
            File imageLine = File.createTempFile(
                    imageFileName + "line",
                    ".jpg",
                    storageDir
            );

            File image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );

            boolean boolS = Imgcodecs.imwrite(imageLine.getAbsolutePath(), srcImage);
            boolean boolD = Imgcodecs.imwrite(image.getAbsolutePath(), destination);


            Log.d(TAG, "unwrap: " + boolD + " " + image.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private Mat loadImage() {
        OpenCVLoader.initDebug();
        Log.d(TAG, "loadImage: " + imagePath);
        return Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_UNCHANGED);
    }


}
