package ir.mehdivijeh.scanner.wrapper;
//author : mehdi vijeh

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.opencv.core.CvType.CV_32FC2;

public class PaperUnWrapper {

    private static final String TAG = "PaperUnWrapper";
    private static final int COL_COUNT = 300;
    private static final int ROW_COUNT = 200;
    private Mat srcImage;
    private Mat desImage;
    private List<double[]> pointPercent;
    private List<int[]> points;
    private int[] pointA; // top left
    private int[] pointB; // top center
    private int[] pointC; // top right
    private int[] pointD; // bottom right
    private int[] pointE; // bottom center
    private int[] pointF; // bottom left
    private int[] centerTop;
    private int[] centerBottom;
    private Line centerLine;

    public PaperUnWrapper(Mat srcImage, List<double[]> pointPercent) {
        this.srcImage = srcImage;
        this.pointPercent = pointPercent;
        desImage = srcImage.clone();

        points = loadPoints();
        mapPointsToMember(points);
        calculateCenters();
        centerLine = new Line(centerBottom, centerTop);
    }


    public Mat unwrap(boolean interpolate) {
        List<List<float[]>> sourceMap = calculateSourceMap();

        if (interpolate) {
            //TODO : NOT COMPLETE
            //unwrapPaperInterpolation();
        } else {
            unwrapPaperPerspective(sourceMap);
        }
        return this.desImage;
    }

    private List<int[]> loadPoints() {
        List<int[]> points = new ArrayList<>();
        for (double[] point : pointPercent) {
            int x = (int) (point[0] * srcImage.width());
            int y = (int) (point[1] * srcImage.height());
            int[] pointN = new int[]{x, y};
            Log.d(TAG, "loadPoints: " + x + " " + y);
            points.add(pointN);
        }
        return points;
    }

    private void mapPointsToMember(List<int[]> points) {
        pointA = points.get(0);
        pointB = points.get(1);
        pointC = points.get(2);
        pointD = points.get(3);
        pointE = points.get(4);
        pointF = points.get(5);
    }

    private void calculateCenters() {
        int centerTopX = (pointA[0] + pointC[0]) / 2;
        int centerTopY = (pointA[1] + pointC[1]) / 2;
        centerTop = new int[]{centerTopX, centerTopY};

        int centerBottomX = (pointD[0] + pointF[0]) / 2;
        int centerBottomY = (pointD[1] + pointF[1]) / 2;
        centerBottom = new int[]{centerBottomX, centerBottomY};

        //centerTop = pointB;
        // centerBottom = pointE;
    }

    private List<List<float[]>> calculateSourceMap() {
        List<float[]> topPoints = calculateEllipsePoint(pointA, pointB, pointC, COL_COUNT);
        List<float[]> bottomPoints = calculateEllipsePoint(pointF, pointE, pointD, COL_COUNT);

        List<List<float[]>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
            List<float[]> row = new ArrayList<>();
            for (int colIndex = 0; colIndex < COL_COUNT; colIndex++) {
                float[] top_point = topPoints.get(colIndex);
                float[] bottom_point = bottomPoints.get(colIndex);

                float deltaX = (top_point[0] - bottom_point[0]) / ((float) (ROW_COUNT - 1));
                float deltaY = (top_point[1] - bottom_point[1]) / ((float) (ROW_COUNT - 1));
                float[] delta = new float[]{deltaX, deltaY};

                float pointX = top_point[0] - delta[0] * rowIndex;
                float pointY = top_point[1] - delta[1] * rowIndex;
                float[] point = new float[]{pointX, pointY};

                row.add(point);
            }
            rows.add(row);
        }
        return rows;
    }


    private List<float[]> calculateEllipsePoint(int[] left, int[] top, int[] right, int pointsCount) {
        float centerX = (left[0] + right[0]) / 2f;
        float centerY = (left[1] + right[1]) / 2f;
        float[] center = new float[]{centerX, centerY};

        float[] aVector = new float[]{left[0] - right[0], left[1] - right[1]};
        float aNorm = norm(aVector) / 2;

        float[] bVector = new float[]{center[0] - top[0], center[1] - top[1]};
        float bNorm = norm(bVector);


        float delta;
        if (top[1] - center[1] > 0) {
            delta = (float) Math.PI / (pointsCount - 1);
        } else {
            delta = (float) -Math.PI / (pointsCount - 1);
        }

        float cosRotRight = (right[0] - center[0]) / aNorm;
        float sinRotRight = (right[1] - center[1]) / aNorm;

      /*  double cosRotLeft = (center[0] - left[0]) / aNorm;
        double sinRotLeft = (center[1] - left[1]) / aNorm;

        if (cosRotRight > 1) {
            cosRotRight = 1;
        }

        if (sinRotRight > 1) {
            sinRotRight = 1;
        }

        if (cosRotLeft > 1) {
            cosRotLeft = 1;
        }

        if (sinRotLeft > 1) {
            sinRotLeft = 1;
        }*/

        List<float[]> points = new ArrayList<>();
        for (int i = 0; i < pointsCount; i++) {
            float phi = delta * i;
            float[] dx_dy = getEllipsePoint(aNorm, bNorm, phi);
            float dx = dx_dy[0];
            float dy = dx_dy[1];

            float x = 0;
            float y = 0;
           // if (isTop) {
                x = Math.round(center[0] + dx * cosRotRight - dy * sinRotRight);
                y = Math.round(center[1] + dx * sinRotRight + dy * cosRotRight);
           /* } else {
                x = Math.round(center[0] + dx * cosRotLeft - dy * sinRotLeft);
                y = Math.round(center[1] + dx * sinRotLeft + dy * cosRotLeft);
            }*/


            points.add(new float[]{x, y});
        }

        Collections.reverse(points);
        return points;
    }

    private float norm(float[] vector) {
        return (float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1]);
    }

    /*
     * Get ellipse radius in polar coordinates
     * */
    private float[] getEllipsePoint(float a, float b, float phi) {
        return new float[]{a * (float) Math.cos(phi), (b * (float) Math.sin(phi))};
    }

    /*
     * Unwrap label using transform
     * */
    private void unwrapPaperPerspective(List<List<float[]>> sourceMap) {
        int width = srcImage.width();
        int height = srcImage.height();

        float dx = ((float) width) / (COL_COUNT - 1);
        float dy = ((float) height) / (ROW_COUNT - 1);

        int dx_int = (int) Math.ceil(dx);
        int dy_int = (int) Math.ceil(dy);

        for (int rowIndex = 0; rowIndex < ROW_COUNT - 1; rowIndex++) {
            for (int colIndex = 0; colIndex < COL_COUNT - 1; colIndex++) {

                Mat src_mat = new Mat(4, 1, CV_32FC2);
                Mat dst_mat = new Mat(4, 1, CV_32FC2);

                src_mat.put(0, 0
                        , sourceMap.get(rowIndex).get(colIndex)[0]
                        , sourceMap.get(rowIndex).get(colIndex)[1]
                        , sourceMap.get(rowIndex).get(colIndex + 1)[0]
                        , sourceMap.get(rowIndex).get(colIndex + 1)[1]
                        , sourceMap.get(rowIndex + 1).get(colIndex)[0]
                        , sourceMap.get(rowIndex + 1).get(colIndex)[1]
                        , sourceMap.get(rowIndex + 1).get(colIndex + 1)[0]
                        , sourceMap.get(rowIndex + 1).get(colIndex + 1)[1]);


                dst_mat.put(0, 0, 0, 0, dx, 0, 0, dy, dx, dy);


                Mat destination = new Mat();
                Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
                Imgproc.warpPerspective(srcImage, destination, m, new Size(dx_int, dy_int));
                int x_offset = (int) (dx * colIndex);
                int y_offset = (int) (dy * rowIndex);


                //Log.d(TAG, "unwrapPaperPerspective: " + desImage.cols() + " " + desImage.rows());
                Log.d(TAG, "unwrapPaperPerspective: " + this.desImage.colRange(x_offset, x_offset + dx_int).cols() + " " + this.desImage.rowRange(y_offset, y_offset + dy_int).rows());
                //Log.d(TAG, "unwrapPaperPerspective: " + destination.cols() + " " + destination.rows());
                //Log.d(TAG, "unwrapPaperPerspective: " + x_offset + " " + (x_offset + dx_int));
                // Log.d(TAG, "unwrapPaperPerspective: " + y_offset + " " + (y_offset + dy_int));
                destination.copyTo(this.desImage.rowRange(y_offset, y_offset + dy_int).colRange(x_offset, x_offset + dx_int));
            }
        }
    }

    /*
     * Unwrap label using interpolation - more accurate method in terms of quality
     * */
    private void unwrapPaperInterpolation(List<List<double[]>> sourceMap) {
        int width = srcImage.width();
        int height = srcImage.height();

        List<List<int[]>> destinationMap = calculateDestinationMap();
    }

    private List<List<int[]>> calculateDestinationMap() {
        int width = srcImage.width();
        int height = srcImage.height();

        float dx = ((float) width) / (COL_COUNT - 1);
        float dy = ((float) height) / (ROW_COUNT - 1);

        List<List<int[]>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
            List<int[]> row = new ArrayList<>();
            for (int colIndex = 0; colIndex < COL_COUNT; colIndex++) {
                row.add(new int[]{((int) dx * colIndex), ((int) dy * rowIndex)});
            }
            rows.add(row);
        }
        return rows;
    }

    public void draw_mesh() {
        List<List<float[]>> calculateSourceMap = calculateSourceMap();
        for (int i = 0; i < calculateSourceMap.size(); i++) {
            for (int j = 0; j < calculateSourceMap.get(i).size(); j++) {
                int pointX = (int) calculateSourceMap.get(i).get(j)[0];
                int pointY = (int) calculateSourceMap.get(i).get(j)[1];

                Point pt1 = new Point(pointX, pointY);
                Point pt2 = new Point(pointX, pointY);

                Imgproc.line(srcImage, pt1, pt2, new Scalar(255, 0, 0), 3);
            }
        }

    }


    public List<double[]> getPoints() {
        List<double[]> pointDouble = new ArrayList<>();
        for (int[] p : points) {
            double[] d = new double[p.length];
            for (int i = 0; i < p.length; i++) {
                d[i] = p[i];
            }
            pointDouble.add(d);
        }
        return pointDouble;
    }
}
