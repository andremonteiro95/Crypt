package pt.ubi.andremonteiro.crypt.cryptutils;

import android.os.AsyncTask;

import java.io.InputStream;
import java.io.OutputStream;

import pt.ubi.andremonteiro.crypt.CryptSuite;

/**
 * Created by André Monteiro on 08/06/2016.
 */
public class DecryptTask extends AsyncTask{

    OutputStream outputStream;
    String password;
    byte[] file, hmacKey, tokenSerial;

    public DecryptTask(byte[] file, OutputStream outputStream, String password, byte[] hmacKey, byte[] tokenSerial){
        this.outputStream=outputStream;
        this.password=password;
        this.file=file;
        this.hmacKey=hmacKey;
        this.tokenSerial=tokenSerial;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            return CryptSuite.decryptFile(file, outputStream, password, hmacKey, tokenSerial);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
