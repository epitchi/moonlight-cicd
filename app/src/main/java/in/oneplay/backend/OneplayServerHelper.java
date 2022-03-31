package in.oneplay.backend;

import android.app.Activity;
import android.content.Intent;

import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.cert.CertificateEncodingException;

import in.oneplay.Game;
import in.oneplay.LimeLog;
import in.oneplay.OneplayGameWrapper;
import in.oneplay.R;
import in.oneplay.binding.PlatformBinding;
import in.oneplay.computers.ComputerManagerService;
import in.oneplay.nvstream.http.ComputerDetails;
import in.oneplay.nvstream.http.GfeHttpResponseException;
import in.oneplay.nvstream.http.NvApp;
import in.oneplay.nvstream.http.NvHTTP;
import in.oneplay.utils.ServerHelper;

public class OneplayServerHelper {
    public static final int ONEPLAY_GAME_REQUEST_CODE = 1;

    public static void doStart(Activity parent, NvApp app, ComputerDetails computer,
                        ComputerManagerService.ComputerManagerBinder managerBinder) {
        if (computer.state == ComputerDetails.State.OFFLINE ||
                ServerHelper.getCurrentAddressFromComputer(computer) == null) {
            LimeLog.warning(parent.getString(R.string.pair_pc_offline));
            return;
        }
        parent.startActivityForResult(createStartIntent(parent, app, computer, managerBinder), ONEPLAY_GAME_REQUEST_CODE);
    }

    public static Intent createStartIntent(Activity parent, NvApp app, ComputerDetails computer,
                                    ComputerManagerService.ComputerManagerBinder managerBinder) {
        Intent intent = new Intent(parent, OneplayGameWrapper.class);
        intent.putExtra(Game.EXTRA_HOST, ServerHelper.getCurrentAddressFromComputer(computer));
        intent.putExtra(Game.EXTRA_APP_NAME, app.getAppName());
        intent.putExtra(Game.EXTRA_APP_ID, app.getAppId());
        intent.putExtra(Game.EXTRA_APP_HDR, app.isHdrSupported());
        intent.putExtra(Game.EXTRA_UNIQUEID, managerBinder.getUniqueId());
        intent.putExtra(Game.EXTRA_PC_UUID, computer.uuid);
        intent.putExtra(Game.EXTRA_PC_NAME, computer.name);
        try {
            if (computer.serverCert != null) {
                intent.putExtra(Game.EXTRA_SERVER_CERT, computer.serverCert.getEncoded());
            }
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        return intent;
    }

    public static void doQuit(final Activity parent,
                              final ComputerDetails computer,
                              final NvApp app,
                              final ComputerManagerService.ComputerManagerBinder managerBinder,
                              final Runnable onComplete) {
        LimeLog.info(parent.getResources().getString(R.string.applist_quit_app) + " " + app.getAppName() + "...");
        new Thread(() -> {
            NvHTTP httpConn;
            String message = null;
            try {
                httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                        managerBinder.getUniqueId(), computer.serverCert, PlatformBinding.getCryptoProvider(parent));
                if (httpConn.quitApp()) {
                    LimeLog.info(parent.getResources().getString(R.string.applist_quit_success) + " " + app.getAppName());
                } else {
                    message = parent.getResources().getString(R.string.applist_quit_fail) + " " + app.getAppName();
                }
            } catch (GfeHttpResponseException e) {
                if (e.getErrorCode() == 599) {
                    message = "This session wasn't started by this device," +
                            " so it cannot be quit. End streaming on the original " +
                            "device or the PC itself. (Error code: "+e.getErrorCode()+")";
                }
                else {
                    message = e.getMessage();
                }
            } catch (UnknownHostException e) {
                message = parent.getResources().getString(R.string.error_unknown_host);
            } catch (FileNotFoundException e) {
                message = parent.getResources().getString(R.string.error_404);
            } catch (IOException | XmlPullParserException e) {
                message = e.getMessage();
                e.printStackTrace();
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            if (message != null) {
                LimeLog.severe(message);
            }
        }).start();
    }
}
