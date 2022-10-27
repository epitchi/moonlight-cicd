package in.oneplay.ui;

import static in.oneplay.utils.UiHelper.dp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import in.oneplay.BuildConfig;

public class DebugDialog extends AlertDialog {
    private static final String SERVER_DEFAULT_IP_ADDRESS = BuildConfig.SERVER_DEFAULT_IP_ADDRESS;
    private static final String DEFAULT_CONNECTION_TIMEOUT = String.valueOf(BuildConfig.SERVER_DEFAULT_CONNECTION_TIMEOUT);
    private static final String DEFAULT_READ_TIMEOUT = String.valueOf(BuildConfig.SERVER_DEFAULT_READ_TIMEOUT);

    private RelativeLayout layout;
    private TextView ipLabel;
    private EditText ipField;
    private TextView connTimeoutLabel;
    private EditText connTimeoutField;
    private TextView readTimeoutLabel;
    private EditText readTimeoutField;

    public DebugDialog(Context context) {
        super(context);
        createLayout(context);
    }

    private void createLayout(Context context) {
        // Layout
        layout = new RelativeLayout(context);
        int padding = dp(context, 20);
        layout.setPadding(padding, padding, padding, padding);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(params);

        // IP label
        ipLabel = new TextView(context);
        ipLabel.setId(View.generateViewId());
        ipLabel.setText("Enter the server IP:");
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        ipLabel.setLayoutParams(params);
        layout.addView(ipLabel);

        // IP field
        ipField = new EditText(context);
        ipField.setId(View.generateViewId());
        ipField.setText(SERVER_DEFAULT_IP_ADDRESS);
        InputFilter[] filters = new InputFilter[1];
        filters[0] = (source, start, end, dest, dstart, dend) -> {
            if (end > start) {
                String destTxt = dest.toString();
                String resultingTxt = destTxt.substring(0, dstart)
                        + source.subSequence(start, end)
                        + destTxt.substring(dend);
                if (!resultingTxt
                        .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                    return "";
                } else {
                    String[] splits = resultingTxt.split("\\.");
                    for (String split : splits) {
                        if (Integer.parseInt(split) > 255) {
                            return "";
                        }
                    }
                }
            }
            return null;
        };
        ipField.setFilters(filters);
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, 48));
        params.addRule(RelativeLayout.BELOW, ipLabel.getId());
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.TEXT_ALIGNMENT_GRAVITY, RelativeLayout.CENTER_VERTICAL);
        ipField.setLayoutParams(params);
        layout.addView(ipField);

        // Connection timeout label
        connTimeoutLabel = new TextView(context);
        connTimeoutLabel.setId(View.generateViewId());
        connTimeoutLabel.setText("Enter the connection timeout, ms:");
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, ipField.getId());
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        connTimeoutLabel.setLayoutParams(params);
        layout.addView(connTimeoutLabel);

        // Connection timeout field
        connTimeoutField = new EditText(context);
        connTimeoutField.setId(View.generateViewId());
        connTimeoutField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        connTimeoutField.setText(DEFAULT_CONNECTION_TIMEOUT);
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, 48));
        params.addRule(RelativeLayout.BELOW, connTimeoutLabel.getId());
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.TEXT_ALIGNMENT_GRAVITY, RelativeLayout.CENTER_VERTICAL);
        connTimeoutField.setLayoutParams(params);
        layout.addView(connTimeoutField);

        // Read timeout label
        readTimeoutLabel = new TextView(context);
        readTimeoutLabel.setId(View.generateViewId());
        readTimeoutLabel.setText("Enter the read timeout, ms:");
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, connTimeoutField.getId());
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        readTimeoutLabel.setLayoutParams(params);
        layout.addView(readTimeoutLabel);

        // Read timeout field
        readTimeoutField = new EditText(context);
        readTimeoutField.setId(View.generateViewId());
        readTimeoutField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        readTimeoutField.setText(DEFAULT_READ_TIMEOUT);
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, 48));
        params.addRule(RelativeLayout.BELOW, readTimeoutLabel.getId());
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.TEXT_ALIGNMENT_GRAVITY, RelativeLayout.CENTER_VERTICAL);
        readTimeoutField.setLayoutParams(params);
        layout.addView(readTimeoutField);

        setButton(AlertDialog.BUTTON_POSITIVE, context.getString(android.R.string.ok), (OnClickListener) null);
        setButton(AlertDialog.BUTTON_NEUTRAL, "Default", (OnClickListener) null);

        setView(layout);

        setCancelable(false);
    }

    public String getIp() {
        return ipField.getText().toString();
    }

    public String getConnectionTimeout() {
        return connTimeoutField.getText().toString();
    }

    public String getReadTimeout() {
        return readTimeoutField.getText().toString();
    }

    public void setDefaults() {
        ipField.setText(SERVER_DEFAULT_IP_ADDRESS);
        connTimeoutField.setText(DEFAULT_CONNECTION_TIMEOUT);
        readTimeoutField.setText(DEFAULT_READ_TIMEOUT);
    }
}
