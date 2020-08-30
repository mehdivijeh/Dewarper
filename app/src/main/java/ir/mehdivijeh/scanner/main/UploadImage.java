package ir.mehdivijeh.scanner.main;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import rx.Observable;

public interface UploadImage {

    @POST("rebook/apiCall/v1")
    @Multipart
    Observable<ResponseBody> upload(@Part("image") RequestBody fullName,
                                                    @Part MultipartBody.Part image);


}
