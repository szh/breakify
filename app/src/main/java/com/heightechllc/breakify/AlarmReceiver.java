package com.heightechllc.breakify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    public AlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //TODO: Ring

        //TODO: Vibrate
        //Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        // Open the RingingActivity
        Intent ringingIntent = new Intent(context, RingingActivity.class);
        ringingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                               Intent.FLAG_ACTIVITY_CLEAR_TASK |
                               Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        context.startActivity(ringingIntent);
    }
}
