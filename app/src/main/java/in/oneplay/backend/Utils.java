package in.oneplay.backend;

import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
    public static boolean getBoolean(JSONObject object, String name, boolean default_value) throws JSONException {
        return object.has(name) ? object.getBoolean(name) : default_value;
    }

    public static int getInt(JSONObject object, String name, int default_value) throws JSONException {
        return object.has(name) ? object.getInt(name) : default_value;
    }

    public static long getLong(JSONObject object, String name, long default_value) throws JSONException {
        return object.has(name) ? object.getLong(name) : default_value;
    }

    public static String getString(JSONObject object, String name, String default_value) throws JSONException {
        return object.has(name) ? object.getString(name) : default_value;
    }
}
