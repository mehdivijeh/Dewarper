package ir.mehdivijeh.scanner.wrapper;

import android.content.Context;

import java.util.List;

public class ImageWrapperBuilder {
    private Context context;
    private String imagePath;
    private List<double[]> pointsPercent;

    public ImageWrapperBuilder(Context context) {
        this.context = context;
    }

    public ImageWrapperBuilder setImagePath(String imagePath) {
        this.imagePath = imagePath;
        return this;
    }

    public ImageWrapperBuilder setPointsPercent(List<double[]> pointsPercent) {
        this.pointsPercent = pointsPercent;
        return this;
    }

    public ImageWrapper build() {
        return new ImageWrapper(context , imagePath, pointsPercent);
    }
}