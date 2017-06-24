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
        //a Bundle containing the activity's previously frozen state, if there was one.
        super.onCreate(savedInstanceState);

        //Attach this class to the xml layout
        setContentView(R.layout.activity_main);

        //Interface to global information about an application environment - needed for broadcasting
        this.context = this;

        //Find the button we will use as a template for the 'spawned' buttons
        //then save it's parameters (size, margins) Template button will be deleted
        templateButton = (Button) findViewById(R.id.templateButton);
        params = templateButton.getLayoutParams();

        //set up a listener for the 'fling' to delete touch - uses the class we copied from somewhere
        GestureDetector.SimpleOnGestureListener gestureListener = new GestureListener();
        gd = new GestureDetector(this, gestureListener);

        //find the add thing button and set up a listener for it
        addThing = (Button) findViewById(R.id.addThingButton);
        addThing.setOnClickListener(new AddThingClick());

        // call method to load previously saved tasks (in a try catch cause of file I/O)
        try {
            AddThingsToList();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        // after loading tasks - if the list is still empty - add a default task
        if (things.size() == 0) {
            things.add("Welcome!  Click here to edit this task or swipe to remove it. Every time you remove a task the timer will reset.");
        }

        //go through all the items in the list and create buttons for them
        for (int i = 0; i < things.size(); i++) {
            AllButtonListeners(things.get(i));
        }

        //delete the template button
        RemoveView(templateButton);
    }

    // delete a task button
    public void RemoveView(View view) {
        ViewGroup vg = (ViewGroup) (view.getParent());
        vg.removeView(view);
    }

    // File I/O to add tasks from a file into the 'things' array
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

    // File I/O to save all the tasks on the buttons to the file and to the things array
    // TODO call this function when the app is closed or paused
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

    // starts the alarm - gets the time from the user, initializes an alarm manager, sets up an intent
    // that calls the receiver class we made, sets up a pending intent which is an intent wrapped up in a delay
    // then sends all of that to the alarm manager.
    // the alarm manager (an internal android class) will wait till the time has passed and then wake up the app
    // and call the receiver class.
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

    // starts the timer on the app to show the time ticking down for the user
    // todo set to minutes and seconds instead of just seconds
    // todo set up the countdown to show as another broadcast-notification?
    private void startTimer(final int secondsHolder) {
        secondsEditText = (EditText) findViewById(R.id.seconds);

        // had to add this because it was bringing up the virtual keyboard as soon as the app started.
        secondsEditText.clearFocus();

        countDownTimer = new CountDownTimer(secondsHolder, 1000) {
            public void onTick(long millisUntilFinished) {
                int timeLeft = (int) millisUntilFinished / 1000;
                secondsEditText.setText(String.valueOf(timeLeft));
            }

            public void onFinish() {
                secondsEditText.setText(String.valueOf(secondsHolder / 1000));
            }

        };
        countDownTimer.start();
    }

    // tests to see if the alarm is set, and if the clock is ticking, just to avaoid errors
    // shuts everything down
    public void AlarmStop(View v) {

        if (countDownTimer != null) {
            countDownTimer.onFinish();
            countDownTimer.cancel();
        }

        if (pendingAlarmIntent != null) {
            pendingAlarmIntent.cancel();
        }

        // the extra in this intent is to make sure the media player isn't turned on if it's already off
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra("AlarmRunning", true);
        context.startService(serviceIntent);
        Log.d("Whatever", "Main Activity Right after stopping alarm");

    }

    // called when a task button is added to creat the button, set the text and add it's listeners
    // called from the initial set up in onCreate and from the 'add thing' button
    public void AllButtonListeners(String theStringFromThings) {
        // find the scroll list in the layout
        ViewGroup listThingView = (ViewGroup) findViewById(R.id.thingListView);

        // create a new button
        Button newButton = new Button(MainActivity.this);
        // put the png background on the button
        newButton.setBackgroundResource(R.drawable.thingbutton);
        // set the button's text from the string sent to this method
        newButton.setText(theStringFromThings);
        // also put that string in a global variable for the dialog builder to use
        // todo This might be redundant - needs some more debugging
        holdTextTemp = theStringFromThings;
        // set up the onClick listener
        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                // put string in a global variable for the dialog builder to use
                holdTextTemp = ((Button) v).getText().toString();

                // set up the dialog - a pop up window to change the text on the button
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                // gets the layout and assigns it to the dialog
                final View dialogView = inflater.inflate(R.layout.dialog_editthing, null);
                dialogBuilder.setView(dialogView);

                // finds the text field on the pop-up and sets the holding text to it
                final EditText editTheThingEditTextField = dialogView.findViewById(R.id.thething);
                editTheThingEditTextField.setText(holdTextTemp);

                // save button on pop-up - saves the new text to the button and the holder
                // todo again - possibly redundant?
                dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ((Button) v).setText(editTheThingEditTextField.getText().toString());
                        holdTextTemp = editTheThingEditTextField.getText().toString();
                    }
                });

                // cancel button on pop-up
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //pass
                    }
                });

                // start the pop up
                // // todo - make sure the virtual keyboard pops up too
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
                    // todo if this is swiped - the alarm should be reset - so cancel the intent, then rest the countDownTimer to whatever it was , then restart intent
                }
                return false;
            }
        });

        listThingView.addView(newButton, params);


    }

    // the listener for the add thing button - here in code instead of in the XML because
    // the method is looking for a string.
    // todo is there a way to add an argument to the 'onClick' in the XML?
    private class AddThingClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            AllButtonListeners("New Task");
        }

    }


}




