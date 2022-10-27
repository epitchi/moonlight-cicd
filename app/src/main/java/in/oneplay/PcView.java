package in.oneplay;

import static in.oneplay.Game.EXTRA_HOST;
import static in.oneplay.Game.EXTRA_PC_NAME;
import static in.oneplay.Game.EXTRA_PC_UUID;
import static in.oneplay.Game.EXTRA_SERVER_CERT;
import static in.oneplay.Game.EXTRA_SESSION_KEY;
import static in.oneplay.Game.EXTRA_UNIQUEID;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONException;
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
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import in.oneplay.backend.OneplayApi;
import in.oneplay.backend.UserSession;
import in.oneplay.binding.PlatformBinding;
import in.oneplay.binding.crypto.AndroidCryptoProvider;
import in.oneplay.computers.ComputerManagerService;
import in.oneplay.nvstream.http.ComputerDetails;
import in.oneplay.nvstream.http.NvApp;
import in.oneplay.nvstream.http.NvHTTP;
import in.oneplay.nvstream.http.PairingManager;
import in.oneplay.nvstream.jni.MoonBridge;
import in.oneplay.preferences.OneplayPreferenceConfiguration;
import in.oneplay.ui.DebugDialog;
import in.oneplay.utils.ServerHelper;
import in.oneplay.utils.UiHelper;

public class PcView extends Activity {
    private WebView webView;
    private ProgressBar progress;
    private Intent currentIntent;
    private FirebaseAnalytics mFirebaseAnalytics;
    private boolean backCallbackRegistered;
    private OnBackInvokedCallback onBackInvokedCallback;
    private boolean isResumed = false;
    private boolean isFirstStart = true;
    private UserSession session;
    private volatile ComputerDetails computer;
    private volatile ComputerManagerService.ComputerManagerBinder managerBinder;
    private DebugDialog debugDialog;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedCallback = () -> {
                // We should always be able to go back because we unregister our callback
                // when we can't go back. Nonetheless, we will still check anyway.
                if (webView.canGoBack()) {
                    webView.goBack();
                }
            };
        }

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

        if (BuildConfig.DEBUG) {
            debugDialog = new DebugDialog(this);
        }

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
                                            ServerHelper.UPDATES_REQUEST_CODE);
                                } catch (IntentSender.SendIntentException e) {
                                    LimeLog.severe("An error occurred during the update", e);
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
                        BuildConfig.CONNECTION_SCHEME,
                        BuildConfig.DOMAIN,
                        BuildConfig.APP_WELCOME_LINK_PATH,
                        BuildConfig.APP_WELCOME_LINK_QUERY,
                        null
                );
            } catch (URISyntaxException e) {
                LimeLog.severe(e);
            }

            webView.setVisibility(View.VISIBLE);
            if (!isResumed && welcomeLink != null) {
                webView.loadUrl(welcomeLink.toString());
            }
            progress.setVisibility(View.GONE);

            if (BuildConfig.DEBUG) {
                debugDialog.setOnShowListener(di -> {
                    debugDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        debugDialog.dismiss();
                        progress.setVisibility(View.VISIBLE);

                        new Thread(() -> {
                            try {
                                int connectionTimeout = Integer.parseInt(debugDialog.getConnectionTimeout());
                                int readTimeout = Integer.parseInt(debugDialog.getReadTimeout());

                                NvHTTP.connectionTimeout = connectionTimeout;
                                NvHTTP.readTimeout = readTimeout;

                                OneplayApi connection = OneplayApi.getInstance();
                                String sessionSignature = connection.startVm(debugDialog.getIp());
                                if (!sessionSignature.isEmpty()) {
                                    runOnUiThread(() -> {
                                        isFirstStart = true;
                                        Uri uri = new Uri.Builder()
                                                .scheme(BuildConfig.CONNECTION_SCHEME)
                                                .authority(BuildConfig.DOMAIN)
                                                .path(BuildConfig.APP_LAUNCH_LINK_PATH)
                                                .encodedQuery("payload=" + sessionSignature)
                                                .build();
                                        Intent intent = new Intent(
                                                Intent.ACTION_VIEW,
                                                uri,
                                                PcView.this,
                                                PcView.class);
                                        startActivity(intent);
                                    });
                                } else {
                                    runOnUiThread(() -> {
                                        progress.setVisibility(View.GONE);
                                        debugDialog.show();
                                        Toast.makeText(PcView.this, "Can't get session signature. Try again.", Toast.LENGTH_LONG).show();
                                    });
                                }
                            } catch (IOException | JSONException e) {
                                runOnUiThread(() -> processingError(e, true));
                            }
                        }).start();
                    });
                    debugDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener((v) -> debugDialog.setDefaults());
                });
                debugDialog.show();
            }
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
                resultCode == ServerHelper.ONEPLAY_GAME_RESULT_REFRESH_ACTIVITY &&
                data != null) {
            new Thread(() -> {
                try {
                    startGame(
                            data.getStringExtra(EXTRA_HOST),
                            data.getStringExtra(EXTRA_UNIQUEID),
                            data.getStringExtra(EXTRA_PC_UUID),
                            data.getStringExtra(EXTRA_PC_NAME),
                            (X509Certificate) data.getSerializableExtra(EXTRA_SERVER_CERT),
                            data.getStringExtra(EXTRA_SESSION_KEY)
                    );
                } catch (XmlPullParserException | IOException e) {
                    LimeLog.severe("Unable to restart the game activity.", e);
                }
            }).start();
        } else if (requestCode == ServerHelper.UPDATES_REQUEST_CODE && resultCode != RESULT_OK) {
            LimeLog.severe("Update flow failed! Result code: " + resultCode, new Throwable());
            checkUpdates();
        } else {
            // Back to user account
            removeComputer();
            startActivity(new Intent(Intent.ACTION_MAIN, null, PcView.this, PcView.class));
        }
    }

    @Override
    // NOTE: This will NOT be called on Android 13+ with android:enableOnBackInvokedCallback="true"
    public void onBackPressed() {
        // Back goes back through the WebView history
        // until no more history remains
        if (webView.canGoBack()) {
            webView.goBack();
        }
        else {
            super.onBackPressed();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initializeWebView() {
        UiModeManager modeMgr = (UiModeManager) this.getSystemService(Context.UI_MODE_SERVICE);
        if (modeMgr.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            webView.setInitialScale(1);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.getSettings().setUseWideViewPort(true);
        }
        if (!BuildConfig.DEBUG) {
            webView.getSettings().setLoadsImagesAutomatically(true);
            webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            webView.getSettings().setDomStorageEnabled(true);
        }
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.getSettings().setUserAgentString(BuildConfig.USER_AGENT);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                refreshBackDispatchState();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                refreshBackDispatchState();
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri.getScheme().equals(BuildConfig.CONNECTION_SCHEME) &&
                        uri.getHost().equals(BuildConfig.DOMAIN) &&
                        uri.getPath().equals(BuildConfig.APP_LAUNCH_LINK_PATH)) {
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
                if (uri.getScheme().equals(BuildConfig.CONNECTION_SCHEME) &&
                        uri.getHost().equals(BuildConfig.DOMAIN) &&
                        uri.getPath().equals(BuildConfig.APP_LAUNCH_LINK_PATH)) {
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

    private void refreshBackDispatchState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (webView.canGoBack() && !backCallbackRegistered) {
                getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                        OnBackInvokedDispatcher.PRIORITY_DEFAULT, onBackInvokedCallback);
                backCallbackRegistered = true;
            }
            else if (!webView.canGoBack() && backCallbackRegistered) {
                getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(onBackInvokedCallback);
                backCallbackRegistered = false;
            }
        }
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
                            ServerHelper.UPDATES_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    LimeLog.severe("An error occurred during the update", e);
                }
            }
        });
    }

    private void connectToComputer() {
        new Thread(() -> {
            Uri uri = currentIntent.getData();
            try {
                session = OneplayApi.getInstance().connectTo(uri);
                OneplayPreferenceConfiguration.savePreferences(this, session.getConfig());
                doAddPc();
            } catch (IOException | JSONException e) {
                processingError(e, false);
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
            LimeLog.warning(e);
            return false;
        }
    }

    private void doAddPc() {
        boolean wrongSiteLocal = false;
        boolean success;
        int portTestResult;

        ComputerDetails fakeDetails = new ComputerDetails();
        try {
            fakeDetails.manualAddress = session.getHostAddress();
            success = managerBinder.addComputerBlocking(fakeDetails);
        } catch (IllegalArgumentException | InterruptedException e) {
            // This can be thrown from OkHttp if the host fails to canonicalize to a valid name.
            // https://github.com/square/okhttp/blob/okhttp_27/okhttp/src/main/java/com/squareup/okhttp/HttpUrl.java#L705
            LimeLog.warning(e);
            success = false;
        }
        if (!success) {
            wrongSiteLocal = isWrongSubnetSiteLocalAddress(session.getHostAddress());
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

        processingError(new Exception(message), true);
    }

    private boolean runningPolling;

    private void doPair() {
        if (computer.state == ComputerDetails.State.OFFLINE ||
                ServerHelper.getCurrentAddressFromComputer(computer) == null) {
            processingError(new Exception(getResources().getString(R.string.pair_pc_offline)), true);
            return;
        }
        if (computer.runningGameId != 0) {
            processingError(new Exception(getResources().getString(R.string.pair_pc_ingame)), true);
            return;
        }
        if (managerBinder == null) {
            processingError(new Exception(getResources().getString(R.string.error_manager_not_running)), true);
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

                if (computer == null) {
                    return;
                }

                PairingManager pm = httpConn.getPairingManager();
                PairingManager.PairState pairState = pm.pair(httpConn.getServerInfo(), session.getKey());
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
                runOnUiThread(() -> processingError(getResources().getString(R.string.error_unknown_host), e, true));
                return;
            } catch (FileNotFoundException e) {
                runOnUiThread(() -> processingError(getResources().getString(R.string.error_404), e, true));
                return;
            } catch (XmlPullParserException | IOException e) {
                runOnUiThread(() -> processingError(e, true));
                return;
            }

            final String finalMessage = message;
            runOnUiThread(() -> processingError(new Exception(finalMessage), true));
        }).start();
    }

    private void startComputerUpdates() {
        managerBinder.startPolling(details -> {
            if (details.pairState == PairingManager.PairState.PAIRED) {
                new Thread(() -> {
                    stopComputerUpdates();
                    try {
                        startGame(
                                ServerHelper.getCurrentAddressFromComputer(computer),
                                managerBinder.getUniqueId(),
                                computer.uuid,
                                computer.name,
                                computer.serverCert,
                                session.getKey()
                        );
                    } catch (XmlPullParserException | IOException e) {
                        processingError(e, true);
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

    private void processingError(Throwable error, boolean isRemoveComputer) {
        processingError("", error, isRemoveComputer);
    }

    private void processingError(String message, Throwable error, boolean isRemoveComputer) {
        LimeLog.severe(message, error);

        if (isRemoveComputer) {
            removeComputer();
        }

        startActivity(new Intent(Intent.ACTION_MAIN, null, PcView.this, PcView.class));
    }

    private void startGame(String host, String uniqueId, String uuid, String pcName,
                           X509Certificate serverCert, String sessionKey)
            throws XmlPullParserException, IOException {
        NvHTTP httpConn = new NvHTTP(host,
                uniqueId,
                serverCert,
                PlatformBinding.getCryptoProvider(PcView.this));

        NvApp currentApp = httpConn.getAppByName("Desktop"); // Always get first app (Desktop)

        if (currentApp != null) {
            Intent intent = ServerHelper.createStartIntent(this, host, currentApp, uniqueId,
                    uuid, pcName, serverCert, sessionKey);
            runOnUiThread(() -> ServerHelper.doStart(this, intent));
        } else {
            processingError(new Exception(getString(R.string.applist_refresh_error_msg)), true);
        }
    }
}