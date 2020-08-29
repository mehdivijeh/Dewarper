package ir.mehdivijeh.scanner.general;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


public class RetrofitProvider {

    private static final String THROWABLE_TAG = "online error happened";
    private static Gson gson = new GsonBuilder()
            .registerTypeAdapter(Long.class, new DateDeserializerAdapter())
            .create();
    private static Retrofit.Builder mBuilder = new Retrofit.Builder()
            .baseUrl(ApiConstants.getApiUrl())
            .addConverterFactory(GsonConverterFactory.create(gson));

    private static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();

    private static Retrofit retrofit;
    private static Retrofit guestRetrofit;

    public static <S> S guestUsing(Class<S> serviceClass) {
        if (guestRetrofit == null) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder mHttpClient = new OkHttpClient.Builder();

            mHttpClient.addInterceptor(logging);



            mHttpClient.readTimeout(60, TimeUnit.SECONDS);
            mHttpClient.connectTimeout(60, TimeUnit.SECONDS);
            OkHttpClient client = mHttpClient.build();

            guestRetrofit = mBuilder.addCallAdapterFactory(RxJavaCallAdapterFactory.create()).client(client).build();
        }
        return guestRetrofit.create(serviceClass);
    }

}
