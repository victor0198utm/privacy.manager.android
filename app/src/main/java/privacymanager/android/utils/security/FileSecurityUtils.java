package privacymanager.android.utils.security;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;


/**
 * Class made for working with encryption/decryption of user files.
 */
public class FileSecurityUtils {
    //Arbitrarily selected 8-byte salt sequence:
    private static final byte[] salt = {
            (byte) 0x43, (byte) 0x76, (byte) 0x95, (byte) 0xc7,
            (byte) 0x5b, (byte) 0xd7, (byte) 0x45, (byte) 0x17
    };

    public static Cipher makeCipher(String pass, Boolean decryptMode) throws GeneralSecurityException {

        //Use a KeyFactory to derive the corresponding key from the passphrase:
        PBEKeySpec keySpec = new PBEKeySpec(pass.toCharArray());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(keySpec);

        //Create parameters from the salt and an arbitrary number of iterations:
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 42);

        //Set up the cipher:
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");

        //Set the cipher mode to decryption or encryption:
        if (decryptMode) {
            cipher.init(Cipher.ENCRYPT_MODE, key, pbeParamSpec);
        } else {
            cipher.init(Cipher.DECRYPT_MODE, key, pbeParamSpec);
        }

        return cipher;
    }


    /**
     * Encrypts one file to a second file using a key derived from a passphrase:
     **/
    public static String encryptFile(String sourcePath, String destinationPath, String pass)
            throws IOException, GeneralSecurityException {
        byte[] decData;
        byte[] encData;
        File inFile = new File(sourcePath);
        //Generate the cipher using pass:
        Cipher cipher = FileSecurityUtils.makeCipher(pass, true);

        FileInputStream inStream = new FileInputStream(inFile);

        int blockSize = 8;

        //Figure out how many bytes are padded
        int paddedCount = blockSize - ((int) inFile.length() % blockSize);

        //Figure out full size including padding
        int padded = (int) inFile.length() + paddedCount;

        decData = new byte[padded];


        inStream.read(decData);

        inStream.close();

        //Write out padding bytes as per PKCS5 algorithm
        for (int i = (int) inFile.length(); i < padded; ++i) {
            decData[i] = (byte) paddedCount;
        }

        //Encrypt the file data:
        encData = cipher.doFinal(decData);


        Log.d("LS STEP: ", "1");
        File file = new File(destinationPath);
        Log.d("LS STEP: ", "2");
        FileOutputStream outStream = new FileOutputStream(file);
        Log.d("LS STEP: ", "3");
        outStream.write(encData);
        Log.d("LS STEP: ", "4");
        outStream.close();
        Log.d("LS STEP: ", "5");

        String encryptedFile = file.toString();
        File encFile = new File(encryptedFile);
        MessageDigest digest = MessageDigest.getInstance("MD5");
        String md5HashEncryptedFile = CheckSumMD5.checksum(digest, encFile);
        Log.d("FileCryptoDeletionError", "MD5 EnfFile" + md5HashEncryptedFile);

        return md5HashEncryptedFile;
    }


    /**
     * Decrypts one file to a second file using a key derived from a passphrase:
     **/
    public static void decryptFile(Context ctx, String sourcePath, String destinationPath, String pass)
            throws GeneralSecurityException, IOException, IllegalBlockSizeException, BadPaddingException {
        byte[] encData;
        byte[] decData;
        File inFile = new File(sourcePath);

        if (inFile == null){
            Toast.makeText(ctx,
                    "Could not find the file!",
                    Toast.LENGTH_LONG)
                    .show();
        }

        //Generate the cipher using pass:
        Cipher cipher = FileSecurityUtils.makeCipher(pass, false);

        //Read in the file:
        FileInputStream inStream = new FileInputStream(inFile);
        encData = new byte[(int) inFile.length()];
        inStream.read(encData);
        inStream.close();
        //Decrypt the file data:
        decData = cipher.doFinal(encData);

        //Figure out how much padding to remove
        int padCount = (int) decData[decData.length - 1];

        if (padCount >= 1 && padCount <= 8) {
            decData = Arrays.copyOfRange(decData, 0, decData.length - padCount);
        }

        FileOutputStream target = new FileOutputStream(new File(destinationPath));
        target.write(decData);
        target.close();
    }
}