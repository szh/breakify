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

        if (!restorePersistedValue)
        {
            // Attempt to parse defaultValue as an int
            initialVal = (Integer) defaultValue;
            return;
        }

        int def;
        if (defaultValue instanceof Number)
            def = (Integer) defaultValue;
        else // Try to parse the value to an Integer. If it's null, just use `1`
            def = (defaultValue != null) ? Integer.parseInt(defaultValue.toString()) : 1;

        initialVal = getPersistedInt(def);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // We default to 1 (see end of `onSetInitialValue()`)
        return a.getInt(index, 1);
    }
}
