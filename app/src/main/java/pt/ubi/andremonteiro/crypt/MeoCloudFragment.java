package pt.ubi.andremonteiro.crypt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.Fragment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pt.ubi.andremonteiro.crypt.meoutils.AccessToken;
import pt.ubi.andremonteiro.crypt.meoutils.GetFilesService;
import pt.ubi.andremonteiro.crypt.meoutils.LoginService;
import pt.ubi.andremonteiro.crypt.meoutils.YubicryptClient;
import pt.ubi.andremonteiro.crypt.meoutils.YubicryptFile;
import retrofit2.Call;

public class MeoCloudFragment extends Fragment {
    View view;
    SharedPreferences mPrefs = null;
    AccessToken accessToken = null;
    ArrayList<YubicryptFile> list;

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
        String json = mPrefs.getString("AccessToken", null);
        accessToken = gson.fromJson(json, AccessToken.class);

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
            textExpDate.setText("Expires \n" + accessToken.getExpirationDate().getTime());
            if (accessToken.getExpirationDate().getTime().after(Util.getCurrentDate()))
            {
                loginButton.setText("Refresh session");
            }
            // REFRESH LIST HERE
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // the intent filter defined in AndroidManifest will handle the return from ACTION_VIEW intent
        Uri uri = getActivity().getIntent().getData();
        if (uri != null && uri.toString().startsWith(YubicryptClient.REDIRECT_URI)) {
            // use the parameter your API exposes for the code (mostly it's "code")
            String code = uri.getQueryParameter("code");
            if (code != null) {
                LoginService loginService = YubicryptClient.createService(LoginService.class, YubicryptClient.CLIENT_ID, YubicryptClient.CLIENT_SECRET);
                Call<AccessToken> call = loginService.getAccessToken("authorization_code", code, YubicryptClient.CLIENT_ID, YubicryptClient.CLIENT_SECRET, YubicryptClient.REDIRECT_URI);
                try {
                    accessToken = call.execute().body();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (accessToken != null){
                    accessToken.setExpirationDate();
                    TextView textExpDate = (TextView)view.findViewById(R.id.textExpirationDate);
                    textExpDate.setText("Expires "+accessToken.getExpirationDate().getTime());
                    // save token at shared preferences
                    SharedPreferences.Editor prefsEditor = mPrefs.edit();
                    Gson gson = new Gson();
                    String json = gson.toJson(accessToken);
                    prefsEditor.putString("AccessToken", json);
                    prefsEditor.apply();

                    //File listing
                    GetFilesService getFilesService = YubicryptClient.createService(GetFilesService.class,accessToken);
                    Call<ArrayList<YubicryptFile>> filescall = getFilesService.getFiles();
                    try {
                        list = filescall.execute().body();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            } else if (uri.getQueryParameter("error") != null) {
                Toast.makeText(getActivity(), "Error on retrieving authorization code.", Toast.LENGTH_SHORT).show();
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
}
