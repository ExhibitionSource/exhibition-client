package net.minecraft.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import exhibition.util.security.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CryptManager {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Generate a new shared secret AES key from a secure random source
     */
    public static SecretKey createNewSharedKey() {
        try {
            KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
            keygenerator.init(128);
            return keygenerator.generateKey();
        } catch (NoSuchAlgorithmException nosuchalgorithmexception) {
            throw new Error(nosuchalgorithmexception);
        }
    }

    /**
     * Generates RSA KeyPair
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keypairgenerator = KeyPairGenerator.getInstance("RSA");
            keypairgenerator.initialize(1024);
            return keypairgenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException nosuchalgorithmexception) {
            nosuchalgorithmexception.printStackTrace();
            LOGGER.error("Key pair generation failed!");
            return null;
        }
    }

    /**
     * Compute a serverId hash for use by sendSessionRequest()
     */
    public static byte[] getServerIdHash(String serverId, PublicKey publicKey, SecretKey secretKey) {
        try {
            return digestOperation("SHA-1", new byte[][]{serverId.getBytes("ISO_8859_1"), secretKey.getEncoded(), publicKey.getEncoded()});
        } catch (UnsupportedEncodingException unsupportedencodingexception) {
            unsupportedencodingexception.printStackTrace();
            return null;
        }
    }

    /**
     * Compute a message digest on arbitrary byte[] data
     */
    private static byte[] digestOperation(String algorithm, byte[]... data) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(algorithm);

            for (byte[] abyte : data) {
                messagedigest.update(abyte);
            }

            return messagedigest.digest();
        } catch (NoSuchAlgorithmException nosuchalgorithmexception) {
            nosuchalgorithmexception.printStackTrace();
            return null;
        }
    }

    /**
     * Create a new PublicKey from encoded X.509 data
     */
    public static PublicKey decodePublicKey(byte[] encodedKey) {
        try {
            EncodedKeySpec encodedkeyspec = new X509EncodedKeySpec(encodedKey);
            KeyFactory keyfactory = KeyFactory.getInstance("RSA");
            return keyfactory.generatePublic(encodedkeyspec);
        } catch (NoSuchAlgorithmException var3) {
            ;
        } catch (InvalidKeySpecException var4) {
            ;
        }

        LOGGER.error("Public key reconstitute failed!");
        return null;
    }

    /**
     * Decrypt shared secret AES key using RSA private key
     */
    public static SecretKey decryptSharedKey(PrivateKey p_75887_0_, byte[] p_75887_1_) {
        return new SecretKeySpec(CryptManager.decryptData(p_75887_0_, p_75887_1_), "AES");
    }


    public static SecretKeySpec getDecrypt() {
        try {
            byte[] secret = Crypto.getUserKeySetOLD(16);
            Class keySpec = Class.forName("javax.crypto.spec.SecretKeySpec");

            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");

            theUnsafe.setAccessible(true);

            Object unsafe = theUnsafe.get(null);

            Object newInstance = unsafeClass.getDeclaredMethod("allocateInstance", Class.class).invoke(unsafe, keySpec);

            byte[] key = new byte[secret.length];
            System.arraycopy(secret, 0, key, 0, secret.length);

            ReflectionUtil.setField(keySpec.getDeclaredField("key"), newInstance, key);
            ReflectionUtil.setField(keySpec.getDeclaredField("algorithm"), newInstance, "AES");

            return (SecretKeySpec) newInstance;
        } catch (Exception ignore) {
        }
        return null;    //new SecretKeySpec(secret, 0, secret.length, "AES")
    }

    public static SecretKeySpec getSecret() {
        byte[] secret = Crypto.getUserKeyOLD(16);
        return new SecretKeySpec(secret, 0, secret.length, "AES");
    }

    public static SecretKeySpec getDecryptNew() {
        byte[] secret = Crypto.getUserKeySet(16);
        return new SecretKeySpec(secret, 0, secret.length, "AES");
    }

    public static SecretKeySpec getSecretNew() {
        try {
            byte[] secret = Crypto.getUserKey(16);
            Class keySpec = Class.forName("javax.crypto.spec.SecretKeySpec");

            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");

            theUnsafe.setAccessible(true);

            Object unsafe = theUnsafe.get(null);

            Object newInstance = unsafeClass.getDeclaredMethod("allocateInstance", Class.class).invoke(unsafe, keySpec);

            byte[] key = new byte[secret.length];
            System.arraycopy(secret, 0, key, 0, secret.length);

            ReflectionUtil.setField(keySpec.getDeclaredField("key"), newInstance, key);
            ReflectionUtil.setField(keySpec.getDeclaredField("algorithm"), newInstance, "AES");

            return (SecretKeySpec) newInstance;
        } catch (Exception ignore) {
        }
        return null;    //new SecretKeySpec(secret, 0, secret.length, "AES")
    }

    /**
     * Encrypt byte[] data with RSA public key
     */
    public static byte[] encryptData(Key key, byte[] data) {
        return cipherOperation(1, key, data);
    }

    /**
     * Decrypt byte[] data with RSA private key
     */
    public static byte[] decryptData(Key key, byte[] data) {
        return cipherOperation(2, key, data);
    }

    /**
     * Encrypt or decrypt byte[] data using the specified key
     */
    private static byte[] cipherOperation(int opMode, Key key, byte[] data) {
        try {
            return createTheCipherInstance(opMode, key.getAlgorithm(), key).doFinal(data);
        } catch (IllegalBlockSizeException illegalblocksizeexception) {
            illegalblocksizeexception.printStackTrace();
        } catch (BadPaddingException badpaddingexception) {
            badpaddingexception.printStackTrace();
        }

        LOGGER.error("Cipher data failed!");
        return null;
    }

    /**
     * Creates the Cipher Instance.
     */
    private static Cipher createTheCipherInstance(int opMode, String transformation, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(opMode, key);
            return cipher;
        } catch (InvalidKeyException invalidkeyexception) {
            invalidkeyexception.printStackTrace();
        } catch (NoSuchAlgorithmException nosuchalgorithmexception) {
            nosuchalgorithmexception.printStackTrace();
        } catch (NoSuchPaddingException nosuchpaddingexception) {
            nosuchpaddingexception.printStackTrace();
        }

        LOGGER.error("Cipher creation failed!");
        return null;
    }

    /**
     * Creates an Cipher instance using the AES/CFB8/NoPadding algorithm. Used for protocol encryption.
     */
    public static Cipher createNetCipherInstance(int opMode, Key key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            cipher.init(opMode, (Key) key, (AlgorithmParameterSpec) (new IvParameterSpec(key.getEncoded())));
            return cipher;
        } catch (GeneralSecurityException generalsecurityexception) {
            throw new RuntimeException(generalsecurityexception);
        }
    }
}
