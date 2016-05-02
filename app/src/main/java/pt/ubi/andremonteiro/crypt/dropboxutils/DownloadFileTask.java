package pt.ubi.andremonteiro.crypt.dropboxutils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.renderscript.ScriptGroup;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import pt.ubi.andremonteiro.crypt.CryptSuite;
import pt.ubi.andremonteiro.crypt.Util;

/**
 * Task to download a file from Dropbox and put it in the Downloads folder
 */
public class DownloadFileTask extends AsyncTask<FileMetadata, Void, File> {

    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;
    private String mPassword;
    private byte[] mHmacKey;
    private byte[] mTokenSerial;

    public interface Callback {
        void onDownloadComplete(File result);
        void onError(Exception e);
    }

    public DownloadFileTask(Context context, DbxClientV2 dbxClient, Callback callback, String password, byte[] hmacKey, byte[] tokenSerial) {
        mContext = context;
        mDbxClient = dbxClient;
        mCallback = callback;
        mPassword = password;
        mHmacKey = hmacKey;
        mTokenSerial = tokenSerial;
    }

    @Override
    protected void onPostExecute(File result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDownloadComplete(result);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected File doInBackground(FileMetadata... params) {
        FileMetadata metadata = params[0];
        try {
            File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, metadata.getName());

            // Make sure the Downloads directory exists.
            if (!path.exists()) {
                if (!path.mkdirs()) {
                    mException = new RuntimeException("Unable to create directory: " + path);
                }
            } else if (!path.isDirectory()) {
                mException = new IllegalStateException("Download path is not a directory: " + path);
                return null;
            }

            // Download the file.
            try (OutputStream outputStream = new FileOutputStream(file)) {
                mDbxClient.files().download(metadata.getPathLower(), metadata.getRev())
                    .download(outputStream);
            }
            //temp file and decrypt
            File temp = File.createTempFile("ybctemp",".temp");
            InputStream is = new FileInputStream(file);
            byte[] fileBytes = Util.getBytesFromInputStream(is);
            byte[] decrypted = CryptSuite.decryptFile(fileBytes,mPassword,mHmacKey,mTokenSerial);
            FileUtils.writeByteArrayToFile(temp, decrypted);
            System.out.println("all gud");

            // Tell android about the file
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(temp));
            mContext.sendBroadcast(intent);

            return temp;
        } catch (DbxException | IOException e) {
            mException = e;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
