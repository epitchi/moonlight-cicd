package in.oneplay;

import android.os.Bundle;

public class OneplayGameWrapper extends Game {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_OK);
    }

    @Override
    public void stageFailed(String stage, int portFlags, int errorCode) {
        super.stageFailed(stage, portFlags, errorCode);
        setResult(errorCode);
    }

    @Override
    public void connectionTerminated(int errorCode) {
        super.connectionTerminated(errorCode);
        setResult(errorCode);
    }
}
