package pt.ubi.andremonteiro.crypt.meoutils;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Streaming;

/**
 * Created by André Monteiro on 14/06/2016.
 */
public interface UploadFileService {
    @Streaming
    @POST("api/File/Upload")
    Call<Void> uploadFile(@Part MultipartBody.Part file);
}
