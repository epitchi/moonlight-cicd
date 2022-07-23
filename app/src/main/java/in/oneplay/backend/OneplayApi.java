package in.oneplay.backend;

import android.net.Uri;
import android.os.Build;

import in.oneplay.BuildConfig;
import in.oneplay.LimeLog;
import in.oneplay.nvstream.http.GfeHttpResponseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
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
    public static int CONNECTION_TIMEOUT = 3000;
    public static int READ_TIMEOUT = 5000;

    public static final int REQUEST_PORT = 47990;

    private static volatile OneplayApi instance;

    // Print URL and content to logcat on debug builds
    private static final boolean verbose = BuildConfig.DEBUG;

    private static final String userAgent = BuildConfig.USER_AGENT;
    private static final String startVmUrl = new Uri.Builder()
            .scheme(BuildConfig.CONNECTION_SCHEME)
            .authority(BuildConfig.API_DOMAIN)
            .encodedPath(BuildConfig.API_START_VM_ENDPOINT)
            .encodedQuery("vm_ip=")
            .build()
            .toString();
    private static final String serverInfoUrl = new Uri.Builder()
            .scheme(BuildConfig.CONNECTION_SCHEME)
            .authority(BuildConfig.API_DOMAIN)
            .encodedPath(BuildConfig.API_GET_SESSION_ENDPOINT)
            .build()
            .toString();
    private static final String quitUrl = new Uri.Builder()
            .scheme(BuildConfig.CONNECTION_SCHEME)
            .authority(BuildConfig.DOMAIN)
            .encodedPath(BuildConfig.APP_QUIT_LINK_PATH)
            .build()
            .toString();
    private static final String eventsUrl = new Uri.Builder()
            .scheme(BuildConfig.CONNECTION_SCHEME)
            .authority(BuildConfig.API_DOMAIN)
            .encodedPath(BuildConfig.API_EVENTS_ENDPOINT)
            .build()
            .toString();

    private final OkHttpClient httpClient;
    private String sessionKey;
    private String userId;
    private String hostAddress;
    private String gameId;

    private ClientConfig clientConfig;

    public interface PinAuthorizationCallback {
        void onResult(boolean result);
    }

    public static OneplayApi getInstance() {
        OneplayApi localInstance = instance;
        if (localInstance == null) {
            synchronized (OneplayApi.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new OneplayApi();
                }
            }
        }

        return localInstance;
    }

    private OneplayApi() {
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

    public String getGameId() {
        return gameId;
    }

    public void connectTo(Uri uri) throws IOException {
        loadServerInfo(uri.getQueryParameter("payload"));

        if (!unpairAll()) {
            throw new IOException("Unable to unpair clients.");
        }
    }

    public String startVm(String serverAddress) throws IOException {
        String response = openHttpConnectionPostToString(startVmUrl + serverAddress);

        String sessionSignature = "";
        try {
            JSONObject responseData = new JSONObject(response).getJSONObject("data");
            sessionSignature = responseData.getString("session_signature");

        } catch (JSONException e) {
            LimeLog.severe(e);
        }

        return sessionSignature;
    }

    public Interceptor getInterceptor(PinAuthorizationCallback c) {
        return (Interceptor.Chain chain) -> {
            Request request = chain.request();
            if ("GET".equals(request.method()) &&
                    hostAddress.equals(request.url().host()) &&
                    "/pair".equals(request.url().encodedPath())) {

                new Thread(() -> c.onResult(setHostSessionKey(sessionKey))).start();
            }

            return chain.proceed(request);
        };
    }

    private void loadServerInfo(String sessionKey) throws IOException {
        String serverInfo = openHttpConnectionPostToString(serverInfoUrl + sessionKey);

        String serverAddress = "";
        String hostSessionKey = "";
        String userId = "";
        String gameId = "";
        ClientConfig clientConfig = null;
        try {
            JSONObject serverData = new JSONObject(serverInfo).getJSONObject("data");
            serverAddress = serverData.getJSONObject("server_details").getString("server_ip");
            hostSessionKey = serverData.getString("host_session_key");
            userId = serverData.getJSONObject("user_details").getString("user_id");
            gameId = serverData.getJSONObject("game_details").getString("id");
            clientConfig = getClientConfig(serverData);
        } catch (JSONException e) {
            LimeLog.severe(e);
        }

        this.hostAddress = serverAddress;
        this.sessionKey = hostSessionKey;
        this.userId = userId;
        this.gameId = gameId;
        this.clientConfig = clientConfig;
    }

    private ClientConfig getClientConfig(JSONObject data) throws JSONException {
        JSONObject otherDetailsData = data.getJSONObject("other_details");
        JSONObject advanceDetailsData = otherDetailsData.getJSONObject("advance_details");

        ClientConfig.AdvanceDetails advanceDetails = new ClientConfig.AdvanceDetails();
//        advanceDetails.setAbsoluteMouseMode(Utils.getBoolean(advanceDetailsData, "absolute_mouse_mode"));
        advanceDetails.setAbsoluteTouchMode(Utils.getBoolean(advanceDetailsData, "absolute_touch_mode"));
//        advanceDetails.setBackgroundGamepad(Utils.getBoolean(advanceDetailsData, "background_gamepad"));
//        advanceDetails.setFramePacing(Utils.getBoolean(advanceDetailsData, "frame_pacing"));
        advanceDetails.setGameOptimizations(Utils.getBoolean(advanceDetailsData, "game_optimizations"));
        advanceDetails.setMultiControl(Utils.getBoolean(advanceDetailsData, "multi_color")); //typo "multi_color" it means "multi controller"
//        advanceDetails.setMuteOnFocusLoss(Utils.getBoolean(advanceDetailsData, "mute_on_focus_loss"));
//        advanceDetails.setPacketSize(Utils.getInt(advanceDetailsData, "packet_size"));
        advanceDetails.setPlayAudioOnHost(Utils.getBoolean(advanceDetailsData, "play_audio_on_host"));
//        advanceDetails.setQuitAppAfter(Utils.getBoolean(advanceDetailsData, "quit_app_after"));
        advanceDetails.setReverseScrollDirection(Utils.getBoolean(advanceDetailsData, "reverse_scroll_direction"));
        advanceDetails.setSwapFaceButtons(Utils.getBoolean(advanceDetailsData, "swap_face_buttons"));
//        advanceDetails.setSwapMouseButtons( Utils.getBoolean(advanceDetailsData, "swap_mouse_buttons"));

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setAudioType(Utils.getString(otherDetailsData, "audio_type"));
        clientConfig.setBitrateKbps(Utils.getInt(otherDetailsData, "bitrate_kbps"));
//        clientConfig.setCaptureSysKeys(Utils.getBoolean(otherDetailsData, "capture_sys_keys"));
        clientConfig.setControllerMouseEmulationEnabled(Utils.getBoolean(otherDetailsData, "controller_mouse_emulation"));
        clientConfig.setControllerUsbDriverSupportEnabled(Utils.getBoolean(otherDetailsData, "controller_usb_driver_support"));
        clientConfig.setFrameDropDisabled(Utils.getBoolean(otherDetailsData, "disable_frame_drop"));
        clientConfig.setHdrEnabled(Utils.getBoolean(otherDetailsData, "enable_hdr"));
        clientConfig.setPerfOverlayEnabled(Utils.getBoolean(otherDetailsData, "enable_perf_overlay"));
        clientConfig.setPipEnabled(Utils.getBoolean(otherDetailsData, "enable_pip"));
        clientConfig.setPostStreamToastEnabled(Utils.getBoolean(otherDetailsData, "enable_post_stream_toast"));
        clientConfig.setGameFps(Utils.getInt(otherDetailsData, "game_fps"));
//        clientConfig.setVsyncEnabled(Utils.getBoolean(otherDetailsData, "is_vsync_enabled"));
//        clientConfig.setMaxBitrateKbps(Utils.getInt(otherDetailsData, "max_bitrate_kbps"));
//        clientConfig.setMaxFps(Utils.getInt(otherDetailsData, "max_fps"));
//        clientConfig.setMaxResolution(Utils.getString(otherDetailsData, "max_resolution"));
        clientConfig.setMouseNavButtonsEnabled(Utils.getBoolean(otherDetailsData, "mouse_nav_buttons"));
        clientConfig.setOnscreenControlsEnabled(Utils.getBoolean(otherDetailsData, "onscreen_controls"));
        clientConfig.setResolution(Utils.getString(otherDetailsData, "resolution"));
        clientConfig.setStreamCodec(Utils.getString(otherDetailsData, "stream_codec"));
        clientConfig.setUnlockFpsEnabled(Utils.getBoolean(otherDetailsData, "unlock_fps"));
        clientConfig.setVibrateOscEnabled(Utils.getBoolean(otherDetailsData, "vibrate_osc"));
//        clientConfig.setVideoDecoderSelection(Utils.getString(otherDetailsData, "video_decoder_selection"));
        clientConfig.setWindowMode(Utils.getString(otherDetailsData, "window_mode"));
        clientConfig.setAdvanceDetails(advanceDetails);
        return clientConfig;
    }

    private boolean unpairAll() throws IOException {
        String unpairUrl = new Uri.Builder()
                .scheme(BuildConfig.CONNECTION_SCHEME)
                .encodedAuthority(this.hostAddress + ":" + REQUEST_PORT)
                .encodedPath("/api/clients/unpair")
                .build()
                .toString();

        String response = openHttpConnectionPostToString(unpairUrl);
        try {
            if (new JSONObject(response).getBoolean("status")) {
                return true;
            }
        } catch (JSONException ignore) { }

        return false;
    }

    public void doQuit() throws IOException {
        openHttpConnectionPostToString(quitUrl + "?" +
                "session_id=" + this.sessionKey + "&" +
                "source=" + "android_app_" + BuildConfig.SHORT_VERSION_NAME);
    }

    public void registerEvent(String text) {
        new Thread(() -> {
            try {
                String body = new JSONObject()
                        .put("user_id", userId != null ? userId : "")
                        .put("error", text)
                        .put("token", sessionKey != null ? sessionKey : "")
                        .put("device", ("android " +
                                Build.VERSION.RELEASE + " " +
                                Build.MANUFACTURER + " " +
                                Build.MODEL).replace(' ', '_'))
                        .put("version", BuildConfig.SHORT_VERSION_NAME)
                        .put("game_id", gameId != null ? gameId : "")
                        .put("vm_ip", hostAddress != null ? hostAddress : "")
                        .toString();

                openHttpConnectionPostToString(eventsUrl, body);

            } catch (JSONException | IOException e) {
                LimeLog.severe(e);
                if (verbose) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean setHostSessionKey(String key) {
        try {
            String pinRequestUrl = new Uri.Builder()
                    .scheme(BuildConfig.CONNECTION_SCHEME)
                    .encodedAuthority(this.hostAddress + ":" + REQUEST_PORT)
                    .encodedPath("/api/pin")
                    .build()
                    .toString();

            // Give time to process request at the server
            Thread.sleep(500);
            String body = new JSONObject().put("pin", key).toString();
            String response = openHttpConnectionPostToString(pinRequestUrl, body);

            return (new JSONObject(response).getBoolean("status"));
        } catch (IOException | JSONException | InterruptedException e) {
            LimeLog.severe(e);
            if (verbose) {
                e.printStackTrace();
            }
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
                .header("User-Agent", userAgent)
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
