package in.oneplay.backend;

import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class Utils {
    @Nullable
    public static Boolean getBoolean(JSONObject object, String name) throws JSONException {
        return object.has(name) ? object.getBoolean(name) : null;
    }

    @Nullable
    public static Integer getInt(JSONObject object, String name) throws JSONException {
        return object.has(name) ? object.getInt(name) : null;
    }

    @Nullable
    public static String getString(JSONObject object, String name) throws JSONException {
        return object.has(name) ? object.getString(name) : null;
    }
}
