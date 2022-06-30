package in.oneplay;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
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

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;

import in.oneplay.backend.OneplayApi;
import in.oneplay.binding.PlatformBinding;
import in.oneplay.binding.crypto.AndroidCryptoProvider;
import in.oneplay.computers.ComputerManagerService;
import in.oneplay.nvstream.http.ComputerDetails;
import in.oneplay.nvstream.http.NvApp;
import in.oneplay.nvstream.http.NvHTTP;
import in.oneplay.nvstream.http.PairingManager;
import in.oneplay.nvstream.jni.MoonBridge;
import in.oneplay.preferences.OneplayPreferenceConfiguration;
import in.oneplay.utils.ServerHelper;
import in.oneplay.utils.UiHelper;

public class PcView extends Activity {
    private static final int UPDATES_REQUEST_CODE = 1;

    private WebView webView;
    private ProgressBar progress;
    private Intent currentIntent;
    private FirebaseAnalytics mFirebaseAnalytics;
    private boolean isResumed = false;
    private boolean isFirstStart = true;
    private NvApp currentApp;
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
                    new AndroidCryptoProvider(PcView.this).getClientCertificate();

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

        setContentView(R.layout.activity_pc_view);

        UiHelper.notifyNewRootView(this);

        // Allow floating expanded PiP overlays while browsing PCs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setShouldDockBigOverlays(false);
        }

        // Fix nav bar overlapping
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        // Set default preferences if we've never been run
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        webView = findViewById(R.id.webview);
        progress = findViewById(R.id.progress);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

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

        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(
                        appUpdateInfo -> {
                            if (appUpdateInfo.updateAvailability()
                                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                                // If an in-app update is already running, resume the update.
                                try {
                                    appUpdateManager.startUpdateFlowForResult(
                                            appUpdateInfo,
                                            AppUpdateType.IMMEDIATE,
                                            this,
                                            UPDATES_REQUEST_CODE);
                                } catch (IntentSender.SendIntentException e) {
                                    LimeLog.severe("An error occurred during the update: " + e.getMessage());
                                }
                            }
                        });


        if (isFirstStart && Intent.ACTION_VIEW.equals(currentIntent.getAction()) && currentIntent.getData() != null) {
            isFirstStart = false;

            webView.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);

            if (managerBinder == null) {
                // Bind to the ComputerManager service
                bindService(new Intent(PcView.this,
                        ComputerManagerService.class), serviceConnection, Service.BIND_AUTO_CREATE);
            } else {
                connectToComputer();
            }
        } else {
            URI welcomeLink = null;
            try {
                welcomeLink = new URI(
                        OneplayApi.SSL_CONNECTION_TYPE,
                        OneplayApi.ONEPLAY_DOMAIN,
                        BuildConfig.ONEPLAY_APP_WELCOME_LINK_PATH,
                        BuildConfig.ONEPLAY_APP_WELCOME_LINK_QUERY,
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
        if (requestCode == ServerHelper.ONEPLAY_GAME_REQUEST_CODE  &&
                resultCode == ServerHelper.ONEPLAY_GAME_RESULT_REFRESH_ACTIVITY) {
            startComputerUpdates();
        } else if (requestCode == UPDATES_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                LimeLog.severe("Update flow failed! Result code: " + resultCode);
                checkUpdates();
            }
        } else {
            currentApp = null;
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
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.getSettings().setUserAgentString(OneplayApi.ONEPLAY_USER_AGENT_BASE + BuildConfig.VERSION_NAME);
        webView.setWebViewClient(new WebViewClient() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri.getScheme().equals(OneplayApi.SSL_CONNECTION_TYPE) &&
                        uri.getHost().equals(OneplayApi.ONEPLAY_DOMAIN) &&
                        uri.getPath().equals(OneplayApi.ONEPLAY_APP_LAUNCH_LINK_PATH)) {
                    isFirstStart = true;
                    Intent newIntent = new Intent(
                            Intent.ACTION_VIEW,
                            uri,
                            PcView.this,
                            PcView.class
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
                        uri.getHost().equals(OneplayApi.ONEPLAY_DOMAIN) &&
                        uri.getPath().equals(OneplayApi.ONEPLAY_APP_LAUNCH_LINK_PATH)) {
                    isFirstStart = true;
                    Intent newIntent = new Intent(
                            Intent.ACTION_VIEW,
                            uri,
                            PcView.this,
                            PcView.class
                    );
                    startActivity(newIntent);

                    return true;
                }
                return false;
            }
        });
    }

    private void checkUpdates() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                // Request the update.
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            // Pass the intent that is returned by 'getAppUpdateInfo()'.
                            appUpdateInfo,
                            // The current activity making the update request.
                            this,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                                    .setAllowAssetPackDeletion(true)
                                    .build(),
                            // Include a request code to later monitor this update request.
                            UPDATES_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    LimeLog.severe("An error occurred during the update: " + e.getMessage());
                }
            }
        });
    }


    private void connectToComputer() {
        new Thread(() -> {
            Uri uri = currentIntent.getData();
            try {
                OneplayApi client = OneplayApi.getInstance();
                client.connectTo(uri);
                OneplayPreferenceConfiguration.savePreferences(this, client.getClientConfig());
                doAddPc(client.getHostAddress());
            } catch (IOException e) {
                processingError(e.getMessage(), false);
            }
        }).start();
    }

    private void removeComputer() {
        if (computer == null) {
            return;
        }

        managerBinder.removeComputer(computer);

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

        ComputerDetails fakeDetails = new ComputerDetails();
        try {
            fakeDetails.manualAddress = host;
            success = managerBinder.addComputerBlocking(fakeDetails);
        } catch (IllegalArgumentException | InterruptedException e) {
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

        String message;

        if (wrongSiteLocal) {
            message = getResources().getString(R.string.conn_error_title) + ": " + getResources().getString(R.string.addpc_wrong_sitelocal);
        } else if (!success) {
            String dialogText;
            if (portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0) {
                dialogText = getResources().getString(R.string.nettest_text_blocked);
            } else {
                dialogText = getResources().getString(R.string.addpc_fail);
            }
            message = getResources().getString(R.string.conn_error_title) + ": " + dialogText;
        } else {
            managerBinder.startPolling(details -> {
                if (details.state == ComputerDetails.State.ONLINE) {
                    computer = details;
                    doPair();
                }
                runningPolling = true;
            });
            return;
        }

        processingError(message, true);
    }

    private boolean runningPolling;

    private void doPair() {
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

        new Thread(() -> {
            NvHTTP httpConn;
            String message;
            try {
                // Stop updates and wait while pairing
                stopComputerUpdates();

                httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                        managerBinder.getUniqueId(),
                        computer.serverCert,
                        PlatformBinding.getCryptoProvider(PcView.this));
                OneplayApi client = OneplayApi.getInstance();
                final String pinStr = client.getSessionKey();
                httpConn.addInterceptor(client.getInterceptor((result) -> {
                    if (!result) runOnUiThread(() -> processingError("Session key not accepted", true));
                }));
                PairingManager pm = httpConn.getPairingManager();

                PairingManager.PairState pairState = pm.pair(httpConn.getServerInfo(), pinStr);
                if (pairState == PairingManager.PairState.PIN_WRONG) {
                    message = getResources().getString(R.string.pair_incorrect_pin);
                } else if (pairState == PairingManager.PairState.FAILED) {
                    message = getResources().getString(R.string.pair_fail);
                } else if (pairState == PairingManager.PairState.ALREADY_IN_PROGRESS) {
                    message = getResources().getString(R.string.pair_already_in_progress);
                } else if (pairState == PairingManager.PairState.PAIRED) {
                    // Pin this certificate for later HTTPS use
                    managerBinder.getComputer(computer.uuid).serverCert = pm.getPairedCert();

                    // Invalidate reachability information after pairing to force
                    // a refresh before reading pair state again
                    managerBinder.invalidateStateForComputer(computer.uuid);

                    runOnUiThread(this::startComputerUpdates);
                    return;
                } else {
                    // Should be no other values
                    message = null;
                }
            } catch (UnknownHostException e) {
                message = getResources().getString(R.string.error_unknown_host);
            } catch (FileNotFoundException e) {
                message = getResources().getString(R.string.error_404);
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
                message = e.getMessage();
            }

            final String finalMessage = message;
            runOnUiThread(() -> processingError(finalMessage, true));
        }).start();

    }

    private void startComputerUpdates() {
        managerBinder.startPolling(details -> {
            if (details.pairState == PairingManager.PairState.PAIRED) {
                new Thread(() -> {
                    stopComputerUpdates();
                    try {
                        NvHTTP httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(details),
                                managerBinder.getUniqueId(),
                                details.serverCert,
                                PlatformBinding.getCryptoProvider(PcView.this));

                        currentApp = httpConn.getAppByName("Desktop"); // Always get first app (Desktop)

                        if (currentApp != null) {
                            runOnUiThread(() -> ServerHelper.doStart(this, currentApp, details, managerBinder));
                        } else {
                            processingError(getString(R.string.applist_refresh_error_msg), true);
                        }
                    } catch (XmlPullParserException | IOException e) {
                        processingError(getString(R.string.applist_refresh_error_msg) + ": " + e.getMessage(), true);
                    }
                }).start();
            }
        });
        runningPolling = true;
    }

    private void stopComputerUpdates() {
        if (managerBinder != null) {
            if (!runningPolling) {
                return;
            }

            managerBinder.stopPolling();

            managerBinder.waitForPollingStopped();

            runningPolling = false;
        }
    }

    private void processingError(String message, boolean isRemoveComputer) {
        if (message != null) {
            LimeLog.severe(message);
        }

        if (isRemoveComputer) {
            removeComputer();
        }

        startActivity(new Intent(Intent.ACTION_MAIN, null, PcView.this, PcView.class));
    }
}