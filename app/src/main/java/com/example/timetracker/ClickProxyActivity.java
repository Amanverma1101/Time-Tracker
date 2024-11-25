//package com.example.timetracker;
//
//
//import android.app.Activity;
//import android.appwidget.AppWidgetManager;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//
//public class ClickProxyActivity extends Activity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        // Get the position from the intent
//        Intent intent = getIntent();
//        int position = intent.getIntExtra("BOX_POSITION", -1);
//
//        if (position != -1) {
//            // Log for debugging
//            Log.d("ClickProxyActivity", "Position received: " + position);
//
//            // Send a broadcast to the WidgetReceiver with the BOX_POSITION
//            Intent broadcastIntent = new Intent(this, WidgetReceiver.class);
//            broadcastIntent.setAction("com.example.timetracker.BOX_CLICK");
//            broadcastIntent.putExtra("BOX_POSITION", position);
//
//            sendBroadcast(broadcastIntent);
//        }
//
//        // Close the activity immediately after processing
//        finish();
//    }
//}
