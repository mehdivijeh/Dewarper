package ir.mehdivijeh.scanner.re

import org.opencv.core.Scalar

const val PAGE_MARGIN_X = 0.0  // reduced px to ignore near L/R edge
const val PAGE_MARGIN_Y = 0.0  // reduced px to ignore near T/B edge
const val ADAPTIVE_WIN_SZ = 55  // window size for adaptive threshold in reduced px

const val TEXT_MIN_WIDTH = 15      // min reduced px width of detected text contour
const val TEXT_MIN_HEIGHT = 2      // min reduced px height of detected text contour
const val TEXT_MIN_ASPECT = 1.5    // filter out text contours below this w/h ratio
const val TEXT_MAX_THICKNESS = 15  // max reduced px thickness of detected text contour

const val EDGE_MAX_OVERLAP = 1.0  // max reduced px horizontal. overlap of contours in span
const val EDGE_MAX_LENGTH = 100.0 //  max reduced px length of edge connecting contours
const val EDGE_ANGLE_COST = 10.0  // cost of angles in edges (trade off vs. length)
const val EDGE_MAX_ANGLE = 7.5    // maximum change in angle allowed between contours

const val SPAN_MIN_WIDTH = 30  // minimum reduced px width for span
const val SPAN_PX_PER_STEP = 20  // reduced px spacing for sampling along spans
const val FOCAL_LENGTH = 4.8  // normalized focal length of camera

val RVEC_IDX = Pair(0, 3)  // index of rvec in params vector
val TVEC_IDX = Pair(3, 6)  // index of tvec in params vector
val CUBIC_IDX = Pair(6, 7)  // index of cubic slopes in params vector

//nice color palette for visualizing contours, etc.
val COLORS = arrayListOf(
        Scalar(255.0, 0.0, 0.0),
        Scalar(255.0, 63.0, 0.0),
        Scalar(255.0, 127.0, 0.0),
        Scalar(255.0, 191.0, 0.0),
        Scalar(255.0, 255.0, 0.0),
        Scalar(191.0, 255.0, 0.0),
        Scalar(127.0, 255.0, 0.0),
        Scalar(63.0, 255.0, 0.0),
        Scalar(0.0, 255.0, 0.0),
        Scalar(0.0, 255.0, 63.0),
        Scalar(0.0, 255.0, 127.0),
        Scalar(0.0, 255.0, 191.0),
        Scalar(0.0, 255.0, 255.0),
        Scalar(0.0, 191.0, 255.0),
        Scalar(0.0, 127.0, 255.0),
        Scalar(0.0, 63.0, 255.0),
        Scalar(0.0, 0.0, 255.0),
        Scalar(63.0, 0.0, 255.0),
        Scalar(127.0, 0.0, 255.0),
        Scalar(191.0, 0.0, 255.0),
        Scalar(255.0, 0.0, 255.0),
        Scalar(255.0, 0.0, 191.0),
        Scalar(255.0, 0.0, 127.0),
        Scalar(255.0, 0.0, 63.0),
)