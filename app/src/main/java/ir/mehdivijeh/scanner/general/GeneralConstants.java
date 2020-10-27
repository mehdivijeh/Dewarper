package ir.mehdivijeh.scanner.general;

import android.os.Environment;

import java.io.File;

public class GeneralConstants {
    public static final String AUTH_CODE = "auth_code";


    public static final String BASE_DIRECTORY_NAME = "Camera Document Capture";
    public static final String TEMP_IMAGE_DIRECTORY_NAME = "Temporary Images";
    public static final String PERM_IMAGE_DIRECTORY_NAME = "Processed Images";



    public static final String BASE_DIRECTORY_FILE_PATH = Environment
            .getDownloadCacheDirectory().getAbsolutePath() + File.separator + BASE_DIRECTORY_NAME;


    public static final String TEMP_IMAGE_DIRECTORY_FILE_PATH = BASE_DIRECTORY_FILE_PATH +
            File.separator + TEMP_IMAGE_DIRECTORY_NAME;

    public static final String PERM_IMAGE_DIRECTORY_FILE_PATH = BASE_DIRECTORY_FILE_PATH +
            File.separator + PERM_IMAGE_DIRECTORY_NAME;

}
