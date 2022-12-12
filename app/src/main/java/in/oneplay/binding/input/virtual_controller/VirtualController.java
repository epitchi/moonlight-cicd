/**
 * Created by Karim Mreisi.
 */

package in.oneplay.binding.input.virtual_controller;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import in.oneplay.LimeLog;
import in.oneplay.binding.input.ControllerHandler;

public class VirtualController {
    public static class ControllerInputContext {
        public short inputMap = 0x0000;
        public byte leftTrigger = 0x00;
        public byte rightTrigger = 0x00;
        public short rightStickX = 0x0000;
        public short rightStickY = 0x0000;
        public short leftStickX = 0x0000;
        public short leftStickY = 0x0000;
    }

    public enum ControllerMode {
        Active,
        MoveButtons,
        ResizeButtons
    }

    private static final boolean _PRINT_DEBUG_INFORMATION = false;

    private final ControllerHandler controllerHandler;
    private final Context context;

    private FrameLayout frame_layout = null;

    private Timer retransmitTimer;

    ControllerMode currentMode = ControllerMode.Active;
    ControllerInputContext inputContext = new ControllerInputContext();

    private List<VirtualControllerElement> elements = new ArrayList<>();

    public VirtualController(final ControllerHandler controllerHandler, FrameLayout layout, final Context context) {
        this.controllerHandler = controllerHandler;
        this.frame_layout = layout;
        this.context = context;
    }

    public void hide() {
        retransmitTimer.cancel();

        for (VirtualControllerElement element : elements) {
            element.setVisibility(View.INVISIBLE);
        }
    }

    public void show() {
        for (VirtualControllerElement element : elements) {
            element.setVisibility(View.VISIBLE);
        }

        // HACK: GFE sometimes discards gamepad packets when they are received
        // very shortly after another. This can be critical if an axis zeroing packet
        // is lost and causes an analog stick to get stuck. To avoid this, we send
        // a gamepad input packet every 100 ms to ensure any loss can be recovered.
        retransmitTimer = new Timer("OSC timer", true);
        retransmitTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendControllerInputContext();
            }
        }, 100, 100);
    }

    public void removeElements() {
        for (VirtualControllerElement element : elements) {
            frame_layout.removeView(element);
        }
        elements.clear();
    }

    public void setOpacity(int opacity) {
        for (VirtualControllerElement element : elements) {
            element.setOpacity(opacity);
        }
    }


    public void addElement(VirtualControllerElement element, int x, int y, int width, int height) {
        elements.add(element);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
        layoutParams.setMargins(x, y, 0, 0);

        frame_layout.addView(element, layoutParams);
    }

    public List<VirtualControllerElement> getElements() {
        return elements;
    }

    private static final void _DBG(String text) {
        if (_PRINT_DEBUG_INFORMATION) {
            LimeLog.info("VirtualController: " + text);
        }
    }

    public void refreshLayout() {
        removeElements();

        // Start with the default layout
        VirtualControllerConfigurationLoader.createDefaultLayout(this, context);

        // Apply user preferences onto the default layout
        VirtualControllerConfigurationLoader.loadFromPreferences(this, context);
    }

    public void setControllerMode(ControllerMode controllerMode) {
        if (this.currentMode != controllerMode) {
            this.currentMode = controllerMode;

            for (VirtualControllerElement element : elements) {
                element.invalidate();
            }
        };
    }

    public void saveProfile() {
        VirtualControllerConfigurationLoader.saveProfile(VirtualController.this, context);
        setControllerMode(ControllerMode.Active);
    }

    public ControllerMode getControllerMode() {
        return currentMode;
    }

    public ControllerInputContext getControllerInputContext() {
        return inputContext;
    }

    void sendControllerInputContext() {
        _DBG("INPUT_MAP + " + inputContext.inputMap);
        _DBG("LEFT_TRIGGER " + inputContext.leftTrigger);
        _DBG("RIGHT_TRIGGER " + inputContext.rightTrigger);
        _DBG("LEFT STICK X: " + inputContext.leftStickX + " Y: " + inputContext.leftStickY);
        _DBG("RIGHT STICK X: " + inputContext.rightStickX + " Y: " + inputContext.rightStickY);

        if (controllerHandler != null) {
            controllerHandler.reportOscState(
                    inputContext.inputMap,
                    inputContext.leftStickX,
                    inputContext.leftStickY,
                    inputContext.rightStickX,
                    inputContext.rightStickY,
                    inputContext.leftTrigger,
                    inputContext.rightTrigger
            );
        }
    }
}
