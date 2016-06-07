package pt.ubi.andremonteiro.crypt;

import android.app.Activity;
import android.content.Intent;
import android.util.Base64;
import android.widget.Toast;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.paddings.PKCS7Padding;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
import java.util.Collections;
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

import pt.ubi.andremonteiro.crypt.cryptutils.Rfc2898DeriveBytes;

/**
 * Created by André Monteiro on 09/03/2016.
 */
public class CryptSuite {

    private static final int iterations = 64000;
    private static final int keysize = 32;
    private static byte[] YC_FILE_SPEC = { 0x59, 0x43, 0x46, 0x01 };
    private static byte[] YC_FILE_CIPHER = { 0x00, 0x00, 0x11, 0x00 };

    public static byte[] encryptFile(InputStream inputStream, String password, byte[] salt, byte[] hmacKey, byte[] tokenSerial) throws Exception {
        Rfc2898DeriveBytes rfc = new Rfc2898DeriveBytes(password,hmacKey,iterations);
        byte[] mackey = rfc.getBytes(keysize);
        byte[] enckey = rfc.getBytes(keysize);
        byte[] iv = new byte[16];
        new Random().nextBytes(iv);

        //HEADER = Version 4 bytes, Cypher Suite 4 bytes, Salt 32 bytes, IV 16 bytes, Iteraçoes 4 bytes, validation 32 bytes

        byte[] encrypted = null;
        try {
            encrypted = blockCipherRunner(inputStream,enckey,iv,true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        //HEADER BEGIN
        byteos.write(YC_FILE_SPEC);
        byteos.write(YC_FILE_CIPHER);
        byteos.write(salt);
        byteos.write(iv);

        // Compatibilidade Windows: reverse iteraçoes
        byte[] iterationsReverse = Util.reverseByteArray(ByteBuffer.allocate(4).putInt(iterations).array());
        byteos.write(iterationsReverse);
        // End compatibilidade

        byteos.write(getValidation(password.getBytes("UTF-8"), tokenSerial));

        //FILE BEGIN
        byteos.write(encrypted);

        //HMAC BEGIN
        byteos.write(getFileHMAC(byteos.toByteArray(), mackey, byteos.toByteArray().length));

        inputStream.close();
        return byteos.toByteArray();
    }

    public static byte[] decryptFile(byte[] file, String password, byte[] hmacKey, byte[] tokenSerial) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (!Arrays.equals(YC_FILE_SPEC,Arrays.copyOfRange(file,0,4))){
            System.out.println("Wrong file specification.");
            return null;
        }
        if (!Arrays.equals(YC_FILE_CIPHER,Arrays.copyOfRange(file,4,8))){
            System.out.println("Wrong cipher version.");
            return null;
        }
        if (!validateKey(file, password.getBytes("UTF-8"), tokenSerial)){
            System.out.println("Invalid credentials. Check if you are entering the correct password and using the correct Yubikey token.");
            return null;
        }

        //if gets to this point -> ready to decrypt
        byte[] iv = Arrays.copyOfRange(file, 40, 56);
        byte[] iterations = Arrays.copyOfRange(file, 56, 60);
        iterations = Util.reverseByteArray(iterations);

        ByteBuffer byteBuffer = ByteBuffer.wrap(iterations);
        int intIters = byteBuffer.getInt();

        /*SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec pbekey = new PBEKeySpec(password.toCharArray(),hmacKey,64000,keysize*8);
        byte[] key = skf.generateSecret(pbekey).getEncoded();*/

        Rfc2898DeriveBytes rfc = new Rfc2898DeriveBytes(password,hmacKey,intIters);
        byte[] mackey = rfc.getBytes(keysize);
        byte[] enckey = rfc.getBytes(keysize);

        try {
            if (!Arrays.equals( getFileHMAC(file,mackey,file.length-32) , Arrays.copyOfRange(file,file.length-32, file.length) )){
                System.out.println("File corrupted. Invalid HMAC.");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

       /* Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, 0, 32, "AES"), new IvParameterSpec(iv));
        //HEADER = Version 4 bytes, Cypher Suite 4 bytes, Salt 32 bytes, IV 16 bytes, Iteraçoes 4 bytes, validation 32 bytes
        byte[] decrypted = cipher.doFinal(Arrays.copyOfRange(file,92,file.length-32));
       */

        ByteArrayInputStream inputStream = new ByteArrayInputStream(Arrays.copyOfRange(file,92,file.length-32));
        byte[] decrypted = null;
        try {
            decrypted = blockCipherRunner(inputStream,enckey,iv,false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decrypted;
    }

    public static boolean validateKey(byte[] file, byte[] password, byte[] tokenSerial){
        try {
            if (Arrays.equals(Arrays.copyOfRange(file, 60, 92), getValidation(password, tokenSerial)))
                return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
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

        // Compatibilidade com as apps do Windows (token serial nr reversed + byte vazio) --
        byte[] tokenSerialReversed = Util.reverseByteArray(tokenSerial);
        byteos.write(tokenSerialReversed);
        byteos.write(new byte[]{0x00});
        // End compatibilidade

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(byteos.toByteArray());
        return md.digest();
    }

    public static byte[] getSaltFromFile(byte[] file) throws IOException { // salt begins at the 9th byte, length 32
        byte[] salt = Arrays.copyOfRange(file, 8, 40);
        return salt;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return skf.generateSecret(spec).getEncoded();
    }

    private static byte[] blockCipherRunner(InputStream inputStream, byte[] key, byte[] iv, boolean encrypt) throws Exception {
        PaddedBufferedBlockCipher blockCipher;         /// charset   http://stackoverflow.com/questions/14397672/bad-padding-exception-pad-block-corrupt-when-calling-dofinal
        blockCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
        blockCipher.init(encrypt, new ParametersWithIV(new KeyParameter(key),iv));
        int blockCipherBlockSize = blockCipher.getBlockSize();
        byte[] buffer = new byte[blockCipherBlockSize * 8];
        byte[] processedBuffer = new byte[blockCipherBlockSize * 8];
        int read;
        int processed;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while (inputStream.available()>0)
        {
            read = inputStream.read(buffer, 0, buffer.length);
            processed = blockCipher.processBytes(buffer, 0, read, processedBuffer, 0);
            outputStream.write(processedBuffer, 0, processed);
        }
        processed = blockCipher.doFinal(processedBuffer, 0);
        outputStream.write(processedBuffer, 0, processed);
        return outputStream.toByteArray();
    }

}
