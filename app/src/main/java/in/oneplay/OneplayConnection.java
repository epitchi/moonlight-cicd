package in.oneplay;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import in.oneplay.backend.OneplayApi;
import in.oneplay.binding.PlatformBinding;
import in.oneplay.binding.crypto.AndroidCryptoProvider;
import in.oneplay.computers.ComputerDatabaseManager;
import in.oneplay.computers.ComputerManagerService;
import in.oneplay.grid.assets.DiskAssetLoader;
import in.oneplay.nvstream.http.ComputerDetails;
import in.oneplay.nvstream.http.NvApp;
import in.oneplay.nvstream.http.NvHTTP;
import in.oneplay.nvstream.http.PairingManager;
import in.oneplay.nvstream.jni.MoonBridge;
import in.oneplay.utils.ServerHelper;

import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

public class OneplayConnection extends Activity {

    private View actionButton;
    private ImageView logo;
    private TextView title;
    private ProgressBar progress;
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

                    // If the app not closed correctly close all apps and remove all exiting computers
                    removeAllComputers();

                    connectToComputer();
                }
            }.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            managerBinder = null;
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (managerBinder != null) {
            removeAllComputers();
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oneplay_connection);

        actionButton = findViewById(R.id.action_button);
        logo = findViewById(R.id.action_button_logo);
        title = findViewById(R.id.action_button_title);
        progress = findViewById(R.id.action_button_progress);

        Intent intent = getIntent();

        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            logo.setVisibility(View.GONE);
            title.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);
            actionButton.setOnClickListener(null);

            // Bind to the ComputerManager service
            bindService(new Intent(OneplayConnection.this,
                    ComputerManagerService.class), serviceConnection, Service.BIND_AUTO_CREATE);
        } else {
            logo.setVisibility(View.VISIBLE);
            title.setVisibility(View.VISIBLE);
            title.setText("Click to go to the home page"); //TODO move to resource
            progress.setVisibility(View.GONE);
            actionButton.setOnClickListener(view -> {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.oneplay.in/login/")); //TODO move to resource
                startActivity(webIntent);
            });
        }
    }

    private void connectToComputer() {
        runOnUiThread(() -> progress.setVisibility(View.VISIBLE));
        new Thread(() -> {
            Uri uri = getIntent().getData();
            try {
                OneplayApi client = new OneplayApi(uri);
                ComputerDetails computer = doAddPc(client.getHostAddress());
                if (computer != null) {
                    doPair(client, computer);
                }
            } catch (IOException exception) {
                processingError("Error create client!"); //TODO move to resource
            }
        }).start();
    }

    private void closeApps(ComputerDetails computer) {
        String rawAppList = computer.rawAppList;

        if (rawAppList == null) {
            return;
        }

        try {
            List<NvApp> apps = NvHTTP.getAppListByReader(new StringReader(rawAppList));
            for (NvApp app : apps) {
                ServerHelper.doQuit(OneplayConnection.this, computer, app, managerBinder, () -> {});
            }
        } catch (XmlPullParserException | IOException ignored) { }
    }

    private void removeAllComputers() {
        // Remove all previously saved computers
        ComputerDatabaseManager dbManager = new ComputerDatabaseManager(OneplayConnection.this);
        DiskAssetLoader diskAssetLoader = new DiskAssetLoader(OneplayConnection.this);
        for (ComputerDetails computer : dbManager.getAllComputers()) {
            closeApps(computer);

            managerBinder.removeComputer(computer);

            diskAssetLoader.deleteAssetsForComputer(computer.uuid);

            // Delete hidden games preference value
            getSharedPreferences(AppView.HIDDEN_APPS_PREF_FILENAME, MODE_PRIVATE)
                    .edit()
                    .remove(computer.uuid)
                    .apply();
        }
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

    @Nullable
    private ComputerDetails doAddPc(String host) {
        boolean wrongSiteLocal = false;
        boolean success;
        int portTestResult;

        ComputerDetails details = new ComputerDetails();
        try {
            details.manualAddress = host;
            success = managerBinder.addComputerBlocking(details);
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
            return details;
        }

        processingError(null);
        return null;
    }

    private void doPair(final OneplayApi client, final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.OFFLINE ||
                ServerHelper.getCurrentAddressFromComputer(computer) == null) {
            processingError(getResources().getString(R.string.pair_pc_offline));
            return;
        }
        if (computer.runningGameId != 0) {
            processingError(getResources().getString(R.string.pair_pc_ingame));
            return;
        }
        if (managerBinder == null) {
            processingError(getResources().getString(R.string.error_manager_not_running));
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
            ServerHelper.doStart(this, new NvApp("app", client.getGameId(), false), computer, managerBinder);
        } else {
            processingError(message);
        }
    }

    private void processingError(String message) {
        if (message != null) {
            LimeLog.warning(message);
        }

        runOnUiThread(() -> {
            progress.setVisibility(View.GONE);

            title.setVisibility(View.VISIBLE);
            title.setText("Try again"); //TODO move to resource
            actionButton.setOnClickListener(view -> {
                title.setVisibility(View.GONE);
                connectToComputer();
                actionButton.setOnClickListener(null);
            });
        });
    }
}