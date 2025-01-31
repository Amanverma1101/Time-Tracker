package com.example.timetracker;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.os.Build;
import android.appwidget.AppWidgetManager;
import android.widget.RemoteViews;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.util.Log;

public class TimerService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "timer_service_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int boxId = intent.getIntExtra(TimeTrackerWidget.EXTRA_POSITION, -1);
        if (boxId != -1) {
            startForeground(NOTIFICATION_ID, buildNotification(boxId));
            startTimer(boxId);
        }
        return START_STICKY;
    }

    private void startTimer(int boxId) {
        TimeTrackerWidget.isRunningCurrently = true;
        final RemoteViews views = new RemoteViews(getPackageName(), R.layout.time_tracker_widget);
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        new CountDownTimer(9500, 1000) {  // 10 seconds for demo
            public void onTick(long millisUntilFinished) {
                String timeLeft = String.format("%02d:%02d", (millisUntilFinished / 1000) / 60, (millisUntilFinished / 1000) % 60);
                views.setTextViewText(R.id.timer_display, timeLeft);
                appWidgetManager.updateAppWidget(new ComponentName(TimerService.this, TimeTrackerWidget.class), views);
            }

            public void onFinish() {
                views.setTextViewText(R.id.timer_display, "00:00");
                TimeTrackerWidget.isRunningCurrently = false;
                appWidgetManager.updateAppWidget(new ComponentName(TimerService.this, TimeTrackerWidget.class), views);
                playBuzzer();
                applyBorderToBox(boxId);
                openPopup(boxId);
                stopForeground(true);
                stopSelf();
            }
        }.start();
    }

    private Notification buildNotification(int boxId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Timer Running for Box " + boxId)
                .setContentText("Tap to view.")
                .setSmallIcon(R.drawable.gym)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void playBuzzer() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
            ringtone.play();
            new Handler().postDelayed(ringtone::stop, 5000);
        } catch (Exception e) {
            Log.e("TimerService", "Error playing sound: " + e.getMessage());
        }
    }

    private void applyBorderToBox(int boxId) {
        // Apply the border to the clicked box after the timer finishes
//        Log.d( "box_click","box clicked1 is : "+boxId);
        Intent intent = new Intent(this, TimeTrackerWidget.class);
        intent.setAction(TimeTrackerWidget.ACTION_APPLY_BORDER);
        intent.putExtra(TimeTrackerWidget.EXTRA_POSITION, boxId);
        sendBroadcast(intent);
    }

    private void openPopup(int boxId) {
        Intent popupIntent = new Intent(this, PopupActivity.class);
        popupIntent.putExtra("BOX_POSITION", boxId);
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(popupIntent);
            Log.d("TimerService", "Popup should now be visible for Box ID: " + boxId);
        } catch (Exception e) {
            Log.e("TimerService", "Failed to open popup", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Timer Notification Channel";
            String description = "Channel for Timer Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
