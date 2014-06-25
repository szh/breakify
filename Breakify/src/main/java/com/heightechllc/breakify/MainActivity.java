package com.heightechllc.breakify;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements View.OnClickListener
{
    // Timer states
    public static String RUNNING = "RUNNING";
    public static String PAUSED = "PAUSED";
    public static String STOPPED = "STOPPED";

    // Work states
    public static String WORK = "WORK";
    public static String BREAK = "BREAK";

    private String tag = "MainActivity";

    private CountDownTimer _countDownTimer;
    private long millisRemaining;
    private String timerState = STOPPED;
    private String workState = WORK;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //
        // Set up components
        //
        FrameLayout clockFrame = (FrameLayout) findViewById(R.id.clockFrame);
        clockFrame.setOnClickListener(this);

        NumberPicker workNumPicker = (NumberPicker) findViewById(R.id.workNumPicker);
        workNumPicker.setMinValue(1);
        workNumPicker.setMaxValue(300);
        workNumPicker.setValue(90);

        NumberPicker breakNumPicker = (NumberPicker) findViewById(R.id.breakNumPicker);
        breakNumPicker.setMinValue(1);
        breakNumPicker.setMaxValue(300);
        breakNumPicker.setValue(10);

        Button resetBtn = (Button) findViewById(R.id.resetBtn);
        resetBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.clockFrame:
                // Either start or pause the timer
                if (timerState == RUNNING) pauseTimer();
                else startTimer();

                break;

            case R.id.resetBtn:
                resetTimer();

                break;
        }
    }

    /**
     * Starts the work or break timer
     * @param duration The number of milliseconds to run the timer for
     */
    private void startTimer(long duration)
    {
        // First hide the number pickers and show the "Reset" btn (TODO: Animate)
        findViewById(R.id.timerSettings).setVisibility(View.GONE);
        findViewById(R.id.resetBtn).setVisibility(View.VISIBLE);

        // Update timer display
        updateTimeLbl(duration);

        // Create the count down timer
        _countDownTimer = new CountDownTimer(duration, 1000)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                // Update the UI
                updateTimeLbl(millisUntilFinished);

                // Record the time remaining
                millisRemaining = millisUntilFinished;
            }

            @Override
            public void onFinish()
            {
                timerState = STOPPED;
                // Update the time display
                updateTimeLbl(0);
                // Hide the start / pause btn (TODO: Animate)
                findViewById(R.id.startStopLbl).setVisibility(View.INVISIBLE);

                // Prompt to start break / resume work with dialog
                promptToSwitchWorkStates();
            }
        };
        _countDownTimer.start();

        timerState = RUNNING;

        // Update the start / stop label
        TextView startStopLbl = (TextView) findViewById(R.id.startStopLbl);
        startStopLbl.setVisibility(View.VISIBLE);
        startStopLbl.setText(R.string.stop);
    }

    /**
     * Starts the work or break timer
     * Calculates the duration automatically, and then calls startTimer(duration)
     */
    private void startTimer()
    {
        long duration;
        if (timerState == PAUSED)
        {
            // Timer already started before, so just get the remaining time
            duration = millisRemaining;
        }
        else
        {
            // Get chosen duration in minutes
            NumberPicker numberPicker;
            if (workState == WORK)
                numberPicker = (NumberPicker) findViewById(R.id.workNumPicker);
            else
                numberPicker = (NumberPicker) findViewById(R.id.breakNumPicker);

            duration = numberPicker.getValue() * 60000; // Multiply into milliseconds
        }

        startTimer(duration);
    }

    /**
     * Pauses the break timer
     */
    private void pauseTimer()
    {
        timerState = PAUSED;
        _countDownTimer.cancel();

        // Update the start / stop label
        TextView startStopLbl = (TextView) findViewById(R.id.startStopLbl);
        startStopLbl.setVisibility(View.VISIBLE);
        startStopLbl.setText(R.string.resume);
    }

    private void resetTimer()
    {
        _countDownTimer.cancel();
        timerState = STOPPED;

        // Reset the UI (TODO: Animate)
        TextView timeLbl = (TextView) findViewById(R.id.timeLbl);
        timeLbl.setText("");

        findViewById(R.id.timerSettings).setVisibility(View.VISIBLE);
        findViewById(R.id.resetBtn).setVisibility(View.GONE);

        // Back to initial state
        workState = WORK;
        TextView stateLbl = (TextView) findViewById(R.id.stateLbl);
        stateLbl.setText(R.string.state_working);

        // Update the start / stop label
        TextView startStopLbl = (TextView) findViewById(R.id.startStopLbl);
        startStopLbl.setVisibility(View.VISIBLE);
        startStopLbl.setText(R.string.start);
    }


    private void updateTimeLbl(long millis)
    {
        // Get formatted time string
        String timeStr = formatTime(millis);

        // Update the clock
        TextView timeLbl = (TextView) findViewById(R.id.timeLbl);
        timeLbl.setText(timeStr);
    }

    /**
     * Prompts the user to switch to the opposite work state (work or break)
     */
    private void promptToSwitchWorkStates()
    {
        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (workState == WORK)
            builder.setMessage(R.string.start_break_prompt);
        else
            builder.setMessage(R.string.start_work_prompt);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                switchWorkStates();
            }
        });
        builder.setNeutralButton(R.string.snooze, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                // Snooze the timer for 3 minutes (TODO: Make this a setting)
                startTimer(180000);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                // User chose to cancel (TODO: Ask user to confirm?)
                resetTimer();
            }
        });

        builder.show();
    }

    /**
     * Switches the workState to the opposite of the current and then starts the timer
     */
    private void switchWorkStates()
    {
        // Set the new state
        TextView stateLbl = (TextView) findViewById(R.id.stateLbl);
        if (workState == WORK)
        {
            workState = BREAK;
            stateLbl.setText(R.string.state_breaking);
        }
        else
        {
            workState = WORK;
            stateLbl.setText(R.string.state_working);
        }

        // Now start the timer
        startTimer();
    }

    //
    // HELPERS
    //

    /**
     * Creates a formatted string representing a time value, in the format Min:Sec, e.g. 06:13
     * @param millis The number of milliseconds in the time
     * @return The formatted string
     */
    private String formatTime(long millis)
    {
        // The '02' makes sure it's 2 digits
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }
}
