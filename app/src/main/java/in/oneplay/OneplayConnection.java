package in.oneplay;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import in.oneplay.backend.OneplayApi;
import in.oneplay.backend.OneplayServerHelper;
import in.oneplay.binding.PlatformBinding;
import in.oneplay.binding.crypto.AndroidCryptoProvider;
import in.oneplay.computers.ComputerManagerService;
import in.oneplay.grid.assets.DiskAssetLoader;
import in.oneplay.nvstream.http.ComputerDetails;
import in.oneplay.nvstream.http.NvApp;
import in.oneplay.nvstream.http.NvHTTP;
import in.oneplay.nvstream.http.PairingManager;
import in.oneplay.nvstream.jni.MoonBridge;
import in.oneplay.preferences.ServerPreferenceConfiguration;
import in.oneplay.utils.ServerHelper;
import in.oneplay.utils.UiHelper;

import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OneplayConnection extends Activity {
    private WebView webView;
    private ProgressBar progress;
    private Intent currentIntent;
    private boolean isResumed = false;
    private boolean isFirstStart = true;
    private ComputerManagerService.ApplistPoller poller;
    private volatile ComputerDetails computer;
    private volatile ComputerManagerService.ComputerManagerBinder managerBinder;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            final ComputerManagerService.ComputerManagerBinder localBinder =
                    ((ComputerManagerService.ComputerManagerBinder) binder);

            // Wait in a separate thread to avoid stalling the UI
            new Thread() {
                @Override
                public void run() {
                    // Wait for the binder to be ready
                    localBinder.waitForReady();

                    // Now make the binder visible
                    managerBinder = localBinder;

                    // Force a keypair to be generated early to avoid discovery delays
                    new AndroidCryptoProvider(OneplayConnection.this).getClientCertificate();

                    connectToComputer();
                }
            }.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            managerBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentIntent = getIntent();

        UiHelper.setLocale(this);

        setContentView(R.layout.activity_oneplay_connection);

        UiHelper.notifyNewRootView(this);

        // Set default preferences if we've never been run
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        webView = findViewById(R.id.webview);
        progress = findViewById(R.id.progress);

        initializeWebView();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        currentIntent = intent;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isResumed = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isFirstStart && Intent.ACTION_VIEW.equals(currentIntent.getAction()) && currentIntent.getData() != null) {
            isFirstStart = false;

            webView.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);

            if (managerBinder == null) {
                // Bind to the ComputerManager service
                bindService(new Intent(OneplayConnection.this,
                        ComputerManagerService.class), serviceConnection, Service.BIND_AUTO_CREATE);
            } else {
                connectToComputer();
            }
        } else {
            URI welcomeLink = null;
            try {
                welcomeLink = new URI(
                        OneplayApi.SSL_CONNECTION_TYPE,
                        getString(R.string.oneplay_domain),
                        getString(R.string.oneplay_app_welcome_link_path),
                        getString(R.string.oneplay_app_welcome_link_query),
                        null
                );
            } catch (URISyntaxException e) {
                LimeLog.severe(e.getMessage());
            }

            webView.setVisibility(View.VISIBLE);
            if (!isResumed && welcomeLink != null) {
                webView.loadUrl(welcomeLink.toString());
            }
            progress.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (managerBinder != null) {
            removeComputer();
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (OneplayServerHelper.ONEPLAY_GAME_REQUEST_CODE == requestCode) {
            // Send quit to Oneplay API
            new Thread(() -> {
                try {
                    OneplayApi.getInstance(this).doQuit();
                } catch (IOException ignored) {}
            }).start();
            // Send quit to NV API
            closeApps(computer);
            // Back to user account
            processingError(null, true);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initializeWebView() {
        if (!BuildConfig.DEBUG) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
        }
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.getSettings().setUserAgentString(getString(R.string.oneplay_user_agent_base) + BuildConfig.VERSION_NAME);
        webView.setWebViewClient(new WebViewClient() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri.getScheme().equals(OneplayApi.SSL_CONNECTION_TYPE) &&
                        uri.getHost().equals(getString(R.string.oneplay_domain)) &&
                        uri.getPath().equals(getString(R.string.oneplay_app_launch_link_path))) {
                    isFirstStart = true;
                    Intent newIntent = new Intent(
                            Intent.ACTION_VIEW,
                            uri,
                            OneplayConnection.this,
                            OneplayConnection.class
                    );
                    startActivity(newIntent);

                    return true;
                }
                return false;
            }

            // For old devices
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                if (uri.getScheme().equals(OneplayApi.SSL_CONNECTION_TYPE) &&
                        uri.getHost().equals(getString(R.string.oneplay_domain)) &&
                        uri.getPath().equals(getString(R.string.oneplay_app_launch_link_path))) {
                    isFirstStart = true;
                    Intent newIntent = new Intent(
                            Intent.ACTION_VIEW,
                            uri,
                            OneplayConnection.this,
                            OneplayConnection.class
                    );
                    startActivity(newIntent);

                    return true;
                }
                return false;
            }
        });
    }

    private void connectToComputer() {
        new Thread(() -> {
            Uri uri = currentIntent.getData();
            try {
                OneplayApi client = OneplayApi.getInstance(this);
                client.connectTo(uri);
                ServerPreferenceConfiguration.savePreferences(this, client.getClientConfig());
                doAddPc(client.getHostAddress());
                if (computer != null) {
                    doPair(client, computer);
                }
            } catch (IOException e) {
                processingError(e.getMessage(), false);
            }
        }).start();
    }

    private void closeApps(ComputerDetails computer) {
        if (computer == null) {
            return;
        }

        String rawAppList = computer.rawAppList;

        if (rawAppList == null) {
            return;
        }

        List<NvApp> apps = getAppList(rawAppList);
        for (NvApp app : apps) {
            OneplayServerHelper.doQuit(OneplayConnection.this, computer, app, managerBinder, () -> {});
        }
    }

    private List<NvApp> getAppList(String rawAppList) {
        List<NvApp> appList = new ArrayList<>();

        if (rawAppList != null) {
            try {
                appList = NvHTTP.getAppListByReader(new StringReader(rawAppList));
            } catch (XmlPullParserException | IOException ignored) { }
        }

        return appList;
    }

    private void removeComputer() {
        if (computer == null) {
            return;
        }

        managerBinder.removeComputer(computer);

        DiskAssetLoader diskAssetLoader = new DiskAssetLoader(OneplayConnection.this);
        diskAssetLoader.deleteAssetsForComputer(computer.uuid);

        // Delete hidden games preference value
        getSharedPreferences(AppView.HIDDEN_APPS_PREF_FILENAME, MODE_PRIVATE)
                .edit()
                .remove(computer.uuid)
                .apply();

        computer = null;
    }

    private boolean isWrongSubnetSiteLocalAddress(String address) {
        try {
            InetAddress targetAddress = InetAddress.getByName(address);
            if (!(targetAddress instanceof Inet4Address) || !targetAddress.isSiteLocalAddress()) {
                return false;
            }

            // We have a site-local address. Look for a matching local interface.
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                    if (!(addr.getAddress() instanceof Inet4Address) || !addr.getAddress().isSiteLocalAddress()) {
                        // Skip non-site-local or non-IPv4 addresses
                        continue;
                    }

                    byte[] targetAddrBytes = targetAddress.getAddress();
                    byte[] ifaceAddrBytes = addr.getAddress().getAddress();

                    // Compare prefix to ensure it's the same
                    boolean addressMatches = true;
                    for (int i = 0; i < addr.getNetworkPrefixLength(); i++) {
                        if ((ifaceAddrBytes[i / 8] & (1 << (i % 8))) != (targetAddrBytes[i / 8] & (1 << (i % 8)))) {
                            addressMatches = false;
                            break;
                        }
                    }

                    if (addressMatches) {
                        return false;
                    }
                }
            }

            // Couldn't find a matching interface
            return true;
        } catch (Exception e) {
            // Catch all exceptions because some broken Android devices
            // will throw an NPE from inside getNetworkInterfaces().
            e.printStackTrace();
            return false;
        }
    }

    private void doAddPc(String host) {
        boolean wrongSiteLocal = false;
        boolean success;
        int portTestResult;

        computer = new ComputerDetails();
        try {
            computer.manualAddress = host;
            success = managerBinder.addComputerBlocking(computer);
        } catch (IllegalArgumentException e) {
            // This can be thrown from OkHttp if the host fails to canonicalize to a valid name.
            // https://github.com/square/okhttp/blob/okhttp_27/okhttp/src/main/java/com/squareup/okhttp/HttpUrl.java#L705
            e.printStackTrace();
            success = false;
        }
        if (!success) {
            wrongSiteLocal = isWrongSubnetSiteLocalAddress(host);
        }
        if (!success && !wrongSiteLocal) {
            // Run the test before dismissing the spinner because it can take a few seconds.
            portTestResult = MoonBridge.testClientConnectivity(ServerHelper.CONNECTION_TEST_SERVER, 443,
                    MoonBridge.ML_PORT_FLAG_TCP_47984 | MoonBridge.ML_PORT_FLAG_TCP_47989);
        } else {
            // Don't bother with the test if we succeeded or the IP address was bogus
            portTestResult = MoonBridge.ML_TEST_RESULT_INCONCLUSIVE;
        }

        if (wrongSiteLocal) {
            LimeLog.warning(getResources().getString(R.string.conn_error_title) + ": " + getResources().getString(R.string.addpc_wrong_sitelocal));
        } else if (!success) {
            String dialogText;
            if (portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0) {
                dialogText = getResources().getString(R.string.nettest_text_blocked);
            } else {
                dialogText = getResources().getString(R.string.addpc_fail);
            }
            LimeLog.warning(getResources().getString(R.string.conn_error_title) + ": " + dialogText);
        } else {
            return;
        }

        removeComputer();
    }

    private void doPair(final OneplayApi client, final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.OFFLINE ||
                ServerHelper.getCurrentAddressFromComputer(computer) == null) {
            processingError(getResources().getString(R.string.pair_pc_offline), true);
            return;
        }
        if (computer.runningGameId != 0) {
            processingError(getResources().getString(R.string.pair_pc_ingame), true);
            return;
        }
        if (managerBinder == null) {
            processingError(getResources().getString(R.string.error_manager_not_running), true);
            return;
        }

        if (BuildConfig.DEBUG) {
            LimeLog.info(getResources().getString(R.string.pairing));
        }

        NvHTTP httpConn;
        String message;
        boolean success = false;
        try {
            httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                    managerBinder.getUniqueId(),
                    computer.serverCert,
                    PlatformBinding.getCryptoProvider(OneplayConnection.this));
            if (httpConn.getPairState() == PairingManager.PairState.PAIRED) {
                // Don't display any toast, but open the app list
                message = null;
                success = true;
            } else {
                final String pinStr = client.getSessionKey();
                httpConn.addInterceptor(client.getInterceptor());
                PairingManager pm = httpConn.getPairingManager();

                PairingManager.PairState pairState = pm.pair(httpConn.getServerInfo(), pinStr);
                if (pairState == PairingManager.PairState.PIN_WRONG) {
                    message = getResources().getString(R.string.pair_incorrect_pin);
                } else if (pairState == PairingManager.PairState.FAILED) {
                    message = getResources().getString(R.string.pair_fail);
                } else if (pairState == PairingManager.PairState.ALREADY_IN_PROGRESS) {
                    message = getResources().getString(R.string.pair_already_in_progress);
                } else if (pairState == PairingManager.PairState.PAIRED) {
                    // Just navigate to the app view without displaying a toast
                    message = null;
                    success = true;

                    // Pin this certificate for later HTTPS use
                    managerBinder.getComputer(computer.uuid).serverCert = pm.getPairedCert();

                    // Invalidate reachability information after pairing to force
                    // a refresh before reading pair state again
                    managerBinder.invalidateStateForComputer(computer.uuid);
                } else {
                    // Should be no other values
                    message = null;
                }
            }
        } catch (UnknownHostException e) {
            message = getResources().getString(R.string.error_unknown_host);
        } catch (FileNotFoundException e) {
            message = getResources().getString(R.string.error_404);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
            message = e.getMessage();
            if (message == null)
                message = "Empty IOException";
        }

        if (success) {
            startComputerUpdates();
        } else {
            processingError(message, true);
        }
    }

    private void startComputerUpdates() {
        // Don't start polling if we're not bound
        if (managerBinder == null) {
            return;
        }

        managerBinder.startPolling(details -> {

            // Don't care about other computers
            if (!details.uuid.equalsIgnoreCase(computer.uuid)) {
                return;
            }

            if (details.state == ComputerDetails.State.OFFLINE) {
                // The PC is unreachable now
                stopComputerUpdates();
                processingError(getString(R.string.lost_connection), true);
                return;
            }

            // Close immediately if the PC is no longer paired
            if (details.state == ComputerDetails.State.ONLINE && details.pairState != PairingManager.PairState.PAIRED) {
                stopComputerUpdates();
                processingError(getString(R.string.scut_not_paired), true);
                return;
            }

            if (details.rawAppList != null) {
                stopComputerUpdates();

                NvApp app = getAppList(details.rawAppList).get(0); // Always get first app (Desktop)
                OneplayServerHelper.doStart(this, app, computer, managerBinder);
            }
        });

        if (poller == null) {
            poller = managerBinder.createAppListPoller(computer);
        }
        poller.start();
    }

    private void stopComputerUpdates() {
        if (poller != null) {
            poller.stop();
        }

        if (managerBinder != null) {
            managerBinder.stopPolling();
        }
    }

    private void processingError(String message, boolean isRemoveComputer) {
        if (message != null) {
            LimeLog.severe(message);
        }

        if (isRemoveComputer) {
            removeComputer();
        }

        startActivity(new Intent(Intent.ACTION_MAIN, null, OneplayConnection.this, OneplayConnection.class));
    }
}