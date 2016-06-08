package pt.ubi.andremonteiro.crypt.cryptutils;

import android.os.AsyncTask;

import java.io.InputStream;
import java.io.OutputStream;
import pt.ubi.andremonteiro.crypt.CryptSuite;

/**
 * Created by André Monteiro on 08/06/2016.
 */
public class EncryptTask extends AsyncTask{

    InputStream inputStream;
    OutputStream outputStream;
    String password;
    byte[] salt, hmacKey, tokenSerial;

    public EncryptTask(InputStream inputStream, OutputStream outputStream, String password, byte[] salt, byte[] hmacKey, byte[] tokenSerial){
        this.inputStream=inputStream;
        this.outputStream=outputStream;
        this.password=password;
        this.salt=salt;
        this.hmacKey=hmacKey;
        this.tokenSerial=tokenSerial;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            CryptSuite.encryptFile(inputStream, outputStream, password, salt, hmacKey, tokenSerial);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return 0;
    }
}
