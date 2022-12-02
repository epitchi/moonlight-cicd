package in.oneplay;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import in.oneplay.backend.OneplayApi;
import in.oneplay.nvstream.http.NvHTTP;
import in.oneplay.ui.DebugDialog;
import in.oneplay.utils.ServerHelper;
import in.oneplay.utils.UiHelper;

public class PcView extends Activity {
    private WebView webView;
    private Intent currentIntent;
    private FirebaseAnalytics mFirebaseAnalytics;
    private boolean backCallbackRegistered;
    private OnBackInvokedCallback onBackInvokedCallback;
    private boolean isResumed = false;
    private boolean isFirstStart = true;
    private DebugDialog debugDialog;

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

            connectToComputer();
        }

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

        if (!isResumed && welcomeLink != null) {
            webView.loadUrl(welcomeLink.toString());
        }

        if (BuildConfig.DEBUG) {
            debugDialog.setOnShowListener(di -> {
                debugDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    int connectionTimeout = Integer.parseInt(debugDialog.getConnectionTimeout());
                    int readTimeout = Integer.parseInt(debugDialog.getReadTimeout());

                    debugDialog.dismiss();

                    new Thread(() -> {
                        try {
                            NvHTTP.connectionTimeout = OneplayApi.connectionTimeout = connectionTimeout;
                            NvHTTP.readTimeout = OneplayApi.readTimeout = readTimeout;

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
                                            Game.class);
                                    startActivity(intent);
                                });
                            } else {
                                runOnUiThread(() -> {
                                    debugDialog.show();
                                    Toast.makeText(PcView.this, "Can't get session signature. Try again.", Toast.LENGTH_LONG).show();
                                });
                            }
                        } catch (IOException | JSONException e) {
                            LimeLog.severe(e);

                            runOnUiThread(() -> startActivity(new Intent(Intent.ACTION_MAIN, null, PcView.this, PcView.class)));
                        }
                    }).start();
                });
                debugDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener((v) -> debugDialog.setDefaults());
            });
            debugDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ServerHelper.UPDATES_REQUEST_CODE && resultCode != RESULT_OK) {
            LimeLog.severe("Update flow failed! Result code: " + resultCode, new Throwable());
            checkUpdates();
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
        Intent intent = new Intent(Intent.ACTION_VIEW, currentIntent.getData(), this, Game.class);
        ServerHelper.doStart(this, intent);
    }
}