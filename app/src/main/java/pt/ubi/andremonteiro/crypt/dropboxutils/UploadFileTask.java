package pt.ubi.andremonteiro.crypt.dropboxutils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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

/**
 * Async task to upload a file to a directory
 */
class UploadFileTask extends AsyncTask<String, Void, FileMetadata> {

    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;
    private String mPassword;
    private byte[] mSalt, mHmacKey, mTokenSerial;

    public interface Callback {
        void onUploadComplete(FileMetadata result);
        void onError(Exception e);
    }

    UploadFileTask(Context context, DbxClientV2 dbxClient, Callback callback, String password, byte[] salt, byte[] hmacKey, byte[] tokenSerial) {
        mContext = context;
        mDbxClient = dbxClient;
        mCallback = callback;
        mPassword=password;
        mSalt=salt;
        mHmacKey=hmacKey;
        mTokenSerial=tokenSerial;
    }

    @Override
    protected void onPostExecute(FileMetadata result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else if (result == null) {
            mCallback.onError(null);
        } else {
            mCallback.onUploadComplete(result);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected FileMetadata doInBackground(String... params) {
        String localUri = params[0];
        File localFile = UriHelpers.getFileForUri(mContext, Uri.parse(localUri));

        if (localFile != null) {
            String remoteFolderPath = params[1];

            // Note - this is not ensuring the name is a valid dropbox file name
            String remoteFileName = localFile.getName();

            try (InputStream inputStream = new FileInputStream(localFile)) {
                byte[] encrypted = CryptSuite.encryptFile(inputStream,mPassword,mSalt,mHmacKey,mTokenSerial);
                InputStream is = new ByteArrayInputStream(encrypted);
                return mDbxClient.files().uploadBuilder(remoteFolderPath + "/" + remoteFileName +".ybc")
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(is);
            } catch (DbxException | IOException e) {
                mException = e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
