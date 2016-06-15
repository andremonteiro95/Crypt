package pt.ubi.andremonteiro.crypt.meoutils;

import java.io.InputStream;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

/**
 * Created by André Monteiro on 14/06/2016.
 */
public interface GetSingleFileService {
    @Streaming
    @GET("/api/File/Download")
    Call<okhttp3.ResponseBody> getFile(@Query("filename") String filename);
}
