package pt.ubi.andremonteiro.crypt;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

/**
 * Created by André Monteiro on 15/03/2016.
 */
public class Util {

    public static byte[] genRandomBytes(){
        byte[] bytes = new byte[32];
        new Random().nextBytes(bytes);
        return bytes;
    }

    public static String byteArrayToString(byte[] x) {
        StringBuilder sb = new StringBuilder();
        for (byte b : x) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line+"\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }

    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        return IOUtils.toByteArray(is);
    }

    public static byte[] getHeaderFromInputStream(InputStream is) throws IOException {
        byte[] header = new byte[92];
        is.read(header,0,92);
        return header;
    }

    public static String getFileNameFromPath(String path){
        String[] tokens = path.split("/");
        return tokens[tokens.length-1];
    }

    public static String getFileNameFromPathDec(String path){
        String[] tokens = path.split("/");
        String[] tokens2 = tokens[tokens.length-1].split(".");
        StringBuilder builder = new StringBuilder();
        for(int i =0; i< tokens2.length -2 ; i++) {
            builder.append(tokens2[i]);
        }
        return builder.toString();
    }

    public static byte[] reverseByteArray(byte[] original){
        byte[] result = original;
        byte temp;
        for(int i = 0; i < result.length / 2; i++)
        {
            temp = result[i];
            result[i] = result[result.length - i - 1];
            result[result.length - i - 1] = temp;
        }
        return result;
    }

}
