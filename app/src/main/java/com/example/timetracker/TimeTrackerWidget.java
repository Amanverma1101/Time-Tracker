package com.example.timetracker;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeTrackerWidget extends AppWidgetProvider {

    public static final String ACTION_UPDATE_TEXTVIEW = "com.example.timetracker.UPDATE_TEXTVIEW";
    public static final String EXTRA_INPUT_TEXT = "com.example.timetracker.EXTRA_INPUT_TEXT";
    public static final String ACTION_CLICK_BOX = "com.example.timetracker.CLICK_BOX";
    public static final String ACTION_OPEN_POPUP = "com.example.timetracker.OPEN_POPUP";
    public static final String EXTRA_POSITION = "com.example.timetracker.EXTRA_POSITION";
    public static final String ACTION_APPLY_BORDER ="com.example.timetracker.ACTION_APPLY_BORDER";

    private static int currentBoxId = -1; // Track the currently clicked box
    public static boolean[] timerRunning = new boolean[10]; // Flag to prevent multiple timers
    private ArrayList<String> valuesList = new ArrayList<>();
    public static ArrayList<String> emojiList = new ArrayList<>();
    public static boolean isRunningCurrently = false;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // Fetch stored labels from SharedPreferences
        List<SettingsActivity.EmojiData> localLabels = getEmojiList(context);
        if (localLabels != null && !localLabels.isEmpty()) {
            valuesList.clear(); // Clear existing list
            emojiList.clear();
            for (SettingsActivity.EmojiData label : localLabels) {
                valuesList.add(label.text); // Store only label text
                emojiList.add(label.emoji);
            }
        }
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);
            List<Integer> completedBoxes = new ArrayList<>();
            // Get today's date in "dd-MM-yyyy" format
            String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user_data").child("user1").child(currentDate);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot boxSnapshot : dataSnapshot.getChildren()) {
                        String boxKey = boxSnapshot.getKey(); // "box1", "box2", etc.
                        int boxPosition = Integer.parseInt(boxKey.replace("box", "")); // Extract box number

                        String text = boxSnapshot.child("data").getValue(String.class);
                        String emoji = boxSnapshot.child("selectedOption").getValue(String.class);
                        String fetch_emoji = emojiList.get(valuesList.indexOf(emoji));

                        if (text == null) text = "";
                        if (fetch_emoji == null) fetch_emoji = "🗓️";

                        int textViewId = getBoxTextViewId(boxPosition);
                        int emojiTextViewId = getBoxImageViewId(boxPosition);

                        if (textViewId != -1 && emojiTextViewId != -1) {
                            views.setTextViewText(textViewId, text);
                            views.setTextViewText(emojiTextViewId, fetch_emoji);
                        }
                        timerRunning[boxPosition - 1] = true;
                        completedBoxes.add(boxPosition);
                    }

                    int[] boxIds = {
                            R.id.box_1, R.id.box_2, R.id.box_3, R.id.box_4, R.id.box_5,
                            R.id.box_6, R.id.box_7, R.id.box_8, R.id.box_9, R.id.box_10
                    };

                    for (int boxId : completedBoxes) {
                        int boxViewId = boxIds[boxId-1];
                        if (boxViewId != -1) {
                            views.setInt(boxViewId, "setBackgroundResource", R.drawable.box_border);
                        }
                    }


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

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("FirebaseError", "Failed to load saved data: " + databaseError.getMessage());
                }
            });
        }
    }
    private List<SettingsActivity.EmojiData> getEmojiList(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("emoji_list", null);
        Type type = new TypeToken<List<SettingsActivity.EmojiData>>() {}.getType();
        return json != null ? gson.fromJson(json, type) : new ArrayList<>();
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Handle box click to trigger timer and apply border

        if (ACTION_CLICK_BOX.equals(action)) {
            int boxId = intent.getIntExtra(EXTRA_POSITION, -1);
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
//            Log.d("WidgetReceiver", "Box clicked: " + boxId);
//            Log.d("TimerState", "timerRunning for Box " + boxId + ": " + timerRunning[boxId - 1]);

            if (boxId != -1 && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && !isRunningCurrently) {
                if (!timerRunning[boxId - 1]) {
                    timerRunning[boxId - 1] = true;
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
            String inputValue = intent.getStringExtra("INPUT_VALUE");
            String inputEmoji = intent.getStringExtra("INPUT_EMOJI");
            String inputOption = intent.getStringExtra("INPUT_OPTION"); // Ensure this gets the label text

            if (boxPosition != -1 && inputOption != null) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);
                int textViewId = getBoxTextViewId(boxPosition);
                int emojiTextViewId = getBoxImageViewId(boxPosition);  // TextView for emoji

                if (textViewId != -1 && emojiTextViewId != -1) {
                    views.setTextViewText(textViewId, inputValue); // Set input value
                    if(inputEmoji!=null && !inputEmoji.isEmpty()) {
                        views.setTextViewText(emojiTextViewId, inputEmoji);
                    }else{
                        views.setTextViewText(emojiTextViewId, "📅");
                    }

                    ComponentName widget = new ComponentName(context, TimeTrackerWidget.class);
                    appWidgetManager.updateAppWidget(widget, views);
                }
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


    private void openPopup(Context context, int boxId) {
        Log.d("openPopup", "Attempting to open Popup for Box ID: " + boxId);
        Intent popupIntent = new Intent(context, PopupActivity.class);
        popupIntent.putExtra("BOX_POSITION", boxId);
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(popupIntent);
    }



//    private void triggerBoxClick(Context context, int boxId, int appWidgetId) {
//        // Create an Intent for the ACTION_CLICK_BOX broadcast
//        Intent clickBoxIntent = new Intent(context, TimeTrackerWidget.class);
//        clickBoxIntent.setAction(ACTION_CLICK_BOX);
//        clickBoxIntent.putExtra(EXTRA_POSITION, boxId);
//        clickBoxIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
//
//        // Send the broadcast to simulate the click
//        context.sendBroadcast(clickBoxIntent);
//    }
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