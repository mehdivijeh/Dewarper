package ir.mehdivijeh.scanner.main;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.apache.commons.math3.analysis.MultivariateFunction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ir.mehdivijeh.scanner.R;
import ir.mehdivijeh.scanner.nwrapper.PythonConvertedDewarper;
import ir.mehdivijeh.scanner.re.Dewarper;
import ir.mehdivijeh.scanner.wrapper.ImageWrapper;
import ir.mehdivijeh.scanner.wrapper.ImageWrapperBuilder;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import static ir.mehdivijeh.scanner.general.GeneralConstants.BASE_DIRECTORY_FILE_PATH;
import static ir.mehdivijeh.scanner.general.GeneralConstants.BASE_DIRECTORY_NAME;
import static ir.mehdivijeh.scanner.general.GeneralConstants.PERM_IMAGE_DIRECTORY_FILE_PATH;
import static ir.mehdivijeh.scanner.general.GeneralConstants.PERM_IMAGE_DIRECTORY_NAME;
import static ir.mehdivijeh.scanner.general.GeneralConstants.TEMP_IMAGE_DIRECTORY_FILE_PATH;
import static ir.mehdivijeh.scanner.general.GeneralConstants.TEMP_IMAGE_DIRECTORY_NAME;


public class MainActivity extends ChooseAvatarAbstract implements MainContract.MainView {


    // Variables
    private String mTempImagePath = null;
    private FrameLayout mBaseLayout = null;

    public static final int CAPTURE_IMAGE = 120;

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 20;
    private String imagePath = null;
    private MainContract.MainPresenter presenter;
    private ImageView imageView;
    private Button btnSelectImage;
    private ProgressDialog progressDialogLoad;


    // Constants
    private static final int PERMISSION_ALL = 100;
    private static final String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/test1.jpg");
        if (file.exists()) {
            Dewarper dewarper = new Dewarper(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/test1.jpg", this);
            dewarper.dewarping();
        }

        getPermission();

        btnSelectImage = findViewById(R.id.btn_open_image);
        imageView = findViewById(R.id.img);
        presenter = new MainPresenter(this);

        btnSelectImage.setOnClickListener(v -> showTakeImagePopup(false));


    }

    private void getPermission() {
        if (!hasPermissions(this, PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS, PERMISSION_ALL);
            }
        } else {
            //initDirectories();
            //captureNewImage();
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void initDirectories() {
        String destPath = getExternalFilesDir(null).getAbsolutePath();

        File baseDirectory = new File(destPath + File.separator + BASE_DIRECTORY_NAME);
        if (!baseDirectory.exists()) {
            Log.i(TAG, "Base Directory Created: " + baseDirectory.mkdirs());
        }

        File tempImageDirectory = new File(destPath + File.separator + TEMP_IMAGE_DIRECTORY_NAME);
        if (!tempImageDirectory.exists()) {
            Log.i(TAG, "Temp Image Directory Created: " + tempImageDirectory.mkdirs());
        }

        File permImageDirectory = new File(destPath + File.separator + PERM_IMAGE_DIRECTORY_NAME);
        if (!permImageDirectory.exists()) {
            Log.i(TAG, "Perm Image Directory Created: " + permImageDirectory.mkdirs());
        }
    }

    private void captureNewImage() {
        // Create Camera Intent
        Intent captureNewImage = new Intent(this, CameraActivity.class);
        startActivityForResult(captureNewImage, CAPTURE_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    getPermission();
                } else {
                    initDirectories();
                    captureNewImage();
                }
            }
        }
    }

    @Override
    public void onImageProvided(Drawable drawable, String path) {
        imagePath = path;

        File file = new File(path);

        //new PythonConvertedDewarper(path);
        //Dewarper dewarper = new Dewarper(path, this);
        //dewarper.dewarping();

        /*RequestBody requestFile =
                RequestBody.create(MediaType.parse("multipart/form-data"), file);


        MultipartBody.Part body =
                MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        RequestBody fullName =
                RequestBody.create(MediaType.parse("multipart/form-data"), "image");


        presenter.uploadImage(fullName, body);
        showDialogLoading();*/
        //startActivityForResult(SelectPointActivity.SelectPointActivityIntent(this, path), REQUEST_CODE);
    }

  /*  @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

            ArrayList<CircleArea> circles = data.getParcelableArrayListExtra(SelectPointActivity.POINT_ARRAY_LIST);
            double width = data.getIntExtra(SelectPointActivity.IMAGE_WIDTH, 0);
            double height = data.getIntExtra(SelectPointActivity.IMAGE_HEIGHT, 0);


              *//*
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
        * *//*

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
    }*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAPTURE_IMAGE:
                if (resultCode == RESULT_OK) {
                    if (data.getExtras() != null && data.getStringExtra("image-path") != null) {
                        // Get Temp Image Path
                        mTempImagePath = data.getStringExtra("image-path");
                        Bitmap bitmap = BitmapFactory.decodeFile(mTempImagePath);
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);


                        File file = new File(mTempImagePath);

                        RequestBody requestFile =
                                RequestBody.create(MediaType.parse("multipart/form-data"), file);


                        MultipartBody.Part body =
                                MultipartBody.Part.createFormData("image", file.getName(), requestFile);

                        RequestBody fullName =
                                RequestBody.create(MediaType.parse("multipart/form-data"), "image");


                        presenter.uploadImage(fullName, body);
                        showDialogLoading();

                    } else {

                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Image Capture Cancelled", Toast.LENGTH_LONG)
                            .show();
                }
                break;
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
