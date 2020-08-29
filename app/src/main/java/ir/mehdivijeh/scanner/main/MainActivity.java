package ir.mehdivijeh.scanner.main;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ir.mehdivijeh.scanner.R;
import ir.mehdivijeh.scanner.wrapper.ImageWrapper;
import ir.mehdivijeh.scanner.wrapper.ImageWrapperBuilder;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class MainActivity extends ChooseAvatarAbstract implements MainContract.MainView {


    public static final int CAPTURE_IMAGE = 100;

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 20;
    private String imagePath = null;
    private MainContract.MainPresenter presenter;
    private ImageView imageView;
    private Button btnSelectImage;
    private ProgressDialog progressDialogLoad;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        captureNewImage();


        btnSelectImage = findViewById(R.id.btn_open_image);
        imageView = findViewById(R.id.img);
        presenter = new MainPresenter(this);

        btnSelectImage.setOnClickListener(v -> showTakeImagePopup(false));


    }

    private void captureNewImage() {
        // Create Camera Intent
        Intent captureNewImage = new Intent(this, CameraActivity.class);
        startActivityForResult(captureNewImage, CAPTURE_IMAGE);
    }


    @Override
    public void onImageProvided(Drawable drawable, String path) {
        imagePath = path;

        File file = new File(path);

        RequestBody requestFile =
                RequestBody.create(MediaType.parse("multipart/form-data"), file);


        MultipartBody.Part body =
                MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        RequestBody fullName =
                RequestBody.create(MediaType.parse("multipart/form-data"), "image");


        presenter.uploadImage(fullName, body);
        showDialogLoading();
        //startActivityForResult(SelectPointActivity.SelectPointActivityIntent(this, path), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

            ArrayList<CircleArea> circles = data.getParcelableArrayListExtra(SelectPointActivity.POINT_ARRAY_LIST);
            double width = data.getIntExtra(SelectPointActivity.IMAGE_WIDTH, 0);
            double height = data.getIntExtra(SelectPointActivity.IMAGE_HEIGHT, 0);


              /*
        |        |                  |        |
        |    B   |                  A        C
        | /    \ |                  | \    / |
        A        C                  |   B    |
        |        |                  |        |
        |        |       OR         |        |
        |        |                  |        |
        F        D                  F        D
        | \    / |                  | \    / |
        |   E    |                  |   E    |
        |        |                  |        |
        * */

            Bitmap image = BitmapFactory.decodeFile(imagePath);


            int radius = circles.get(0).getRadius();
            List<double[]> pointsPercent = new ArrayList<>();
            pointsPercent.add(new double[]{(circles.get(0).getCenterX()) / width, (circles.get(0).getCenterY()) / height});  //A
            pointsPercent.add(new double[]{(circles.get(1).getCenterX()) / width, (circles.get(1).getCenterY()) / height});  //B
            pointsPercent.add(new double[]{(circles.get(2).getCenterX()) / width, (circles.get(2).getCenterY()) / height});  //C
            pointsPercent.add(new double[]{(circles.get(3).getCenterX()) / width, (circles.get(3).getCenterY()) / height});  //D
            pointsPercent.add(new double[]{(circles.get(4).getCenterX()) / width, (circles.get(4).getCenterY()) / height});  //E
            pointsPercent.add(new double[]{(circles.get(5).getCenterX()) / width, (circles.get(5).getCenterY()) / height});  //F


            Log.d(TAG, "onActivityResult: " + circles.get(0).getCenterX() / width + "   " + (circles.get(0).getCenterY()) / height);
            Log.d(TAG, "onActivityResult: " + circles.get(1).getCenterX() / width + "   " + (circles.get(1).getCenterY()) / height);
            Log.d(TAG, "onActivityResult: " + circles.get(2).getCenterX() / width + "   " + (circles.get(2).getCenterY()) / height);
            Log.d(TAG, "onActivityResult: " + circles.get(3).getCenterX() / width + "   " + (circles.get(3).getCenterY()) / height);
            Log.d(TAG, "onActivityResult: " + circles.get(4).getCenterX() / width + "   " + (circles.get(4).getCenterY()) / height);
            Log.d(TAG, "onActivityResult: " + circles.get(5).getCenterX() / width + "   " + (circles.get(5).getCenterY()) / height);


            createImageWrapper(pointsPercent);
        }
    }

    private void showDialogLoading() {
        progressDialogLoad = ProgressDialog.show(this, null, null, true);
        progressDialogLoad.setContentView(R.layout.dialog_loading);
        progressDialogLoad.setCancelable(false);
        Objects.requireNonNull(progressDialogLoad.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        progressDialogLoad.show();
    }

    private void createImageWrapper(List<double[]> pointsPercent) {

        ImageWrapper imageWrapper = new ImageWrapperBuilder(this)
                .setImagePath(imagePath)
                .setPointsPercent(pointsPercent)
                .build();

        imageWrapper.unwrap();
    }

    @Override
    public void onDeleteImageClicked() {

    }

    @Override
    public void onResponse(Bitmap bitmap) {
        if (progressDialogLoad != null && progressDialogLoad.isShowing())
            progressDialogLoad.dismiss();

        imageView.setImageBitmap(bitmap);
        imageView.setVisibility(View.VISIBLE);
        btnSelectImage.setVisibility(View.GONE);
    }
}
