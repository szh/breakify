/**
 * Copyright (C) 2014  Shlomo Zalman Heigh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

/**
 * Round timer view. Draws a border with two colors, to indicate the progress of the timer.
 * Based on CircleTimerView from the Android clock app (deskclock), which is licensed under the
 *  Apache License, Version 2.0
 */
public class CircleTimerView extends View implements View.OnTouchListener {

    private int redColor;
    private int redColorPressed;
    private int borderColor;
    private int borderColorPressed;
    private int backgroundColor;
    private int backgroundColorPressed;
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
     * The TextView to display the remaining time on
     */
    private TextView timeLbl;

    /**
     * The last seconds value that the timeLbl was updated with
     */
    private int lastUpdatedSecond = -1;

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

    public void setTotalTime(long t) {
        totalIntervalTime = t;
        postInvalidate();
    }

    public long getTotalTime() {
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

    public long getRemainingTime() {
        // Total time - past time
        return totalIntervalTime - currentIntervalTime;
    }


    private void init(Context c) {

        Resources resources = c.getResources();
        strokeSize = resources.getDimension(R.dimen.circletimer_stroke_width);
        dotRadius = resources.getDimension(R.dimen.circletimer_dot_size) / 2f;
        markerStrokeSize = resources.getDimension(R.dimen.circletimer_marker_size);
        radiusOffset = Math.max(dotRadius, markerStrokeSize / 2f);
        screenDensity = resources.getDisplayMetrics().density;
        borderColor = resources.getColor(R.color.timer_border);
        borderColorPressed = resources.getColor(R.color.timer_border_pressed);
        redColor = resources.getColor(R.color.timer_red);
        redColorPressed = resources.getColor(R.color.timer_red_pressed);
        backgroundColor = resources.getColor(R.color.timer_background);
        backgroundColorPressed = resources.getColor(R.color.timer_background_pressed);
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        redDotPaint.setAntiAlias(true);
        redDotPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStyle(Paint.Style.FILL);

        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        // Store the current value of `pressed`
        boolean pressedValue = pressed;

        // Check whether the user is pressing on the View
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                break;
            case MotionEvent.ACTION_UP:
                if (pressed) { // Don't react if it wasn't pressed in the first place
                    pressed = false;
                    // Notify the clickListener that the View has been clicked
                    if (clickListener != null) clickListener.onClick(this);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // If the user moved outside the view, cancel the press
                if (pressed && (motionEvent.getX() < 0 || motionEvent.getX() > getWidth() ||
                        motionEvent.getY() < 0 || motionEvent.getY() > getHeight())) {
                    pressed = false;
                    break;
                }
            default:
                // Ignore other event actions
                return false;
        }

        // Redraw if the value of `pressed` has changed
        if (pressedValue != pressed) invalidate();

        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {

        int xCenter = getWidth() / 2 + 1;
        int yCenter = getHeight() / 2;

        borderPaint.setStrokeWidth(strokeSize);
        float radius = Math.min(xCenter, yCenter) - radiusOffset;

        // Draw the background
        backgroundPaint.setColor(getBackgroundColor());
        canvas.drawCircle(xCenter, yCenter, radius, backgroundPaint);

        if (intervalStartTime == -1) {
            // just draw a complete white circle, no red arc needed
            borderPaint.setColor(getBorderColor());
            canvas.drawCircle(xCenter, yCenter, radius, borderPaint);
            drawRedDot(canvas, 0f, xCenter, yCenter, radius);
        } else {
            if (animate) {
                currentIntervalTime = SystemClock.elapsedRealtime() - intervalStartTime + accumulatedTime;
                // Update the TextView that displays the time remaining
                updateTimeLbl(totalIntervalTime - currentIntervalTime);
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

    public void setTimeDisplay(TextView lbl) {
        timeLbl = lbl;
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

    /**
     * Updates the label that displays how much time is remaining
     * @param millis The The number of milliseconds remaining
     */
    public void updateTimeLbl(long millis) {
        if (timeLbl == null) return;

        // Check if we already updated the TextView for this second
        int seconds = (int) millis / 1000;
        if (seconds == lastUpdatedSecond) return;

        // Get formatted time string with seconds + 1, since we want it to say 10:00 (or whatever),
        //  not 09:59 for the first second. Why? Because when humans count down from 10, we start
        //  with 10, not 9. Same way for the last second, we want it to say 00:01, not 00:00.
        //  There is technically something like 00:00.45, etc., but it would make that into 00:00,
        //  which isn't the way people talk - we say "you have one second left" until the time is
        //  completely up, and only then do we say "zero seconds left". This is also the way most
        //  countdown timers I tried work. Alternatively, we can consider rounding, i.e., 0.0-0.49
        //  would be 0 and 0.5-0.9 would be 1, but I'm not sure that's a great model either.
        // If anyone thinks otherwise, please let me know!
        String timeStr = formatTime(++seconds);

        // Update the clock display
        timeLbl.setText(timeStr);
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
    /**
     * Get the correct background color for the current `pressed` state
     */
    protected int getBackgroundColor() {
        return pressed ? backgroundColorPressed : backgroundColor;
    }

    /**
     * Creates a formatted string representing a time value, in the format Min:Sec, e.g. 06:13
     *
     * @param seconds The number of seconds in the time
     * @return The formatted string
     */
    private String formatTime(long seconds) {
        String timeStr = "";

        // Start with hours, if there are any
        long hours = TimeUnit.SECONDS.toHours(seconds);
        if (hours > 0)
        {
            timeStr = hours + ":";
            // Remove the seconds of the hours from `seconds`, so they don't get counted as
            //  minutes too (we do similar thing in calculating seconds, see below)
            seconds -= TimeUnit.HOURS.toSeconds(hours);
        }

        // Now calculate the minutes and seconds
        // The '02' makes sure it's 2 digits
        timeStr += String.format("%02d:%02d",
                TimeUnit.SECONDS.toMinutes(seconds),
                TimeUnit.SECONDS.toSeconds(seconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds))
        );

        return timeStr;
    }

}
