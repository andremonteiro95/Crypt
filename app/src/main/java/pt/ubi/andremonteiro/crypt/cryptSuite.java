package pt.ubi.andremonteiro.crypt;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Created by André Monteiro on 09/03/2016.
 */
public class cryptSuite {

    private static final int iterations = 64000;

    public void encryptFile(File file, byte[] password, byte[] salt, byte[] hmacKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec key = new PBEKeySpec(password.toString().toCharArray(),salt,iterations);
        byte[] hash= skf.generateSecret(key).getEncoded();
    }

}
