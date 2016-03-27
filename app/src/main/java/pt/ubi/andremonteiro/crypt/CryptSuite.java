package pt.ubi.andremonteiro.crypt;

import android.app.Activity;
import android.content.Intent;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by André Monteiro on 09/03/2016.
 */
public class CryptSuite {

    private static final int iterations = 64000;
    private static final int keysize = 32;
    private static byte[] YC_FILE_SPEC = { 0x59, 0x43, 0x46, 0x01 };
    private static byte[] YC_FILE_CIPHER = { 0x01, 0x01, 0x01, 0x01 };

    public static byte[] encryptFile(InputStream inputStream, byte[] password, byte[] salt, byte[] hmacKey, byte[] tokenSerial) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException {
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec pbekey = new PBEKeySpec(password.toString().toCharArray(),hmacKey,iterations,keysize*8);

        byte[] key = skf.generateSecret(pbekey).getEncoded();
        byte[] iv = new byte[16];
        new Random().nextBytes(iv);
        //System.out.println("Salt: " + Util.byteArrayToString(salt));
        //System.out.println("Key:  " + Util.byteArrayToString(key));
        //System.out.println("IV:   " + Util.byteArrayToString(iv));

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        try {
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, 0, 32, "AES"), new IvParameterSpec(iv));
        }
        catch(IllegalArgumentException e){
            //System.out.println("Key too short");
            e.printStackTrace();
        }
        //HEADER = Version 4 bytes, Cypher Suite 4 bytes, Salt 32 bytes, IV 16 bytes, Iteraçoes 4 bytes, validation 32 bytes
        String s=Util.getStringFromInputStream(inputStream);
        byte[] encrypted = cipher.doFinal(s.getBytes());

        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        //HEADER BEGIN
        byteos.write(YC_FILE_SPEC);
        byteos.write(YC_FILE_CIPHER);
        byteos.write(salt);
        byteos.write(iv);
        byteos.write(ByteBuffer.allocate(4).putInt(iterations).array());
        byteos.write(getValidation(password, tokenSerial));
        //FILE BEGIN
        byteos.write(encrypted);
        //HMAC BEGIN
        byteos.write(getFileHMAC(byteos.toByteArray(), hmacKey, byteos.toByteArray().length));

        inputStream.close();
        System.out.println("ENC: "+ Util.byteArrayToString(byteos.toByteArray()));
        return byteos.toByteArray();
    }

    public static byte[] decryptFile(InputStream inputStream, byte[] password, byte[] hmacKey, byte[] tokenSerial) throws IOException {
        byte[] analysis = new byte[4];
        inputStream.read(analysis,0,4);
        System.out.println("THIS SHIT: "+ Util.byteArrayToString(analysis));
        if (analysis != YC_FILE_SPEC){
            System.out.println("ERROR BITCH!");
            return null;
        }
        System.out.println("ALL K ");
        return null;
    }

    public static byte[] getFileHMAC(byte[] array, byte[] hmacKey, int length) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(hmacKey, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        mac.update(array, 0, length);
        byte[] digest = mac.doFinal();
        return digest;
    }

    public static byte[] getValidation(byte[] password, byte[] tokenSerial) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        byteos.write(password);
        byteos.write(tokenSerial);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(byteos.toByteArray());
        return md.digest();
    }

    public static byte[] getSaltFromFile(InputStream inputStream) throws IOException { // salt begins at the 9th byte, length 32
        byte[] salt = new byte[128];
        //int x = inputStream.read(salt,8,32);
        System.out.println(Util.getStringFromInputStream(inputStream));


        //System.out.println("INTINT "+x);
        //ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //baos.
        System.out.println(Util.byteArrayToString(salt));
        return salt;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return skf.generateSecret(spec).getEncoded();
    }

}
