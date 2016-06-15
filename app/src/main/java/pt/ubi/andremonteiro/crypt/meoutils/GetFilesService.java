package pt.ubi.andremonteiro.crypt.meoutils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;

/**
 * Created by André Monteiro on 13/06/2016.
 */
public interface GetFilesService {
    @GET("/api/File/GetFiles")
    Call<ArrayList<YubicryptFile>> getFiles();
}
