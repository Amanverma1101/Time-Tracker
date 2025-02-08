package com.example.timetracker;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
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
import java.util.Calendar;
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
    public static final String ACTION_REFRESH_WIDGET = "com.example.timetracker.REFRESH_WIDGET";

    private static int currentBoxId = -1; // Track the currently clicked box
    public static boolean[] timerRunning = new boolean[36]; // Flag to prevent multiple timers
    private ArrayList<String> valuesList = new ArrayList<>();
    public static ArrayList<String> emojiList = new ArrayList<>();
    public static boolean isRunningCurrently = false;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        scheduleWidgetReset(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);
//        clearAllBoxes(views);
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        if (userId != null) {
            updateBoxLabels(context, views);
            Intent refreshIntent = new Intent(context, TimeTrackerWidget.class);
            refreshIntent.setAction(ACTION_REFRESH_WIDGET);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            views.setOnClickPendingIntent(R.id.timer_display, pendingIntent);

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
//            views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);
                List<Integer> completedBoxes = new ArrayList<>();
                // Get today's date in "dd-MM-yyyy" format
                String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user_data").child(userId).child(currentDate);

                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int currentSegment = getCurrentTimeSegment();
                        int startBoxIndex = (currentSegment - 1) * 36 + 1;

                        for (DataSnapshot boxSnapshot : dataSnapshot.getChildren()) {
                            String boxKey = boxSnapshot.getKey();
                            int actualBoxNumber = Integer.parseInt(boxKey.replace("box", "")); // Extract box number

                            // Only process boxes that belong to the current time segment
                            if (actualBoxNumber >= startBoxIndex && actualBoxNumber < startBoxIndex + 36) {
                                int boxPosition = actualBoxNumber - startBoxIndex + 1; // Map to 1-36

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
                        }

                        applyBordersToFilledBoxes(views, context, appWidgetId, completedBoxes);
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("FirebaseError", "Failed to load saved data: " + databaseError.getMessage());
                    }
                });
            }
        } else {
//            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);
            views.setTextViewText(R.id.timer_display, "Please login inside Application & setup this widget again!");

            // Clear any other widgets (e.g., the boxes) and apply the "Please Login" message
            for (int appWidgetId : appWidgetIds) {
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }

    }
    private void applyBordersToFilledBoxes(RemoteViews views,Context context,int appWidgetId, List<Integer> completedBoxes) {
        int[] boxIds = {
                R.id.box_1, R.id.box_2, R.id.box_3, R.id.box_4, R.id.box_5, R.id.box_6,
                R.id.box_7, R.id.box_8, R.id.box_9, R.id.box_10, R.id.box_11, R.id.box_12,
                R.id.box_13, R.id.box_14, R.id.box_15, R.id.box_16, R.id.box_17, R.id.box_18,
                R.id.box_19, R.id.box_20, R.id.box_21, R.id.box_22, R.id.box_23, R.id.box_24,
                R.id.box_25, R.id.box_26, R.id.box_27, R.id.box_28, R.id.box_29, R.id.box_30,
                R.id.box_31, R.id.box_32, R.id.box_33, R.id.box_34, R.id.box_35, R.id.box_36
        };

        for (int boxPosition : completedBoxes) {
            if (boxPosition >= 1 && boxPosition <= 36) {
                int boxViewId = boxIds[boxPosition - 1];
                views.setInt(boxViewId, "setBackgroundResource", R.drawable.box_border);
            }
        }

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

    }


    private List<SettingsActivity.EmojiData> getEmojiList(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("emoji_list", null);
        Type type = new TypeToken<List<SettingsActivity.EmojiData>>() {}.getType();
        return json != null ? gson.fromJson(json, type) : new ArrayList<>();
    }
    private void updateBoxLabels(Context context, RemoteViews views) {
        int currentSegment = getCurrentTimeSegment();
        Log.d("UPDATE_BOX_LABEL", "updateBoxLabels: "+currentSegment);
        int startBoxIndex = (currentSegment - 1) * 36 + 1;  // Compute starting box ID for storage
        int[] boxIds = {
                R.id.box_1, R.id.box_2, R.id.box_3, R.id.box_4, R.id.box_5, R.id.box_6,
                R.id.box_7, R.id.box_8, R.id.box_9, R.id.box_10, R.id.box_11, R.id.box_12,
                R.id.box_13, R.id.box_14, R.id.box_15, R.id.box_16, R.id.box_17, R.id.box_18,
                R.id.box_19, R.id.box_20, R.id.box_21, R.id.box_22, R.id.box_23, R.id.box_24,
                R.id.box_25, R.id.box_26, R.id.box_27, R.id.box_28, R.id.box_29, R.id.box_30,
                R.id.box_31, R.id.box_32, R.id.box_33, R.id.box_34, R.id.box_35, R.id.box_36
        };
        for (int i = 0; i < 36; i++) {
            int actualBoxNumber = startBoxIndex + i;  // Adjusted box ID for Firebase storage
            String timeSlot = getTimeForBox(actualBoxNumber);

            int textViewId = getBoxTextViewId(i + 1);  // UI Box ID remains 1-36
            int emojiTextViewId = getBoxImageViewId(i + 1); // Get corresponding emoji TextView

            if (textViewId != -1) {
                views.setTextViewText(textViewId, timeSlot);  // Update time slot display
            }
            if (emojiTextViewId != -1) {
                views.setTextViewText(emojiTextViewId, "⏳");  // Reset emoji to "⏳"
            }
            views.setInt(boxIds[i], "setBackgroundResource", android.R.color.transparent);
            timerRunning[i] = false;
        }
    }
    private String getTimeForBox(int actualBoxNumber) {
        int startMinutes = (actualBoxNumber - 1) * 10;
        int startHour = startMinutes / 60;
        int startMinute = startMinutes % 60;

        int endMinutes = startMinutes + 10;
        int endHour = endMinutes / 60;
        int endMinute = endMinutes % 60;

        return String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute);
    }
    public void scheduleWidgetReset(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TimeTrackerWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        int nextResetHour = ((calendar.get(Calendar.HOUR_OF_DAY) / 6) + 1) * 6;
        if (nextResetHour >= 24) nextResetHour = 0;

        calendar.set(Calendar.HOUR_OF_DAY, nextResetHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

//
//    private void clearAllBoxes(RemoteViews views) {
//        int[] boxIds = {
//                R.id.box_1, R.id.box_2, R.id.box_3, R.id.box_4, R.id.box_5, R.id.box_6,
//                R.id.box_7, R.id.box_8, R.id.box_9, R.id.box_10, R.id.box_11, R.id.box_12,
//                R.id.box_13, R.id.box_14, R.id.box_15, R.id.box_16, R.id.box_17, R.id.box_18,
//                R.id.box_19, R.id.box_20, R.id.box_21, R.id.box_22, R.id.box_23, R.id.box_24,
//                R.id.box_25, R.id.box_26, R.id.box_27, R.id.box_28, R.id.box_29, R.id.box_30,
//                R.id.box_31, R.id.box_32, R.id.box_33, R.id.box_34, R.id.box_35, R.id.box_36
//        };
//
//        for (int boxId : boxIds) {
//            views.setTextViewText(boxId, ""); // Reset text
////            views.setInt(boxId, "setBackgroundResource", R.drawable.box_default); // Reset border
//        }
//    }




    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Handle box click to trigger timer and apply border

        if (ACTION_CLICK_BOX.equals(action)) {
            int boxId = intent.getIntExtra(EXTRA_POSITION, -1);
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
//            Log.d("WidgetReceiver", "Box clicked: " + boxId);
//            Log.d("TimerState", "timerRunning for Box " + boxId + ": " + timerRunning[boxId - 1]);
//                int currentSegment = getCurrentTimeSegment();
//                int actualBoxNumber = (currentSegment - 1) * 36 + boxId;
//
//                if (boxId != -1) {
//                    Intent serviceIntent = new Intent(context, TimerService.class);
//                    serviceIntent.putExtra(EXTRA_POSITION, actualBoxNumber);  // Store using actual box ID
//                    context.startService(serviceIntent);
//                }


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
        else if (ACTION_REFRESH_WIDGET.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, TimeTrackerWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            Log.d("TimeTrackerWidget", "Timer TextView clicked - refreshing widget.");

            // **Trigger onUpdate() manually**
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
        else {
            super.onReceive(context, intent); // Handle default behavior
        }
    }

    public int getCurrentTimeSegment() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 0 && hour < 6) return 1;   // 00:00 - 06:00
        if (hour >= 6 && hour < 12) return 2;  // 06:00 - 12:00
        if (hour >= 12 && hour < 18) return 3; // 12:00 - 18:00
        return 4;                              // 18:00 - 00:00
    }

    private void applyBorderToBox(Context context, int boxId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.time_tracker_widget);
        int[] boxIds = {
                R.id.box_1, R.id.box_2, R.id.box_3, R.id.box_4, R.id.box_5, R.id.box_6,
                R.id.box_7, R.id.box_8, R.id.box_9, R.id.box_10, R.id.box_11, R.id.box_12,
                R.id.box_13, R.id.box_14, R.id.box_15, R.id.box_16, R.id.box_17, R.id.box_18,
                R.id.box_19, R.id.box_20, R.id.box_21, R.id.box_22, R.id.box_23, R.id.box_24,
                R.id.box_25, R.id.box_26, R.id.box_27, R.id.box_28, R.id.box_29, R.id.box_30,
                R.id.box_31, R.id.box_32, R.id.box_33, R.id.box_34, R.id.box_35, R.id.box_36
        };


        if (boxId >= 1 && boxId <= 36) {
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
                R.id.imageView1, R.id.imageView2, R.id.imageView3, R.id.imageView4, R.id.imageView5, R.id.imageView6,
                R.id.imageView7, R.id.imageView8, R.id.imageView9, R.id.imageView10, R.id.imageView11, R.id.imageView12,
                R.id.imageView13, R.id.imageView14, R.id.imageView15, R.id.imageView16, R.id.imageView17, R.id.imageView18,
                R.id.imageView19, R.id.imageView20, R.id.imageView21, R.id.imageView22, R.id.imageView23, R.id.imageView24,
                R.id.imageView25, R.id.imageView26, R.id.imageView27, R.id.imageView28, R.id.imageView29, R.id.imageView30,
                R.id.imageView31, R.id.imageView32, R.id.imageView33, R.id.imageView34, R.id.imageView35, R.id.imageView36
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
            case 11: return R.id.box_text_11;
            case 12: return R.id.box_text_12;
            case 13: return R.id.box_text_13;
            case 14: return R.id.box_text_14;
            case 15: return R.id.box_text_15;
            case 16: return R.id.box_text_16;
            case 17: return R.id.box_text_17;
            case 18: return R.id.box_text_18;
            case 19: return R.id.box_text_19;
            case 20: return R.id.box_text_20;
            case 21: return R.id.box_text_21;
            case 22: return R.id.box_text_22;
            case 23: return R.id.box_text_23;
            case 24: return R.id.box_text_24;
            case 25: return R.id.box_text_25;
            case 26: return R.id.box_text_26;
            case 27: return R.id.box_text_27;
            case 28: return R.id.box_text_28;
            case 29: return R.id.box_text_29;
            case 30: return R.id.box_text_30;
            case 31: return R.id.box_text_31;
            case 32: return R.id.box_text_32;
            case 33: return R.id.box_text_33;
            case 34: return R.id.box_text_34;
            case 35: return R.id.box_text_35;
            case 36: return R.id.box_text_36;
            default: return -1;
        }
    }
}