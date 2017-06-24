package com.rogueduckstudios.dothething;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public ArrayList<String> things = new ArrayList<>();
    Button addThing;
    Button templateButton;
    EditText secondsEditText;
    ViewGroup.LayoutParams params;
    GestureDetector gd;
    AlarmManager alarmManager;
    Context context;
    PendingIntent pendingAlarmIntent;
    String holdTextTemp;
    CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        this.context = this;

        templateButton = (Button) findViewById(R.id.templateButton);
        params = templateButton.getLayoutParams();

        GestureDetector.SimpleOnGestureListener gestureListener = new GestureListener();
        gd = new GestureDetector(this, gestureListener);

        addThing = (Button) findViewById(R.id.addThingButton);
        addThing.setOnClickListener(new AddThingClick());

        try {
            AddThingsToList();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (things.size() == 0) {
            things.add("Welcome!  Click here to edit this task or swipe to remove it. Every time you remove a task the timer will reset.");
        }

        for (int i = 0; i < things.size(); i++) {
            AllButtonListeners(things.get(i));
        }

        RemoveView(templateButton);
    }

    public void RemoveView(View view) {
        ViewGroup vg = (ViewGroup) (view.getParent());
        vg.removeView(view);
    }

    public void AddThingsToList() throws IOException, ClassNotFoundException {
        Log.d("Whatever", "in Add things to list");

        File file = new File(getFilesDir(), "t.tmp");
        Log.d("Whatever", "theFile: " + file);

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        try {
            things = (ArrayList<String>) ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void UpdateListFromButtons(View v) throws IOException {

        things.clear();
        ArrayList<View> allButtons;
        allButtons = (findViewById(R.id.thingListView)).getTouchables();
        for (int i = 0; i < allButtons.size(); i++) {
            Button buttonInView = ((Button) allButtons.get(i));
            things.add(buttonInView.getText().toString());
        }

        File file = new File(getFilesDir(), "t.tmp");
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(things);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        try {
            things = (ArrayList<String>) ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < things.size(); i++) {
            Log.d("Whatever", "List After save " + things.get(i));
        }

        this.finishAffinity();


    }

    public void AlarmStart(View v) {

        secondsEditText = (EditText) findViewById(R.id.seconds);
        int secondsHolder = Integer.parseInt(secondsEditText.getText().toString());

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        pendingAlarmIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + secondsHolder * 1000, pendingAlarmIntent);

        startTimer(secondsHolder * 1000);

        Log.d("Whatever", "Main Activity Right after setting alarm");
    }

    private void startTimer(final int secondsHolder) {
        secondsEditText = (EditText) findViewById(R.id.seconds);

        secondsEditText.clearFocus();

        countDownTimer = new CountDownTimer(secondsHolder, 1000) {
            public void onTick(long millisUntilFinished) {
                int timeLeft = (int) millisUntilFinished / 1000;
                secondsEditText.setText(String.valueOf(timeLeft));
                Log.d("whatever", "Is this working?" + millisUntilFinished);
            }

            public void onFinish() {
                secondsEditText.setText(String.valueOf(secondsHolder / 1000));
            }

        };
        countDownTimer.start();
    }

    public void AlarmStop(View v) {

        if (countDownTimer != null) {
            countDownTimer.onFinish();
            countDownTimer.cancel();
        }

        if (pendingAlarmIntent != null) {
            pendingAlarmIntent.cancel();
        }

        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra("AlarmRunning", true);
        context.startService(serviceIntent);
        Log.d("Whatever", "Main Activity Right after stopping alarm");

    }

    public void AllButtonListeners(String theStringFromThings) {
        ViewGroup listThingView = (ViewGroup) findViewById(R.id.thingListView);
        Button newButton = new Button(MainActivity.this);
        newButton.setBackgroundResource(R.drawable.thingbutton);
        newButton.setText(theStringFromThings);
        holdTextTemp = theStringFromThings;
        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.d("Whatever", "NewButton is Clicked  " + v);
                //   if (holdTextTemp == "")
                holdTextTemp = ((Button) v).getText().toString();

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.dialog_editthing, null);
                dialogBuilder.setView(dialogView);

                final EditText editTheThingEditTextField = dialogView.findViewById(R.id.thething);
                editTheThingEditTextField.setText(holdTextTemp);

                dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ((Button) v).setText(editTheThingEditTextField.getText().toString());
                        holdTextTemp = editTheThingEditTextField.getText().toString();
                    }
                });

                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //pass
                    }
                });

                AlertDialog b = dialogBuilder.create();
                b.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                b.show();
                holdTextTemp = "";
            }
        });

        newButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (gd.onTouchEvent(motionEvent)) {  // the only thing in the class that returns true is fling
                    RemoveView(view);
                    // cancel the intent, then rest the countDownTimer to whatever it was , then restart intent
                }
                return false;
            }
        });

        listThingView.addView(newButton, params);


    }

    private class AddThingClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            AllButtonListeners("New Task");
        }

    }


}




