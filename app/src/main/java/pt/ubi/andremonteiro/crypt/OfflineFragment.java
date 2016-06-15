package pt.ubi.andremonteiro.crypt;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import pt.ubi.andremonteiro.crypt.cryptutils.DecryptTask;
import pt.ubi.andremonteiro.crypt.cryptutils.EncryptTask;
import pt.ubi.andremonteiro.crypt.cryptutils.Rfc2898DeriveBytes;


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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v=inflater.inflate(R.layout.fragment_offline, container, false);
        buttonEncrypt = (Button)v.findViewById(R.id.offlineEncryptButton);
        buttonDecrypt = (Button)v.findViewById(R.id.offlineDecryptButton);

        buttonEncrypt.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, CALL_ENCRYPTION);
            }
        });

        buttonDecrypt.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, CALL_DECRYPTION);
            }
        });
        return v;
    }

    byte[] saltHMAC, keyHMAC, encrypted, tokenSerial;
    InputStream inputStream;
    OutputStream outputStream;
    String password, filename;
    int RESULT_CHALLENGE = 64, RESULT_SAVE_ENCRYPTED = 75, RESULT_SAVE_DECRYPTED = 76, CALL_ENCRYPTION = 62, CALL_DECRYPTION = 63, CHALLENGE_ENC = 66, CHALLENGE_DEC = 67;
    Context ctx;

    private void encryptMethod(){
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.password_dialog, null);
        ctx = getActivity();
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(ctx);
        alertBuilder.setView(view);
        final EditText userInput = (EditText) view.findViewById(R.id.userinput);
        alertBuilder.setCancelable(true).setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                password = userInput.getText().toString();
                saltHMAC = Util.genRandomBytes();
                challengeMethod(saltHMAC, CHALLENGE_ENC);
            }
        });
        Dialog dialog = alertBuilder.create();
        dialog.show();
    }

    private void decryptMethod(){
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.password_dialog, null);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
        alertBuilder.setView(view);
        final EditText userInput = (EditText) view.findViewById(R.id.userinput);
        alertBuilder.setCancelable(true).setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                password = userInput.getText().toString();
                byte[] salt = new byte[32];
                try {
                    encrypted = Util.getBytesFromInputStream(inputStream);
                    salt = CryptSuite.getSaltFromFile(encrypted);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                challengeMethod(salt, CHALLENGE_DEC);
            }
        });
        Dialog dialog = alertBuilder.create();
        dialog.show();
    }

    private void challengeMethod(byte[] saltHMAC, int challenge_mode){
        Intent intent = new Intent(getActivity(),Challenge.class);
        intent.putExtra("challenge", saltHMAC);
        startActivityForResult(intent, challenge_mode);
    }

    ProgressDialog dialog;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // TODO Auto-generated method stub
        if(resultCode == Activity.RESULT_CANCELED){
            Toast.makeText(ctx, "Encryption canceled.", Toast.LENGTH_SHORT).show();
        }
        else if (resultCode == RESULT_CHALLENGE){ // Result from challenge: 64
            keyHMAC = data.getByteArrayExtra("response");
            tokenSerial = data.getByteArrayExtra("serial");
            try {
                if (requestCode == CHALLENGE_ENC) saveFile(null,CALL_ENCRYPTION);
                else saveFile(null,CALL_DECRYPTION);
            } catch (Exception e) { e.printStackTrace(); }
        }
        else if(requestCode == RESULT_SAVE_ENCRYPTED){
            try {
                saveFile(data.getData(),CALL_ENCRYPTION);
                ctx = getActivity();
                dialog=new ProgressDialog(ctx);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setCancelable(false);
                dialog.setMessage("Encrypting");
                dialog.show();
                new EncryptTask(inputStream, outputStream, password, saltHMAC, keyHMAC, tokenSerial){
                    @Override
                    protected void onPostExecute(Object o) {
                        super.onPostExecute(o);
                        dialog.dismiss();
                        if (o instanceof String){
                            Toast.makeText(ctx, "Unexpected error: "+o, Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == RESULT_SAVE_DECRYPTED){
            try {
                saveFile(data.getData(),CALL_DECRYPTION);
                ctx = getActivity();
                dialog=new ProgressDialog(ctx);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setCancelable(false);
                dialog.setMessage("Decrypting");
                dialog.show();
                new DecryptTask(encrypted, outputStream, password, keyHMAC, tokenSerial){
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
                    }
                }.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(data != null){
            ContentResolver cr = getActivity().getContentResolver();
            try {
                inputStream = cr.openInputStream(data.getData());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (requestCode == CALL_ENCRYPTION){
                filename=Util.getFileNameFromPath(data.getData().getPath());
                encryptMethod();
            }
            else if (requestCode == CALL_DECRYPTION){
                filename=Util.getFileNameFromPathDec(data.getData().getPath());
                decryptMethod();
            }
        }

    }

    public void saveFile(Uri uri, int mode) throws IOException {
        if (uri==null){
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_TITLE, filename + ".ybc");
            if (mode == CALL_ENCRYPTION)
                startActivityForResult(intent, RESULT_SAVE_ENCRYPTED);
            else if (mode == CALL_DECRYPTION)
                startActivityForResult(intent, RESULT_SAVE_DECRYPTED);
            return;
        }
        ContentResolver cr = getActivity().getContentResolver();
        outputStream = cr.openOutputStream(uri,"w");
        /*if (mode == CALL_ENCRYPTION) outputStream.write(encrypted);
        else outputStream.write(decrypted);
        outputStream.close();
        encrypted = null;
        decrypted = null;*/
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
