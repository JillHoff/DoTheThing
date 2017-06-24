package com.rogueduckstudios.dothething;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Intent serviceIntent = new Intent(context, AlarmService.class);
        context.startService(serviceIntent);

        Log.d("Whatever", "AlarmReceiver after starting service");
    }
}
