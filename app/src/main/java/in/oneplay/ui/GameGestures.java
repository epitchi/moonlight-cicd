package in.oneplay.ui;

import android.os.ResultReceiver;
import android.view.View;

public interface GameGestures {
    void toggleKeyboard();
    void hideKeyboard(View view, ResultReceiver receiver);
}
