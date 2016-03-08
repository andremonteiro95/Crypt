package pt.ubi.andremonteiro.crypt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OfflineFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OfflineFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OfflineFragment extends android.app.Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    View v=null;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public OfflineFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OfflineFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OfflineFragment newInstance(String param1, String param2) {
        OfflineFragment fragment = new OfflineFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    Button buttonEncrypt, buttonDecrypt;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    StringBuilder text;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v=inflater.inflate(R.layout.fragment_offline, container, false);
        buttonEncrypt = (Button)v.findViewById(R.id.offlineEncryptButton);


        buttonEncrypt.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //intent.setType("*/*");
                //startActivityForResult(intent, 1);
                String x = "1234567812345678123456781234567812345678123456781234567812345678";
                byte[] xb = new byte[64];
                xb = x.getBytes();
                Intent intent = new Intent(getActivity(),Challenge.class);
                intent.putExtra("challenge", xb);
                startActivityForResult(intent, 1);
                /*byte[] example = new byte[64];
                String string = "1234";
                example = string.getBytes();
                ChallengeResponse cr = new ChallengeResponse(getActivity(),example);
                cr.challengeYubiKey();*/
            }
        });

        Button showDialog = (Button) v.findViewById(R.id.offlineDecryptButton);
        final String userinputtext=null;
        showDialog.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.password_dialog, null);

                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                alertBuilder.setView(view);
                final EditText userInput = (EditText) view.findViewById(R.id.userinput);

                alertBuilder.setCancelable(true)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.out.println(userinputtext);

                            }
                        });
                Dialog dialog = alertBuilder.create();
                dialog.show();
            }
        });







        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if(data != null){
            String filepath = data.getData().getPath();
            System.out.println(data.getStringExtra("response"));
            String[] tokens = filepath.split(":");
            File file = new File(Environment.getExternalStorageDirectory(), tokens[1]);
            text = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                br.close();
            }
            catch (IOException e) {
                System.out.println("IO Exception thrown.");
            }
            System.out.println(text);
        }

    }
    // TODO: Rename method, update argument and hook method into UI event
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

}
