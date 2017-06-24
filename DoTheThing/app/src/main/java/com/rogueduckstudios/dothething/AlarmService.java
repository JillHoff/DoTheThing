package com.rogueduckstudios.dothething;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.BundleCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AlarmService extends Service {

    Context context;
    MediaPlayer pennyPlayer;
    Boolean serviceIsStarted = false;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.context = this;

        Log.d("Whatever", "inside service");

        NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (pennyPlayer == null)
            pennyPlayer = MediaPlayer.create(this, R.raw.penny);

        serviceIsStarted = intent.getBooleanExtra("AlarmRunning", false);

        if (serviceIsStarted) {
            serviceIsStarted = false;
            pennyPlayer.stop();
            stopSelf();
            mNM.cancelAll();

        } else {
            serviceIsStarted = true;
            pennyPlayer.start();

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
