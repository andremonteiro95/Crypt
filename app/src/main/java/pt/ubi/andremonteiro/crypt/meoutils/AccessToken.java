package pt.ubi.andremonteiro.crypt.meoutils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by André Monteiro on 08/06/2016.
 */
public class AccessToken {

    public String access_token;
    public String token_type;
    public int expires_in;
    public String refresh_token;
    public Calendar expirationDate;

    public String getAccess_token() {
        return access_token;
    }



    public String getToken_type() {
        // OAuth requires uppercase Authorization HTTP header value for token type
        if ( ! Character.isUpperCase(token_type.charAt(0))) {
            token_type =
                    Character
                            .toString(token_type.charAt(0))
                            .toUpperCase() + token_type.substring(1);
        }
        return token_type;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public void setExpires_in(Object expires_in) {
        System.out.println("Class: "+expires_in.getClass());
        /*Date date = date.
        this.expires_in = expires_in;*/
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public void setExpirationDate(){
        expirationDate = new GregorianCalendar(TimeZone.getTimeZone("Europe/London"));
        expirationDate.add(Calendar.MILLISECOND,TimeZone.getTimeZone("Europe/London").getDSTSavings());
        expirationDate.add(Calendar.SECOND,expires_in);
    }

    public Calendar getExpirationDate() {
        return expirationDate;
    }
}