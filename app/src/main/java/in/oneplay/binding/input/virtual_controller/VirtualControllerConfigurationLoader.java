/**
 * Created by Karim Mreisi.
 */

package in.oneplay.binding.input.virtual_controller;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;

import in.oneplay.LimeLog;
import in.oneplay.nvstream.input.ControllerPacket;
import in.oneplay.preferences.PreferenceConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

public class VirtualControllerConfigurationLoader {
    public static final String OSC_PREFERENCE = "OSC";

    private static int getPercent(
            int percent,
            int total) {
        return (int) (((float) total / (float) 100) * (float) percent);
    }

    // The default controls are specified using a grid of 128*72 cells at 16:9
    private static int screenScale(int units, int height) {
        return (int) (((float) height / (float) 72) * (float) units);
    }

    private static DigitalPad createDigitalPad(
            final VirtualController controller,
            final Context context) {

        DigitalPad digitalPad = new DigitalPad(controller, context);
        digitalPad.addDigitalPadListener(new DigitalPad.DigitalPadListener() {
            @Override
            public void onDirectionChange(int direction) {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();

                if (direction == DigitalPad.DIGITAL_PAD_DIRECTION_NO_DIRECTION) {
                    inputContext.inputMap &= ~ControllerPacket.LEFT_FLAG;
                    inputContext.inputMap &= ~ControllerPacket.RIGHT_FLAG;
                    inputContext.inputMap &= ~ControllerPacket.UP_FLAG;
                    inputContext.inputMap &= ~ControllerPacket.DOWN_FLAG;

                    controller.sendControllerInputContext();
                    return;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_LEFT) > 0) {
                    inputContext.inputMap |= ControllerPacket.LEFT_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_RIGHT) > 0) {
                    inputContext.inputMap |= ControllerPacket.RIGHT_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_UP) > 0) {
                    inputContext.inputMap |= ControllerPacket.UP_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_DOWN) > 0) {
                    inputContext.inputMap |= ControllerPacket.DOWN_FLAG;
                }
                controller.sendControllerInputContext();
            }
        });

        return digitalPad;
    }

    private static DigitalButton createDigitalButton(
            final int elementId,
            final int keyShort,
            final int keyLong,
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        DigitalButton button = new DigitalButton(controller, elementId, layer, context);
        button.setText(text);
        button.setIcon(icon);

        button.addDigitalButtonListener(new DigitalButton.DigitalButtonListener() {
            @Override
            public void onClick() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap |= keyShort;

                controller.sendControllerInputContext();
            }

            @Override
            public void onLongClick() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap |= keyLong;

                controller.sendControllerInputContext();
            }

            @Override
            public void onRelease() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap &= ~keyShort;
                inputContext.inputMap &= ~keyLong;

                controller.sendControllerInputContext();
            }
        });

        return button;
    }

    private static DigitalButton createLeftTrigger(
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        LeftTrigger button = new LeftTrigger(controller, layer, context);
        button.setText(text);
        button.setIcon(icon);
        return button;
    }

    private static DigitalButton createRightTrigger(
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        RightTrigger button = new RightTrigger(controller, layer, context);
        button.setText(text);
        button.setIcon(icon);
        return button;
    }

    private static AnalogStick createLeftStick(
            final VirtualController controller,
            final Context context) {
        return new LeftAnalogStick(controller, context);
    }

    private static AnalogStick createRightStick(
            final VirtualController controller,
            final Context context) {
        return new RightAnalogStick(controller, context);
    }



    // Face buttons are defined based on the Y button (button number 9)
    private static final int BUTTON_BASE_X = 100;
    private static final int BUTTON_BASE_Y = 7;
    private static final int BUTTON_SIZE = 8;

    private static final int DPAD_BASE_X = 14;
    private static final int DPAD_BASE_Y = 7;
    private static final int DPAD_SIZE = 20;

    private static final int ANALOG_L_BASE_X = 14;
    private static final int ANALOG_L_BASE_Y = 44;
    private static final int ANALOG_R_BASE_X = 93;
    private static final int ANALOG_R_BASE_Y = 36;
    private static final int ANALOG_SIZE = 19;

    private static final int LB_BASE_X = 14;
    private static final int RB_BASE_X = 114;
    private static final int LB_BASE_Y = 30;
    private static final int RB_BASE_Y = 38;
    private static final int LB_RB_WIDTH = 11;
    private static final int LB_RB_HEIGHT = 9;

    private static final int LT_BASE_X = 79;
    private static final int RT_BASE_X = 114;
    private static final int LT_RT_BASE_Y = 51;
    private static final int LT_RT_WIDTH = 11;
    private static final int LT_RT_HEIGHT = 9;

    private static final int START_X = 58;
    private static final int BACK_X = 42;
    private static final int START_BACK_Y = 1;
    private static final int START_BACK_WIDTH = 14;
    private static final int START_BACK_HEIGHT = 7;

    public static void createDefaultLayout(final VirtualController controller, final Context context) {

        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        PreferenceConfiguration config = PreferenceConfiguration.readPreferences(context);

        // Displace controls on the right by this amount of pixels to account for different aspect ratios
        int rightDisplacement = screen.widthPixels - screen.heightPixels * 16 / 9;

        int height = screen.heightPixels;

        // NOTE: Some of these getPercent() expressions seem like they can be combined
        // into a single call. Due to floating point rounding, this isn't actually possible.

        controller.addElement(createDigitalPad(controller, context),
                screenScale(DPAD_BASE_X, height),
                screenScale(DPAD_BASE_Y, height),
                screenScale(DPAD_SIZE, height),
                screenScale(DPAD_SIZE, height)
        );
        controller.addElement(createDigitalButton(
                VirtualControllerElement.EID_A,
                !config.flipFaceButtons ? ControllerPacket.A_FLAG : ControllerPacket.B_FLAG, 0, 1,
                !config.flipFaceButtons ? "A" : "B", -1, controller, context),
                screenScale(BUTTON_BASE_X, height) + rightDisplacement,
                screenScale(BUTTON_BASE_Y + 2 * BUTTON_SIZE, height),
                screenScale(BUTTON_SIZE, height),
                screenScale(BUTTON_SIZE, height)
        );
        controller.addElement(createDigitalButton(
                VirtualControllerElement.EID_B,
                config.flipFaceButtons ? ControllerPacket.A_FLAG : ControllerPacket.B_FLAG, 0, 1,
                config.flipFaceButtons ? "A" : "B", -1, controller, context),
                screenScale(BUTTON_BASE_X + BUTTON_SIZE, height) + rightDisplacement,
                screenScale(BUTTON_BASE_Y + BUTTON_SIZE, height),
                screenScale(BUTTON_SIZE, height),
                screenScale(BUTTON_SIZE, height)
        );
        controller.addElement(createDigitalButton(
                VirtualControllerElement.EID_X,
                !config.flipFaceButtons ? ControllerPacket.X_FLAG : ControllerPacket.Y_FLAG, 0, 1,
                !config.flipFaceButtons ? "X" : "Y", -1, controller, context),
                screenScale(BUTTON_BASE_X - BUTTON_SIZE, height) + rightDisplacement,
                screenScale(BUTTON_BASE_Y + BUTTON_SIZE, height),
                screenScale(BUTTON_SIZE, height),
                screenScale(BUTTON_SIZE, height)
        );
        controller.addElement(createDigitalButton(
                VirtualControllerElement.EID_Y,
                config.flipFaceButtons ? ControllerPacket.X_FLAG : ControllerPacket.Y_FLAG, 0, 1,
                config.flipFaceButtons ? "X" : "Y", -1, controller, context),
                screenScale(BUTTON_BASE_X, height) + rightDisplacement,
                screenScale(BUTTON_BASE_Y, height),
                screenScale(BUTTON_SIZE, height),
                screenScale(BUTTON_SIZE, height)
        );
        controller.addElement(createLeftTrigger(
                1, "LT", -1, controller, context),
                screenScale(LT_BASE_X, height) + rightDisplacement,
                screenScale(LT_RT_BASE_Y, height),
                screenScale(LT_RT_WIDTH, height),
                screenScale(LT_RT_HEIGHT, height)
        );
        controller.addElement(createRightTrigger(
                1, "RT", -1, controller, context),
                screenScale(RT_BASE_X, height) + rightDisplacement,
                screenScale(LT_RT_BASE_Y, height),
                screenScale(LT_RT_WIDTH, height),
                screenScale(LT_RT_HEIGHT, height)
        );
        controller.addElement(createDigitalButton(
                VirtualControllerElement.EID_LB,
                ControllerPacket.LB_FLAG, 0, 1, "LB", -1, controller, context),
                screenScale(LB_BASE_X, height),
                screenScale(LB_BASE_Y, height),
                screenScale(LB_RB_WIDTH, height),
                screenScale(LB_RB_HEIGHT, height)
        );
        controller.addElement(createDigitalButton(
                VirtualControllerElement.EID_RB,
                ControllerPacket.RB_FLAG, 0, 1, "RB", -1, controller, context),
                screenScale(RB_BASE_X, height) + rightDisplacement,
                screenScale(RB_BASE_Y, height),
                screenScale(LB_RB_WIDTH, height),
                screenScale(LB_RB_HEIGHT, height)
        );
        controller.addElement(createLeftStick(controller, context),
                screenScale(ANALOG_L_BASE_X, height),
                screenScale(ANALOG_L_BASE_Y, height),
                screenScale(ANALOG_SIZE, height),
                screenScale(ANALOG_SIZE, height)
        );
        controller.addElement(createRightStick(controller, context),
                screenScale(ANALOG_R_BASE_X, height) + rightDisplacement,
                screenScale(ANALOG_R_BASE_Y, height),
                screenScale(ANALOG_SIZE, height),
                screenScale(ANALOG_SIZE, height)
        );
        controller.addElement(createDigitalButton(
                VirtualControllerElement.EID_BACK,
                ControllerPacket.BACK_FLAG, 0, 2, "BACK", -1, controller, context),
                screenScale(BACK_X, height),
                screenScale(START_BACK_Y, height),
                screenScale(START_BACK_WIDTH, height),
                screenScale(START_BACK_HEIGHT, height)
        );
        controller.addElement(createDigitalButton(
                VirtualControllerElement.EID_START,
                ControllerPacket.PLAY_FLAG, 0, 3, "START", -1, controller, context),
                screenScale(START_X, height),
                screenScale(START_BACK_Y, height),
                screenScale(START_BACK_WIDTH, height),
                screenScale(START_BACK_HEIGHT, height)
        );

        controller.setOpacity(config.oscOpacity);
    }

    public static void saveProfile(final VirtualController controller,
                                   final Context context) {
        SharedPreferences.Editor prefEditor = context.getSharedPreferences(OSC_PREFERENCE, Activity.MODE_PRIVATE).edit();

        for (VirtualControllerElement element : controller.getElements()) {
            String prefKey = ""+element.elementId;
            try {
                prefEditor.putString(prefKey, element.getConfiguration().toString());
            } catch (JSONException e) {
                LimeLog.warning(e);
            }
        }

        prefEditor.apply();
    }

    public static void loadFromPreferences(final VirtualController controller, final Context context) {
        SharedPreferences pref = context.getSharedPreferences(OSC_PREFERENCE, Activity.MODE_PRIVATE);

        for (VirtualControllerElement element : controller.getElements()) {
            String prefKey = ""+element.elementId;

            String jsonConfig = pref.getString(prefKey, null);
            if (jsonConfig != null) {
                try {
                    element.loadConfiguration(new JSONObject(jsonConfig));
                } catch (JSONException e) {
                    LimeLog.warning(e);

                    // Remove the corrupt element from the preferences
                    pref.edit().remove(prefKey).apply();
                }
            }
        }
    }
}
