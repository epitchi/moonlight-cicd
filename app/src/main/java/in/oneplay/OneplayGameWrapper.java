package in.oneplay;

import android.os.Bundle;

public class OneplayGameWrapper extends Game {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_OK);
    }
}
