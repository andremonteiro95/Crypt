package pt.ubi.andremonteiro.crypt;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.users.FullAccount;

import pt.ubi.andremonteiro.crypt.dropboxutils.DropboxClientFactory;
import pt.ubi.andremonteiro.crypt.dropboxutils.FilesActivity;
import pt.ubi.andremonteiro.crypt.dropboxutils.GetCurrentAccountTask;
import pt.ubi.andremonteiro.crypt.dropboxutils.PicassoClient;

public class DropboxFragment extends android.app.Fragment {
    final static String MEOCLOUD_ID = "af700c6f-c533-405f-b193-5c7a4f9754f4";
    final static String MEOCLOUD_SECRET ="199512721491065626740309022447265693076";
    final static String DROPBOX_ID ="qfe0bqco2hs9uv4";
    final static String DROPBOX_SECRET ="f6ogn80v8q0aklh";
    View v;

    Context context;


    private OnFragmentInteractionListener mListener;

    public DropboxFragment() {
        // Required empty public constructor
    }

    public static DropboxFragment newInstance(String param1, String param2) {
        DropboxFragment fragment = new DropboxFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
    }

    @Override
    public void onResume(){
        super.onResume();
        // visibilities
        if (hasToken()) {
            v.findViewById(R.id.dropboxButtonAuth).setVisibility(View.GONE);
            v.findViewById(R.id.dropboxName).setVisibility(View.VISIBLE);
            v.findViewById(R.id.dropboxEmail).setVisibility(View.VISIBLE);
            v.findViewById(R.id.dropboxType).setVisibility(View.VISIBLE);
            v.findViewById(R.id.dropboxButtonFiles).setVisibility(View.VISIBLE);
        } else {
            v.findViewById(R.id.dropboxButtonAuth).setVisibility(View.VISIBLE);
            v.findViewById(R.id.dropboxName).setVisibility(View.GONE);
            v.findViewById(R.id.dropboxEmail).setVisibility(View.GONE);
            v.findViewById(R.id.dropboxType).setVisibility(View.GONE);
            v.findViewById(R.id.dropboxButtonFiles).setVisibility(View.GONE);
        }

        // token
        SharedPreferences prefs = context.getSharedPreferences("dropbox", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        if (accessToken == null) {
            accessToken = Auth.getOAuth2Token();
            if (accessToken != null) {
                prefs.edit().putString("access-token", accessToken).apply();
                initAndLoadData(accessToken);
            }
        } else {
            initAndLoadData(accessToken);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v= inflater.inflate(R.layout.fragment_dropbox, container, false);
        Button buttonAuth = (Button)v.findViewById(R.id.dropboxButtonAuth);
        Button buttonFiles = (Button)v.findViewById(R.id.dropboxButtonFiles);
        buttonAuth.setOnClickListener(new Button.OnClickListener(){
              @Override
              public void onClick(View v) {
                  Auth.startOAuth2Authentication(context, DROPBOX_ID);
              }
          }
        );

        buttonFiles.setOnClickListener(new Button.OnClickListener(){
              @Override
              public void onClick(View v) {
                  startActivity(FilesActivity.getIntent(context, ""));
              }
          }
        );

        return v;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    // Important methods
    private void initAndLoadData(String accessToken) {
        DropboxClientFactory.init(accessToken);
        PicassoClient.init(context, DropboxClientFactory.getClient());
        loadData();
    }

    protected void loadData() {
        new GetCurrentAccountTask(DropboxClientFactory.getClient(), new GetCurrentAccountTask.Callback() {
            @Override
            public void onComplete(FullAccount result) {
                ((TextView) v.findViewById(R.id.dropboxEmail)).setText(result.getEmail());
                ((TextView) v.findViewById(R.id.dropboxName)).setText(result.getName().getDisplayName());
                ((TextView) v.findViewById(R.id.dropboxType)).setText(result.getAccountType().name());
            }

            @Override
            public void onError(Exception e) {
                Log.e(getClass().getName(), "Failed to get account details.", e);
            }
        }).execute();
    }

    protected boolean hasToken() {
        SharedPreferences prefs = context.getSharedPreferences("dropbox", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        return accessToken != null;
    }
}
