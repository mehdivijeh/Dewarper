package ir.mehdivijeh.scanner.re

const val PAGE_MARGIN_X = 50.0  // reduced px to ignore near L/R edge
const val PAGE_MARGIN_Y = 20.0  // reduced px to ignore near T/B edge
const val ADAPTIVE_WIN_SZ = 55  // window size for adaptive threshold in reduced px

const val TEXT_MIN_WIDTH = 15      // min reduced px width of detected text contour
const val TEXT_MIN_HEIGHT = 2      // min reduced px height of detected text contour
const val TEXT_MIN_ASPECT = 1.5    // filter out text contours below this w/h ratio
const val TEXT_MAX_THICKNESS = 10  // max reduced px thickness of detected text contour