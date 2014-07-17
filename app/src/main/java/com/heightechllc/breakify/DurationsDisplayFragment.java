package com.heightechllc.breakify;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Fragment for displaying the current settings for the work and break durations
 */
public class DurationsDisplayFragment extends Fragment implements View.OnClickListener {

    private View v;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Set the layout
        v = inflater.inflate(R.layout.durations_display, container);

        // Set up UI components
        ImageButton editBtn = (ImageButton) v.findViewById(R.id.edit_btn);
        editBtn.setOnClickListener(this);

        // Return the inflated layout
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Good place to update the values, since it's called when the fragment is created, as well
        //  as when it comes back onto the screen, which happens after the user changes the settings
        //  in the SettingsActivity
        updateValues();
    }

    @Override
    public void onClick(View view) {
        // Check if the user clicked the "Edit" button
        if (view.getId() != R.id.edit_btn) return;

        // Show the settings activity
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Update the text of all the duration displays
     */
    public void updateValues() {
        // Get the labels for displaying the durations
        TextView workDisplay = (TextView) v.findViewById(R.id.work_duration_display);
        TextView breakDisplay = (TextView) v.findViewById(R.id.break_duration_display);

        // Get the current setting for each value
        int workNum, breakNum;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        workNum = prefs.getInt(SettingsFragment.KEY_WORK_DURATION, 0);
        breakNum = prefs.getInt(SettingsFragment.KEY_BREAK_DURATION, 0);

        // Construct the text for each value: the int + 'minutes' - e.g., "90 minutes"
        String mins = getResources().getString(R.string.minutes);
        workDisplay.setText(workNum + " " + mins);
        breakDisplay.setText(breakNum + " " + mins);
    }
}
