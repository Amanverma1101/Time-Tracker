package com.example.timetracker;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TimeTrackerWidget extends AppWidgetProvider {

    public static final String ACTION_UPDATE_TEXTVIEW = "com.example.timetracker.UPDATE_TEXTVIEW";
    public static final String EXTRA_INPUT_TEXT = "com.example.timetracker.EXTRA_INPUT_TEXT";
    public static final String ACTION_CLICK_BOX = "com.example.timetracker.CLICK_BOX";
    public static final String ACTION_OPEN_POPUP = "com.example.timetracker.OPEN_POPUP";
    public static final String EXTRA_POSITION = "com.example.timetracker.EXTRA_POSITION";
    public static final String ACTION_APPLY_BORDER ="com.example.timetracker.ACTION_APPLY_BORDER";

    private static int currentBoxId = -1; // Track the currently clicked box
    public static boolean[] timerRunning = new boolean[10]; // Flag to prevent multiple timers

    public static boolean isRunningCurrently = false;
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);

            int[] boxIds = {
                    R.id.box_1, R.id.box_2, R.id.box_3, R.id.box_4, R.id.box_5,
                    R.id.box_6, R.id.box_7, R.id.box_8, R.id.box_9, R.id.box_10
            };

            // Set up PendingIntent for box clicks
            for (int i = 0; i < boxIds.length; i++) {
                final int boxPosition = i + 1; // Position 1-10
                Intent clickBoxIntent = new Intent(context, TimeTrackerWidget.class);
                clickBoxIntent.setAction(ACTION_CLICK_BOX);
                clickBoxIntent.putExtra(EXTRA_POSITION, boxPosition);
                clickBoxIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId); // Pass appWidgetId
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context, boxPosition, clickBoxIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(boxIds[i], pendingIntent);
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Handle box click to trigger timer and apply border

        if (ACTION_CLICK_BOX.equals(action)) {
            int boxId = intent.getIntExtra(EXTRA_POSITION, -1);
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.d("WidgetReceiver", "Box clicked: " + boxId);
            Log.d("TimerState", "timerRunning for Box " + boxId + ": " + timerRunning[boxId - 1]);

            if (boxId != -1 && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && !isRunningCurrently) {
                if (!timerRunning[boxId - 1]) {
                    timerRunning[boxId - 1] = true;
//                    new Handler().postDelayed(() -> openPopup(context, boxId), 11000);
                    currentBoxId = boxId - 1;  // Store the clicked box ID (0-indexed)
//                    startTimer(context, appWidgetId, boxId);
                    startTimerService(context, boxId, appWidgetId);


//                    Log.d("WidgetReceiver", "PopupActivity launched for Box: " + boxId);

                } else {
                    // If the timer is done, apply the border to the clicked box
                    Log.d("WidgetReceiver", "Timer already running for Box: " + boxId);

                    applyBorderToBox(context, boxId); // Pass appWidgetId here
                    openPopup(context, boxId); // Open the popup after the border is applied
                }
            }
        }
        else if ("com.example.timetracker.UPDATE_BOX".equals(action)) {
            int boxPosition = intent.getIntExtra("BOX_POSITION", -1);
            int inputOption = intent.getIntExtra("INPUT_OPTION", -1); // Default to 0 if not found
            String inputValue = intent.getStringExtra("INPUT_VALUE");
            String option = String.valueOf((inputOption - 7));

            if (boxPosition != -1 && inputOption != -1) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);
                int textViewId = getBoxTextViewId(boxPosition);
                int emojiTextViewId = getBoxImageViewId(boxPosition);  // Since you're using TextView to show emojis now

                // Fetch the emoji from Firebase
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user_data").child("user1").child("labels").child(option);
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String emoji = dataSnapshot.child("emoji").getValue(String.class);
                        if (emoji == null) emoji = "🤨"; // Default emoji if none found

                        if (textViewId != -1 && emojiTextViewId != -1) {
                            views.setTextViewText(textViewId, inputValue); // Set input value to the TextView
                            views.setTextViewText(emojiTextViewId, emoji); // Set emoji to the emoji TextView

                            ComponentName widget = new ComponentName(context, TimeTrackerWidget.class);
                            appWidgetManager.updateAppWidget(widget, views);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("FirebaseError", "Failed to fetch emoji: " + databaseError.getMessage());
                    }
                });
            }
        }

        else if ("com.example.timetracker.START_TIMER_ACTION".equals(intent.getAction())) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                startTimer(context, appWidgetId, currentBoxId + 1); // Start the timer for the selected box
            }
        }
        else if (ACTION_APPLY_BORDER.equals(action)) {
            int boxId = intent.getIntExtra(EXTRA_POSITION, -1);
//            Log.d( "box_click","box clicked2 is : "+boxId);
            if (boxId != -1) {
                applyBorderToBox(context, boxId);
            }
        }
        else if(ACTION_OPEN_POPUP.equals(action)){
            int boxId = intent.getIntExtra(EXTRA_POSITION, -1);

            if (boxId != -1) {
                openPopup(context, boxId);
                Log.d( "popup_recieved","popup2 : "+boxId);
            }
        }
        else {
            super.onReceive(context, intent); // Handle default behavior
        }
    }

    private void startTimer(Context context, int appWidgetId, int boxId) {
        timerRunning[boxId-1] = true;
        isRunningCurrently = true;
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);
        int timerDuration = 10; // Timer duration in seconds

        // Set initial timer text
        views.setTextViewText(R.id.timer_display, "00:10");
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);

        // Use CountDownTimer for live updates
        new CountDownTimer(timerDuration * 1000, 1000) {
            int secondsRemaining = timerDuration;

            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining--;
                Log.d( "onTick","secondsRemaining: "+secondsRemaining);
                String timerText = String.format("%02d:%02d", secondsRemaining / 60, secondsRemaining % 60);
                views.setTextViewText(R.id.timer_display, timerText);
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
            }

            @Override
            public void onFinish() {
                views.setTextViewText(R.id.timer_display, "00:00");
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
                openPopup(context, boxId);

                isRunningCurrently = false;

                // Play the buzzer sound
                playBuzzer(context);

                // Apply border to the clicked box after the timer finishes
                applyBorderToBox(context, boxId); // Correctly pass appWidgetId

//                openPopup(context, boxId);
                triggerBoxClick(context, boxId, appWidgetId);
            }
        }.start();
    }

    private void applyBorderToBox(Context context, int boxId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);
        int[] boxIds = {
                R.id.box_1, R.id.box_2, R.id.box_3, R.id.box_4, R.id.box_5,
                R.id.box_6, R.id.box_7, R.id.box_8, R.id.box_9, R.id.box_10
        };

        if (boxId >= 1 && boxId <= 10) {
            views.setInt(boxIds[boxId - 1], "setBackgroundResource", R.drawable.box_border);
            ComponentName componentName = new ComponentName(context, TimeTrackerWidget.class);
            appWidgetManager.updateAppWidget(componentName, views);
        }
    }

    private void playBuzzer(Context context) {
        try {
            // Get the default alarm tone URI
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            Ringtone ringtone = RingtoneManager.getRingtone(context, alarmUri);
            ringtone.play();

            // Stop after 5 seconds
            new Handler().postDelayed(ringtone::stop, 5000);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error playing alarm tone", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPopup(Context context, int boxId) {
        Log.d("openPopup", "Attempting to open Popup for Box ID: " + boxId);
        Intent popupIntent = new Intent(context, PopupActivity.class);
        popupIntent.putExtra("BOX_POSITION", boxId);
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(popupIntent);

//        try {
//            context.startActivity(popupIntent);
//            Log.d("openPopup", "Popup should now be visible for Box ID: " + boxId);
//        } catch (Exception e) {
//            Log.e("openPopup", "Failed to open popup", e);
//        }
    }



    private void triggerBoxClick(Context context, int boxId, int appWidgetId) {
        // Create an Intent for the ACTION_CLICK_BOX broadcast
        Intent clickBoxIntent = new Intent(context, TimeTrackerWidget.class);
        clickBoxIntent.setAction(ACTION_CLICK_BOX);
        clickBoxIntent.putExtra(EXTRA_POSITION, boxId);
        clickBoxIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        // Send the broadcast to simulate the click
        context.sendBroadcast(clickBoxIntent);
    }
    @SuppressLint("NewApi")
    private void startTimerService(Context context, int boxId, int appWidgetId) {
        Intent serviceIntent = new Intent(context, TimerService.class);
        serviceIntent.putExtra(EXTRA_POSITION, boxId);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
    private int getImageResourceId(int inputValue) {
        Log.d("img_tag", "getImageResourceId: "+inputValue);
        switch (inputValue) {
            case 0:
                return R.drawable.yt;
            case 1:
                return R.drawable.insta;
            case 2:
                return R.drawable.med;
            case 3:
                return R.drawable.food;
            case 4:
                return R.drawable.gym;
            case 5:
                return R.drawable.read;
            case 6:
                return R.drawable.run;
            default:
                return R.drawable.def; // default image if no match is found
        }
    }

    private int getBoxImageViewId(int boxPosition) {
        int[] imageViewIds = {
                R.id.imageView1, R.id.imageView2, R.id.imageView3, R.id.imageView4, R.id.imageView5,
                R.id.imageView6, R.id.imageView7, R.id.imageView8, R.id.imageView9, R.id.imageView10
        };
        return (boxPosition > 0 && boxPosition <= imageViewIds.length) ? imageViewIds[boxPosition - 1] : -1;
    }


    private int getBoxTextViewId(int boxPosition) {
        switch (boxPosition) {
            case 1: return R.id.box_text_1;
            case 2: return R.id.box_text_2;
            case 3: return R.id.box_text_3;
            case 4: return R.id.box_text_4;
            case 5: return R.id.box_text_5;
            case 6: return R.id.box_text_6;
            case 7: return R.id.box_text_7;
            case 8: return R.id.box_text_8;
            case 9: return R.id.box_text_9;
            case 10: return R.id.box_text_10;
            default: return -1;
        }
    }
}