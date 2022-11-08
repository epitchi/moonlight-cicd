package in.oneplay.backend;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

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

import in.oneplay.BuildConfig;
import in.oneplay.LimeLog;
import in.oneplay.OneplayApp;
import in.oneplay.nvstream.http.GfeHttpResponseException;
import in.oneplay.preferences.PreferenceConfiguration;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OneplayApi {
    public static int connectionTimeout = 3000;
    public static int readTimeout = 5000;

    private static volatile OneplayApi instance;

    // Print URL and content to logcat on debug builds
    private static final boolean verbose = BuildConfig.DEBUG;

    private static final String userAgent = BuildConfig.USER_AGENT;
    private static final HttpUrl baseStartVmUrl = new HttpUrl.Builder()
            .scheme(BuildConfig.CONNECTION_SCHEME)
            .host(BuildConfig.API_DOMAIN)
            .encodedPath(BuildConfig.API_START_VM_ENDPOINT)
            .build();
    private static final HttpUrl baseServerInfoUrl = new HttpUrl.Builder()
            .scheme(BuildConfig.CONNECTION_SCHEME)
            .host(BuildConfig.API_DOMAIN)
            .encodedPath(BuildConfig.API_GET_SESSION_ENDPOINT)
            .build();
    private static final HttpUrl baseStopVmUrl = new HttpUrl.Builder()
            .scheme(BuildConfig.CONNECTION_SCHEME)
            .host(BuildConfig.DOMAIN)
            .encodedPath(BuildConfig.APP_QUIT_LINK_PATH)
            .build();
    private static final HttpUrl baseEventsUrl = new HttpUrl.Builder()
            .scheme(BuildConfig.CONNECTION_SCHEME)
            .host(BuildConfig.API_DOMAIN)
            .encodedPath(BuildConfig.API_EVENTS_ENDPOINT)
            .build();

    private final OkHttpClient httpClient;

    private UserSession session;
    private Integer pinPort;

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
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                .build();
    }

    public UserSession connectTo(Uri uri) throws IOException, JSONException {
        HttpUrl serverInfoUrl = baseServerInfoUrl.newBuilder()
                .addEncodedPathSegment(uri.getQueryParameter("payload"))
                .build();
        String serverInfo = openHttpConnectionPostToString(serverInfoUrl);

        this.session = UserSession.formJsonString(serverInfo);
        Integer pinPort = this.session.getConfig().getPortDetails().getPinPort();
        this.pinPort = (pinPort != null) ? pinPort : DEFAULT_PIN_PORT;

        if (!unpairAll()) {
            throw new IOException("Unable to unpair clients.");
        }

        return this.session;
    }

    public boolean setPin(String key) throws IOException, JSONException {
        if (session == null || pinPort == null) {
            return false;
        }

        HttpUrl pinRequestUrl = new HttpUrl.Builder()
                .scheme(BuildConfig.CONNECTION_SCHEME)
                .host(session.getHostAddress())
                .port(pinPort)
                .addEncodedPathSegments("api/pin")
                .build();

        String body = new JSONObject().put("pin", key).toString();
        String response = openHttpConnectionPostToString(pinRequestUrl, body);

        return (new JSONObject(response).getBoolean("status"));
    }

    public String startVm(String serverAddress) throws IOException, JSONException {
        HttpUrl startVmUrl = baseStartVmUrl.newBuilder()
                .addEncodedQueryParameter("vm_ip", serverAddress)
                .build();
        String response = openHttpConnectionPostToString(startVmUrl);

        String sessionSignature = "";
        JSONObject responseData = new JSONObject(response).getJSONObject("data");
        sessionSignature = responseData.getString("session_signature");

        return sessionSignature;
    }

    public boolean stopVm(String sessionKey) throws IOException {
        HttpUrl stopVmUrl = baseStopVmUrl.newBuilder()
                .addEncodedQueryParameter("session_id", sessionKey)
                .addEncodedQueryParameter("source", "android_app_" + BuildConfig.SHORT_VERSION_NAME)
                .build();
        openHttpConnectionPostToString(stopVmUrl);

        return true;
    }

    public boolean unpairAll() throws IOException, JSONException {
        if (session == null || pinPort == null) {
            return false;
        }

        HttpUrl unpairAllUrl = new HttpUrl.Builder()
                .scheme(BuildConfig.CONNECTION_SCHEME)
                .host(session.getHostAddress())
                .port(pinPort)
                .addEncodedPathSegments("api/clients/unpair")
                .build();

        String response = openHttpConnectionPostToString(unpairAllUrl);

        return new JSONObject(response).getBoolean("status");
    }

    public void registerEvent(String text) throws IOException, JSONException {
        String userId = session != null && session.getUserId() != null ? session.getUserId() : "";
        String sessionKey = session != null && session.getKey() != null ? session.getKey() : "";
        String gameId = session != null && session.getGameId() != null ? session.getGameId() : "";
        String hostAddress = session != null && session.getHostAddress() != null ? session.getHostAddress() : "";

        String body = new JSONObject()
                .put("user_id", userId)
                .put("error", text)
                .put("token", sessionKey)
                .put("device", ("android " +
                        Build.VERSION.RELEASE + " " +
                        Build.MANUFACTURER + " " +
                        Build.MODEL).replace(' ', '_'))
                .put("version", BuildConfig.SHORT_VERSION_NAME)
                .put("game_id", gameId)
                .put("vm_ip", hostAddress)
                .toString();

        openHttpConnectionPostToString(baseEventsUrl, body);
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

    private ResponseBody openHttpConnectionPost(HttpUrl baseUrl, String requestBodyString) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl)
                .header("User-Agent", userAgent)
                .post(RequestBody.create(
                        requestBodyString,
                        MediaType.get("application/json; charset=utf-8")))
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
            throw new FileNotFoundException(baseUrl.toString());
        }
        else {
            throw new GfeHttpResponseException(response.code(), response.message());
        }
    }

    private String openHttpConnectionPostToString(HttpUrl baseUrl) throws IOException {
        return openHttpConnectionPostToString(baseUrl, "");
    }

    private String openHttpConnectionPostToString(HttpUrl baseUrl, String requestBodyString) throws IOException {
        try {
            if (verbose) {
                LimeLog.info("Requesting URL: "+baseUrl+"\nBody: "+requestBodyString);
            }

            ResponseBody resp = openHttpConnectionPost(baseUrl, requestBodyString);
            String respString = resp.string();
            resp.close();

            if (verbose) {
                LimeLog.info(baseUrl+" -> "+respString);
            }

            return respString;
        } catch (IOException e) {
            LimeLog.severe(e);

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
