package in.oneplay.backend;

import org.json.JSONException;
import org.json.JSONObject;

public class UserSession {
    private final String key;
    private final String userId;
    private final String hostAddress;
    private final String gameId;
    private final ClientConfig config;

    private UserSession(
            String sessionKey,
            String userId,
            String hostAddress,
            String gameId,
            ClientConfig config
    ) {
        this.key = sessionKey;
        this.userId = userId;
        this.hostAddress = hostAddress;
        this.gameId = gameId;
        this.config = config;
    }

    public static UserSession formJsonString(String json) throws JSONException {
        JSONObject serverData = new JSONObject(json).getJSONObject("data");
        return new UserSession(
                serverData.getString("host_session_key"),
                serverData.getJSONObject("user_details").getString("user_id"),
                serverData.getJSONObject("server_details").getString("server_ip"),
                serverData.getJSONObject("game_details").getString("id"),
                ClientConfig.fromJsonString(serverData.toString())
        );
    }

    public String getKey() {
        return key;
    }

    public String getUserId() {
        return userId;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public String getGameId() {
        return gameId;
    }

    public ClientConfig getConfig() {
        return config;
    }
}
