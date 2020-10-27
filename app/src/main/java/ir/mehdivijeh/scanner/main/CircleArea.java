package ir.mehdivijeh.scanner.main;


import android.os.Parcel;
import android.os.Parcelable;

public class CircleArea implements Parcelable {

    private int radius;
    private int centerX;
    private int centerY;

    CircleArea(int centerX, int centerY, int radius) {
        this.radius = radius;
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setCenterX(int centerX) {
        this.centerX = centerX;
    }

    public void setCenterY(int centerY) {
        this.centerY = centerY;
    }

    public int getRadius() {
        return radius;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    @Override
    public String toString() {
        return "Circle[" + centerX + ", " + centerY + ", " + radius + "]";
    }


    protected CircleArea(Parcel in) {
        radius = in.readInt();
        centerX = in.readInt();
        centerY = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(radius);
        dest.writeInt(centerX);
        dest.writeInt(centerY);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CircleArea> CREATOR = new Parcelable.Creator<CircleArea>() {
        @Override
        public CircleArea createFromParcel(Parcel in) {
            return new CircleArea(in);
        }

        @Override
        public CircleArea[] newArray(int size) {
            return new CircleArea[size];
        }
    };
}