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
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

public class NumberPickerPreference extends DialogPreference {

    private NumberPicker picker;
    private int min, max;
    private boolean minutes;
    private Integer initialVal;

    private static final int FALLBACK_DEFAULT = 1;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Get the min and max values from the XML attributes
        TypedArray pickerStyles = context.obtainStyledAttributes(attrs,
                                                            R.styleable.NumberPickerPreference);
        min = pickerStyles.getInt(R.styleable.NumberPickerPreference_minValue, 1);
        max = pickerStyles.getInt(R.styleable.NumberPickerPreference_maxValue, 300);
        minutes = pickerStyles.getBoolean(R.styleable.NumberPickerPreference_minutes, false);

        pickerStyles.recycle();

        setDialogLayoutResource(R.layout.number_picker_preference);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // Set the number picker's values
        picker = (NumberPicker) view.findViewById(R.id.num_picker);
        picker.setMinValue(min);
        picker.setMaxValue(max);

        if (initialVal != null) picker.setValue(initialVal);

        // Show the "minutes" label if the number picker is for minutes
        //  (defaults to `visibility="gone"`)
        if (minutes) {
            TextView minutesLbl = (TextView) view.findViewById(R.id.minutesLbl);
            minutesLbl.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (which == DialogInterface.BUTTON_POSITIVE) {
            // User clicked "OK", so save the current selection
            initialVal = picker.getValue();
            persistInt(initialVal);
            callChangeListener(initialVal);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);

        if (restorePersistedValue) {
            initialVal = getPersistedInt(FALLBACK_DEFAULT);
        } else {
            initialVal = (Integer) defaultValue;
            persistInt(initialVal);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // We default to 1 (see end of `onSetInitialValue()`)
        return a.getInt(index, FALLBACK_DEFAULT);
    }
}
