package com.rogueduckstudios.dothething;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AlarmService extends Service {

    Context context;
    Boolean serviceIsStarted = false;
    Ringtone r = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.context = this;

        serviceIsStarted = intent.getBooleanExtra("AlarmRunning", false);

        if (r == null)
            {
                String alarmUri = intent.getStringExtra("ringtone");
                r = RingtoneManager.getRingtone(this, Uri.parse(alarmUri));
            }

        Log.d("Whatever", "inside service");

        NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (serviceIsStarted) {
            serviceIsStarted = false;
            stopSelf();
            mNM.cancelAll();
            r.stop();
            r = null;

        } else {
            serviceIsStarted = true;

            try
                {
                    r.play();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }

            Intent goBackToMainActivityIntent = new Intent(this.getApplicationContext(), MainActivity.class);
            PendingIntent pendingGoBackToMainActivityIntent = PendingIntent.getActivity(this, 0, goBackToMainActivityIntent, 0);

            Notification myBuilder = new NotificationCompat.Builder(context)
                    .setContentTitle("Do The Thing" + "!")
                    .setContentText("Click me!")
                    .setSmallIcon(R.drawable.checkboximage)
                    .setContentIntent(pendingGoBackToMainActivityIntent)
                    .setAutoCancel(true)
                    .build();

            mNM.notify(027, myBuilder);

            Log.d("Whatever", serviceIsStarted.toString());
        }
            // We want this service to continue running until it is explicitly
            // stopped, so return sticky.
            return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Whatever", "service destroyed");

    }

}
