package pt.ubi.andremonteiro.crypt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.ScriptGroup;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.Fragment;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import pt.ubi.andremonteiro.crypt.cryptutils.DecryptTask;
import pt.ubi.andremonteiro.crypt.cryptutils.EncryptTask;
import pt.ubi.andremonteiro.crypt.meoutils.AccessToken;
import pt.ubi.andremonteiro.crypt.meoutils.FileListAdapter;
import pt.ubi.andremonteiro.crypt.meoutils.GetFilesService;
import pt.ubi.andremonteiro.crypt.meoutils.GetSingleFileService;
import pt.ubi.andremonteiro.crypt.meoutils.LoginService;
import pt.ubi.andremonteiro.crypt.meoutils.UploadFileService;
import pt.ubi.andremonteiro.crypt.meoutils.YubicryptClient;
import pt.ubi.andremonteiro.crypt.meoutils.YubicryptFile;
import retrofit2.Call;
import retrofit2.Callback;
import okhttp3.Response;
import retrofit2.Retrofit;

public class MeoCloudFragment extends Fragment {
    View view;
    SharedPreferences mPrefs = null;
    AccessToken accessToken = null;
    ArrayList<YubicryptFile> list;
    String mAccessToken = "accessToken";
    String mRefresh = "Refresh session";
    ListView fileList;
    ProgressDialog dialog = null;
    Context ctx;
    byte[] saltHMAC, keyHMAC, tokenSerial, fileArray;
    String password, filename;
    private String code;
    int RESULT_CHALLENGE = 64, RESULT_SAVE_ENCRYPTED = 75, RESULT_SAVE_DECRYPTED = 76, CALL_ENCRYPTION = 62, CALL_DECRYPTION = 63, CHALLENGE_ENC = 66, CHALLENGE_DEC = 67;
    File file;
    boolean downloading = false, uploading = false;
    InputStream inputStream;
    ByteArrayOutputStream outputStream;

    private OnFragmentInteractionListener mListener;

    public MeoCloudFragment() {
        // Required empty public constructor
    }

    public static MeoCloudFragment newInstance(String param1, String param2) {
        MeoCloudFragment fragment = new MeoCloudFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = getActivity().getPreferences(getActivity().MODE_PRIVATE);
        Gson gson = new Gson();
        String json = mPrefs.getString(mAccessToken, null);
        accessToken = gson.fromJson(json, AccessToken.class);
        ctx = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_meocloud, container, false);
        Button loginButton = (Button) view.findViewById(R.id.meoLoginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(YubicryptClient.API_BASE_URL + "/OAuth/Authorize" + "?"+ "response_type=code" + "&client_id="
                                + YubicryptClient.CLIENT_ID + "&redirect_uri=" + YubicryptClient.REDIRECT_URI + "&scope=files keys"));
                startActivity(intent);
            }
        });

        if (accessToken != null)
        {
            TextView textExpDate = (TextView)view.findViewById(R.id.textExpirationDate);
            textExpDate.setText("Expires " + accessToken.getExpirationDate().getTime());
            if (accessToken.getExpirationDate().getTime().after(Util.getCurrentDate()))
            {
                loginButton.setText(mRefresh);
            }

        }

        FloatingActionButton fab = (FloatingActionButton)view.findViewById(R.id.meofab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploading = true;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, CALL_ENCRYPTION);
            }
        });

        fileList = (ListView)view.findViewById(R.id.meoFileListView);
        fileList.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    if (accessToken.getExpirationDate().getTime().before(Util.getCurrentDate())){
                        Toast.makeText(ctx, "The session has expired. Please login again.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    downloading = true;
                    YubicryptFile file = (YubicryptFile)fileList.getItemAtPosition(position);
                    filename = file.getFileName();
                    GetSingleFileService getSingleFileService = YubicryptClient.createService(GetSingleFileService.class, accessToken);
                    Call<okhttp3.ResponseBody> call = getSingleFileService.getFile(file.getFileName()+".ybc");
                    if (dialog == null) {
                        dialog = new ProgressDialog(ctx);
                        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        dialog.setCancelable(false);
                    }
                    dialog.setMessage("Downloading");
                    dialog.show();
                    call.enqueue(new Callback<okhttp3.ResponseBody>() {
                        @Override
                        public void onResponse(Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                            if (!response.isSuccessful()) return;
                            try {
                                InputStream inputStream = response.body().byteStream();
                                fileArray = IOUtils.toByteArray(inputStream);
                                System.out.println("file: \n"+Util.byteArrayToString(fileArray));
                                saltHMAC = CryptSuite.getSaltFromFile(fileArray);
                                getYubikeyInfo();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            dialog.dismiss();
                        }

                        @Override
                        public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                            Toast.makeText(ctx, "Error: "+t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    );
                }
            }
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // the intent filter defined in AndroidManifest will handle the return from ACTION_VIEW intent
        Uri uri = getActivity().getIntent().getData();
        if (uri != null && uri.toString().startsWith(YubicryptClient.REDIRECT_URI)) {
            // use the parameter your API exposes for the code (mostly it's "code")
            code = uri.getQueryParameter("code");
            if (code != null) {
                new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        LoginService loginService = YubicryptClient.createService(LoginService.class, YubicryptClient.CLIENT_ID, YubicryptClient.CLIENT_SECRET);
                        Call<AccessToken> call = loginService.getAccessToken("authorization_code", code, YubicryptClient.CLIENT_ID, YubicryptClient.CLIENT_SECRET, YubicryptClient.REDIRECT_URI);
                        try {
                            accessToken = call.execute().body();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        super.onPostExecute(o);
                        if (accessToken != null) {
                            accessToken.setExpirationDate();
                            TextView textExpDate = (TextView) view.findViewById(R.id.textExpirationDate);
                            textExpDate.setText("Expires " + accessToken.getExpirationDate().getTime());
                            Button btn = (Button)view.findViewById(R.id.meoLoginButton);
                            btn.setText(mRefresh);

                            // save token at shared preferences
                            SharedPreferences.Editor prefsEditor = mPrefs.edit();
                            Gson gson = new Gson();
                            String json = gson.toJson(accessToken);
                            prefsEditor.putString(mAccessToken, json);
                            prefsEditor.apply();
                        }
                    }
                }.execute();
            } else if (uri.getQueryParameter("error") != null) {
                Toast.makeText(getActivity(), "Error on retrieving authorization code.", Toast.LENGTH_SHORT).show();
            }
        }
        if (!(downloading || uploading)) populateListView();
    }

    public void populateListView(){
        if (accessToken != null) {
            if(accessToken.getExpirationDate().getTime().after(Util.getCurrentDate())){
                if (dialog == null){
                    dialog = new ProgressDialog(ctx);
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setCancelable(false);
                }
                dialog.setMessage("Refreshing file list");
                dialog.show();
                new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        GetFilesService getFilesService = YubicryptClient.createService(GetFilesService.class, accessToken);
                        Call<ArrayList<YubicryptFile>> filescall = getFilesService.getFiles();
                        try {
                            list = filescall.execute().body();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        super.onPostExecute(o);
                        if (list != null){
                            FileListAdapter adapter = new FileListAdapter(getActivity(), 0, list);
                            fileList.setAdapter(adapter);
                        }
                        dialog.dismiss();
                    }
                }.execute();
            }
        }
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) { super.onAttach(context); }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    private void getYubikeyInfo(){
        View view = LayoutInflater.from(ctx).inflate(R.layout.password_dialog, null);
        android.app.AlertDialog.Builder alertBuilder = new android.app.AlertDialog.Builder(ctx);
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
        Intent intent = new Intent(ctx,Challenge.class);
        intent.putExtra("challenge", saltHMAC);
        startActivityForResult(intent, challenge_mode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == getActivity().RESULT_CANCELED)
        {
            uploading=false; downloading=false;
        }
        if(data != null && requestCode == CALL_ENCRYPTION){
            ContentResolver cr = getActivity().getContentResolver();
            try {
                inputStream = cr.openInputStream(data.getData());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (requestCode == CALL_ENCRYPTION){
                filename=Util.getFileNameFromPath(data.getData().getPath());
                saltHMAC = Util.genRandomBytes();
                getYubikeyInfo();
            }
        }
        if (resultCode == RESULT_CHALLENGE) { // Result from challenge: 64
            keyHMAC = data.getByteArrayExtra("response");
            tokenSerial = data.getByteArrayExtra("serial");
            if (downloading) {
                try {
                    if (dialog == null) {
                        dialog = new ProgressDialog(ctx);
                        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        dialog.setCancelable(false);
                    }
                    dialog.setMessage("Decrypting");
                    dialog.show();
                    File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    file = new File(path, filename);
                    FileOutputStream fos = new FileOutputStream(file);
                    new DecryptTask(fileArray, fos, password, keyHMAC, tokenSerial) {
                        @Override
                        protected void onPostExecute(Object o) {
                            super.onPostExecute(o);
                            dialog.dismiss();
                            if (o instanceof String) {
                                Toast.makeText(ctx, "Unexpected error: " + o, Toast.LENGTH_LONG).show();
                            }
                            switch ((int) o) {
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
                                default:
                                    viewFileInExternalApp(file);
                            }
                            downloading = false;
                        }
                    }.execute();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (uploading){
                if (dialog == null) {
                    dialog = new ProgressDialog(ctx);
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setCancelable(false);
                }
                dialog.setMessage("Encrypting");
                dialog.show();
                outputStream = new ByteArrayOutputStream();
                new EncryptTask(inputStream, outputStream, password, saltHMAC, keyHMAC, tokenSerial){
                    @Override
                    protected void onPostExecute(Object o) {
                        super.onPostExecute(o);
                        dialog.dismiss();
                        if (o instanceof String){
                            Toast.makeText(ctx, "Unexpected error: "+o, Toast.LENGTH_LONG).show();
                        }

                        UploadFileService uploadService = YubicryptClient.createUploadService(UploadFileService.class, accessToken);

                        okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse("multipart/form-data"),outputStream.toByteArray());
                        MultipartBody.Part body = MultipartBody.Part.createFormData(filename,"file",requestFile);

                        Call<Void> call = uploadService.uploadFile(body);
                        call.enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                                Log.v("Upload", "success");
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Log.e("Upload error:", t.getMessage());
                            }
                        });
                    }
                }.execute();
            }


        }
    }

    private void viewFileInExternalApp(File result) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String ext = result.getName().substring(result.getName().indexOf(".") + 1);
        String type = mime.getMimeTypeFromExtension(ext);

        intent.setDataAndType(Uri.fromFile(result), type);

        // Check for a handler first to avoid a crash
        PackageManager manager = getActivity().getPackageManager();
        List<ResolveInfo> resolveInfo = manager.queryIntentActivities(intent, 0);
        if (resolveInfo.size() > 0) {
            startActivity(intent);
        }
    }

}
