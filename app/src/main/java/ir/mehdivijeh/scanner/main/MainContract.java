package ir.mehdivijeh.scanner.main;

import android.graphics.Bitmap;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

class MainContract {

    interface MainView{
        void onResponse(Bitmap bitmap);
    }

    interface MainPresenter{
        void uploadImage(RequestBody requestBody , MultipartBody.Part image);
        void onDestroy();
    }
}
