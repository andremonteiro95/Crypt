package pt.ubi.andremonteiro.crypt.meoutils;

import okhttp3.OkHttpClient;

/**
 * Created by André Monteiro on 08/06/2016.
 */
public class YubicryptClient {

    final private String apiBaseUrl = "https://yubicryptubi.azurewebsites.net";
    final private String clientId = "123456";
    final private String clientSecret = "abcdef";
    final private String redirectUrl = "https://callback.com";

    OkHttpClient httpClient = new OkHttpClient();

    public void go(){
       // httpClient.
    }
}
