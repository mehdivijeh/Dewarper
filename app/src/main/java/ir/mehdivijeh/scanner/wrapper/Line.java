package ir.mehdivijeh.scanner.wrapper;
//author : mehdi vijeh

/*
 *  For line formula y(x) = k * x + b, calc k and b params
 *  If the line is vertical, set "vertical" attr to True and save "x" position of the line
 */

public class Line {

    private int[] point1;
    private int[] point2;
    private double k;
    private double b;
    private double fixed_x;
    private double angel;
    private double angel_cos;
    private double angel_sis;
    private boolean IsVertical = false;

    public Line(int[] point1, int[] point2) {
        this.point1 = point1;
        this.point2 = point2;

        setLineProps();
    }

    private void setLineProps() {
        double k_normal;
        if (point1[0] - point2[0] != 0) {
            k = (point2[1] - point1[1]) / (point2[0] - point1[0]);
            b = point2[1] - k * point2[0];
            k_normal = -1 / k;
        }else {
            IsVertical = true;
            fixed_x = point2[0];
            k_normal = 0;
        }
        angel = Math.atan(k_normal);
        angel_cos = Math.cos(angel);
        angel_cos = Math.sin(angel);
    }

}
