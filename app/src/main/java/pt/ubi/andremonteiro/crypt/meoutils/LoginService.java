package pt.ubi.andremonteiro.crypt.meoutils;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Created by André Monteiro on 13/06/2016.
 */
public interface LoginService {
    @FormUrlEncoded
    @POST("/OAuth/Token")
    Call<AccessToken> getAccessToken(
            @Field("grant_type") String grantType,
            @Field("code") String code,
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret,
            @Field("redirect_uri") String redirectUri
    );

}