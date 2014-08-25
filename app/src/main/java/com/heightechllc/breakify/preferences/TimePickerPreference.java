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

package com.heightechllc.breakify.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Custom preference for time preference values, using a {@link android.widget.TimePicker}
 */
public class TimePickerPreference extends DialogPreference {

    /**
     * If we can't find a default value, we fall back on this.
     */
    private static final String FALLBACK_DEFAULT = "09:00";

    private TimePicker picker;

    /**
     * The preference value
     */
    private String value = FALLBACK_DEFAULT;
    private int valueHour;
    private int valueMinute;

    /**
     * Get a human-readable locale-formatted String for the time picker preference value
     * @param timeValue The value from the TimePickerPreference
     */
    public static String getHumanReadable(String timeValue, Context context) {
        String[] split = timeValue.split(":");
        int hours = Integer.valueOf(split[0]);
        int minutes = Integer.valueOf(split[1]);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        return DateFormat.getTimeFormat(context).format(cal.getTime());
    }

    public TimePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    @Override
    protected View onCreateDialogView() {
        // We don't need a layout xml file, just a simple TimePicker widget
        picker = new TimePicker(getContext());
        picker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
        return picker;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        picker.setCurrentHour(valueHour);
        picker.setCurrentMinute(valueMinute);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            picker.clearFocus();
            // User clicked "OK", so save the current selection
            valueHour = picker.getCurrentHour();
            valueMinute = picker.getCurrentMinute();
            value = String.valueOf(valueHour) + ":" + String.valueOf(valueMinute);

            if (callChangeListener(value)) {
                persistString(value);
            }
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);

        if (restorePersistedValue) {
            value = getPersistedString(FALLBACK_DEFAULT);
        } else {
            value = ((String) defaultValue);
            if (shouldPersist()) persistString(value);
        }

        // Split the time into hours and minutes
        String[] split = value.split(":");
        valueHour = Integer.valueOf(split[0]);
        valueMinute = Integer.valueOf(split[1]);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

}
