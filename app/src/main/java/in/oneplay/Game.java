package in.oneplay;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Rational;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import in.oneplay.backend.OneplayApi;
import in.oneplay.backend.UserSession;
import in.oneplay.binding.PlatformBinding;
import in.oneplay.binding.audio.AndroidAudioRenderer;
import in.oneplay.binding.crypto.AndroidCryptoProvider;
import in.oneplay.binding.input.ControllerHandler;
import in.oneplay.binding.input.KeyboardTranslator;
import in.oneplay.binding.input.capture.InputCaptureManager;
import in.oneplay.binding.input.capture.InputCaptureProvider;
import in.oneplay.binding.input.driver.UsbDriverService;
import in.oneplay.binding.input.evdev.EvdevListener;
import in.oneplay.binding.input.touch.AbsoluteTouchContext;
import in.oneplay.binding.input.touch.RelativeTouchContext;
import in.oneplay.binding.input.touch.TouchContext;
import in.oneplay.binding.input.virtual_controller.VirtualController;
import in.oneplay.binding.video.CrashListener;
import in.oneplay.binding.video.MediaCodecDecoderRenderer;
import in.oneplay.binding.video.MediaCodecHelper;
import in.oneplay.binding.video.PerfOverlayListener;
import in.oneplay.computers.IdentityManager;
import in.oneplay.nvstream.NvConnection;
import in.oneplay.nvstream.NvConnectionListener;
import in.oneplay.nvstream.StreamConfiguration;
import in.oneplay.nvstream.http.ComputerDetails;
import in.oneplay.nvstream.http.GfeHttpResponseException;
import in.oneplay.nvstream.http.NvApp;
import in.oneplay.nvstream.http.NvHTTP;
import in.oneplay.nvstream.http.PairingManager;
import in.oneplay.nvstream.input.KeyboardPacket;
import in.oneplay.nvstream.input.MouseButtonPacket;
import in.oneplay.nvstream.jni.MoonBridge;
import in.oneplay.preferences.GlPreferences;
import in.oneplay.preferences.OneplayPreferenceConfiguration;
import in.oneplay.preferences.PreferenceConfiguration;
import in.oneplay.ui.GameGestures;
import in.oneplay.ui.StreamView;
import in.oneplay.utils.Dialog;
import in.oneplay.utils.NetHelper;
import in.oneplay.utils.ServerHelper;
import in.oneplay.utils.SpinnerDialog;
import in.oneplay.utils.UiHelper;

public class Game extends Activity implements SurfaceHolder.Callback,
    OnGenericMotionListener, OnTouchListener, NvConnectionListener, EvdevListener,
    OnSystemUiVisibilityChangeListener, GameGestures, StreamView.InputCallbacks,
    PerfOverlayListener, UsbDriverService.UsbDriverStateListener
{
    private int lastButtonState = 0;

    // Only 2 touches are supported
    private final TouchContext[] touchContextMap = new TouchContext[2];
    private long threeFingerDownTime = 0;

    private static final int REFERENCE_HORIZ_RES = 1280;
    private static final int REFERENCE_VERT_RES = 720;

    private static final int STYLUS_DOWN_DEAD_ZONE_DELAY = 100;
    private static final int STYLUS_DOWN_DEAD_ZONE_RADIUS = 20;

    private static final int STYLUS_UP_DEAD_ZONE_DELAY = 150;
    private static final int STYLUS_UP_DEAD_ZONE_RADIUS = 50;

    private static final int THREE_FINGER_TAP_THRESHOLD = 300;

    private ControllerHandler controllerHandler;
    private KeyboardTranslator keyboardTranslator;
    private VirtualController virtualController;

    private PreferenceConfiguration prefConfig;
    private SharedPreferences tombstonePrefs;

    private NvConnection conn;
    private SpinnerDialog spinner;
    private boolean displayedFailureDialog = false;
    private boolean connecting = false;
    private boolean connected = false;
    private boolean autoEnterPip = false;
    private boolean surfaceCreated = false;
    private boolean attemptedConnection = false;
    private int suppressPipRefCount = 0;
    private String pcName;
    private String appName;

    private InputCaptureProvider inputCaptureProvider;
    private int modifierFlags = 0;
    private boolean grabbedInput = true;
    private boolean grabComboDown = false;
    private StreamView streamView;
    private long lastAbsTouchUpTime = 0;
    private long lastAbsTouchDownTime = 0;
    private float lastAbsTouchUpX, lastAbsTouchUpY;
    private float lastAbsTouchDownX, lastAbsTouchDownY;

    private boolean isHidingOverlays;
    private TextView notificationOverlayView;
    private int requestedNotificationOverlayVisibility = View.GONE;
    private TextView performanceOverlayView;
    private ImageButton settingsButton;

    private MediaCodecDecoderRenderer decoderRenderer;
    private boolean reportedCrash;

    private WifiManager.WifiLock highPerfWifiLock;
    private WifiManager.WifiLock lowLatencyWifiLock;

    private boolean isNeedRefresh = false;
    private boolean isNeedReload = false;

    private boolean connectedToUsbDriverService = false;
    private ServiceConnection usbDriverServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            UsbDriverService.UsbDriverBinder binder = (UsbDriverService.UsbDriverBinder) iBinder;
            binder.setListener(controllerHandler);
            binder.setStateListener(Game.this);
            binder.start();
            connectedToUsbDriverService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            connectedToUsbDriverService = false;
        }
    };

    private Intent currentIntent;
    private IdentityManager idManager;

    public static final String EXTRA_HOST = "Host";
    public static final String EXTRA_APP_NAME = "AppName";
    public static final String EXTRA_APP_ID = "AppId";
    public static final String EXTRA_UNIQUE_ID = "UniqueId";
    public static final String EXTRA_PC_UUID = "UUID";
    public static final String EXTRA_PC_NAME = "PcName";
    public static final String EXTRA_APP_HDR = "HDR";
    public static final String EXTRA_SERVER_CERT = "ServerCert";
    public static final String EXTRA_SESSION_KEY = "SessionKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Thread(() -> {
            // Force a keypair to be generated early to avoid discovery delays
            new AndroidCryptoProvider(Game.this).getClientCertificate();
        }).start();

        currentIntent = getIntent();

        UiHelper.setLocale(this);

        // We don't want a title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Full-screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // If we're going to use immersive mode, we want to have
        // the entire screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        }

        // We specified userLandscape in the manifest which isn't supported until 4.3,
        // so we must fall back at runtime to sensorLandscape.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        // Listen for UI visibility events
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        // Change volume button behavior
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Inflate the content
        setContentView(R.layout.activity_game);

        // Start the spinner
        spinner = SpinnerDialog.displayDialog(this, getResources().getString(R.string.conn_establishing_title),
                getResources().getString(R.string.conn_establishing_msg), true);

        // Read the stream preferences
        prefConfig = PreferenceConfiguration.readPreferences(this);
        tombstonePrefs = Game.this.getSharedPreferences("DecoderTombstone", 0);

        if (prefConfig.stretchVideo || shouldIgnoreInsetsForResolution(prefConfig.width, prefConfig.height)) {
            // Allow the activity to layout under notches if the fill-screen option
            // was turned on by the user or it's a full-screen native resolution
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
        }

        // Listen for events on the game surface
        streamView = findViewById(R.id.surfaceView);
        streamView.setOnGenericMotionListener(this);
        streamView.setOnTouchListener(this);
        streamView.setInputCallbacks(this);

        settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setAlpha(0.13f);
        settingsButton.setFocusable(false);

        settingsButton.setOnClickListener((view) -> {
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                createMenu(settingsButton, R.menu.game_setting_menu, (menuItem) -> {
                    if (menuItem.getItemId() == R.id.show_keyboard) {
                        Runnable showKeyboardRunnable = this::toggleKeyboard;
                        streamView.postDelayed(showKeyboardRunnable, 500);
                        streamView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                            @Override
                            public void onViewAttachedToWindow(View v) {}

                            @Override
                            public void onViewDetachedFromWindow(View v) {
                                streamView.removeOnAttachStateChangeListener(this);
                                view.removeCallbacks(showKeyboardRunnable);
                            }
                        });
                    } else if (menuItem.getItemId() == R.id.show_stream_settings) {
                        createMenu(settingsButton, R.menu.stream_settings_menu, (streamMenuItem) -> {
                            if (streamMenuItem.getItemId() == R.id.show_basic_settings) {
                                PopupMenu basicSettingsMenu = createMenu(settingsButton, R.menu.basic_settings_menu, (basicMenuItem) -> {
                                    if (basicMenuItem.getItemId() == R.id.list_resolution) {
                                        createListDialog(
                                                OneplayPreferenceConfiguration.getResolution(Game.this),
                                                R.string.title_resolution_list,
                                                R.array.resolution_values,
                                                R.array.resolution_names,
                                                OneplayPreferenceConfiguration::setScreenResolution
                                        ).show();
                                    } else if (basicMenuItem.getItemId() == R.id.list_fps) {
                                        createListDialog(
                                                prefConfig.fps,
                                                R.string.title_fps_list,
                                                R.array.fps_values,
                                                R.array.fps_names,
                                                OneplayPreferenceConfiguration::setFps
                                        ).show();
                                    } else if (basicMenuItem.getItemId() == R.id.seekbar_bitrate_kbps) {
                                        createSeekBarDialog(
                                                prefConfig.bitrate / 1000,
                                                R.string.title_seekbar_bitrate,
                                                R.string.suffix_seekbar_bitrate_mbps,
                                                1,
                                                1,
                                                prefConfig.maxBitrate / 1000,
                                                OneplayPreferenceConfiguration::setBitrateKbps
                                        ).show();
                                    } else if (basicMenuItem.getItemId() == R.id.frame_pacing) {
                                        createListDialog(
                                                prefConfig.framePacing,
                                                R.string.title_fps_list,
                                                R.array.video_frame_pacing_values,
                                                R.array.video_frame_pacing_names,
                                                OneplayPreferenceConfiguration::setFramePacing
                                        ).show();
                                    } else if (basicMenuItem.getItemId() == R.id.checkbox_stretch_video) {
                                        initCheckboxBehavior(
                                                basicMenuItem,
                                                OneplayPreferenceConfiguration::setWindowMode
                                        );
                                    } else {
                                        return false;
                                    }

                                    return true;
                                });

                                // Initialize checkbox
                                basicSettingsMenu.getMenu().findItem(R.id.checkbox_stretch_video)
                                        .setChecked(prefConfig.stretchVideo);

                                basicSettingsMenu.show();
                            } else if (streamMenuItem.getItemId() == R.id.show_on_screen_settings) {
                                PopupMenu onscreenControlSettingMenu = createMenu(settingsButton, R.menu.onscreen_control_settings_menu, (onscreenControlMenuItem) -> {
                                    if (onscreenControlMenuItem.getItemId() == R.id.checkbox_show_onscreen_controls) {
                                        initCheckboxBehavior(
                                                onscreenControlMenuItem,
                                                OneplayPreferenceConfiguration::setOnscreenController
                                        );
                                    } else if (onscreenControlMenuItem.getItemId() == R.id.checkbox_vibrate_osc) {
                                        initCheckboxBehavior(
                                                onscreenControlMenuItem,
                                                OneplayPreferenceConfiguration::setVibrateOsc
                                        );
                                    } else if (onscreenControlMenuItem.getItemId() == R.id.seekbar_osc_opacity) {
                                        createSeekBarDialog(
                                                prefConfig.oscOpacity,
                                                R.string.dialog_title_osc_opacity,
                                                R.string.suffix_osc_opacity,
                                                0,
                                                1,
                                                100,
                                                OneplayPreferenceConfiguration::setOscOpacity
                                        ).show();
                                    } else if (onscreenControlMenuItem.getItemId() == R.id.button_move_buttons) {
                                        if (virtualController != null) {
                                            virtualController.setControllerMode(VirtualController.ControllerMode.MoveButtons);
                                        }
                                    } else if (onscreenControlMenuItem.getItemId() == R.id.button_resize_buttons) {
                                        if (virtualController != null) {
                                            virtualController.setControllerMode(VirtualController.ControllerMode.ResizeButtons);
                                        }
                                    } else if (onscreenControlMenuItem.getItemId() == R.id.button_save_profile) {
                                        if (virtualController != null) {
                                            virtualController.saveProfile();
                                        }
                                    } else if (onscreenControlMenuItem.getItemId() == R.id.reset_osc) {
                                        createSimpleDialog(
                                                R.string.dialog_title_reset_osc,
                                                R.string.dialog_text_reset_osc,
                                                () -> {
                                                    OneplayPreferenceConfiguration.resetOsc(Game.this);
                                                    Toast.makeText(Game.this, R.string.toast_reset_osc_success, Toast.LENGTH_SHORT).show();
                                                    if (virtualController != null) {
                                                        virtualController.refreshLayout();
                                                    }
                                                }
                                        ).show();
                                    } else {
                                        return false;
                                    }

                                    return true;
                                });

                                // Initialize checkbox
                                MenuItem checkboxShowOnscreenControls = onscreenControlSettingMenu.getMenu().findItem(R.id.checkbox_show_onscreen_controls);
                                checkboxShowOnscreenControls.setChecked(prefConfig.onscreenController);
                                MenuItem checkboxVibrateOsc = onscreenControlSettingMenu.getMenu().findItem(R.id.checkbox_vibrate_osc);
                                checkboxVibrateOsc.setChecked(prefConfig.vibrateOsc);
                                checkboxVibrateOsc.setVisible(prefConfig.onscreenController);
                                MenuItem seekbarOscOpacity = onscreenControlSettingMenu.getMenu().findItem(R.id.seekbar_osc_opacity);
                                seekbarOscOpacity.setVisible(prefConfig.onscreenController);
                                MenuItem buttonMoveButtons = onscreenControlSettingMenu.getMenu().findItem(R.id.button_move_buttons);
                                buttonMoveButtons.setVisible(prefConfig.onscreenController);
                                MenuItem buttonResizeButtons = onscreenControlSettingMenu.getMenu().findItem(R.id.button_resize_buttons);
                                buttonResizeButtons.setVisible(prefConfig.onscreenController);
                                MenuItem buttonSaveProfile = onscreenControlSettingMenu.getMenu().findItem(R.id.button_save_profile);
                                buttonSaveProfile.setVisible(prefConfig.onscreenController);

                                onscreenControlSettingMenu.show();
                            } else if (streamMenuItem.getItemId() == R.id.show_advanced_settings) {
                                PopupMenu advancedSettingMenu = createMenu(settingsButton, R.menu.advanced_settings_menu, (advancedMenuItem) -> {
                                    if (advancedMenuItem.getItemId() == R.id.checkbox_enable_perf_overlay) {
                                        initCheckboxBehavior(
                                                advancedMenuItem,
                                                (context, value) -> {
                                                    performanceOverlayView.setVisibility(value ? View.VISIBLE : View.GONE);
                                                    prefConfig.enablePerfOverlay = value;
                                                    OneplayPreferenceConfiguration.setEnablePerfOverlay(context, value);
                                                },
                                                false
                                        );
                                    } else {
                                        return false;
                                    }

                                    return true;
                                });

                                // Initialize checkbox
                                advancedSettingMenu.getMenu().findItem(R.id.checkbox_enable_perf_overlay)
                                        .setChecked(prefConfig.enablePerfOverlay);

                                advancedSettingMenu.show();
                            } else {
                                return false;
                            }

                            return true;
                        }).show();
                    } else if (menuItem.getItemId() == R.id.relaunch_game) {
                        reloadActivity();
                    } else if (menuItem.getItemId() == R.id.quit_stream) {
                        finish();
                    } else if (menuItem.getItemId() == R.id.report_issue) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(Game.this);
                        LayoutInflater inflater = Game.this.getLayoutInflater();

                        View reportIssueView = inflater.inflate(R.layout.dialog_report_issue, findViewById(R.id.dialog_issue_view));

                        TextView reportIssueText = reportIssueView.findViewById(R.id.dialog_report_issue_text);

                        builder.setView(reportIssueView)
                                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                                    String message = reportIssueText.getText().toString();
                                    if (!message.isEmpty()) {
                                        new Thread(() -> {
                                            try {
                                                OneplayApi.getInstance().registerEvent(message);
                                            } catch (IOException e) {
                                                LimeLog.severe(e);
                                            }
                                        }).start();
                                    } else {
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss());

                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else {
                        return false;
                    }

                    return true;
                }).show();
            }, 200);

            ResultReceiver callback = new ResultReceiver(handler);
            hideKeyboard(streamView, callback);
        });

        notificationOverlayView = findViewById(R.id.notificationOverlay);

        performanceOverlayView = findViewById(R.id.performanceOverlay);

        inputCaptureProvider = InputCaptureManager.getInputCaptureProvider(this, this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The view must be focusable for pointer capture to work.
            streamView.setFocusable(true);
            streamView.setDefaultFocusHighlightEnabled(false);
            streamView.setOnCapturedPointerListener(new View.OnCapturedPointerListener() {
                @Override
                public boolean onCapturedPointer(View view, MotionEvent motionEvent) {
                    return handleMotionEvent(view, motionEvent);
                }
            });
        }

        idManager = new IdentityManager(this);

        // The connection will be started when the surface gets created
        streamView.getHolder().addCallback(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (virtualController != null) {
            // Refresh layout of OSC for possible new screen size
            virtualController.refreshLayout();
        }

        // Hide on-screen overlays in PiP mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isInPictureInPictureMode()) {
                isHidingOverlays = true;

                if (virtualController != null) {
                    virtualController.hide();
                }

                performanceOverlayView.setVisibility(View.GONE);
                notificationOverlayView.setVisibility(View.GONE);

                // Update GameManager state to indicate we're in PiP (still gaming, but interruptible)
                UiHelper.notifyStreamEnteringPiP(this);
            }
            else {
                isHidingOverlays = false;

                // Restore overlays to previous state when leaving PiP

                if (virtualController != null) {
                    virtualController.show();
                }

                if (prefConfig.enablePerfOverlay) {
                    performanceOverlayView.setVisibility(View.VISIBLE);
                }

                notificationOverlayView.setVisibility(requestedNotificationOverlayVisibility);

                // Update GameManager state to indicate we're out of PiP (gaming, non-interruptible)
                UiHelper.notifyStreamExitingPiP(this);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private PictureInPictureParams getPictureInPictureParams(boolean autoEnter) {
        PictureInPictureParams.Builder builder =
                new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(prefConfig.width, prefConfig.height))
                        .setSourceRectHint(new Rect(
                                streamView.getLeft(), streamView.getTop(),
                                streamView.getRight(), streamView.getBottom()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter);
            builder.setSeamlessResizeEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (appName != null) {
                builder.setTitle(appName);
                if (pcName != null) {
                    builder.setSubtitle(pcName);
                }
            }
            else if (pcName != null) {
                builder.setTitle(pcName);
            }
        }

        return builder.build();
    }

    private void updatePipAutoEnter() {
        if (!prefConfig.enablePip) {
            return;
        }

        boolean autoEnter = connected && suppressPipRefCount == 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setPictureInPictureParams(getPictureInPictureParams(autoEnter));
        }
        else {
            autoEnterPip = autoEnter;
        }
    }

    public void setMetaKeyCaptureState(boolean enabled) {
        // This uses custom APIs present on some Samsung devices to allow capture of
        // meta key events while streaming.
        try {
            Class<?> semWindowManager = Class.forName("com.samsung.android.view.SemWindowManager");
            Method getInstanceMethod = semWindowManager.getMethod("getInstance");
            Object manager = getInstanceMethod.invoke(null);

            if (manager != null) {
                Class<?>[] parameterTypes = new Class<?>[2];
                parameterTypes[0] = String.class;
                parameterTypes[1] = boolean.class;
                Method requestMetaKeyEventMethod = semWindowManager.getDeclaredMethod("requestMetaKeyEvent", parameterTypes);
                requestMetaKeyEventMethod.invoke(manager, this.getComponentName(), enabled);
            }
            else {
                LimeLog.warning("SemWindowManager.getInstance() returned null");
            }
        } catch (ClassNotFoundException e) {
            LimeLog.warning(e);
        } catch (NoSuchMethodException e) {
            LimeLog.warning(e);
        } catch (InvocationTargetException e) {
            LimeLog.warning(e);
        } catch (IllegalAccessException e) {
            LimeLog.warning(e);
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();

        // PiP is only supported on Oreo and later, and we don't need to manually enter PiP on
        // Android S and later. On Android R, we will use onPictureInPictureRequested() instead.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (autoEnterPip) {
                try {
                    // This has thrown all sorts of weird exceptions on Samsung devices
                    // running Oreo. Just eat them and close gracefully on leave, rather
                    // than crashing.
                    enterPictureInPictureMode(getPictureInPictureParams(false));
                } catch (Exception e) {
                    LimeLog.warning(e);
                }
            }
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.R)
    public boolean onPictureInPictureRequested() {
        // Enter PiP when requested unless we're on Android 12 which supports auto-enter.
        if (autoEnterPip && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            enterPictureInPictureMode(getPictureInPictureParams(false));
        }
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // We can't guarantee the state of modifiers keys which may have
        // lifted while focus was not on us. Clear the modifier state.
        this.modifierFlags = 0;

        // With Android native pointer capture, capture is lost when focus is lost,
        // so it must be requested again when focus is regained.
        inputCaptureProvider.onWindowFocusChanged(hasFocus);
    }

    private boolean isRefreshRateEqualMatch(float refreshRate) {
        return refreshRate >= prefConfig.fps &&
                refreshRate <= prefConfig.fps + 3;
    }

    private boolean isRefreshRateGoodMatch(float refreshRate) {
        return refreshRate >= prefConfig.fps &&
                Math.round(refreshRate) % prefConfig.fps <= 3;
    }

    private boolean shouldIgnoreInsetsForResolution(int width, int height) {
        // Never ignore insets for non-native resolutions
        if (!PreferenceConfiguration.isNativeResolution(width, height)) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display display = getWindowManager().getDefaultDisplay();
            for (Display.Mode candidate : display.getSupportedModes()) {
                // Ignore insets if this is an exact match for the display resolution
                if ((width == candidate.getPhysicalWidth() && height == candidate.getPhysicalHeight()) ||
                        (height == candidate.getPhysicalWidth() && width == candidate.getPhysicalHeight())) {
                    return true;
                }
            }
        }

        return false;
    }

    private float prepareDisplayForRendering() {
        Display display = getWindowManager().getDefaultDisplay();
        WindowManager.LayoutParams windowLayoutParams = getWindow().getAttributes();
        float displayRefreshRate;

        // On M, we can explicitly set the optimal display mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode bestMode = display.getMode();
            boolean isNativeResolutionStream = PreferenceConfiguration.isNativeResolution(prefConfig.width, prefConfig.height);
            boolean refreshRateIsGood = isRefreshRateGoodMatch(bestMode.getRefreshRate());
            boolean refreshRateIsEqual = isRefreshRateEqualMatch(bestMode.getRefreshRate());
            boolean isTelevision = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);

            for (Display.Mode candidate : display.getSupportedModes()) {
                boolean refreshRateReduced = candidate.getRefreshRate() < bestMode.getRefreshRate();
                boolean resolutionReduced = candidate.getPhysicalWidth() < bestMode.getPhysicalWidth() ||
                        candidate.getPhysicalHeight() < bestMode.getPhysicalHeight();
                boolean resolutionFitsStream = candidate.getPhysicalWidth() >= prefConfig.width &&
                        candidate.getPhysicalHeight() >= prefConfig.height;

                LimeLog.info("Examining display mode: "+candidate.getPhysicalWidth()+"x"+
                        candidate.getPhysicalHeight()+"x"+candidate.getRefreshRate());

                if (candidate.getPhysicalWidth() > 4096 && prefConfig.width <= 4096) {
                    // Avoid resolutions options above 4K to be safe
                    continue;
                }

                // On non-4K streams, we force the resolution to never change unless it's above
                // 60 FPS, which may require a resolution reduction due to HDMI bandwidth limitations,
                // or it's a native resolution stream.
                if (prefConfig.width < 3840 && prefConfig.fps <= 60 && !isNativeResolutionStream) {
                    if (display.getMode().getPhysicalWidth() != candidate.getPhysicalWidth() ||
                            display.getMode().getPhysicalHeight() != candidate.getPhysicalHeight()) {
                        continue;
                    }
                }

                // Make sure the resolution doesn't regress unless if it's over 60 FPS
                // where we may need to reduce resolution to achieve the desired refresh rate.
                if (resolutionReduced && !(prefConfig.fps > 60 && resolutionFitsStream)) {
                    continue;
                }

                if (prefConfig.framePacing != PreferenceConfiguration.FRAME_PACING_MIN_LATENCY &&
                        refreshRateIsEqual && !isRefreshRateEqualMatch(candidate.getRefreshRate())) {
                    // If we had an equal refresh rate and this one is not, skip it. In min latency
                    // mode, we want to always prefer the highest frame rate even though it may cause
                    // microstuttering.
                    continue;
                }
                else if (refreshRateIsGood) {
                    // We've already got a good match, so if this one isn't also good, it's not
                    // worth considering at all.
                    if (!isRefreshRateGoodMatch(candidate.getRefreshRate())) {
                        continue;
                    }

                    // We don't want ever reduce our refresh rate unless we found an exact
                    // match and we're not in min latency mode.
                    if (refreshRateReduced) {
                        if (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_MIN_LATENCY) {
                            continue;
                        }
                        else if (!isRefreshRateEqualMatch(candidate.getRefreshRate())) {
                            continue;
                        }

                        // For refresh rates lower than 50hz, we want to check if the device is a TV.
                        // Some TV's may have issues when attempting to lower its refresh rate
                        // As opposed to mobile devices, which are designed to lower refresh rate
                        // for battery life reasons.
                        else if(isTelevision && candidate.getRefreshRate() < 50) {
                            continue;
                        }
                    }
                }
                else if (!isRefreshRateGoodMatch(candidate.getRefreshRate())) {
                    // We didn't have a good match and this match isn't good either, so just don't
                    // reduce the refresh rate.
                    if (refreshRateReduced) {
                        continue;
                    }
                } else {
                    // We didn't have a good match and this match is good. Prefer this refresh rate
                    // even if it reduces the refresh rate. Lowering the refresh rate can be beneficial
                    // when streaming a 60 FPS stream on a 90 Hz device. We want to select 60 Hz to
                    // match the frame rate even if the active display mode is 90 Hz.
                }

                bestMode = candidate;
                refreshRateIsGood = isRefreshRateGoodMatch(candidate.getRefreshRate());
                refreshRateIsEqual = isRefreshRateEqualMatch(candidate.getRefreshRate());
            }
            LimeLog.info("Selected display mode: "+bestMode.getPhysicalWidth()+"x"+
                    bestMode.getPhysicalHeight()+"x"+bestMode.getRefreshRate());
            windowLayoutParams.preferredDisplayModeId = bestMode.getModeId();
            displayRefreshRate = bestMode.getRefreshRate();
        }
        // On L, we can at least tell the OS that we want a refresh rate
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            float bestRefreshRate = display.getRefreshRate();
            for (float candidate : display.getSupportedRefreshRates()) {
                LimeLog.info("Examining refresh rate: "+candidate);

                if (candidate > bestRefreshRate) {
                    // Ensure the frame rate stays around 60 Hz for <= 60 FPS streams
                    if (prefConfig.fps <= 60) {
                        if (candidate >= 63) {
                            continue;
                        }
                    }

                    bestRefreshRate = candidate;
                }
            }
            LimeLog.info("Selected refresh rate: "+bestRefreshRate);
            windowLayoutParams.preferredRefreshRate = bestRefreshRate;
            displayRefreshRate = bestRefreshRate;
        }
        else {
            // Otherwise, the active display refresh rate is just
            // whatever is currently in use.
            displayRefreshRate = display.getRefreshRate();
        }

        // Enable HDMI ALLM (game mode) on Android R
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowLayoutParams.preferMinimalPostProcessing = true;
        }

        // Apply the display mode change
        getWindow().setAttributes(windowLayoutParams);

        // From 4.4 to 5.1 we can't ask for a 4K display mode, so we'll
        // need to hint the OS to provide one.
        boolean aspectRatioMatch = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            // On KitKat and later (where we can use the whole screen via immersive mode), we'll
            // calculate whether we need to scale by aspect ratio or not. If not, we'll use
            // setFixedSize so we can handle 4K properly. The only known devices that have
            // >= 4K screens have exactly 4K screens, so we'll be able to hit this good path
            // on these devices. On Marshmallow, we can start changing to 4K manually but no
            // 4K devices run 6.0 at the moment.
            Point screenSize = new Point(0, 0);
            display.getSize(screenSize);

            double screenAspectRatio = ((double)screenSize.y) / screenSize.x;
            double streamAspectRatio = ((double)prefConfig.height) / prefConfig.width;
            if (Math.abs(screenAspectRatio - streamAspectRatio) < 0.001) {
                LimeLog.info("Stream has compatible aspect ratio with output display");
                aspectRatioMatch = true;
            }
        }

        if (prefConfig.stretchVideo || aspectRatioMatch) {
            // Set the surface to the size of the video
            streamView.getHolder().setFixedSize(prefConfig.width, prefConfig.height);
        }
        else {
            // Set the surface to scale based on the aspect ratio of the stream
            streamView.setDesiredAspectRatio((double)prefConfig.width / (double)prefConfig.height);
        }

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            // TVs may take a few moments to switch refresh rates, and we can probably assume
            // it will be eventually activated.
            // TODO: Improve this
            return displayRefreshRate;
        }
        else {
            // Use the lower of the current refresh rate and the selected refresh rate.
            // The preferred refresh rate may not actually be applied (ex: Battery Saver mode).
            return Math.min(getWindowManager().getDefaultDisplay().getRefreshRate(), displayRefreshRate);
        }
    }

    @SuppressLint("InlinedApi")
    private final Runnable hideSystemUi = new Runnable() {
            @Override
            public void run() {
                // TODO: Do we want to use WindowInsetsController here on R+ instead of
                // SYSTEM_UI_FLAG_IMMERSIVE_STICKY? They seem to do the same thing as of S...

                // In multi-window mode on N+, we need to drop our layout flags or we'll
                // be drawing underneath the system UI.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
                    Game.this.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                }
                // Use immersive mode on 4.4+ or standard low profile on previous builds
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Game.this.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                else {
                    Game.this.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LOW_PROFILE);
                }
            }
    };

    private void hideSystemUi(int delay) {
        Handler h = getWindow().getDecorView().getHandler();
        if (h != null) {
            h.removeCallbacks(hideSystemUi);
            h.postDelayed(hideSystemUi, delay);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);

        // In multi-window, we don't want to use the full-screen layout
        // flag. It will cause us to collide with the system UI.
        // This function will also be called for PiP so we can cover
        // that case here too.
        if (isInMultiWindowMode) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // Disable performance optimizations for foreground
            getWindow().setSustainedPerformanceMode(false);
            decoderRenderer.notifyVideoBackground();
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // Enable performance optimizations for foreground
            getWindow().setSustainedPerformanceMode(true);
            decoderRenderer.notifyVideoForeground();
        }

        // Correct the system UI visibility flags
        hideSystemUi(50);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        currentIntent = intent;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Intent.ACTION_VIEW.equals(currentIntent.getAction()) && currentIntent.getData() != null) {
            getServerInfo(currentIntent.getData());
            return;
        }

        if (attemptedConnection) {
            return;
        } else {
            attemptedConnection = true;
        }

        appName = currentIntent.getStringExtra(EXTRA_APP_NAME);
        pcName = currentIntent.getStringExtra(EXTRA_PC_NAME);

        String host = currentIntent.getStringExtra(EXTRA_HOST);
        int appId = currentIntent.getIntExtra(EXTRA_APP_ID, StreamConfiguration.INVALID_APP_ID);
        String uniqueId = currentIntent.getStringExtra(EXTRA_UNIQUE_ID);
        String uuid = currentIntent.getStringExtra(EXTRA_PC_UUID);
        boolean appSupportsHdr = currentIntent.getBooleanExtra(EXTRA_APP_HDR, false);
        X509Certificate derCert = (X509Certificate) currentIntent.getSerializableExtra(EXTRA_SERVER_CERT);

        // Warn the user if they're on a metered connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr.isActiveNetworkMetered()) {
            displayTransientMessage(getResources().getString(R.string.conn_metered));
        }

        // Make sure Wi-Fi is fully powered up
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            highPerfWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Moonlight High Perf Lock");
            highPerfWifiLock.setReferenceCounted(false);
            highPerfWifiLock.acquire();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                lowLatencyWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Moonlight Low Latency Lock");
                lowLatencyWifiLock.setReferenceCounted(false);
                lowLatencyWifiLock.acquire();
            }
        } catch (SecurityException e) {
            // Some Samsung Galaxy S10+/S10e devices throw a SecurityException from
            // WifiLock.acquire() even though we have android.permission.WAKE_LOCK in our manifest.
            LimeLog.warning(e);
        }

        X509Certificate serverCert = null;
        try {
            if (derCert != null) {
                serverCert = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(new ByteArrayInputStream(derCert.getEncoded()));
            }
        } catch (CertificateException e) {
            LimeLog.warning(e);
        }

        if (appId == StreamConfiguration.INVALID_APP_ID) {
            finish();
            return;
        }

        // Initialize the MediaCodec helper before creating the decoder
        GlPreferences glPrefs = GlPreferences.readPreferences(this);
        MediaCodecHelper.initialize(this, glPrefs.glRenderer);

        // Check if the user has enabled HDR
        boolean willStreamHdr = false;
        if (prefConfig.enableHdr) {
            // Start our HDR checklist
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Display display = getWindowManager().getDefaultDisplay();
                Display.HdrCapabilities hdrCaps = display.getHdrCapabilities();

                // We must now ensure our display is compatible with HDR10
                if (hdrCaps != null) {
                    // getHdrCapabilities() returns null on Lenovo Lenovo Mirage Solo (vega), Android 8.0
                    for (int hdrType : hdrCaps.getSupportedHdrTypes()) {
                        if (hdrType == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                            willStreamHdr = true;
                            break;
                        }
                    }
                }

                if (!willStreamHdr) {
                    // Nope, no HDR for us :(
                    Toast.makeText(this, "Display does not support HDR10", Toast.LENGTH_LONG).show();
                }
            }
            else {
                Toast.makeText(this, "HDR requires Android 7.0 or later", Toast.LENGTH_LONG).show();
            }
        }

        // Check if the user has enabled performance stats overlay
        if (prefConfig.enablePerfOverlay) {
            performanceOverlayView.setVisibility(View.VISIBLE);
        }

        decoderRenderer = new MediaCodecDecoderRenderer(
                this,
                prefConfig,
                new CrashListener() {
                    @Override
                    public void notifyCrash(Exception e) {
                        // The MediaCodec instance is going down due to a crash
                        // let's tell the user something when they open the app again

                        // We must use commit because the app will crash when we return from this function
                        tombstonePrefs.edit().putInt("CrashCount", tombstonePrefs.getInt("CrashCount", 0) + 1).commit();
                        reportedCrash = true;
                    }
                },
                tombstonePrefs.getInt("CrashCount", 0),
                connMgr.isActiveNetworkMetered(),
                willStreamHdr,
                glPrefs.glRenderer,
                this);

        // Don't stream HDR if the decoder can't support it
        if (willStreamHdr && !decoderRenderer.isHevcMain10Hdr10Supported()) {
            willStreamHdr = false;
            Toast.makeText(this, "Decoder does not support HEVC Main10HDR10", Toast.LENGTH_LONG).show();
        }

        // Display a message to the user if HEVC was forced on but we still didn't find a decoder
        if (prefConfig.videoFormat == PreferenceConfiguration.FORCE_H265_ON && !decoderRenderer.isHevcSupported()) {
            Toast.makeText(this, "No HEVC decoder found.\nFalling back to H.264.", Toast.LENGTH_LONG).show();
        }

        int gamepadMask = ControllerHandler.getAttachedControllerMask(this);
        if (!prefConfig.multiController) {
            // Always set gamepad 1 present for when multi-controller is
            // disabled for games that don't properly support detection
            // of gamepads removed and replugged at runtime.
            gamepadMask = 1;
        }
        if (prefConfig.onscreenController) {
            // If we're using OSC, always set at least gamepad 1.
            gamepadMask |= 1;
        }

        // Set to the optimal mode for streaming
        float displayRefreshRate = prepareDisplayForRendering();
        LimeLog.info("Display refresh rate: "+displayRefreshRate);

        // If the user requested frame pacing using a capped FPS, we will need to change our
        // desired FPS setting here in accordance with the active display refresh rate.
        int roundedRefreshRate = Math.round(displayRefreshRate);
        int chosenFrameRate = prefConfig.fps;
        if (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS) {
            if (prefConfig.fps >= roundedRefreshRate) {
                if (prefConfig.fps > roundedRefreshRate + 3) {
                    // Use frame drops when rendering above the screen frame rate
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Using drop mode for FPS > Hz");
                } else if (roundedRefreshRate <= 49) {
                    // Let's avoid clearly bogus refresh rates and fall back to legacy rendering
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED;
                    LimeLog.info("Bogus refresh rate: " + roundedRefreshRate);
                }
                else {
                    chosenFrameRate = roundedRefreshRate - 1;
                    LimeLog.info("Adjusting FPS target for screen to " + chosenFrameRate);
                }
            }
        }

        boolean vpnActive = NetHelper.isActiveNetworkVpn(this);
        if (vpnActive) {
            LimeLog.info("Detected active network is a VPN");
        }

        StreamConfiguration config = new StreamConfiguration.Builder()
                .setResolution(prefConfig.width, prefConfig.height)
                .setLaunchRefreshRate(prefConfig.fps)
                .setRefreshRate(chosenFrameRate)
                .setApp(new NvApp(appName != null ? appName : "app", appId, appSupportsHdr))
                .setBitrate(prefConfig.bitrate)
                .setEnableSops(prefConfig.enableSops)
                .enableLocalAudioPlayback(prefConfig.playHostAudio)
                .setMaxPacketSize(vpnActive ? 1024 : 1392) // Lower MTU on VPN
                .setRemoteConfiguration(vpnActive ? // Use remote optimizations on VPN
                        StreamConfiguration.STREAM_CFG_REMOTE :
                        StreamConfiguration.STREAM_CFG_AUTO)
                .setHevcBitratePercentageMultiplier(75)
                .setHevcSupported(decoderRenderer.isHevcSupported())
                .setEnableHdr(willStreamHdr)
                .setAttachedGamepadMask(gamepadMask)
                .setClientRefreshRateX100((int)(displayRefreshRate * 100))
                .setAudioConfiguration(prefConfig.audioConfiguration)
                .setAudioEncryption(true)
                .setHttpPort(prefConfig.httpPort)
                .setHttpsPort(prefConfig.httpsPort)
                .setAudioPort(prefConfig.audioPort)
                .setVideoPort(prefConfig.videoPort)
                .setControlPort(prefConfig.controlPort)
                .setRtspPort(prefConfig.rtspPort)
                .build();

        // Initialize the connection
        conn = new NvConnection(host, uniqueId, config, PlatformBinding.getCryptoProvider(this), serverCert);
        controllerHandler = new ControllerHandler(this, conn, this, prefConfig);
        keyboardTranslator = new KeyboardTranslator();

        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(controllerHandler, null);
        inputManager.registerInputDeviceListener(keyboardTranslator, null);

        // Initialize touch contexts
        for (int i = 0; i < touchContextMap.length; i++) {
            if (!prefConfig.touchscreenTrackpad) {
                touchContextMap[i] = new AbsoluteTouchContext(conn, i, streamView);
            }
            else {
                touchContextMap[i] = new RelativeTouchContext(conn, i,
                        REFERENCE_HORIZ_RES, REFERENCE_VERT_RES,
                        streamView, prefConfig);
            }
        }

        // Use sustained performance mode on N+ to ensure consistent
        // CPU availability
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getWindow().setSustainedPerformanceMode(true);
        }

        if (prefConfig.onscreenController) {
            // create virtual onscreen controller
            virtualController = new VirtualController(controllerHandler,
                    (FrameLayout)streamView.getParent(),
                    this);
            virtualController.refreshLayout();
            virtualController.show();
        }

        if (prefConfig.usbDriver) {
            // Start the USB driver
            bindService(new Intent(this, UsbDriverService.class),
                    usbDriverServiceConnection, Service.BIND_AUTO_CREATE);
        }

        if (!decoderRenderer.isAvcSupported()) {
            // If we can't find an AVC decoder, we can't proceed
            showErrorDialog(getString(R.string.conn_error_title),
                    "This device or ROM doesn't support hardware accelerated H.264 playback.");
            return;
        }

        // Update GameManager state to indicate we're "loading" while connecting
        UiHelper.notifyStreamConnecting(Game.this);

        decoderRenderer.setRenderTarget(streamView.getHolder());
        conn.start(new AndroidAudioRenderer(Game.this, prefConfig.enableAudioFx),
                decoderRenderer, Game.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (controllerHandler != null) {
            inputManager.unregisterInputDeviceListener(controllerHandler);
        }
        if (keyboardTranslator != null) {
            inputManager.unregisterInputDeviceListener(keyboardTranslator);
        }

        if (lowLatencyWifiLock != null) {
            lowLatencyWifiLock.release();
        }
        if (highPerfWifiLock != null) {
            highPerfWifiLock.release();
        }

        if (connectedToUsbDriverService) {
            // Unbind from the discovery service
            unbindService(usbDriverServiceConnection);
        }

        // Destroy the capture provider
        inputCaptureProvider.destroy();
    }

    @Override
    protected void onStop() {
        super.onStop();

        SpinnerDialog.closeDialogs(this);
        Dialog.closeDialogs();

        if (virtualController != null) {
            virtualController.hide();
        }

        if (conn != null) {
            int videoFormat = decoderRenderer.getActiveVideoFormat();

            displayedFailureDialog = true;
            stopConnection();

            if (prefConfig.enableLatencyToast) {
                int averageEndToEndLat = decoderRenderer.getAverageEndToEndLatency();
                int averageDecoderLat = decoderRenderer.getAverageDecoderLatency();
                String message = null;
                if (averageEndToEndLat > 0) {
                    message = getResources().getString(R.string.conn_client_latency)+" "+averageEndToEndLat+" ms";
                    if (averageDecoderLat > 0) {
                        message += " ("+getResources().getString(R.string.conn_client_latency_hw)+" "+averageDecoderLat+" ms)";
                    }
                }
                else if (averageDecoderLat > 0) {
                    message = getResources().getString(R.string.conn_hardware_latency)+" "+averageDecoderLat+" ms";
                }

                // Add the video codec to the post-stream toast
                if (message != null) {
                    if (videoFormat == MoonBridge.VIDEO_FORMAT_H265_MAIN10) {
                        message += " [HEVC HDR]";
                    }
                    else if (videoFormat == MoonBridge.VIDEO_FORMAT_H265) {
                        message += " [HEVC]";
                    }
                    else if (videoFormat == MoonBridge.VIDEO_FORMAT_H264) {
                        message += " [H.264]";
                    }
                }

                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
            }

            // Clear the tombstone count if we terminated normally
            if (!reportedCrash && tombstonePrefs.getInt("CrashCount", 0) != 0) {
                tombstonePrefs.edit()
                        .putInt("CrashCount", 0)
                        .putInt("LastNotifiedCrashCount", 0)
                        .apply();
            }
        }

        finish();
    }

    private void quitApp(Runnable doAfterQuit) {
        String appName = currentIntent.getStringExtra(EXTRA_APP_NAME);
        LimeLog.info(getString(R.string.applist_quit_app) + " " + appName + "...");
        try {
            if (conn.stopApp()) {
                LimeLog.info(getString(R.string.applist_quit_success) + " " + appName);
            } else {
                LimeLog.severe(getString(R.string.applist_quit_fail) + " " + appName);
            }
        } catch (GfeHttpResponseException e) {
            if (e.getErrorCode() == 599) {
                LimeLog.severe("This session wasn't started by this device," +
                        " so it cannot be quit. End streaming on the original " +
                        "device or the PC itself. (Error code: " + e.getErrorCode() + ")", e);
            } else {
                LimeLog.severe(e);
            }
        } catch (UnknownHostException e) {
            LimeLog.severe(getString(R.string.error_unknown_host), e);
        } catch (FileNotFoundException e) {
            LimeLog.severe(getString(R.string.error_404), e);
        } catch (IOException | XmlPullParserException e) {
            LimeLog.severe(e.getMessage());
        } finally {
            if (doAfterQuit != null) {
                doAfterQuit.run();
            }
        }
    }

    private void stopVm() {
        try {
            String sessionKey = currentIntent.getStringExtra(EXTRA_SESSION_KEY);
            OneplayApi.getInstance().stopVm(sessionKey);
        } catch (IOException e) {
            LimeLog.severe(e);
        }
    }

    private final Runnable toggleGrab = new Runnable() {
        @Override
        public void run() {
            if (grabbedInput) {
                inputCaptureProvider.disableCapture();
            }
            else {
                inputCaptureProvider.enableCapture();
            }

            grabbedInput = !grabbedInput;
        }
    };

    // Returns true if the key stroke was consumed
    private boolean handleSpecialKeys(int androidKeyCode, boolean down) {
        int modifierMask = 0;

        if (androidKeyCode == KeyEvent.KEYCODE_CTRL_LEFT ||
            androidKeyCode == KeyEvent.KEYCODE_CTRL_RIGHT) {
            modifierMask = KeyboardPacket.MODIFIER_CTRL;
        }
        else if (androidKeyCode == KeyEvent.KEYCODE_SHIFT_LEFT ||
                 androidKeyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            modifierMask = KeyboardPacket.MODIFIER_SHIFT;
        }
        else if (androidKeyCode == KeyEvent.KEYCODE_ALT_LEFT ||
                 androidKeyCode == KeyEvent.KEYCODE_ALT_RIGHT) {
            modifierMask = KeyboardPacket.MODIFIER_ALT;
        }

        if (down) {
            this.modifierFlags |= modifierMask;
        }
        else {
            this.modifierFlags &= ~modifierMask;
        }

        // Check if Ctrl+Shift+Z is pressed
        if (androidKeyCode == KeyEvent.KEYCODE_Z &&
            (modifierFlags & (KeyboardPacket.MODIFIER_CTRL | KeyboardPacket.MODIFIER_SHIFT)) ==
                (KeyboardPacket.MODIFIER_CTRL | KeyboardPacket.MODIFIER_SHIFT))
        {
            if (down) {
                // Now that we've pressed the magic combo
                // we'll wait for one of the keys to come up
                grabComboDown = true;
            }
            else {
                // Toggle the grab if Z comes up
                Handler h = getWindow().getDecorView().getHandler();
                if (h != null) {
                    h.postDelayed(toggleGrab, 250);
                }

                grabComboDown = false;
            }

            return true;
        }
        // Toggle the grab if control or shift comes up
        else if (grabComboDown) {
            Handler h = getWindow().getDecorView().getHandler();
            if (h != null) {
                h.postDelayed(toggleGrab, 250);
            }

            grabComboDown = false;
            return true;
        }

        // Not a special combo
        return false;
    }

    // We cannot simply use modifierFlags for all key event processing, because
    // some IMEs will not generate real key events for pressing Shift. Instead
    // they will simply send key events with isShiftPressed() returning true,
    // and we will need to send the modifier flag ourselves.
    private byte getModifierState(KeyEvent event) {
        // Start with the global modifier state to ensure we cover the case
        // detailed in https://github.com/moonlight-stream/moonlight-android/issues/840
        byte modifier = getModifierState();
        if (event.isShiftPressed()) {
            modifier |= KeyboardPacket.MODIFIER_SHIFT;
        }
        if (event.isCtrlPressed()) {
            modifier |= KeyboardPacket.MODIFIER_CTRL;
        }
        if (event.isAltPressed()) {
            modifier |= KeyboardPacket.MODIFIER_ALT;
        }
        return modifier;
    }

    private byte getModifierState() {
        return (byte) modifierFlags;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return handleKeyDown(event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean handleKeyDown(KeyEvent event) {
        // Pass-through virtual navigation keys
        if ((event.getFlags() & KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0) {
            return false;
        }

        // Handle a synthetic back button event that some Android OS versions
        // create as a result of a right-click. This event WILL repeat if
        // the right mouse button is held down, so we ignore those.
        int eventSource = event.getSource();
        if ((eventSource == InputDevice.SOURCE_MOUSE ||
                eventSource == InputDevice.SOURCE_MOUSE_RELATIVE) &&
                event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

            // Send the right mouse button event if mouse back and forward
            // are disabled. If they are enabled, handleMotionEvent() will take
            // care of this.
            if (!prefConfig.mouseNavButtons) {
                conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
            }

            // Always return true, otherwise the back press will be propagated
            // up to the parent and finish the activity.
            return true;
        }

        boolean handled = false;

        if (ControllerHandler.isGameControllerDevice(event.getDevice())) {
            // Always try the controller handler first, unless it's an alphanumeric keyboard device.
            // Otherwise, controller handler will eat keyboard d-pad events.
            handled = controllerHandler.handleButtonDown(event);
        }

        if (!handled) {
            // Try the keyboard handler
            short translated = keyboardTranslator.translate(event.getKeyCode(), event.getDeviceId());
            if (translated == 0) {
                return false;
            }

            // Let this method take duplicate key down events
            if (handleSpecialKeys(event.getKeyCode(), true)) {
                return true;
            }

            // Eat repeat down events
            if (event.getRepeatCount() > 0) {
                return true;
            }

            // Pass through keyboard input if we're not grabbing
            if (!grabbedInput) {
                return false;
            }

            byte modifiers = getModifierState(event);
            if (KeyboardTranslator.needsShift(event.getKeyCode())) {
                modifiers |= KeyboardPacket.MODIFIER_SHIFT;
            }

            // Hack: gboard doesn't send shift key event for symbols or key up it before a key pressed
            if ((modifiers & KeyboardPacket.MODIFIER_SHIFT) == KeyboardPacket.MODIFIER_SHIFT &&
                    (getModifierState() & KeyboardPacket.MODIFIER_SHIFT) != KeyboardPacket.MODIFIER_SHIFT) {
                conn.sendKeyboardInput(keyboardTranslator.translate(KeyEvent.KEYCODE_SHIFT_LEFT, event.getDeviceId()), KeyboardPacket.KEY_DOWN, modifiers);
            }

            conn.sendKeyboardInput(translated, KeyboardPacket.KEY_DOWN, modifiers);
        }

        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return handleKeyUp(event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean handleKeyUp(KeyEvent event) {
        // Pass-through virtual navigation keys
        if ((event.getFlags() & KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0) {
            return false;
        }

        // Handle a synthetic back button event that some Android OS versions
        // create as a result of a right-click.
        int eventSource = event.getSource();
        if ((eventSource == InputDevice.SOURCE_MOUSE ||
                eventSource == InputDevice.SOURCE_MOUSE_RELATIVE) &&
                event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

            // Send the right mouse button event if mouse back and forward
            // are disabled. If they are enabled, handleMotionEvent() will take
            // care of this.
            if (!prefConfig.mouseNavButtons) {
                conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
            }

            // Always return true, otherwise the back press will be propagated
            // up to the parent and finish the activity.
            return true;
        }

        boolean handled = false;
        if (ControllerHandler.isGameControllerDevice(event.getDevice())) {
            // Always try the controller handler first, unless it's an alphanumeric keyboard device.
            // Otherwise, controller handler will eat keyboard d-pad events.
            handled = controllerHandler.handleButtonUp(event);
        }

        if (!handled) {
            // Try the keyboard handler
            short translated = keyboardTranslator.translate(event.getKeyCode(), event.getDeviceId());
            if (translated == 0) {
                return false;
            }

            if (handleSpecialKeys(event.getKeyCode(), false)) {
                return true;
            }

            // Pass through keyboard input if we're not grabbing
            if (!grabbedInput) {
                return false;
            }

            byte modifiers = getModifierState(event);
            if (KeyboardTranslator.needsShift(event.getKeyCode())) {
                modifiers |= KeyboardPacket.MODIFIER_SHIFT;
            }
            conn.sendKeyboardInput(translated, KeyboardPacket.KEY_UP, modifiers);

            // Hack: gboard doesn't send shift key event for symbols or key up it before a key pressed
            if ((modifiers & KeyboardPacket.MODIFIER_SHIFT) == KeyboardPacket.MODIFIER_SHIFT &&
                    (getModifierState() & KeyboardPacket.MODIFIER_SHIFT) != KeyboardPacket.MODIFIER_SHIFT) {
                conn.sendKeyboardInput(keyboardTranslator.translate(KeyEvent.KEYCODE_SHIFT_LEFT, event.getDeviceId()), KeyboardPacket.KEY_UP, modifiers);
            }
        }

        return true;
    }

    private TouchContext getTouchContext(int actionIndex)
    {
        if (actionIndex < touchContextMap.length) {
            return touchContextMap[actionIndex];
        }
        else {
            return null;
        }
    }

    @Override
    public void toggleKeyboard() {
        LimeLog.info("Toggling keyboard overlay");
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);
    }

    @Override
    public void hideKeyboard(View view, ResultReceiver receiver) {
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0, receiver);
        }
    }

    // Returns true if the event was consumed
    // NB: View is only present if called from a view callback
    private boolean handleMotionEvent(View view, MotionEvent event) {
        // Pass through keyboard input if we're not grabbing
        if (!grabbedInput) {
            return false;
        }

        int eventSource = event.getSource();
        if ((eventSource & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
            if (controllerHandler.handleMotionEvent(event)) {
                return true;
            }
        }
        else if ((eventSource & InputDevice.SOURCE_CLASS_POINTER) != 0 ||
                 (eventSource & InputDevice.SOURCE_CLASS_POSITION) != 0 ||
                 eventSource == InputDevice.SOURCE_MOUSE_RELATIVE)
        {
            // This case is for mice and non-finger touch devices
            if (eventSource == InputDevice.SOURCE_MOUSE ||
                    (eventSource & InputDevice.SOURCE_CLASS_POSITION) != 0 || // SOURCE_TOUCHPAD
                    eventSource == InputDevice.SOURCE_MOUSE_RELATIVE ||
                    (event.getPointerCount() >= 1 &&
                            (event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE ||
                                    event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS ||
                                    event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER)) ||
                    eventSource == 12290) // 12290 = Samsung DeX mode desktop mouse
            {
                int changedButtons = event.getButtonState() ^ lastButtonState;

                // Ignore mouse input if we're not capturing from our input source
                if (!inputCaptureProvider.isCapturingActive()) {
                    // We return true here because otherwise the events may end up causing
                    // Android to synthesize d-pad events.
                    return true;
                }

                // Always update the position before sending any button events. If we're
                // dealing with a stylus without hover support, our position might be
                // significantly different than before.
                if (inputCaptureProvider.eventHasRelativeMouseAxes(event)) {
                    // Send the deltas straight from the motion event
                    short deltaX = (short)inputCaptureProvider.getRelativeAxisX(event);
                    short deltaY = (short)inputCaptureProvider.getRelativeAxisY(event);

                    if (deltaX != 0 || deltaY != 0) {
                        if (prefConfig.absoluteMouseMode) {
                            conn.sendMouseMoveAsMousePosition(deltaX, deltaY, (short)view.getWidth(), (short)view.getHeight());
                        }
                        else {
                            conn.sendMouseMove(deltaX, deltaY);
                        }
                    }
                }
                else if ((eventSource & InputDevice.SOURCE_CLASS_POSITION) != 0) {
                    // If this input device is not associated with the view itself (like a trackpad),
                    // we'll convert the device-specific coordinates to use to send the cursor position.
                    // This really isn't ideal but it's probably better than nothing.
                    //
                    // Trackpad on newer versions of Android (Oreo and later) should be caught by the
                    // relative axes case above. If we get here, we're on an older version that doesn't
                    // support pointer capture.
                    InputDevice device = event.getDevice();
                    if (device != null) {
                        InputDevice.MotionRange xRange = device.getMotionRange(MotionEvent.AXIS_X, eventSource);
                        InputDevice.MotionRange yRange = device.getMotionRange(MotionEvent.AXIS_Y, eventSource);

                        // All touchpads coordinate planes should start at (0, 0)
                        if (xRange != null && yRange != null && xRange.getMin() == 0 && yRange.getMin() == 0) {
                            int xMax = (int)xRange.getMax();
                            int yMax = (int)yRange.getMax();

                            // Touchpads must be smaller than (65535, 65535)
                            if (xMax <= Short.MAX_VALUE && yMax <= Short.MAX_VALUE) {
                                conn.sendMousePosition((short)event.getX(), (short)event.getY(),
                                                       (short)xMax, (short)yMax);
                            }
                        }
                    }
                }
                else if (view != null) {
                    // Otherwise send absolute position based on the view for SOURCE_CLASS_POINTER
                    updateMousePosition(view, event);
                }

                if (event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
                    // Send the vertical scroll packet
                    conn.sendMouseHighResScroll((short)(event.getAxisValue(MotionEvent.AXIS_VSCROLL) * 120));
                }

                if ((changedButtons & MotionEvent.BUTTON_PRIMARY) != 0) {
                    if ((event.getButtonState() & MotionEvent.BUTTON_PRIMARY) != 0) {
                        conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
                    }
                    else {
                        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
                    }
                }

                // Mouse secondary or stylus primary is right click (stylus down is left click)
                if ((changedButtons & (MotionEvent.BUTTON_SECONDARY | MotionEvent.BUTTON_STYLUS_PRIMARY)) != 0) {
                    if ((event.getButtonState() & (MotionEvent.BUTTON_SECONDARY | MotionEvent.BUTTON_STYLUS_PRIMARY)) != 0) {
                        conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
                    }
                    else {
                        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
                    }
                }

                // Mouse tertiary or stylus secondary is middle click
                if ((changedButtons & (MotionEvent.BUTTON_TERTIARY | MotionEvent.BUTTON_STYLUS_SECONDARY)) != 0) {
                    if ((event.getButtonState() & (MotionEvent.BUTTON_TERTIARY | MotionEvent.BUTTON_STYLUS_SECONDARY)) != 0) {
                        conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_MIDDLE);
                    }
                    else {
                        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_MIDDLE);
                    }
                }

                if (prefConfig.mouseNavButtons) {
                    if ((changedButtons & MotionEvent.BUTTON_BACK) != 0) {
                        if ((event.getButtonState() & MotionEvent.BUTTON_BACK) != 0) {
                            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_X1);
                        }
                        else {
                            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_X1);
                        }
                    }

                    if ((changedButtons & MotionEvent.BUTTON_FORWARD) != 0) {
                        if ((event.getButtonState() & MotionEvent.BUTTON_FORWARD) != 0) {
                            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_X2);
                        }
                        else {
                            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_X2);
                        }
                    }
                }

                // Handle stylus presses
                if (event.getPointerCount() == 1 && event.getActionIndex() == 0) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                            lastAbsTouchDownTime = event.getEventTime();
                            lastAbsTouchDownX = event.getX(0);
                            lastAbsTouchDownY = event.getY(0);

                            // Stylus is left click
                            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
                        } else if (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) {
                            lastAbsTouchDownTime = event.getEventTime();
                            lastAbsTouchDownX = event.getX(0);
                            lastAbsTouchDownY = event.getY(0);

                            // Eraser is right click
                            conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
                        }
                    }
                    else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                            lastAbsTouchUpTime = event.getEventTime();
                            lastAbsTouchUpX = event.getX(0);
                            lastAbsTouchUpY = event.getY(0);

                            // Stylus is left click
                            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
                        } else if (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) {
                            lastAbsTouchUpTime = event.getEventTime();
                            lastAbsTouchUpX = event.getX(0);
                            lastAbsTouchUpY = event.getY(0);

                            // Eraser is right click
                            conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
                        }
                    }
                }

                lastButtonState = event.getButtonState();
            }
            // This case is for fingers
            else
            {
                if (virtualController != null &&
                        (virtualController.getControllerMode() == VirtualController.ControllerMode.MoveButtons ||
                         virtualController.getControllerMode() == VirtualController.ControllerMode.ResizeButtons)) {
                    // Ignore presses when the virtual controller is being configured
                    return true;
                }

                if (view == null && !prefConfig.touchscreenTrackpad) {
                    // Absolute touch events should be dropped outside our view.
                    return true;
                }

                int actionIndex = event.getActionIndex();

                int eventX = (int)event.getX(actionIndex);
                int eventY = (int)event.getY(actionIndex);

                // Special handling for 3 finger gesture
                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN &&
                        event.getPointerCount() == 3) {
                    // Three fingers down
                    threeFingerDownTime = event.getEventTime();

                    // Cancel the first and second touches to avoid
                    // erroneous events
                    for (TouchContext aTouchContext : touchContextMap) {
                        aTouchContext.cancelTouch();
                    }

                    return true;
                }

                TouchContext context = getTouchContext(actionIndex);
                if (context == null) {
                    return false;
                }

                switch (event.getActionMasked())
                {
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_DOWN:
                    for (TouchContext touchContext : touchContextMap) {
                        touchContext.setPointerCount(event.getPointerCount());
                    }
                    context.touchDownEvent(eventX, eventY, event.getEventTime(), true);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                    if (event.getPointerCount() == 1 &&
                            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || (event.getFlags() & MotionEvent.FLAG_CANCELED) == 0)) {
                        // All fingers up
                        if (event.getEventTime() - threeFingerDownTime < THREE_FINGER_TAP_THRESHOLD) {
                            // This is a 3 finger tap to bring up the keyboard
                            toggleKeyboard();
                            return true;
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (event.getFlags() & MotionEvent.FLAG_CANCELED) != 0) {
                        context.cancelTouch();
                    }
                    else {
                        context.touchUpEvent(eventX, eventY, event.getEventTime());
                    }

                    for (TouchContext touchContext : touchContextMap) {
                        touchContext.setPointerCount(event.getPointerCount() - 1);
                    }
                    if (actionIndex == 0 && event.getPointerCount() > 1 && !context.isCancelled()) {
                        // The original secondary touch now becomes primary
                        context.touchDownEvent((int)event.getX(1), (int)event.getY(1), event.getEventTime(), false);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    // ACTION_MOVE is special because it always has actionIndex == 0
                    // We'll call the move handlers for all indexes manually

                    // First process the historical events
                    for (int i = 0; i < event.getHistorySize(); i++) {
                        for (TouchContext aTouchContextMap : touchContextMap) {
                            if (aTouchContextMap.getActionIndex() < event.getPointerCount())
                            {
                                aTouchContextMap.touchMoveEvent(
                                        (int)event.getHistoricalX(aTouchContextMap.getActionIndex(), i),
                                        (int)event.getHistoricalY(aTouchContextMap.getActionIndex(), i),
                                        event.getHistoricalEventTime(i));
                            }
                        }
                    }

                    // Now process the current values
                    for (TouchContext aTouchContextMap : touchContextMap) {
                        if (aTouchContextMap.getActionIndex() < event.getPointerCount())
                        {
                            aTouchContextMap.touchMoveEvent(
                                    (int)event.getX(aTouchContextMap.getActionIndex()),
                                    (int)event.getY(aTouchContextMap.getActionIndex()),
                                    event.getEventTime());
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    for (TouchContext aTouchContext : touchContextMap) {
                        aTouchContext.cancelTouch();
                        aTouchContext.setPointerCount(0);
                    }
                    break;
                default:
                    return false;
                }
            }

            // Handled a known source
            return true;
        }

        // Unknown class
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return handleMotionEvent(null, event) || super.onTouchEvent(event);

    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return handleMotionEvent(null, event) || super.onGenericMotionEvent(event);

    }

    private void updateMousePosition(View view, MotionEvent event) {
        // X and Y are already relative to the provided view object
        float eventX = event.getX(0);
        float eventY = event.getY(0);

        if (event.getPointerCount() == 1 && event.getActionIndex() == 0 &&
                (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER ||
                event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS))
        {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_HOVER_ENTER:
                case MotionEvent.ACTION_HOVER_EXIT:
                case MotionEvent.ACTION_HOVER_MOVE:
                    if (event.getEventTime() - lastAbsTouchUpTime <= STYLUS_UP_DEAD_ZONE_DELAY &&
                            Math.sqrt(Math.pow(eventX - lastAbsTouchUpX, 2) + Math.pow(eventY - lastAbsTouchUpY, 2)) <= STYLUS_UP_DEAD_ZONE_RADIUS) {
                        // Enforce a small deadzone between touch up and hover or touch down to allow more precise double-clicking
                        return;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    if (event.getEventTime() - lastAbsTouchDownTime <= STYLUS_DOWN_DEAD_ZONE_DELAY &&
                            Math.sqrt(Math.pow(eventX - lastAbsTouchDownX, 2) + Math.pow(eventY - lastAbsTouchDownY, 2)) <= STYLUS_DOWN_DEAD_ZONE_RADIUS) {
                        // Enforce a small deadzone between touch down and move or touch up to allow more precise double-clicking
                        return;
                    }
                    break;
            }
        }

        // We may get values slightly outside our view region on ACTION_HOVER_ENTER and ACTION_HOVER_EXIT.
        // Normalize these to the view size. We can't just drop them because we won't always get an event
        // right at the boundary of the view, so dropping them would result in our cursor never really
        // reaching the sides of the screen.
        eventX = Math.min(Math.max(eventX, 0), view.getWidth());
        eventY = Math.min(Math.max(eventY, 0), view.getHeight());

        conn.sendMousePosition((short)eventX, (short)eventY, (short)view.getWidth(), (short)view.getHeight());
    }

    @Override
    public boolean onGenericMotion(View view, MotionEvent event) {
        return handleMotionEvent(view, event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        return handleMotionEvent(view, event);
    }

    @Override
    public void stageStarting(final String stage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (spinner != null) {
                    spinner.setMessage(getResources().getString(R.string.conn_starting) + " " + stage);
                }
            }
        });
    }

    @Override
    public void stageComplete(String stage) {
    }

    private void stopConnection() {
        if (connecting || connected) {
            connecting = connected = false;
            updatePipAutoEnter();

            controllerHandler.stop();

            // Update GameManager state to indicate we're no longer in game
            UiHelper.notifyStreamEnded(this);

            // Stop may take a few hundred ms to do some network I/O to tell
            // the server we're going away and clean up. Let it run in a separate
            // thread to keep things smooth for the UI. Inside moonlight-common,
            // we prevent another thread from starting a connection before and
            // during the process of stopping this one.
            new Thread() {
                public void run() {
                    conn.stop(() -> {
                        if (isNeedRefresh) {
                            isNeedRefresh = false;
                            restartActivity();
                        } else {
                            quitApp(() -> {
                                if (isNeedReload) {
                                    isNeedReload = false;
                                    restartActivity();
                                } else {
                                    stopVm();
                                }
                            });
                        }
                    });
                }
            }.start();
        }
    }

    @Override
    public void stageFailed(final String stage, final int portFlags, final int errorCode) {
        // Perform a connection test if the failure could be due to a blocked port
        // This does network I/O, so don't do it on the main thread.
        final int portTestResult = MoonBridge.testClientConnectivity(ServerHelper.CONNECTION_TEST_SERVER, 443, portFlags);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!displayedFailureDialog) {
                    displayedFailureDialog = true;
                    LimeLog.severe(new Exception(stage + " failed: " + errorCode));

                    // If video initialization failed and the surface is still valid, display extra information for the user
                    if (stage.contains("video") && streamView.getHolder().getSurface().isValid()) {
                        Toast.makeText(Game.this, getResources().getText(R.string.video_decoder_init_failed), Toast.LENGTH_LONG).show();
                    }

                    String dialogText = getResources().getString(R.string.conn_error_msg) + " " + stage +" (error "+errorCode+")";

                    if (portFlags != 0) {
                        dialogText += "\n\n" + getResources().getString(R.string.check_ports_msg) + "\n" +
                                MoonBridge.stringifyPortFlags(portFlags, "\n");
                    }

                    if (portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0)  {
                        dialogText += "\n\n" + getResources().getString(R.string.nettest_text_blocked);
                    }

                    showErrorDialog(getString(R.string.conn_error_title), dialogText);
                }
            }
        });
    }

    @Override
    public void connectionTerminated(final int errorCode) {
        // Perform a connection test if the failure could be due to a blocked port
        // This does network I/O, so don't do it on the main thread.
        final int portFlags = MoonBridge.getPortFlagsFromTerminationErrorCode(errorCode);
        final int portTestResult = MoonBridge.testClientConnectivity(ServerHelper.CONNECTION_TEST_SERVER,443, portFlags);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Let the display go to sleep now
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // Enable cursor visibility again
                inputCaptureProvider.disableCapture();

                // Disable meta key capture
                setMetaKeyCaptureState(false);

                if (!displayedFailureDialog) {
                    displayedFailureDialog = true;
                    stopConnection();

                    // Display the error dialog if it was an unexpected termination.
                    // Otherwise, just finish the activity immediately.
                    if (errorCode != MoonBridge.ML_ERROR_GRACEFUL_TERMINATION) {
                        String message;

                        if (portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0) {
                            // If we got a blocked result, that supersedes any other error message
                            message = getResources().getString(R.string.nettest_text_blocked);
                        }
                        else {
                            switch (errorCode) {
                                case MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC:
                                    message = getResources().getString(R.string.no_video_received_error);
                                    break;

                                case MoonBridge.ML_ERROR_NO_VIDEO_FRAME:
                                    message = getResources().getString(R.string.no_frame_received_error);
                                    break;

                                case MoonBridge.ML_ERROR_UNEXPECTED_EARLY_TERMINATION:
                                case MoonBridge.ML_ERROR_PROTECTED_CONTENT:
                                    message = getResources().getString(R.string.early_termination_error);
                                    break;

                                default:
                                    message = getResources().getString(R.string.conn_terminated_msg);
                                    break;
                            }
                        }

                        if (portFlags != 0) {
                            message += "\n\n" + getResources().getString(R.string.check_ports_msg) + "\n" +
                                    MoonBridge.stringifyPortFlags(portFlags, "\n");
                        }

                        LimeLog.severe(new Exception("Connection terminated: " + errorCode + ". " + message));
                        showErrorDialog(getString(R.string.conn_terminated_title), message);
                    }
                    else {
                        finish();
                    }
                }
            }
        });
    }

    @Override
    public void connectionStatusUpdate(final int connectionStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (prefConfig.disableWarnings) {
                    return;
                }

                if (connectionStatus == MoonBridge.CONN_STATUS_POOR) {
                    if (prefConfig.bitrate > 5000) {
                        notificationOverlayView.setText(getResources().getString(R.string.slow_connection_msg));
                    }
                    else {
                        notificationOverlayView.setText(getResources().getString(R.string.poor_connection_msg));
                    }

                    requestedNotificationOverlayVisibility = View.VISIBLE;
                }
                else if (connectionStatus == MoonBridge.CONN_STATUS_OKAY) {
                    requestedNotificationOverlayVisibility = View.GONE;
                }

                if (!isHidingOverlays) {
                    notificationOverlayView.setVisibility(requestedNotificationOverlayVisibility);
                }
            }
        });
    }

    @Override
    public void connectionStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (spinner != null) {
                    spinner.dismiss();
                    spinner = null;
                }

                connected = true;
                connecting = false;
                updatePipAutoEnter();

                // Hide the mouse cursor now after a short delay.
                // Doing it before dismissing the spinner seems to be undone
                // when the spinner gets displayed. On Android Q, even now
                // is too early to capture. We will delay a second to allow
                // the spinner to dismiss before capturing.
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        inputCaptureProvider.enableCapture();
                    }
                }, 500);

                // Keep the display on
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // Enable meta key capture
                setMetaKeyCaptureState(true);

                // Update GameManager state to indicate we're in game
                UiHelper.notifyStreamConnected(Game.this);

                hideSystemUi(1000);
            }
        });
    }

    @Override
    public void displayMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Game.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void displayTransientMessage(final String message) {
        if (!prefConfig.disableWarnings) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Game.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void rumble(short controllerNumber, short lowFreqMotor, short highFreqMotor) {
        LimeLog.info(String.format((Locale)null, "Rumble on gamepad %d: %04x %04x", controllerNumber, lowFreqMotor, highFreqMotor));

        controllerHandler.handleRumble(controllerNumber, lowFreqMotor, highFreqMotor);
    }

    @Override
    public void setHdrMode(boolean enabled) {
        LimeLog.info("Display HDR mode: " + (enabled ? "enabled" : "disabled"));
        decoderRenderer.setHdrMode(enabled);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!surfaceCreated) {
            throw new IllegalStateException("Surface changed before creation!");
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceCreated = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Tell the OS about our frame rate to allow it to adapt the display refresh rate appropriately
            holder.getSurface().setFrameRate(prefConfig.fps, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (!surfaceCreated) {
            throw new IllegalStateException("Surface destroyed before creation!");
        }

        if (attemptedConnection) {
            // Let the decoder know immediately that the surface is gone
            decoderRenderer.prepareForStop();

            if (connected) {
                stopConnection();
            }
        }
    }

    @Override
    public void mouseMove(int deltaX, int deltaY) {
        conn.sendMouseMove((short) deltaX, (short) deltaY);
    }

    @Override
    public void mouseButtonEvent(int buttonId, boolean down) {
        byte buttonIndex;

        switch (buttonId)
        {
        case EvdevListener.BUTTON_LEFT:
            buttonIndex = MouseButtonPacket.BUTTON_LEFT;
            break;
        case EvdevListener.BUTTON_MIDDLE:
            buttonIndex = MouseButtonPacket.BUTTON_MIDDLE;
            break;
        case EvdevListener.BUTTON_RIGHT:
            buttonIndex = MouseButtonPacket.BUTTON_RIGHT;
            break;
        case EvdevListener.BUTTON_X1:
            buttonIndex = MouseButtonPacket.BUTTON_X1;
            break;
        case EvdevListener.BUTTON_X2:
            buttonIndex = MouseButtonPacket.BUTTON_X2;
            break;
        default:
            LimeLog.warning("Unhandled button: "+buttonId);
            return;
        }

        if (down) {
            conn.sendMouseButtonDown(buttonIndex);
        }
        else {
            conn.sendMouseButtonUp(buttonIndex);
        }
    }

    @Override
    public void mouseScroll(byte amount) {
        conn.sendMouseScroll(amount);
    }

    @Override
    public void keyboardEvent(boolean buttonDown, short keyCode) {
        short keyMap = keyboardTranslator.translate(keyCode, -1);
        if (keyMap != 0) {
            // handleSpecialKeys() takes the Android keycode
            if (handleSpecialKeys(keyCode, buttonDown)) {
                return;
            }

            if (buttonDown) {
                conn.sendKeyboardInput(keyMap, KeyboardPacket.KEY_DOWN, getModifierState());
            }
            else {
                conn.sendKeyboardInput(keyMap, KeyboardPacket.KEY_UP, getModifierState());
            }
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        // Don't do anything if we're not connected
        if (!connected) {
            return;
        }

        // This flag is set for all devices
        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            hideSystemUi(2000);
        }
        // This flag is only set on 4.4+
        else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT &&
                 (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
            hideSystemUi(2000);
        }
        // This flag is only set before 4.4+
        else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT &&
                 (visibility & View.SYSTEM_UI_FLAG_LOW_PROFILE) == 0) {
            hideSystemUi(2000);
        }
    }

    @Override
    public void onPerfUpdate(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                performanceOverlayView.setText(text);
            }
        });
    }

    @Override
    public void onUsbPermissionPromptStarting() {
        // Disable PiP auto-enter while the USB permission prompt is on-screen. This prevents
        // us from entering PiP while the user is interacting with the OS permission dialog.
        suppressPipRefCount++;
        updatePipAutoEnter();
    }

    @Override
    public void onUsbPermissionPromptCompleted() {
        suppressPipRefCount--;
        updatePipAutoEnter();
    }

    private void reloadActivity() {
        isNeedReload = true;
        stopConnection();
    }

    private void refreshActivity() {
        isNeedRefresh = true;
        stopConnection();
    }

    private void restartActivity() {
        runOnUiThread(() -> {
            finish();
            ServerHelper.doStart(Game.this, currentIntent);
        });
    }

    private PopupMenu createMenu(View anchor, int menuRes, PopupMenu.OnMenuItemClickListener listener) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenuInflater().inflate(menuRes, menu.getMenu());
        menu.setOnMenuItemClickListener(listener);
        return menu;
    }

    private void initCheckboxBehavior(MenuItem item, BiConsumer<Context, Boolean> setter) {
        initCheckboxBehavior(item, setter, true);
    }

    private void initCheckboxBehavior(MenuItem item, BiConsumer<Context, Boolean> setter, boolean isNeedRestart) {
        item.setChecked(!item.isChecked());
        setter.accept(this, item.isChecked());
        if (isNeedRestart) {
            refreshActivity();
        }
    }

    private AlertDialog createSimpleDialog(int resTitle, int message, Runnable setMethod) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(resTitle).setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, id) -> setMethod.run())
                .setNegativeButton(android.R.string.no, (dialog, id) -> dialog.dismiss());
        return builder.create();
    }

    private AlertDialog createListDialog(Object currentValue, int resTitle, int resValuesArray,
                                         int resNamesArray, BiConsumer<Context, String> setMethod) {
        List<String> valuesList = Arrays.asList(getResources().getStringArray(resValuesArray));
        int currentIndex = valuesList.indexOf(String.valueOf(currentValue));
        AtomicInteger selectedIndex = new AtomicInteger(currentIndex);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(resTitle)
                .setSingleChoiceItems(resNamesArray, currentIndex, (dialog, which) -> selectedIndex.set(which))
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (currentIndex != selectedIndex.get()) {
                        setMethod.accept(this, valuesList.get(selectedIndex.get()));
                        refreshActivity();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss());
        return builder.create();
    }

    private AlertDialog createSeekBarDialog(int currentValue, int resTitle, int resValueLabel,
                                            int minValue, int stepValue, int maxValue,
                                            BiConsumer<Context, Integer> setMethod) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        View seekBarView = inflater.inflate(R.layout.dialog_seekbar, findViewById(R.id.dialog_seekbar));

        final int[] selectedValue = {currentValue};

        ((TextView) seekBarView.findViewById(R.id.dialog_seekbar_title)).setText(resTitle);
        ((TextView) seekBarView.findViewById(R.id.seekbar_value_label)).setText(resValueLabel);

        TextView dialogSeekBarValue = seekBarView.findViewById(R.id.seekbar_value);
        dialogSeekBarValue.setText(String.valueOf(selectedValue[0]));

        SeekBar.OnSeekBarChangeListener dialogSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBark, int progress, boolean fromUser) {
                progress = minValue + progress;
                progress = progress / stepValue;
                progress = progress * stepValue;
                dialogSeekBarValue.setText(String.valueOf(progress));
                selectedValue[0] = progress;
            }
        };

        SeekBar dialogSeekBar = seekBarView.findViewById(R.id.seekbar);
        dialogSeekBar.setMax(maxValue - minValue);
        dialogSeekBar.incrementProgressBy(stepValue);
        dialogSeekBar.setProgress(selectedValue[0]);
        dialogSeekBar.setOnSeekBarChangeListener(dialogSeekBarListener);

        builder.setView(seekBarView)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (currentValue != selectedValue[0]) {
                        setMethod.accept(this, selectedValue[0]);
                        refreshActivity();
                    } else {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss());

        return builder.create();
    }

    private void getServerInfo(Uri uri) {
        spinner.setMessage(getString(R.string.getting_server_info));

        if (BuildConfig.DEBUG) {
            LimeLog.info(getResources().getString(R.string.getting_server_info));
        }

        new Thread(() -> {
            try {
                UserSession session = OneplayApi.getInstance().connectTo(uri);
                OneplayPreferenceConfiguration.savePreferences(this, session.getConfig());
                doAddPc(session);
            } catch (IOException | JSONException e) {
                LimeLog.severe(e);

                showErrorDialog(getString(R.string.conn_error_title),
                        getString(R.string.unable_to_connect_error));
            }
        }).start();
    }

    private void doAddPc(UserSession session) {
        runOnUiThread(() -> spinner.setMessage(getString(R.string.configuring_connection)));

        if (BuildConfig.DEBUG) {
            LimeLog.info(getResources().getString(R.string.configuring_connection));
        }

        int portTestResult;
        ComputerDetails computer = new ComputerDetails();
        computer.activeAddress = session.getHostAddress();

        try {
            NvHTTP http = new NvHTTP(computer.activeAddress, idManager.getUniqueId(), computer.serverCert,
                    PlatformBinding.getCryptoProvider(Game.this));

            computer.update(http.getComputerDetails());

            if (computer.state == ComputerDetails.State.ONLINE) {
                doPair(http, computer, session);
            } else {
                throw new IllegalStateException();
            }
        } catch (Exception e) {
            // IllegalArgumentException can be thrown from OkHttp if the host fails to canonicalize to a valid name.
            // https://github.com/square/okhttp/blob/okhttp_27/okhttp/src/main/java/com/squareup/okhttp/HttpUrl.java#L705

            String message;

            if (e instanceof IllegalStateException) {
                message = getResources().getString(R.string.pair_pc_offline);
            } else {
                // Run the test before dismissing the spinner because it can take a few seconds.
                portTestResult = MoonBridge.testClientConnectivity(ServerHelper.CONNECTION_TEST_SERVER, 443,
                        MoonBridge.ML_PORT_FLAG_TCP_47984 | MoonBridge.ML_PORT_FLAG_TCP_47989);

                if (portTestResult != 0) {
                    message = getResources().getString(R.string.nettest_text_blocked);
                } else {
                    message = getResources().getString(R.string.addpc_fail);
                }
            }

            LimeLog.severe(message, e);

            showErrorDialog(getString(R.string.conn_error_title), message);
        }
    }

    private void doPair(NvHTTP http, ComputerDetails computer, UserSession session) {
        runOnUiThread(() -> spinner.setMessage(getString(R.string.pairing)));

        if (BuildConfig.DEBUG) {
            LimeLog.info(getResources().getString(R.string.pairing));
        }

        String message;

        try {
            if (computer.runningGameId != 0) {
                throw new IllegalStateException();
            }

            PairingManager pm = http.getPairingManager();
            PairingManager.PairState pairState = pm.pair(http.getServerInfo(), session.getKey());
            if (pairState == PairingManager.PairState.PIN_WRONG) {
                message = getResources().getString(R.string.pair_incorrect_pin);
            } else if (pairState == PairingManager.PairState.ALREADY_IN_PROGRESS) {
                message = getResources().getString(R.string.pair_already_in_progress);
            } else if (pairState == PairingManager.PairState.PAIRED) {
                // Pin this certificate for later HTTPS use
                computer.pairState = pairState;
                computer.serverCert = pm.getPairedCert();

                startGame(
                        http,
                        computer.activeAddress,
                        idManager.getUniqueId(),
                        computer.uuid,
                        computer.name,
                        computer.serverCert,
                        session.getKey()
                );

                return;
            } else {
                message = getResources().getString(R.string.pair_fail);
            }
        } catch (Exception e) {
            if (e instanceof  UnknownHostException) {
                message = getResources().getString(R.string.error_unknown_host);
            } else if (e instanceof FileNotFoundException) {
                message = getResources().getString(R.string.error_404);
            } else if (e instanceof IllegalStateException) {
                message = getResources().getString(R.string.pair_pc_ingame);
            } else {
                message = getResources().getString(R.string.pair_fail);
            }

            LimeLog.severe(message, e);
        }

        showErrorDialog(getString(R.string.conn_error_title), message);
    }

    private void startGame(NvHTTP http, String host, String uniqueId, String uuid, String pcName,
                           X509Certificate serverCert, String sessionKey) {
        try {
            NvApp currentApp = http.getAppByName("Desktop"); // Always get first app (Desktop)

            Intent intent = ServerHelper.createStartIntent(this, host, currentApp, uniqueId,
                    uuid, pcName, serverCert, sessionKey);
            runOnUiThread(() -> ServerHelper.doStart(this, intent));
        } catch (XmlPullParserException | IOException e) {
            String message = getString(R.string.applist_refresh_error_msg);

            LimeLog.severe(message, e);

            showErrorDialog(getString(R.string.conn_error_title), message);
        }
    }

    private void showErrorDialog(String title, String message) {
        runOnUiThread(() -> {
            if (spinner != null) {
                spinner.dismiss();
                spinner = null;
            }

            Dialog.displayDialog(Game.this, title, message, true);
        });
    }
}
