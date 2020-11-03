package ir.mehdivijeh.scanner.main;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import id.zelory.compressor.Compressor;
import ir.mehdivijeh.scanner.R;

import static com.theartofdev.edmodo.cropper.CropImageView.ScaleType.CENTER_CROP;
import static com.theartofdev.edmodo.cropper.CropImageView.ScaleType.CENTER_INSIDE;

public abstract class ChooseAvatarAbstract extends AppCompatActivity {

    public abstract void onImageProvided(Drawable drawable, String path);

    public abstract void onDeleteImageClicked();

    private String cameraFilePath;
    private static final int REQUEST_CODE_CAMERA_CAPTURE = 102;
    private static final int REQUEST_PERMISSION_STORAGE = 1;
    private static final int REQUEST_PERMISSION_CAMERA = 2;
    private static final int SELECT_GALLERY_IMAGE_CODE = 7;

    public void showTakeImagePopup(boolean isShowDeleteImage) {
        final Dialog mDialog = new Dialog(this);
        mDialog.setCancelable(true);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.setContentView(R.layout.dialog_image_picker);
        //TextUtil.setFonts(mDialog.getWindow().getDecorView());

        ImageView imgClose = mDialog.findViewById(R.id.img_close);
        TextView tvGallery = mDialog.findViewById(R.id.text_button_first);
        TextView tvCamera = mDialog.findViewById(R.id.text_button_middle);
        TextView tvDeletePicture = mDialog.findViewById(R.id.text_button_end);

        if (!isShowDeleteImage) {
            tvDeletePicture.setVisibility(View.GONE);
        }

        imgClose.setOnClickListener(v -> {
            mDialog.dismiss();
        });
        tvGallery.setOnClickListener(v -> {
            mDialog.dismiss();
            selectFromAlbum();
        });
        tvCamera.setOnClickListener(v -> {
            mDialog.dismiss();
            takePhotoClick();
        });
        tvDeletePicture.setOnClickListener(v -> {
            mDialog.dismiss();
            onDeleteImageClicked();
        });
        mDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT
                , WindowManager.LayoutParams.WRAP_CONTENT);

        mDialog.show();
    }

    protected void takePhotoClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestTakePhotoPermissions();
        } else {
            launchCamera();
        }
    }

    private void requestTakePhotoPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CAMERA);
            return;
        }
        launchCamera();
    }

    public void launchCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider", createImageFile()));
            startActivityForResult(intent, REQUEST_CODE_CAMERA_CAPTURE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(getExternalFilesDir(
                Environment.DIRECTORY_DCIM), "Camera");

        if (!storageDir.exists()) {
            boolean isCreated = storageDir.mkdir();
        }

        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        cameraFilePath = "file://" + image.getAbsolutePath();
        return image;
    }


    private void selectFromAlbum() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            openAlbumWithPermissionsCheck();
        } else {
            openAlbum();
        }
    }

    private void openAlbum() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_GALLERY_IMAGE_CODE);
    }

    private void openAlbumWithPermissionsCheck() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_STORAGE);
            return;
        }
        openAlbum();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_STORAGE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openAlbum();
        } else if (requestCode == REQUEST_PERMISSION_CAMERA
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_GALLERY_IMAGE_CODE:
                    startCropActivity(data.getData());
//                    onImageProvided(retrieveDrawableFromUri(data.getData()), data.getData().getPath());
                    break;
                case REQUEST_CODE_CAMERA_CAPTURE:
                    Uri resultUri = Uri.parse(cameraFilePath);
                    startCropActivity(resultUri);
                    break;
                case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                    CropImage.ActivityResult result = CropImage.getActivityResult(data);
                    assert result != null;
                   /* if (result.getUri().getPath() != null) {
                        File file = new File(result.getUri().getPath());
                        try {
                            File compressFile = new Compressor(this).compressToFile(file);
                            Drawable d = Drawable.createFromPath(compressFile.getAbsolutePath());
                            onImageProvided(d, compressFile.getAbsolutePath());
                        } catch (IOException e) {
                            onImageProvided(retrieveDrawableFromUri(result.getUri()), result.getUri().getPath());
                            e.printStackTrace();
                        }
                    }*/

                    onImageProvided(retrieveDrawableFromUri(result.getUri()), result.getUri().getPath());
                    break;
            }
        }
    }

    private void startCropActivity(Uri sourceUri) {
        CropImage.activity(sourceUri)
                //.setCropShape(CropImageView.CropShape.RECTANGLE)
                .setFixAspectRatio(false)
                .setScaleType(CENTER_INSIDE)
                //.setRequestedSize(640, 480)
                .setAutoZoomEnabled(false)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }

    private Drawable retrieveDrawableFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            return Drawable.createFromStream(inputStream, uri.toString());
        } catch (FileNotFoundException e) {
            return ResourcesCompat.getDrawable(getResources(), R.drawable.ic_user, null);
            //return getResources().getDrawable(R.drawable.ic_person_24dp);
        }
    }

}
