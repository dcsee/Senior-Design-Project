package atlantis.com.atlantis.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ricardo on 3/1/15.
 */
public class LogUtils {
    private static final int JSON_INDENT = 4;

    private static final int DEFAULT_MAX_LENGTH_TO_LOG = 100;

    public static void logBytes(String name, byte[] bytes) {
        logBytes(name, bytes, DEFAULT_MAX_LENGTH_TO_LOG);
    }

    public static void logBytes(String name, byte[] bytes, int count) {
        System.out.println(name + "-----");
        int i = 0;
        for(byte b : bytes) {
            System.out.print(b + " ");
            if(i++ > count) break;
        }
        System.out.println("------");
    }

    public static JSONArray bytesToJSON(byte[] bytes) {
        JSONArray result = new JSONArray();
        for(byte b : bytes) {
            result.put(b);
        }
        return result;
    }

    public static void logJSON(String tag, Object object) {
        try {
            if(object instanceof JSONObject) {
                Log.d(tag, ((JSONObject)object).toString(JSON_INDENT));
            } else if(object instanceof JSONArray) {
                Log.d(tag, ((JSONArray) object).toString(JSON_INDENT));
            } else {
                Log.d(tag, object.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
