package com.vishnu.quickgoorder.crypto;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DESCore {

    // DES uses a 56-bit key (8 bytes, with the least significant bit of each byte as the parity bit)
    private static final String DES_ALGORITHM = "DES";
    private static final String TRANSFORMATION = "DES/CBC/PKCS5Padding";
    private static final String CHARSET_NAME = "UTF-8";
    private static final byte[] DES_KEY = "firebase".getBytes();
    private static final byte[] IV = "esaberif".getBytes();


    public static String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKey key = new SecretKeySpec(DES_KEY, DES_ALGORITHM);
        IvParameterSpec iv = new IvParameterSpec(IV);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        byte[] encryptedData = cipher.doFinal(data.getBytes(CHARSET_NAME));
        return Base64.encodeToString(encryptedData, Base64.URL_SAFE | Base64.NO_WRAP);
    }

    public static String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKey key = new SecretKeySpec(DES_KEY, DES_ALGORITHM);
        IvParameterSpec iv = new IvParameterSpec(IV);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
        byte[] decryptedData = cipher.doFinal(decodedData);
        return new String(decryptedData, CHARSET_NAME);
    }
}
