package com.example.timetracker;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class TimeTrackerWidget extends AppWidgetProvider {

    public static final String ACTION_UPDATE_TEXTVIEW = "com.example.timetracker.UPDATE_TEXTVIEW";
    public static final String EXTRA_INPUT_TEXT = "com.example.timetracker.EXTRA_INPUT_TEXT";
    public static final String ACTION_CLICK_BOX = "com.example.timetracker.CLICK_BOX";
    public static final String EXTRA_POSITION = "com.example.timetracker.EXTRA_POSITION";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);

            int[] boxIds = {
                    R.id.box_1, R.id.box_2, R.id.box_3, R.id.box_4, R.id.box_5,
                    R.id.box_6, R.id.box_7, R.id.box_8, R.id.box_9, R.id.box_10
            };

            for (int i = 0; i < boxIds.length; i++) {
                Intent popupIntent = new Intent(context, PopupActivity.class);
                popupIntent.putExtra("BOX_POSITION", i + 1);
                popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, i, popupIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(boxIds[i], pendingIntent);
            }

            Intent timerIntent = new Intent(context, TimeTrackerWidget.class);
            timerIntent.setAction("com.example.timetracker.START_TIMER_ACTION");
            timerIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            PendingIntent timerPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, timerIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.start_timer_btn, timerPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if ("com.example.timetracker.BOX_CLICK".equals(action)) {
            int boxId = intent.getIntExtra("BOX_POSITION", -1);

            if (boxId != -1) {
                Log.d("WidgetReceiver", "Box clicked: " + boxId);

                // Open the PopupActivity for the clicked box
                Intent popupIntent = new Intent(context, PopupActivity.class);
                popupIntent.putExtra("BOX_POSITION", boxId); // Pass the box ID to the popup
                popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Ensure the popup opens properly
                context.startActivity(popupIntent);
            }
        }
        else {
            super.onReceive(context, intent); // Handle default behavior
        }


        if ("com.example.timetracker.UPDATE_BOX".equals(action)) {
            int boxPosition = intent.getIntExtra("BOX_POSITION", -1);
            String inputValue = intent.getStringExtra("INPUT_VALUE");
            Log.d("PopupActivityDebug", "selectedNumber: " + boxPosition);
            Log.d("PopupActivityDebug", "inputValue: " + inputValue);
            if (boxPosition != -1 && inputValue != null) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);

                int textViewId = getBoxTextViewId(boxPosition);
                if (textViewId != -1) {
                    views.setTextViewText(textViewId, inputValue);
                }

                String summaryText = "Box " + boxPosition + ": " + inputValue;
                views.setTextViewText(R.id.box_comp, summaryText);

                ComponentName widget = new ComponentName(context, TimeTrackerWidget.class);
                appWidgetManager.updateAppWidget(widget, views);
            }
        }
        else {
            super.onReceive(context, intent); // Handle default behavior
        }

        if ("com.example.timetracker.START_TIMER_ACTION".equals(intent.getAction())) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                handleTimerStart(context, appWidgetId);
            }
        }
    }

    private void handleTimerStart(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);

        // Initialize the timer duration (1 minute)
        int timerDuration = 60; // in seconds

        // Use a CountDownTimer for live updates
        new CountDownTimer(timerDuration * 1000, 1000) {
            int secondsRemaining = timerDuration;

            @Override
            public void onTick(long millisUntilFinished) {
                // Update the TextView with the remaining time
                secondsRemaining--;
                String timerText = String.format("%02d:%02d", secondsRemaining / 60, secondsRemaining % 60);
                views.setTextViewText(R.id.timer_display, timerText);
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
            }

            @Override
            public void onFinish() {
                // Reset the TextView
                views.setTextViewText(R.id.timer_display, "00:00");
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);

                // Show a toast
                Toast.makeText(context, "1 minute is over!", Toast.LENGTH_LONG).show();

                // Play the buzzer sound
                playBuzzer(context);
            }
        }.start();
    }

    private void playBuzzer(Context context) {
        try {
            // Get the default alarm tone URI
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            // If no alarm tone is set, fall back to notification sound
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            // Play the alarm tone
            Ringtone ringtone = RingtoneManager.getRingtone(context, alarmUri);
            ringtone.play();

            // Optional: Stop the ringtone after a certain duration
            new Handler().postDelayed(ringtone::stop, 5000); // Stops after 5 seconds
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error playing alarm tone", Toast.LENGTH_SHORT).show();
        }
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
