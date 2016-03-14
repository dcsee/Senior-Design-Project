package atlantis.com.atlantis.encryption;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.utils.HashingUtils;
import atlantis.com.model.impls.SecureRandomGenerator;

/**
 * Created by jvronsky on 2/10/15.
 *
 * The LocalEncryption class uses AES/CBC/PKCDS5PADDING, in order to be able to encrypt and
 * decrypt we have to use the same IV (Initialization Vector) which will be saved in
 * SharedPreferences. The IV is a random string of 16 bytes used to help encrypt the text
 * diagram can be found here {http://en.wikipedia.org/wiki/File:CBC_encryption.svg}
 */
public class LocalEncryption {

    // Algorithm used to generate the key.
    private static final String ENCRYPTION_KEY_ALGORITHM = "PBKDF2WithHmacSHA1";
    // Encryption algorithm used to create encryption key.
    private static final String KEY_SPEC_ALGORITHM = "AES";
    // Encrpytion algorithm used.
    private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5PADDING";
    // The length of pin required by user.
    public static final int USER_PIN_LENGTH = 4;
    // Valid pin regex expression
    private static final String VALID_PIN_REGEX_EXPRESSION = "[0-9]+";

    // Length of the encryption key generated.
    private static final int KEY_LENGTH =  256;
    // Large number of iterations, larger the number the more secure and take much longer.
    private static final int ITERATION = 2153;
    // Tag for logger.
    private static final String TAG = "LOCAL_ENCRYPTION";
    // Default values for the SharedPrefrences values.
    private static final String DEFAULT_NOT_FOUND_SALT = "default";
    private static final String DEFAULT_NOT_FOUND_KEY = "default";
    private static final String DEFAULT_HASHED_VALUE = "default";
    private static final String DEFAULT_IV_VALUE = "default";
    private static final boolean DEFAULT_IS_LOCKED = true;

    // Size of salts.
    private static final int SALT_SIZE_IN_BYTES = 8;
    // Random key that will be store for pin checking purposes.
    private static final int RANDOM_KEY_SIZE = 32;
    private static final float ENCRYPTION_ALGORITHM_BLOCK_SIZE = 16;
    private static final int IV_SIZE = 16;

    // Singleton for this class
    private static LocalEncryption mInstance = null;

    // Key generated to use for encryption.
    private final byte[] encryptionKey;

    // Application context.
    private final Context context;

    private LocalEncryption(Context context, String pin) {
        this.context = context;
        encryptionKey = generateEncryptKeyFromPin(pin);
    }

    public static int lengthOfEncryptedData(int lengthOfOriginalData) {
        return (int) (((int) Math.ceil((float)(lengthOfOriginalData + 1)/ ENCRYPTION_ALGORITHM_BLOCK_SIZE))
                        * ENCRYPTION_ALGORITHM_BLOCK_SIZE);
    }

    /**
     * Get instance of the localEncryption singleton
     * @param context of the calling applications
     * @return instance of LocalEncryption singleton
     * @throws Exception
     */
    public static LocalEncryption getInstance(Context context) throws NotAuthenticatedException, PINNotCreatedException {
        if(!hasPINBeenSetup(context)) {
            throw new PINNotCreatedException("No Encryption PIN was created");
        }
        if(mInstance == null) {
            Log.d(TAG, "Instance is null");
            throw new NotAuthenticatedException("User did not authenticate");
        }
        return mInstance;
    }

    public static boolean isUserPinValid(String pin) {
        return (pin.length() == USER_PIN_LENGTH) && pin.matches(VALID_PIN_REGEX_EXPRESSION);
    }

    /**
     * Logs into the application. Compare the pin and returns an instance of log in is successful.
     * @param context of the applications
     * @param pin pin to use to log in
     * @return instance of the local encryption
     */
    public static boolean login(Context context, String pin) throws PINCreationFailedException {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(
                        context.getString(R.string.pin_key_file), Context.MODE_PRIVATE);
        String randomPin = sharedPreferences.getString(context.getString(R.string.pin_key),
                DEFAULT_NOT_FOUND_KEY);
        try {
            LocalEncryption localEncryption = new LocalEncryption(context, pin);
            byte[] savedEncryptedRandomPin = Base64.decode(
                    sharedPreferences.getString(
                            context.getString(
                                    R.string.hashed_pin_key), DEFAULT_HASHED_VALUE),
                    Base64.DEFAULT);
            byte[] hashedEncryptedRandomPin = HashingUtils.generateCheckSum(
                    localEncryption.encrypt(randomPin, getIv(context)));
            // If log in was successful.
            if(Arrays.equals(savedEncryptedRandomPin, hashedEncryptedRandomPin)) {
                setIsLocked(context, false);
                mInstance = localEncryption;
                return true;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if pin was created and stored in ShaerdPrefrences
     * @param context of the application
     * @return true if key was created
     */
    public static boolean doesPinExist(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(
                        context.getString(R.string.pin_key_file), Context.MODE_PRIVATE);
        String s = sharedPreferences.getString(context.getString(R.string.hashed_pin_key), DEFAULT_HASHED_VALUE);
        return !s.equals(DEFAULT_HASHED_VALUE);
    }

    /**
     * Create of change a key.
     * @param context of the application
     * @param pin to replace or create
     * @return true if creation was successful
     */
    public static boolean createPinKey(Context context, String pin) throws PINCreationFailedException {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(
                        context.getString(R.string.pin_key_file), Context.MODE_PRIVATE);
        saveIv(context, new SecureRandomGenerator().getBytes(IV_SIZE));
        byte[] randomPin = new SecureRandomGenerator().getBytes(RANDOM_KEY_SIZE);
        String randomPinEncoded = Base64.encodeToString(randomPin, Base64.DEFAULT);
        LocalEncryption localEncryption = new LocalEncryption(context, pin);
        try {
            // Generate checksum from the encoded random bytes.
            String hashedRandomPin = Base64.encodeToString(
                    HashingUtils.generateCheckSum(localEncryption.encrypt(randomPinEncoded,
                            getIv(context))),
                    Base64.DEFAULT);
            // Save random key and its encrypted checksum.
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(context.getString(R.string.pin_key), randomPinEncoded);
            editor.putString(context.getString(R.string.hashed_pin_key), hashedRandomPin);
            boolean commitSuccessful = editor.commit();
            // Wait for successful commit.
            while(!commitSuccessful) {
                commitSuccessful = editor.commit();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        login(context, pin);
        return true;
    }

    /**
     * Saves the IV used for the first block encrypted
     * @param context of the application
     * @param iv to save
     */
    private static void saveIv(Context context, byte[] iv) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.iv_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.iv_key),
                Base64.encodeToString(iv, Base64.DEFAULT));
        while(!editor.commit());
    }

    /**
     * Get the iv saved in the system
     * @param context of the application
     * @return the IV that was generated
     */
    private static byte[] getIv(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.iv_file), Context.MODE_PRIVATE);
        String receivedIv = sharedPreferences.getString(context.getString(R.string.iv_key),
                DEFAULT_IV_VALUE);
        if(receivedIv.equals(DEFAULT_IV_VALUE)) {
            return null;
        } else {
            return Base64.decode(receivedIv, Base64.DEFAULT);
        }
    }
    /**
     * Check the isLocked variable.
     * @param context of the application
     * @param isLocked whether to put the state as locked or unlocked
     */
    public static void setIsLocked(Context context, boolean isLocked) {
        if(isLocked) {
            mInstance = null;
        }
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(
                        context.getString(R.string.is_app_lock_status_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(context.getString(R.string.is_app_lock_status), isLocked);
        boolean commitSuccessful = editor.commit();
        while(!commitSuccessful) {
            commitSuccessful = editor.commit();
        }
    }

    /**
     * Get the application state from SharedPrefrences.
     * @param context of the application
     * @return whether app is locked or not
     */
    public static boolean getIsLocked(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(
                        context.getString(R.string.is_app_lock_status_file), Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(context.getString(R.string.is_app_lock_status),
                DEFAULT_IS_LOCKED);
    }

    /**
     * Encrypt a string with the key that was generated for the class.
     * @param s string to encrypt
     * @return encrypted bytes
     */
    public byte[] encrypt(String s) throws PINCreationFailedException {
        return encrypt(Base64.decode(s, Base64.DEFAULT));
    }

    /**
     * Encrypt an array of bytes with the key that was generated from pin.
     * @param data to be encrypted
     * @return encrypted bytes
     */
    public byte[] encrypt(byte[] data) throws PINCreationFailedException {
        return encrypt(data, getIv(context));
    }

    public byte[] encryptWithPIN(byte[] data, String pin) throws PINCreationFailedException {
        byte[] key = generateEncryptKeyFromPin(pin);
        return encrypt(data, getIv(context), key);
    }

    /**
     * Decrypt an array of bytes with the key that was generated with the pin.
     * @param data encrypted data
     * @return decrypted data
     */
    public byte[] decrypt(byte[] data) throws PINCreationFailedException {
        return decrypt(data, getIv(context));
    }

    private byte[] encrypt(String data, byte[] iv) throws PINCreationFailedException {
        return encrypt(Base64.decode(data, Base64.DEFAULT), iv);
    }

    /**
     * Encrypt an array of bytes with the key that was generated from pin with iv.
     * @param data to be encrypted
     * @param iv to encode data with
     * @return encrypted bytes
     */
    private byte[] encrypt(byte[] data, byte[] iv) throws PINCreationFailedException {
        byte[] encrypted;
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, KEY_SPEC_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
            encrypted = cipher.doFinal(data);
        } catch (Exception e) {
            throw new PINCreationFailedException(e.getMessage());
        }
        return encrypted;
    }

    /**
     * Encrypt an array of bytes with the key that was generated from pin with iv.
     * @param data to be encrypted
     * @param iv to encode data with
     * @param key key to use for encryption
     * @return encrypted bytes
     */
    private byte[] encrypt(byte[] data, byte[] iv, byte[] key) throws PINCreationFailedException {
        byte[] encrypted;
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, KEY_SPEC_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
            encrypted = cipher.doFinal(data);
        } catch (Exception e) {
            throw new PINCreationFailedException(e.getMessage());
        }
        return encrypted;
    }

    /**
     * Decrypt an array of bytes with the key that was generated with the pin with iv.
     * @param data encrypted data
     * @param iv used to encode data
     * @return decrypted data
     */
    private byte[] decrypt(byte[] data, byte[] iv) throws PINCreationFailedException {
        byte[] decrypted;
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, KEY_SPEC_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
            decrypted = cipher.doFinal(data);
        } catch (Exception e) {
            throw new PINCreationFailedException(e.getMessage());
        }
        return decrypted;
    }

    /**
     * Get the user generated salt. If no salt find function generate a random salt.
     * @return bytes of salt
     */
    private byte[] getUserSalt() {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(
                        context.getString(R.string.salt_file), Context.MODE_PRIVATE);
        String salt = sharedPreferences.getString(context.getString(R.string.salt_key),
                DEFAULT_NOT_FOUND_SALT);
        // Create salt.
        if(salt.equals(DEFAULT_NOT_FOUND_SALT)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            byte[] generatedSalt = new SecureRandomGenerator().getBytes(SALT_SIZE_IN_BYTES);
            salt = Base64.encodeToString(generatedSalt, Base64.DEFAULT);
            editor.putString(context.getString(R.string.salt_key), salt);
            editor.commit();
            return generatedSalt;
        } else {
            return Base64.decode(salt, Base64.DEFAULT);
        }
    }

    /**
     * Generates secure key to use for AES encryption from a pin.
     * @param pin to transform to secret key
     */
    private byte[] generateEncryptKeyFromPin(String pin) {
        byte[] salt = getUserSalt();
        try {
            SecretKeyFactory secretKeyFactory =
                    SecretKeyFactory.getInstance(ENCRYPTION_KEY_ALGORITHM);
            KeySpec keySpec = new PBEKeySpec(pin.toCharArray(), salt, ITERATION, KEY_LENGTH);
            return secretKeyFactory.generateSecret(keySpec).getEncoded();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.d(TAG, "No algorithm found");
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Check if hasPINBeenSetup exist and available.
     * @param context
     * @return
     */
    private static boolean hasPINBeenSetup(Context context) {
        return (!getIsLocked(context) || doesPINKeyExist(context));
    }

    /**
     * Checks if PIN already created, if so an instance can be returned.
     * @param context
     * @return
     */
    private static boolean doesPINKeyExist(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(
                        context.getString(R.string.pin_key_file), Context.MODE_PRIVATE);
        String pinKey = sharedPreferences.getString(
                context.getString(R.string.pin_key), DEFAULT_NOT_FOUND_KEY);
        return !pinKey.equals(DEFAULT_NOT_FOUND_KEY);
    }
}
