package com.heightechllc.breakify;

import android.app.Activity;
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
    private String tag = "MainActivity";

    private CountDownTimer _countDownTimer;

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
                if (_countDownTimer != null) pauseTimer();
                else startTimer();
                break;
            case R.id.resetBtn:
                // Revert UI
                findViewById(R.id.timerSettings).setVisibility(View.VISIBLE);
                findViewById(R.id.resetBtn).setVisibility(View.GONE);

                break;
        }
    }

    /**
     * Starts the work timer
     */
    private void startTimer()
    {
        // First hide the number pickers and show the "Reset" btn
        findViewById(R.id.timerSettings).setVisibility(View.GONE);
        findViewById(R.id.resetBtn).setVisibility(View.VISIBLE);

        // Get chosen duration in minutes
        NumberPicker workNumPicker = (NumberPicker) findViewById(R.id.workNumPicker);
        int duration = workNumPicker.getValue() * 60000; // Multiply into milliseconds

        // Update timer display
        updateTimeLbl(duration);

        // Create the count down timer
        _countDownTimer = new CountDownTimer(duration, 1000)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                updateTimeLbl(millisUntilFinished);
            }

            @Override
            public void onFinish()
            {
                //TODO: Implement onFinish()
            }
        };
        _countDownTimer.start();
    }

    /**
     * Pauses the break timer
     */
    private void pauseTimer()
    {
        _countDownTimer.cancel();
    }

    /**
     * Resets the break timer
     */
    private void resetTimer()
    {

    }


    private void updateTimeLbl(long millis)
    {
        // Get formatted time string
        String timeStr = formatTime(millis);

        // Update the clock
        TextView timeLbl = (TextView) findViewById(R.id.timeLbl);
        timeLbl.setText(timeStr);
    }

    //
    // HELPERS
    //

    private String formatTime(long millis)
    {
        return String.format("%02d:%d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }
}
