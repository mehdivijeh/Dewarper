package ir.mehdivijeh.scanner.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PointSelector extends View {

    private static final String TAG = "CirclesDrawingView";

    /**
     * Stores data about single circle
     */
    private Rect mMeasuredRect;

    /**
     * Paint to draw circles
     */
    private Paint mCirclePaint;

    /**
     * Paint to draw rect
     */
    private Paint mRectPaint;

    private CircleArea mCircleLeftTop;
    private CircleArea mCircleCenterTop;
    private CircleArea mCircleRightTop;
    private CircleArea mCircleRightBottom;
    private CircleArea mCircleCenterBottom;
    private CircleArea mCircleCenterLeft;

    // Radius in pixels
    private final static int RADIUS_SIZE = 50;

    private static final int CIRCLES_LIMIT = 6;

    /**
     * All available circles
     */
    private ArrayList<CircleArea> mCircles = new ArrayList<>(CIRCLES_LIMIT);
    private SparseArray<CircleArea> mCirclePointer = new SparseArray<>(CIRCLES_LIMIT);

    /**
     * Default constructor
     *
     * @param ct {@link android.content.Context}
     */
    public PointSelector(final Context ct) {
        super(ct);

        init();
    }

    public PointSelector(final Context ct, final AttributeSet attrs) {
        super(ct, attrs);

        init();
    }

    public PointSelector(final Context ct, final AttributeSet attrs, final int defStyle) {
        super(ct, attrs, defStyle);

        init();
    }

    private void init() {

        mCirclePaint = new Paint();

        mCirclePaint.setColor(Color.BLUE);
        mCirclePaint.setStrokeWidth(40);
        mCirclePaint.setStyle(Paint.Style.FILL);

        mRectPaint = new Paint();
        mRectPaint.setColor(getContext().getResources().getColor(android.R.color.holo_blue_dark));
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(10);

        mCircleLeftTop = createCircle(200, 200);
        mCircleLeftTop.setCenterX(200);
        mCircleLeftTop.setCenterY(200);
        mCirclePointer.put(0, mCircleLeftTop);

        mCircleCenterTop = createCircle(400, 100);
        mCircleCenterTop.setCenterX(400);
        mCircleCenterTop.setCenterY(100);
        mCirclePointer.put(1, mCircleCenterTop);

        mCircleRightTop = createCircle(600, 200);
        mCircleRightTop.setCenterX(600);
        mCircleRightTop.setCenterY(200);
        mCirclePointer.put(2, mCircleRightTop);

        mCircleRightBottom = createCircle(600, 800);
        mCircleRightBottom.setCenterX(600);
        mCircleRightBottom.setCenterY(800);
        mCirclePointer.put(3, mCircleRightBottom);

        mCircleCenterBottom = createCircle(400, 900);
        mCircleCenterBottom.setCenterX(400);
        mCircleCenterBottom.setCenterY(900);
        mCirclePointer.put(4, mCircleCenterBottom);

        mCircleCenterLeft = createCircle(200, 800);
        mCircleCenterLeft.setCenterX(200);
        mCircleCenterLeft.setCenterY(800);
        mCirclePointer.put(5, mCircleCenterLeft);
    }

    @Override
    public void onDraw(final Canvas canvas) {

        for (CircleArea circle : mCircles) {
            canvas.drawCircle(circle.getCenterX(), circle.getCenterY(), circle.getRadius(), mCirclePaint);
        }

        for (int i = 0; i < mCircles.size(); i++) {
            if (i < mCircles.size() - 1) {
                canvas.drawLine(mCircles.get(i).getCenterX(), mCircles.get(i).getCenterY(),
                        mCircles.get(i + 1).getCenterX(), mCircles.get(i + 1).getCenterY(), mRectPaint);
            } else {
                canvas.drawLine(mCircles.get(i).getCenterX(), mCircles.get(i).getCenterY(),
                        mCircles.get(0).getCenterX(), mCircles.get(0).getCenterY(), mRectPaint);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        boolean handled = false;

        CircleArea touchedCircle;
        int xTouch;
        int yTouch;
        int pointerId;
        int actionIndex = event.getActionIndex();

        // get touch event coordinates and make transparent circle from it
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.w(TAG, "Pointer down");
                // obtain pointer ids and check circles
                pointerId = event.getPointerId(actionIndex);

                xTouch = (int) event.getX(actionIndex);
                yTouch = (int) event.getY(actionIndex);

                // check if we've touched inside some circle
                touchedCircle = obtainTouchedCircle(xTouch, yTouch);

                if (touchedCircle != null) {
                    mCirclePointer.put(pointerId, touchedCircle);
                    touchedCircle.setCenterX(xTouch);
                    touchedCircle.setCenterY(yTouch);
                    invalidate();
                    handled = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                final int pointerCount = event.getPointerCount();

                Log.w(TAG, "Move");

                for (actionIndex = 0; actionIndex < pointerCount; actionIndex++) {
                    // Some pointer has moved, search it by pointer id
                    pointerId = event.getPointerId(actionIndex);

                    xTouch = (int) event.getX(actionIndex);
                    yTouch = (int) event.getY(actionIndex);

                    touchedCircle = mCirclePointer.get(pointerId);

                    if (null != touchedCircle) {
                        touchedCircle.setCenterX(xTouch);
                        touchedCircle.setCenterY(yTouch);
                    }
                }
                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_UP:
                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // not general pointer was up
                pointerId = event.getPointerId(actionIndex);

                mCirclePointer.remove(pointerId);
                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_CANCEL:
                handled = true;
                break;

            default:
                // do nothing
                break;
        }

        return super.onTouchEvent(event) || handled;
    }

    /**
     * create circle with x and y
     *
     * @param circleX int x of circle
     * @param circleY int y of circle
     * @return new {@link CircleArea}
     */
    private CircleArea createCircle(final int circleX, final int circleY) {
        CircleArea newCircle = new CircleArea(circleX, circleY, RADIUS_SIZE);
        mCircles.add(newCircle);
        return newCircle;
    }


    /**
     * Search circle based on touch area
     *
     * @param xTouch int x of touch
     * @param yTouch int y of touch
     * @return obtained {@link CircleArea}
     */
    private CircleArea obtainTouchedCircle(final int xTouch, final int yTouch) {

        return getTouchedCircle(xTouch, yTouch);
    }

    /**
     * Determines touched circle
     *
     * @param xTouch int x touch coordinate
     * @param yTouch int y touch coordinate
     * @return {@link CircleArea} touched circle or null if no circle has been touched
     */
    private CircleArea getTouchedCircle(final int xTouch, final int yTouch) {
        CircleArea touched = null;

        for (CircleArea circle : mCircles) {
            if ((circle.getCenterX() - xTouch) * (circle.getCenterX() - xTouch) + (circle.getCenterY() - yTouch) * (circle.getCenterY() - yTouch) <= circle.getRadius() * circle.getRadius()) {
                touched = circle;
                break;
            }
        }

        return touched;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mMeasuredRect = new Rect(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }


    public ArrayList<CircleArea> getCircles() {
        return mCircles;
    }
}