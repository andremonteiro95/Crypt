package pt.ubi.andremonteiro.crypt.dropboxutils;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.List;

import pt.ubi.andremonteiro.crypt.Challenge;
import pt.ubi.andremonteiro.crypt.CryptSuite;
import pt.ubi.andremonteiro.crypt.R;
import pt.ubi.andremonteiro.crypt.Util;
import pt.ubi.andremonteiro.crypt.cryptutils.DecryptTask;


/**
 * Activity that displays the content of a path in dropbox and lets users navigate folders,
 * and upload/download files
 */
public class FilesActivity extends DropboxActivity {
    private static final String TAG = FilesActivity.class.getName();

    public final static String EXTRA_PATH = "FilesActivity_Path";
    private static final int PICKFILE_REQUEST_CODE = 1;

    private String mPath;
    private FilesAdapter mFilesAdapter;
    private FileMetadata mSelectedFile;
    private String uploadFileUri;
    boolean isDownload = false;

    public static Intent getIntent(Context context, String path) {
        Intent filesIntent = new Intent(context, FilesActivity.class);
        filesIntent.putExtra(FilesActivity.EXTRA_PATH, path);
        return filesIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String path = getIntent().getStringExtra(EXTRA_PATH);
        mPath = path == null ? "" : path;

        setContentView(R.layout.activity_files);

       /* Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);*/

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performWithPermissions(FileAction.UPLOAD);
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.files_list);
        mFilesAdapter = new FilesAdapter(PicassoClient.getPicasso(), new FilesAdapter.Callback() {
            @Override
            public void onFolderClicked(FolderMetadata folder) {
                startActivity(FilesActivity.getIntent(FilesActivity.this, folder.getPathLower()));
            }

            @Override
            public void onFileClicked(final FileMetadata file) {
                mSelectedFile = file;
                performWithPermissions(FileAction.DOWNLOAD);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mFilesAdapter);

        mSelectedFile = null;
        loadData();
    }

    private void launchFilePicker() {
        // Launch intent to pick file for upload
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }

    int RESULT_CHALLENGE = 64, CHALLENGE_ENC = 66;
    ProgressDialog dialog;
    Context ctx;
    File file;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKFILE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                uploadFileUri = data.getData().toString();
                saltHMAC = Util.genRandomBytes();
                getYubikeyInfo();
                // This is the result of a call to launchFilePicker
            }
        }
        if (resultCode == RESULT_CHALLENGE) { // Result from challenge: 64
            keyHMAC = data.getByteArrayExtra("response");
            tokenSerial = data.getByteArrayExtra("serial");
            if (isDownload){
                try {
                    ctx = this;
                    dialog=new ProgressDialog(this);
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setCancelable(false);
                    dialog.setMessage("Decrypting");
                    dialog.show();
                    File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    file = new File(path, "things.jpg");
                    FileOutputStream fos = new FileOutputStream(file);
                    new DecryptTask(resultArray, fos, password, keyHMAC, tokenSerial){
                        @Override
                        protected void onPostExecute(Object o) {
                            super.onPostExecute(o);
                            dialog.dismiss();
                            if (o instanceof String){
                                Toast.makeText(ctx, "Unexpected error: "+o, Toast.LENGTH_LONG).show();
                            }
                            switch ((int)o){
                                case 1:
                                    Toast.makeText(ctx, "Wrong file specification.", Toast.LENGTH_LONG).show();
                                    break;
                                case 2:
                                    Toast.makeText(ctx, "Wrong cipher version.", Toast.LENGTH_LONG).show();
                                    break;
                                case 3:
                                    Toast.makeText(ctx, "Invalid credentials. Check if you are entering the correct password and using the correct Yubikey token.", Toast.LENGTH_LONG).show();
                                    break;
                                case 4:
                                    Toast.makeText(ctx, "File corrupted. Invalid HMAC.", Toast.LENGTH_LONG).show();
                                    break;
                            }
                            viewFileInExternalApp(file);
                        }
                    }.execute();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else uploadFile();
        }
    }

    @Override
    public void onRequestPermissionsResult(int actionCode, @NonNull String [] permissions, @NonNull int [] grantResults) {
        FileAction action = FileAction.fromCode(actionCode);

        boolean granted = true;
        for (int i = 0; i < grantResults.length; ++i) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                Log.w(TAG, "User denied " + permissions[i] +
                        " permission to perform file action: " + action);
                granted = false;
                break;
            }
        }

        if (granted) {
            performAction(action);
        } else {
            switch (action) {
                case UPLOAD:
                    Toast.makeText(this,
                            "Can't upload file: read access denied. " +
                                    "Please grant storage permissions to use this functionality.",
                            Toast.LENGTH_LONG)
                        .show();
                    break;
                case DOWNLOAD:
                    Toast.makeText(this,
                            "Can't download file: write access denied. " +
                                    "Please grant storage permissions to use this functionality.",
                            Toast.LENGTH_LONG)
                        .show();
                    break;
            }
        }
    }

    private void performAction(FileAction action) {
        switch(action) {
            case UPLOAD: {
                launchFilePicker();
                break;
            }
            case DOWNLOAD:
                if (mSelectedFile != null) {
                    isDownload=true;
                    downloadFile(mSelectedFile);
                } else {
                    Log.e(TAG, "No file selected to download.");
                }
                break;
            default:
                Log.e(TAG, "Can't perform unhandled file action: " + action);
        }
    }

    @Override
    protected void loadData() {

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMessage("Loading");
        dialog.show();

        new ListFolderTask(DropboxClientFactory.getClient(), new ListFolderTask.Callback() {
            @Override
            public void onDataLoaded(ListFolderResult result) {
                dialog.dismiss();

                mFilesAdapter.setFiles(result.getEntries());
            }

            @Override
            public void onError(Exception e) {
                dialog.dismiss();

                Log.e(TAG, "Failed to list folder.", e);
                Toast.makeText(FilesActivity.this,
                        "An error has occurred",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }).execute(mPath);
    }

    File resultDownload;
    byte[] resultArray=null;

    private void downloadFile(FileMetadata file) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMessage("Downloading");
        dialog.show();

        new DownloadFileTask(FilesActivity.this, DropboxClientFactory.getClient(), new DownloadFileTask.Callback() {
            @Override
            public void onDownloadComplete(File result) {


                if (result != null) {
                    resultDownload = result;

                    try {
                        resultArray = IOUtils.toByteArray(new FileInputStream(resultDownload));
                        saltHMAC = CryptSuite.getSaltFromFile(resultArray);
                        getYubikeyInfo();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                   // viewFileInExternalApp(result);
                }
                dialog.dismiss();
            }

            @Override
            public void onError(Exception e) {
                dialog.dismiss();

                Log.e(TAG, "Failed to download file.", e);
                Toast.makeText(FilesActivity.this,
                        "An error has occurred",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }).execute(file);

    }

    private void viewFileInExternalApp(File result) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String ext = result.getName().substring(result.getName().indexOf(".") + 1);
        String type = mime.getMimeTypeFromExtension(ext);

        intent.setDataAndType(Uri.fromFile(result), type);

        // Check for a handler first to avoid a crash
        PackageManager manager = getPackageManager();
        List<ResolveInfo> resolveInfo = manager.queryIntentActivities(intent, 0);
        if (resolveInfo.size() > 0) {
            startActivity(intent);
        }
    }

    byte[] saltHMAC, keyHMAC, tokenSerial;
    String password;

    private void uploadFile() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMessage("Uploading");
        dialog.show();
        new UploadFileTask(this, DropboxClientFactory.getClient(), new UploadFileTask.Callback()  {
            @Override
            public void onUploadComplete(FileMetadata result) {
                dialog.dismiss();

                String message = result.getName() + " size " + result.getSize() + " modified " +
                        DateFormat.getDateTimeInstance().format(result.getClientModified());
                Toast.makeText(FilesActivity.this, message, Toast.LENGTH_SHORT)
                        .show();

                // Reload the folder
                loadData();
            }

            @Override
            public void onError(Exception e) {
                dialog.dismiss();

                Log.e(TAG, "Failed to upload file.", e);
                Toast.makeText(FilesActivity.this,
                        "An error has occurred",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }, password,saltHMAC,keyHMAC,tokenSerial).execute(uploadFileUri, mPath);
    }

    private void performWithPermissions(final FileAction action) {
        if (hasPermissionsForAction(action)) {
            performAction(action);
            return;
        }
        requestPermissionsForAction(action);
    }

    private boolean hasPermissionsForAction(FileAction action) {
        for (String permission : action.getPermissions()) {
            int result = ContextCompat.checkSelfPermission(this, permission);
            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldDisplayRationaleForAction(FileAction action) {
        for (String permission : action.getPermissions()) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    private void requestPermissionsForAction(FileAction action) {
        ActivityCompat.requestPermissions(
                this,
                action.getPermissions(),
                action.getCode()
        );
    }

    private enum FileAction {
        DOWNLOAD(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        UPLOAD(Manifest.permission.READ_EXTERNAL_STORAGE);

        private static final FileAction [] values = values();

        private final String [] permissions;

        FileAction(String ... permissions) {
            this.permissions = permissions;
        }

        public int getCode() {
            return ordinal();
        }

        public String [] getPermissions() {
            return permissions;
        }

        public static FileAction fromCode(int code) {
            if (code < 0 || code >= values.length) {
                throw new IllegalArgumentException("Invalid FileAction code: " + code);
            }
            return values[code];
        }
    }

    void getYubikeyInfo(){
        View view = LayoutInflater.from(this).inflate(R.layout.password_dialog, null);
        android.app.AlertDialog.Builder alertBuilder = new android.app.AlertDialog.Builder(this);
        alertBuilder.setView(view);
        final EditText userInput = (EditText) view.findViewById(R.id.userinput);
        alertBuilder.setCancelable(true).setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                password = userInput.getText().toString();
                challengeMethod(saltHMAC, CHALLENGE_ENC);
            }
        });
        Dialog dialog = alertBuilder.create();
        dialog.show();
    }

    private void challengeMethod(byte[] saltHMAC, int challenge_mode){
        Intent intent = new Intent(this,Challenge.class);
        intent.putExtra("challenge", saltHMAC);
        startActivityForResult(intent, challenge_mode);
    }
}
