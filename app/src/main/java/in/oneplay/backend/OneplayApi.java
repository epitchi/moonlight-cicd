package in.oneplay.backend;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import in.oneplay.BuildConfig;
import in.oneplay.LimeLog;
import in.oneplay.R;
import in.oneplay.nvstream.http.GfeHttpResponseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OneplayApi {
    public static final int CONNECTION_TIMEOUT = 10000;
    public static final int READ_TIMEOUT = 15000;

    public static final String SSL_CONNECTION_TYPE = "https";
    public static final int ONEPLAY_PIN_REQUEST_PORT = 47990;

    private static volatile OneplayApi instance;

    // Print URL and content to logcat on debug builds
    private static final boolean verbose = BuildConfig.DEBUG;

    private final OkHttpClient httpClient;
    private String baseServerInfoUrl;
    private String baseQuitUrl;
    private String basePinRequestUrl;
    private String sessionKey;
    private String hostAddress;
    private ClientConfig clientConfig;
    private int gameId;

    public static OneplayApi getInstance(Context context) {
        OneplayApi localInstance = instance;
        if (localInstance == null) {
            synchronized (OneplayApi.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new OneplayApi(context);
                }
            }
        }

        return localInstance;
    }

    private OneplayApi(Context context) {
        try {
            this.baseServerInfoUrl = new URI(
                    context.getString(R.string.oneplay_api_get_session_link)
            ).toString();
            this.baseQuitUrl = new URI(
                    SSL_CONNECTION_TYPE,
                    context.getString(R.string.oneplay_domain),
                    context.getString(R.string.oneplay_app_quit_link_path),
                    null
            ).toString();
        } catch (URISyntaxException ignored) {}


        this.httpClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.MILLISECONDS))
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public int getGameId() {
        return gameId;
    }

    public void connectTo(Uri uri) throws IOException {
        loadServerInfo(uri.getQueryParameter("payload"));

        try {
            this.basePinRequestUrl = new URI(
                    SSL_CONNECTION_TYPE,
                    null,
                    this.hostAddress,
                    ONEPLAY_PIN_REQUEST_PORT,
                    null,
                    null,
                    null
            ).toString();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        unpairAll();
    }

    public Interceptor getInterceptor() {
        return (Interceptor.Chain chain) -> {
            Request request = chain.request();
            if ("GET".equals(request.method()) &&
                    hostAddress.equals(request.url().host()) &&
                    "/pair".equals(request.url().encodedPath())) {

                new Thread(() -> setHostSessionKey(sessionKey)).start();
            }

            return chain.proceed(request);
        };
    }

    private void loadServerInfo(String sessionKey) throws IOException {
        String serverInfo = openHttpConnectionPostToString(
                baseServerInfoUrl + sessionKey);

        String serverAddress = "";
        String hostSessionKey = "";
        ClientConfig clientConfig = null;
        int gameId = -1;
        try {
            JSONObject serverData = new JSONObject(serverInfo).getJSONObject("data");
            serverAddress = serverData.getJSONObject("server_details").getString("server_ip");
            hostSessionKey = serverData.getString("host_session_key");
            clientConfig = getClientConfig(serverData);
            gameId = serverData.getJSONObject("game_details").getInt("id");
        } catch (JSONException e) {
            LimeLog.warning(e.getMessage());
        }

        this.hostAddress = serverAddress;
        this.sessionKey = hostSessionKey;
        this.clientConfig = clientConfig;
        this.gameId = gameId;
    }

    private ClientConfig getClientConfig(JSONObject data) throws JSONException {
        JSONObject otherDetailsData = data.getJSONObject("other_details");
        JSONObject advanceDetailsData = otherDetailsData.getJSONObject("advance_details");

        ClientConfig.AdvanceDetails advanceDetails = new ClientConfig.AdvanceDetails(
                Utils.getBoolean(advanceDetailsData, "absolute_mouse_mode", false),
                Utils.getBoolean(advanceDetailsData, "absolute_touch_mode", true),
                Utils.getBoolean(advanceDetailsData, "background_gamepad", false),
                Utils.getBoolean(advanceDetailsData, "frame_pacing", false),
                Utils.getBoolean(advanceDetailsData, "game_optimizations", true),
                Utils.getBoolean(advanceDetailsData, "multi_color", true),
                Utils.getBoolean(advanceDetailsData, "mute_on_focus_loss", false),
                Utils.getInt(advanceDetailsData, "packet_size", 0),
                Utils.getBoolean(advanceDetailsData, "play_audio_on_host", false),
                Utils.getBoolean(advanceDetailsData, "quit_app_after", true),
                Utils.getBoolean(advanceDetailsData, "reverse_scroll_direction", false),
                Utils.getBoolean(advanceDetailsData, "swap_face_buttons", false),
                Utils.getBoolean(advanceDetailsData, "swap_mouse_buttons", false)
        );

        return new ClientConfig(
                Utils.getString(advanceDetailsData, "audio_type", "stereo"),
                Utils.getLong(advanceDetailsData, "bitrate_kbps", 40000),
                Utils.getBoolean(advanceDetailsData, "capture_sys_keys", false),
                Utils.getInt(advanceDetailsData, "game_fps", 60),
                Utils.getBoolean(advanceDetailsData, "is_vsync_enabled", false),
                Utils.getLong(advanceDetailsData, "max_bitrate_kbps", 40000),
                Utils.getInt(advanceDetailsData, "max_fps", 60),
                Utils.getString(advanceDetailsData, "max_resolution", "3840x2160"),
                Utils.getString(advanceDetailsData, "resolution", "1280x720"),
                Utils.getString(advanceDetailsData, "stream_codec", "auto"),
                Utils.getString(advanceDetailsData, "video_decoder_selection", "auto"),
                Utils.getString(advanceDetailsData, "window_mode", "fullscreen"),
                advanceDetails
        );
    }

    private boolean unpairAll() throws IOException {
        String response = openHttpConnectionPostToString(basePinRequestUrl + "/api/clients/unpair");
        try {
            if (new JSONObject(response).getBoolean("status")) {
                return true;
            }
        } catch (JSONException ignore) { }

        return false;
    }

    public void doQuit() throws IOException {
        openHttpConnectionPostToString(baseQuitUrl + "?" +
                "session_id=" + this.sessionKey + "&" +
                "source=" + "android_app_" + BuildConfig.VERSION_NAME);
    }

    private boolean setHostSessionKey(String key) {
        try {
            // Give time to process request at the server
            Thread.sleep(500);
            String body = new JSONObject().put("pin", key).toString();
            String response = openHttpConnectionPostToString(basePinRequestUrl + "/api/pin", body);

            if (new JSONObject(response).getBoolean("status")) {
                return true;
            }
        } catch (IOException | JSONException | InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    // This hack is Android-specific but we do it on all platforms
    // because it doesn't really matter
    private OkHttpClient performAndroidTlsHackOnePlay(OkHttpClient client) {
        // Doing this each time we create a socket is required
        // to avoid the SSLv3 fallback that causes connection failures
        try {
            X509TrustManager TRUST_ALL_CERTS = new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[] {};
                }
            };


            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[] { TRUST_ALL_CERTS }, new SecureRandom());

            // TLS 1.2 is not enabled by default prior to Android 5.0, so we'll need a custom
            // SSLSocketFactory in order to connect to GFE 3.20.4 which requires TLSv1.2 or later.
            // We don't just always use TLSv12SocketFactory because explicitly specifying TLS versions
            // prevents later TLS versions from being negotiated even if client and server otherwise
            // support them.
            return client.newBuilder()
                    .hostnameVerifier((String hostname, SSLSession session) -> true)
                    .sslSocketFactory(
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                                    sc.getSocketFactory() : new TLSv12SocketFactory(sc), TRUST_ALL_CERTS)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private ResponseBody openHttpConnectionPost(String url, String requestBodyString) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("Connection", "close")
                .post(RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"),
                        requestBodyString))
                .build();
        Response response = performAndroidTlsHackOnePlay(httpClient).newCall(request).execute();

        ResponseBody body = response.body();

        if (response.isSuccessful()) {
            return body;
        }

        // Unsuccessful, so close the response body
        if (body != null) {
            body.close();
        }

        if (response.code() == 404) {
            throw new FileNotFoundException(url);
        }
        else {
            throw new GfeHttpResponseException(response.code(), response.message());
        }
    }

    private String openHttpConnectionPostToString(String url) throws IOException {
        return openHttpConnectionPostToString(url, "");
    }

    private String openHttpConnectionPostToString(String url, String requestBodyString) throws IOException {
        try {
            if (verbose) {
                LimeLog.info("Requesting URL: "+url+"\nBody: "+requestBodyString);
            }

            ResponseBody resp = openHttpConnectionPost(url, requestBodyString);
            String respString = resp.string();
            resp.close();

            if (verbose) {
                LimeLog.info(url+" -> "+respString);
            }

            return respString;
        } catch (IOException e) {
            if (verbose) {
                e.printStackTrace();
            }

            throw e;
        }
    }

    // Based on example code from https://blog.dev-area.net/2015/08/13/android-4-1-enable-tls-1-1-and-tls-1-2/
    private static class TLSv12SocketFactory extends SSLSocketFactory {
        private SSLSocketFactory internalSSLSocketFactory;

        public TLSv12SocketFactory(SSLContext context) {
            internalSSLSocketFactory = context.getSocketFactory();
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return internalSSLSocketFactory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return internalSSLSocketFactory.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket() throws IOException {
            return enableTLSv12OnSocket(internalSSLSocketFactory.createSocket());
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return enableTLSv12OnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return enableTLSv12OnSocket(internalSSLSocketFactory.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return enableTLSv12OnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return enableTLSv12OnSocket(internalSSLSocketFactory.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return enableTLSv12OnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
        }

        private Socket enableTLSv12OnSocket(Socket socket) {
            if (socket instanceof SSLSocket) {
                // TLS 1.2 is not enabled by default prior to Android 5.0. We must enable it
                // explicitly to ensure we can communicate with GFE 3.20.4 which blocks TLS 1.0.
                ((SSLSocket)socket).setEnabledProtocols(new String[] {"TLSv1.2"});
            }
            return socket;
        }
    }
}
