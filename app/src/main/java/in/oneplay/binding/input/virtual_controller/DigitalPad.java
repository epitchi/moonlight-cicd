/**
 * Created by Karim Mreisi.
 */

package in.oneplay.binding.input.virtual_controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

public class DigitalPad extends VirtualControllerElement {
    public final static int DIGITAL_PAD_DIRECTION_NO_DIRECTION = 0;
    int direction = DIGITAL_PAD_DIRECTION_NO_DIRECTION;
    public final static int DIGITAL_PAD_DIRECTION_LEFT = 1;
    public final static int DIGITAL_PAD_DIRECTION_UP = 2;
    public final static int DIGITAL_PAD_DIRECTION_RIGHT = 4;
    public final static int DIGITAL_PAD_DIRECTION_DOWN = 8;
    List<DigitalPadListener> listeners = new ArrayList<>();

    private static final int DPAD_MARGIN = 5;

    private final Paint paint = new Paint();
    private final Point point1 = new Point();
    private final Point point2 = new Point();
    private final Point point3 = new Point();
    private final Point point4 = new Point();
    private final Point point5 = new Point();
    private final Point point6 = new Point();
    private final Point point7 = new Point();
    private final Point point8 = new Point();

    public DigitalPad(VirtualController controller, Context context) {
        super(controller, context, EID_DPAD);
    }

    public void addDigitalPadListener(DigitalPadListener listener) {
        listeners.add(listener);
    }

    @Override
    protected void onElementDraw(Canvas canvas) {
        // set transparent background
        canvas.drawColor(Color.TRANSPARENT);

        paint.setTextSize(getPercent(getCorrectWidth(), 20));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStrokeWidth(getDefaultStrokeWidth());

        // draw left rect
        point1.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 36),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 63));
        point2.set(DPAD_MARGIN + getDefaultStrokeWidth() + (int) getPercent(getWidth(), 8),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 63));
        point3.set(DPAD_MARGIN + getDefaultStrokeWidth(),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 63));
        point4.set(DPAD_MARGIN + getDefaultStrokeWidth(),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 55));
        point5.set(DPAD_MARGIN + getDefaultStrokeWidth(),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 44));
        point6.set(DPAD_MARGIN + getDefaultStrokeWidth(),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 36));
        point7.set(DPAD_MARGIN + getDefaultStrokeWidth() +
                (int) getPercent(getWidth(), 8),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 36));
        point8.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 36),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 36));

        paint.setColor(
                (direction & DIGITAL_PAD_DIRECTION_LEFT) > 0 ? pressedColor : getDefaultColor());
        paint.setStyle(Paint.Style.STROKE);

        drawPath(canvas);

        // draw up rect
        point1.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 36),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 36));
        point2.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 36),
                DPAD_MARGIN + getDefaultStrokeWidth() + (int) getPercent(getHeight(), 8));
        point3.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 36),
                DPAD_MARGIN + getDefaultStrokeWidth());
        point4.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 44),
                DPAD_MARGIN + getDefaultStrokeWidth());
        point5.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 55),
                DPAD_MARGIN + getDefaultStrokeWidth());
        point6.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 63),
                DPAD_MARGIN + getDefaultStrokeWidth());
        point7.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 63),
                DPAD_MARGIN + getDefaultStrokeWidth() + (int) getPercent(getHeight(), 8));
        point8.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 63),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 36));

        paint.setColor(
                (direction & DIGITAL_PAD_DIRECTION_UP) > 0 ? pressedColor : getDefaultColor());
        paint.setStyle(Paint.Style.STROKE);

        drawPath(canvas);

        // draw right rect
        point1.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 63),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 36));
        point2.set((int) getPercent(getWidth(), 92) - DPAD_MARGIN - getDefaultStrokeWidth(),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 36));
        point3.set((int) getPercent(getWidth(), 100) - DPAD_MARGIN - getDefaultStrokeWidth(),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 36));
        point4.set((int) getPercent(getWidth(), 100) - DPAD_MARGIN - getDefaultStrokeWidth(),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 44));
        point5.set((int) getPercent(getWidth(), 100) - DPAD_MARGIN - getDefaultStrokeWidth(),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 55));
        point6.set((int) getPercent(getWidth(), 100) - DPAD_MARGIN - getDefaultStrokeWidth(),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 63));
        point7.set((int) getPercent(getWidth(), 92),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 63));
        point8.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 63),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 63));

        paint.setColor(
                (direction & DIGITAL_PAD_DIRECTION_RIGHT) > 0 ? pressedColor : getDefaultColor());
        paint.setStyle(Paint.Style.STROKE);

        drawPath(canvas);

        // draw down rect
        point1.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 63),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 63));
        point2.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 63),
                (int) getPercent(getHeight(), 92) - DPAD_MARGIN - getDefaultStrokeWidth());
        point3.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 63),
                (int) getPercent(getHeight(), 100) - DPAD_MARGIN - getDefaultStrokeWidth());
        point4.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 55),
                (int) getPercent(getHeight(), 100) - DPAD_MARGIN - getDefaultStrokeWidth());
        point5.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 44),
                (int) getPercent(getHeight(), 100) - DPAD_MARGIN - getDefaultStrokeWidth());
        point6.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 36),
                (int) getPercent(getHeight(), 100) - DPAD_MARGIN - getDefaultStrokeWidth());
        point7.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 36),
                (int) getPercent(getHeight(), 92) - DPAD_MARGIN - getDefaultStrokeWidth());
        point8.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 36),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 63));

        paint.setColor(
                (direction & DIGITAL_PAD_DIRECTION_DOWN) > 0 ? pressedColor : getDefaultColor());
        paint.setStyle(Paint.Style.STROKE);

        drawPath(canvas);

        // draw left up line
        point1.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 12),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 36));
        point2.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 18),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 18));
        point3.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 36),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 12));

        paint.setColor((
                        (direction & DIGITAL_PAD_DIRECTION_LEFT) > 0 &&
                                (direction & DIGITAL_PAD_DIRECTION_UP) > 0
                ) ? pressedColor : getDefaultColor()
        );
        paint.setStyle(Paint.Style.STROKE);
        drawDiagonal(canvas);

        // draw up right line
        point1.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 63),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 12));
        point2.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 81),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 18));
        point3.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 87),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 36));

        paint.setColor((
                        (direction & DIGITAL_PAD_DIRECTION_UP) > 0 &&
                                (direction & DIGITAL_PAD_DIRECTION_RIGHT) > 0
                ) ? pressedColor : getDefaultColor()
        );
        paint.setStyle(Paint.Style.STROKE);
        drawDiagonal(canvas);

        // draw right down line
        point1.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 87),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 63));
        point2.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 81),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 81));
        point3.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 63),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 87));

        paint.setColor((
                        (direction & DIGITAL_PAD_DIRECTION_RIGHT) > 0 &&
                                (direction & DIGITAL_PAD_DIRECTION_DOWN) > 0
                ) ? pressedColor : getDefaultColor()
        );
        paint.setStyle(Paint.Style.STROKE);
        drawDiagonal(canvas);

        // draw down left line
        point1.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 36),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 87));
        point2.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 18),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 81));
        point3.set(getDefaultStrokeWidth() / 2 + (int) getPercent(getWidth(), 12),
                getDefaultStrokeWidth() / 2 + (int) getPercent(getHeight(), 63));

        paint.setColor((
                        (direction & DIGITAL_PAD_DIRECTION_DOWN) > 0 &&
                                (direction & DIGITAL_PAD_DIRECTION_LEFT) > 0
                ) ? pressedColor : getDefaultColor()
        );
        paint.setStyle(Paint.Style.STROKE);
        drawDiagonal(canvas);
    }

    private void drawPath(Canvas canvas) {
        Path path = new Path();
        path.moveTo(point1.x, point1.y);
        path.lineTo(point2.x, point2.y);
        path.quadTo(point3.x, point3.y, point4.x, point4.y);
        path.lineTo(point5.x, point5.y);
        path.quadTo(point6.x, point6.y, point7.x, point7.y);
        path.lineTo(point8.x, point8.y);

        canvas.drawPath(path, paint);
    }

    private void drawDiagonal(Canvas canvas) {
        Path path = new Path();
        path.moveTo(point1.x, point1.y);
        path.quadTo(point2.x, point2.y, point3.x, point3.y);

        canvas.drawPath(path, paint);
    }

    private void newDirectionCallback(int direction) {
        _DBG("direction: " + direction);

        // notify listeners
        for (DigitalPadListener listener : listeners) {
            listener.onDirectionChange(direction);
        }
    }

    @Override
    public boolean onElementTouchEvent(MotionEvent event) {
        // get masked (not specific to a pointer) action
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE: {
                direction = 0;

                if (event.getX() < getPercent(getWidth(), 33)) {
                    direction |= DIGITAL_PAD_DIRECTION_LEFT;
                }
                if (event.getX() > getPercent(getWidth(), 66)) {
                    direction |= DIGITAL_PAD_DIRECTION_RIGHT;
                }
                if (event.getY() > getPercent(getHeight(), 66)) {
                    direction |= DIGITAL_PAD_DIRECTION_DOWN;
                }
                if (event.getY() < getPercent(getHeight(), 33)) {
                    direction |= DIGITAL_PAD_DIRECTION_UP;
                }
                newDirectionCallback(direction);
                invalidate();

                return true;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                direction = 0;
                newDirectionCallback(direction);
                invalidate();

                return true;
            }
            default: {
            }
        }

        return true;
    }

    public interface DigitalPadListener {
        void onDirectionChange(int direction);
    }
}
