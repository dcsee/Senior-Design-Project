package atlantis.com.atlantis.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

/**
 * Created by jvronsky on 5/18/15.
 * Helps to handle shared prefrences
 */
public class SharedPreferencesHelper {

    private static final int ATTEMPTS_BEFORE_QUITING = 10;
    private static final int TIMEOUT_DELAY = 200;

    SharedPreferences mSharedPreferences;
    Context mContext;

    /**
     * Create shared preferences object to interact with. Default is private mode.
     * @param mContext
     * @param sharedPreferencesFolder
     */
    public SharedPreferencesHelper(Context mContext, String sharedPreferencesFolder) {
        this(mContext, sharedPreferencesFolder, Context.MODE_PRIVATE);
    }

    public SharedPreferencesHelper(Context mContext, String sharedPreferencesFolder, int mode) {
        this.mContext = mContext;
        mSharedPreferences = mContext.getSharedPreferences(sharedPreferencesFolder, mode);
    }

    public void put(String key, int value) throws CouldNotWriteToSharedPreferencesException {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(key, value);
        commit(editor);
    }

    public void put(String key, float value) throws CouldNotWriteToSharedPreferencesException {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putFloat(key, value);
        commit(editor);
    }

    public void put(String key, long value) throws CouldNotWriteToSharedPreferencesException {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(key, value);
        commit(editor);
    }

    public void put(String key, String value) throws CouldNotWriteToSharedPreferencesException {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, value);
        commit(editor);
    }

    public void put(String key, byte[] value) throws CouldNotWriteToSharedPreferencesException {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, Base64.encodeToString(value, Base64.DEFAULT));
        commit(editor);
    }

    public void put(String key,  boolean value) throws CouldNotWriteToSharedPreferencesException {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(key, value);
        commit(editor);
    }


    public int get(String key, int defaultValue) {
        return mSharedPreferences.getInt(key, defaultValue);
    }

    public float get(String key, float defaultValue) {
        return mSharedPreferences.getFloat(key, defaultValue);
    }

    public long get(String key, long defaultValue) {
        return mSharedPreferences.getLong(key, defaultValue);
    }

    public String get(String key, String defaultValue) {
        return mSharedPreferences.getString(key, defaultValue);
    }

    public boolean get(String key, boolean defaultValue) {
        return mSharedPreferences.getBoolean(key, defaultValue);
    }

    public byte[] getBytes(String key, byte[] defaultValue) {
        String encoded = mSharedPreferences.getString(key, null);
        if(encoded != null) {
            return Base64.decode(encoded, Base64.DEFAULT);
        } else {
            return null;
        }
    }

    public void clear() throws CouldNotWriteToSharedPreferencesException {
        commit(mSharedPreferences.edit().clear());
    }

    private boolean commit(SharedPreferences.Editor editor) throws CouldNotWriteToSharedPreferencesException {
        int tries = 0;
        while(tries < ATTEMPTS_BEFORE_QUITING) {
            if(editor.commit()) {
                return true;
            } else {
                tries++;
                try {
                    Thread.sleep(TIMEOUT_DELAY);
                } catch (InterruptedException e) {
                    //If can't sleep not a huge deal try again.
                    e.printStackTrace();
                }
            }
        }
        throw new CouldNotWriteToSharedPreferencesException(
                "Something went terribly wrong while writing to shared-preferences");
    }

    public class CouldNotWriteToSharedPreferencesException extends Throwable {
        public CouldNotWriteToSharedPreferencesException(String s) {
            super(s);
        }
    }
}
