package ir.mehdivijeh.scanner.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashSet;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

class MainPresenter implements MainContract.MainPresenter {

    private static final String TAG = "MainPresenter";
    private MainContract.MainView view;
    private HashSet<Subscription> subscriptions = new HashSet<>();

    public MainPresenter(MainContract.MainView view) {
        this.view = view;
    }

    @Override
    public void uploadImage(RequestBody requestBody, MultipartBody.Part image) {
       /* subscriptions.add(
                RetrofitProvider.guestUsing(UploadImage.class)
                        .upload(requestBody, image)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(responseBodyCall -> {
                            try {
                                byte[] bytes = responseBodyCall.bytes();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                view.onResponse(bitmap);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }, throwable -> {
                            Toast.makeText(((Context) (view)), throwable.getMessage(), Toast.LENGTH_LONG).show();
                        }));*/
    }

    @Override
    public void onDestroy() {
        for (Subscription subscription : subscriptions)
            if (!subscription.isUnsubscribed())
                subscription.unsubscribe();
    }

}
