package in.oneplay.utils;

import android.app.Activity;
import android.content.Intent;

import java.security.cert.CertificateEncodingException;

import in.oneplay.Game;
import in.oneplay.LimeLog;
import in.oneplay.R;
import in.oneplay.computers.ComputerManagerService;
import in.oneplay.nvstream.http.ComputerDetails;
import in.oneplay.nvstream.http.NvApp;

public class ServerHelper {
    public static final String CONNECTION_TEST_SERVER = "android.conntest.moonlight-stream.org";

    public static final int ONEPLAY_GAME_REQUEST_CODE = 1;
    public static final int ONEPLAY_GAME_RESULT_REFRESH_ACTIVITY = 2;

    public static String getCurrentAddressFromComputer(ComputerDetails computer) {
        return computer.activeAddress;
    }

    public static Intent createStartIntent(Activity parent, NvApp app, ComputerDetails computer,
                                           ComputerManagerService.ComputerManagerBinder managerBinder) {
        Intent intent = new Intent(parent, Game.class);
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

    public static void doStart(Activity parent, NvApp app, ComputerDetails computer,
                               ComputerManagerService.ComputerManagerBinder managerBinder) {
        if (computer.state == ComputerDetails.State.OFFLINE ||
                ServerHelper.getCurrentAddressFromComputer(computer) == null) {
            LimeLog.warning(parent.getString(R.string.pair_pc_offline));
            return;
        }
        parent.startActivityForResult(createStartIntent(parent, app, computer, managerBinder), ONEPLAY_GAME_REQUEST_CODE);
    }
}
