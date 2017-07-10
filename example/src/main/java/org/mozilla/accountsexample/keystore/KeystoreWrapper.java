package org.mozilla.accountsexample.keystore;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.KeyPairGeneratorSpec;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import org.mozilla.accountsexample.AppGlobals;
import org.mozilla.accountsexample.MainActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;


public class KeystoreWrapper {
    static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    static final String PREF_KEY_SYMMETRIC_KEY = "key-b64-ciphered";
    static final String PREF_KEY_INIT_VECTOR = "initvector-b64";
    static final String CERT_ALIAS = "moz-lockbox-alias";
    static final String AES_CIPHER = "AES";
    static final String PREF_KEY_PIN_CODE = "pincode-b64-ciphered";

    KeyStore keyStore;
    final SecretKey symmetricKey;
    final Context context;
    byte cryptoInitVector[] = new byte[16];// IV for AES is 16 bytes

    public KeystoreWrapper(Context context) {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        try {
            keyStore.load(null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        this.context = context;
        pkCryptoInit();

        String secretKeyAsBase64CipherText = AppGlobals.prefs(context).getString(PREF_KEY_SYMMETRIC_KEY, null);
        if (secretKeyAsBase64CipherText == null) {
            // assume nothing in prefs, create and save all the bits needed
            symmetricKey = makeSymmetricKey();

            SecureRandom random = new SecureRandom();
            random.nextBytes(cryptoInitVector);

            secretKeyAsBase64CipherText = pkCryptoEncryptToString(symmetricKey.getEncoded());

            SharedPreferences.Editor edit = AppGlobals.prefs(context).edit();
            edit.putString(PREF_KEY_INIT_VECTOR, Base64.encodeToString(cryptoInitVector, Base64.DEFAULT));
            edit.putString(PREF_KEY_SYMMETRIC_KEY, secretKeyAsBase64CipherText);
            edit.commit();
        } else {
            String initVectBase64 = AppGlobals.prefs(context).getString(PREF_KEY_INIT_VECTOR, "");
            cryptoInitVector = Base64.decode(initVectBase64, Base64.DEFAULT);
            byte[] key = pkCryptoDecryptString(secretKeyAsBase64CipherText);
            symmetricKey = new SecretKeySpec(key, 0, key.length, AES_CIPHER);
        }
    }

    public String getPINCode() {
        String pin = AppGlobals.prefs(context).getString(PREF_KEY_PIN_CODE, null);
        if (TextUtils.isEmpty(pin)) {
            return null;
        }
        return symmetricDecrypt(pin);
    }

    public void setPINCode(String code) {
        SharedPreferences.Editor editor = AppGlobals.prefs(context).edit();
        editor.putString(PREF_KEY_PIN_CODE, symmetricEncrypt(code));
        editor.commit();
    }

    public SecretKey makeSymmetricKey() {
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance(AES_CIPHER);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }
        keyGenerator.init(128); // TODO: check avail lengths
        return keyGenerator.generateKey();
    }

    // returns base64 cipher text
    public String pkCryptoEncryptToString(byte[] bytes) {
        try {
            Cipher cipher = pkCryptoMakeCipher(Cipher.ENCRYPT_MODE);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            cipherOutputStream.write(bytes);
            cipherOutputStream.close();
            byte[] vals = outputStream.toByteArray();
            String encrypted = Base64.encodeToString(vals, Base64.DEFAULT);
            return encrypted;
        } catch (Exception e) {
            Toast.makeText(context, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
            Log.e("", Log.getStackTraceString(e));
        }
        return null;
    }

    Cipher pkCryptoMakeCipher(int cryptoMode) {
        try {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(CERT_ALIAS, null);
            java.security.Key rsaKey = (cryptoMode == Cipher.DECRYPT_MODE) ?
                    privateKeyEntry.getPrivateKey() :
                    privateKeyEntry.getCertificate().getPublicKey();

//KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;
//            Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//            output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(cryptoMode, rsaKey);
            return cipher;
        } catch (Exception e) {
            Log.e("", Log.getStackTraceString(e));
            return null;
        }
    }

    public byte[] pkCryptoDecryptString(String cipherTextBase64) {
        try {
            Cipher cipher = pkCryptoMakeCipher(Cipher.DECRYPT_MODE);
            final byte[] cipherBytes = Base64.decode(cipherTextBase64, Base64.DEFAULT);
            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(cipherBytes), cipher);
            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte)nextByte);
            }
            byte[] bytes = new byte[values.size()];
            for(int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }
            //String finalText = new String(bytes, StandardCharsets.UTF_8);
            return bytes;
        } catch (Exception e) {
            Toast.makeText(context, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
            Log.e("", Log.getStackTraceString(e));
        }
        return null;
    }

    private void pkCryptoInit() {
        try {
            // Create new key if needed
            if (!keyStore.containsAlias(CERT_ALIAS)) {
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 1);
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(CERT_ALIAS)
                        .setSubject(new X500Principal("CN=Mozilla Lockbox, O=Mozilla Lockbox Team"))
                        .setSerialNumber(BigInteger.ONE)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                generator.initialize(spec);

                KeyPair keyPair = generator.generateKeyPair();
            }
        } catch (Exception e) {
            //Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
            Log.e("", Log.getStackTraceString(e));
        }
    }

    Cipher symmetricCryptoMakeCipher(int cryptoMode) {
        SecretKeySpec keySpec = new SecretKeySpec(symmetricKey.getEncoded(), AES_CIPHER);
        IvParameterSpec ivspec = new IvParameterSpec(cryptoInitVector);

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cipher.init(cryptoMode, symmetricKey, ivspec);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return cipher;
    }

    public String symmetricDecrypt(String cipherTextBase64) {
        Cipher cipher = symmetricCryptoMakeCipher(Cipher.DECRYPT_MODE);
        byte[] cipherBytes = Base64.decode(cipherTextBase64, Base64.DEFAULT);
        byte[] decryptedBytes = new byte[0];
        try {
            decryptedBytes = cipher.doFinal(cipherBytes);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // returns as base64
    public String symmetricEncrypt(String plainText) {
        Cipher cipher = symmetricCryptoMakeCipher(Cipher.ENCRYPT_MODE);
        byte[] cipherBytes = new byte[0];
        try {
            cipherBytes = cipher.doFinal(plainText.getBytes());
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        String cipherText = Base64.encodeToString(cipherBytes, Base64.DEFAULT);
        return cipherText;
    }

    public void symmetricTest() {
        try {
            Cipher cipher = symmetricCryptoMakeCipher(Cipher.ENCRYPT_MODE);
            String clearText = "I am an Employee";
            byte[] clearTextBytes = clearText.getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = cipher.doFinal(clearTextBytes);
            String cipherText = new String(cipherBytes, StandardCharsets.UTF_8);

            Cipher cipher2 = symmetricCryptoMakeCipher(Cipher.DECRYPT_MODE);
            byte[] decryptedBytes = cipher2.doFinal(cipherBytes);
            String decryptedText = new String(decryptedBytes, StandardCharsets.UTF_8);

            System.out.println("Before encryption: " + clearText);
            System.out.println("After encryption: " + cipherText);
            System.out.println("After decryption: " + decryptedText);

            assert(clearText.equals(decryptedText));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
