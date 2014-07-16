package com.heightechllc.breakify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class AlarmReceiver extends BroadcastReceiver {
    public AlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: Show notification

        // Check if a call is active or ringing
        TelephonyManager telephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        boolean inCall = (telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE);
        // Start ringing and / or vibrating
        AlarmRinger.start(context, inCall);

        // Open the RingingActivity
        Intent ringingIntent = new Intent(context, RingingActivity.class);
        ringingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                               Intent.FLAG_ACTIVITY_CLEAR_TASK |
                               Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        context.startActivity(ringingIntent);
    }
}
