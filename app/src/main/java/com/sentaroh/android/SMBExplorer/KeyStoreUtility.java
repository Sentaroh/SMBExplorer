package com.sentaroh.android.SMBExplorer;
/*
The MIT License (MIT)
Copyright (c) 2011 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Calendar;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.x500.X500Principal;

public class KeyStoreUtility {
    private final static String PROVIDER = "AndroidKeyStore";
    private final static String ALGORITHM_AES = "AES";
    private final static String ALGORITHM_RSA = "RSA";
    private final static String CIPHER_TRANSFORMATION_AES = "AES/CBC/PKCS7Padding";
    private final static String CIPHER_TRANSFORMATION_RSA = "RSA/ECB/PKCS1Padding";
//    final static boolean LOG_MESSAGE_ENABLED=false;

    private final static Logger slf4jLog = LoggerFactory.getLogger(KeyStoreUtility.class);

    final static public String makeSHA256Hash(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.reset();
        md.update(input);
        byte[] digest = md.digest();

        String hexStr = "";
        for (int i = 0; i < digest.length; i++) {
            hexStr +=  Integer.toString( ( digest[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return hexStr;
    }

    final static public String makeSHA256Hash(String input) throws NoSuchAlgorithmException {
        return makeSHA256Hash(input.getBytes());
    }


    static final private String SAVED_KEY_VALUE ="settings_key_store_util_aes_save_key";
    static final private String SAVED_KEY_TYPE="settings_key_store_util_save_key_type";

    public static String getGeneratedPassword(Context context, String alias) throws Exception {
//        KeyStore keyStore = null;
//        keyStore = KeyStore.getInstance(PROVIDER);
//        keyStore.load(null);
//        keyStore.deleteEntry(alias);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key_type=prefs.getString(SAVED_KEY_TYPE, "");
        slf4jLog.info("getGeneratedPassword entered, alias="+alias+", TYPE="+key_type);
        if (key_type.equals("")) {
            if (Build.VERSION.SDK_INT>=23) return getGeneratedPasswordByAes(context, alias);
            else return getGeneratedPasswordByRsa(context, alias);
        } else {
            if (key_type.equals(ALGORITHM_RSA)) return getGeneratedPasswordByRsa(context, alias);
            else return getGeneratedPasswordByAes(context, alias);
        }
    }

    private static String getGeneratedPasswordByRsa(Context context, String alias) throws Exception {
        slf4jLog.info("getGeneratedPasswordByRsa entered, alias="+alias);
        KeyStore keyStore = null;
        byte[] bytes = null;
        String saved_key="";
        String generated_password="";
        keyStore = KeyStore.getInstance(PROVIDER);
        keyStore.load(null);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        saved_key=prefs.getString(SAVED_KEY_VALUE, "");
        if (!keyStore.containsAlias(alias) || saved_key.equals("")) {
            prefs.edit().putString(SAVED_KEY_TYPE, ALGORITHM_RSA).commit();
            if (!keyStore.containsAlias(alias)) {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_RSA, PROVIDER);
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 100);
                KeyPairGeneratorSpec kgs=new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(alias)
                        .setSubject(new X500Principal(String.format("CN=%s", alias)))
                        .setSerialNumber(BigInteger.valueOf(1000000))
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                keyPairGenerator.initialize(kgs);
                keyPairGenerator.generateKeyPair();
            }

            PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();

            generated_password=generateRandomPassword(32, true, true, true, true);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_RSA);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            bytes = cipher.doFinal(generated_password.getBytes("UTF-8"));

            saved_key= Base64.encodeToString(bytes, Base64.NO_WRAP);

            prefs.edit().putString(SAVED_KEY_VALUE, saved_key).commit();
        } else {
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, null);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_RSA);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            bytes = Base64.decode(saved_key, Base64.NO_WRAP);

            byte[] b = cipher.doFinal(bytes);
            generated_password=new String(b);
        }
        slf4jLog.info("getGeneratedPasswordByRsa ended");
        return generated_password;
    }

    private static String getGeneratedPasswordByAes(Context context, String alias) throws Exception {
        slf4jLog.info("getGeneratedPasswordByAes entered, alias="+alias);
        KeyStore keyStore = null;

        String saved_key="";
        String generated_password="";

        keyStore = KeyStore.getInstance(PROVIDER);
        keyStore.load(null);

        Cipher cipher = null;
        cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_AES);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        saved_key=prefs.getString(SAVED_KEY_VALUE, "");
//        saved_key="";
        IvParameterSpec ivParameterSpec = generateInitializationVector(alias);
        if (!keyStore.containsAlias(alias) || saved_key.equals("")) {
            prefs.edit().putString(SAVED_KEY_TYPE, ALGORITHM_AES).commit();
            if (!keyStore.containsAlias(alias)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER);
                keyGenerator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT| KeyProperties.PURPOSE_DECRYPT)
                        .setCertificateSubject(new X500Principal("CN="+alias))
                        .setCertificateSerialNumber(BigInteger.ONE)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setRandomizedEncryptionRequired(false)
                        .build());
                keyGenerator.generateKey();
            }
            SecretKey secretKey = (SecretKey) keyStore.getKey(alias, null);
            generated_password=generateRandomPassword(32, true, true, true, true);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] enc_bytes = cipher.doFinal(generated_password.getBytes("UTF-8"));
            saved_key= Base64.encodeToString(enc_bytes, Base64.NO_WRAP);
            prefs.edit().putString(SAVED_KEY_VALUE, saved_key).commit();
        } else {
            SecretKey secretKey = (SecretKey) keyStore.getKey(alias, null);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] enc_bytes = Base64.decode(saved_key, Base64.NO_WRAP);
            byte[] dec_bytes = cipher.doFinal(enc_bytes);
            generated_password=new String(dec_bytes);
        }
        slf4jLog.info("getGeneratedPasswordByAes exit");

        return generated_password;
    }

    private static IvParameterSpec generateInitializationVector(String seed) throws NoSuchAlgorithmException {
        IvParameterSpec iv=null;
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.reset();
        byte[] buffer = seed.getBytes();
        md.update(buffer);
        byte[] digest = md.digest();
        iv=new IvParameterSpec(digest);
        return iv;
    }

    private static String generateRandomPassword(int max_length, boolean upperCase, boolean lowerCase, boolean numbers, boolean specialCharacters) {
        String upperCaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseChars = "abcdefghijklmnopqrstuvwxyz";
        String numberChars = "0123456789";
        String specialChars = "!@#$%^&*()_-+=<>?/{}~|";
        String allowedChars = "";

        Random rn = new Random();
        StringBuilder sb = new StringBuilder(max_length);

        //this will fulfill the requirements of atleast one character of a type.
        if(upperCase) {
            allowedChars += upperCaseChars;
            sb.append(upperCaseChars.charAt(rn.nextInt(upperCaseChars.length()-1)));
        }

        if(lowerCase) {
            allowedChars += lowerCaseChars;
            sb.append(lowerCaseChars.charAt(rn.nextInt(lowerCaseChars.length()-1)));
        }

        if(numbers) {
            allowedChars += numberChars;
            sb.append(numberChars.charAt(rn.nextInt(numberChars.length()-1)));
        }

        if(specialCharacters) {
            allowedChars += specialChars;
            sb.append(specialChars.charAt(rn.nextInt(specialChars.length()-1)));
        }


        //fill the allowed length from different chars now.
        for(int i=sb.length();i < max_length;++i){
            sb.append(allowedChars.charAt(rn.nextInt(allowedChars.length())));
        }

        return  sb.toString();
    }

}
