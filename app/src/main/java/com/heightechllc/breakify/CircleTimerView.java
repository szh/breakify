package com.heightechllc.breakify;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Round timer view.
 * Based on CircleTimerView from the Android clock app (deskclock)
 * Created by szh on 6/30/14.
 */
public class CircleTimerView extends View implements View.OnTouchListener {

    private int redColor;
    private int redColorPressed;
    private int borderColor;
    private int borderColorPressed;
    private int backgroundColor;
    private long totalIntervalTime = 0;
    private long intervalStartTime = -1;
    private long markerTime = -1;
    private long currentIntervalTime = 0;
    private long accumulatedTime = 0;
    private boolean paused = false;
    private boolean animate = false;
    private static float strokeSize = 4;
    private static float dotRadius = 6;
    private static float markerStrokeSize = 2;
    private final Paint borderPaint = new Paint();
    private final Paint backgroundPaint = new Paint();
    private final Paint redDotPaint = new Paint();
    private final RectF arcRect = new RectF();
    private float radiusOffset;   // amount to remove from radius to account for markers on circle
    private float screenDensity;

    /**
     * Whether the view is currently pressed
     */
    private boolean pressed;

    /**
     * The listener to notify when the user clicks the View
     */
    private OnClickListener clickListener;

    @SuppressWarnings("unused")
    public CircleTimerView(Context context) {
        this(context, null);
    }

    public CircleTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void setIntervalTime(long t) {
        totalIntervalTime = t;
        postInvalidate();
    }

    public long getIntervalTime() {
        return totalIntervalTime;
    }

    public void setMarkerTime(long t) {
        markerTime = t;
        postInvalidate();
    }

    public void reset() {
        intervalStartTime = -1;
        markerTime = -1;
        postInvalidate();
    }

    public void startIntervalAnimation() {
        intervalStartTime = SystemClock.elapsedRealtime();
        animate = true;
        invalidate();
        paused = false;
    }

    public void stopIntervalAnimation() {
        animate = false;
        intervalStartTime = -1;
        accumulatedTime = 0;
    }

    public boolean isAnimating() {
        return (intervalStartTime != -1);
    }

    public void pauseIntervalAnimation() {
        animate = false;
        accumulatedTime += SystemClock.elapsedRealtime() - intervalStartTime;
        paused = true;
    }

    public void abortIntervalAnimation() {
        animate = false;
    }

    public void setPassedTime(long time, boolean drawRed) {
        // The onDraw() method checks if intervalStartTime has been set before drawing any red.
        // Without drawRed, intervalStartTime should not be set here at all, and would remain at -1
        // when the state is reconfigured after exiting and re-entering the application.
        // If the timer is currently running, this drawRed will not be set, and will have no effect
        // because intervalStartTime will be set when the thread next runs.
        // When the timer is not running, intervalStartTime will not be set upon loading the state,
        // and no red will be drawn, so drawRed is used to force onDraw() to draw the red portion,
        // despite the timer not running.
        currentIntervalTime = accumulatedTime = time;
        if (drawRed) {
            intervalStartTime = SystemClock.elapsedRealtime();
        }
        postInvalidate();
    }

    public long getPassedTime() {
        // Total time - remaining time
        return totalIntervalTime - currentIntervalTime;
    }


    private void init(Context c) {

        Resources resources = c.getResources();
        strokeSize = resources.getDimension(R.dimen.circletimer_stroke_width);
        dotRadius = resources.getDimension(R.dimen.circletimer_dot_size) / 2f;
        markerStrokeSize = resources.getDimension(R.dimen.circletimer_marker_size);
        radiusOffset = dotRadius;
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderColor = resources.getColor(R.color.timer_border);
        borderColorPressed = resources.getColor(R.color.timer_border_pressed);
        redColor = resources.getColor(R.color.timer_red);
        redColorPressed = resources.getColor(R.color.timer_red_pressed);
        backgroundColor = resources.getColor(R.color.timer_background);
        screenDensity = resources.getDisplayMetrics().density;
        redDotPaint.setAntiAlias(true);
        redDotPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(backgroundColor);

        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        // Store the current value of `pressed`
        boolean pressedValue = pressed;

        // Check whether the user is pressing on the View
        if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) pressed = true;
        else if (motionEvent.getActionMasked() == MotionEvent.ACTION_UP) {
            pressed = false;
            // Notify the clickListener that the View has been clicked
            if (clickListener != null) clickListener.onClick(this);
        } else return false; // Ignore other event actions

        // Redraw if the value of `pressed` has changed
        if (pressedValue != pressed) invalidate();

        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        int xCenter = getWidth() / 2 + 1;
        int yCenter = getHeight() / 2;

        borderPaint.setStrokeWidth(strokeSize);
        float radius = Math.min(xCenter, yCenter) - radiusOffset;

        // Draw the background
        canvas.drawCircle(xCenter, yCenter, radius, backgroundPaint);

        if (intervalStartTime == -1) {
            // just draw a complete white circle, no red arc needed
            borderPaint.setColor(getBorderColor());
            canvas.drawCircle(xCenter, yCenter, radius, borderPaint);
            drawRedDot(canvas, 0f, xCenter, yCenter, radius);
        } else {
            if (animate) {
                currentIntervalTime = SystemClock.elapsedRealtime() - intervalStartTime + accumulatedTime;
            }
            //draw a combination of red and white arcs to create a circle
            arcRect.top = yCenter - radius;
            arcRect.bottom = yCenter + radius;
            arcRect.left = xCenter - radius;
            arcRect.right = xCenter + radius;
            float redPercent = (float) currentIntervalTime / (float) totalIntervalTime;
            // prevent timer from doing more than one full circle
            redPercent = (redPercent > 1) ? 1 : redPercent;

            float whitePercent = 1 - (redPercent > 1 ? 1 : redPercent);
            // draw red arc here
            borderPaint.setColor(getRedColor());
            canvas.drawArc(arcRect, 270, -redPercent * 360, false, borderPaint);

            // draw white arc here
            borderPaint.setStrokeWidth(strokeSize);
            borderPaint.setColor(getBorderColor());
            canvas.drawArc(arcRect, 270, +whitePercent * 360, false, borderPaint);

            if (markerTime != -1 && radius > 0 && totalIntervalTime != 0) {
                borderPaint.setStrokeWidth(markerStrokeSize);
                float angle = (float) (markerTime % totalIntervalTime) / (float) totalIntervalTime * 360;
                // draw 2dips thick marker
                // the formula to draw the marker 1 unit thick is:
                // 180 / (radius * Math.PI)
                // after that we have to scale it by the screen density
                canvas.drawArc(arcRect, 270 + angle, screenDensity *
                        (float) (360 / (radius * Math.PI)), false, borderPaint);
            }
            drawRedDot(canvas, redPercent, xCenter, yCenter, radius);
        }
        if (animate) {
            invalidate();
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        clickListener = l;
    }

    @Override
    public boolean hasOnClickListeners() {
        return clickListener != null;
    }

    protected void drawRedDot(
            Canvas canvas, float degrees, int xCenter, int yCenter, float radius) {
        float dotPercent;
        dotPercent = 270 - degrees * 360;

        // Select color based on whether the view is pressed
        redDotPaint.setColor(getRedColor());

        final double dotRadians = Math.toRadians(dotPercent);
        canvas.drawCircle(xCenter + (float) (radius * Math.cos(dotRadians)),
                yCenter + (float) (radius * Math.sin(dotRadians)), dotRadius, redDotPaint);
    }

    //
    // HELPERS
    //

    /**
     * Get the correct border color for the current `pressed` state
     */
    protected int getBorderColor() {
        return pressed ? borderColorPressed : borderColor;
    }
    /**
     * Get the correct red color for the current `pressed` state
     */
    protected int getRedColor() {
        return pressed ? redColorPressed : redColor;
    }

}
