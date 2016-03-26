package com.taibah.busservice.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.taibah.busservice.MainActivity;
import com.taibah.busservice.R;
import com.google.android.gms.gcm.GcmListenerService;
import com.taibah.busservice.fragments.HomeFragment;
import com.taibah.busservice.fragments.MapsFragment;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";
    String notificationStatus;
    JSONObject jsonObject;
    int value;
    public static boolean studentStatusChanged;

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String output = data.toString();
        String real = output.substring(7, output.length() - 1);
        System.out.println(real);
        try {
            jsonObject = new JSONObject(real);
            notificationStatus = jsonObject.getString("activity");
            value = Integer.parseInt(jsonObject.getString("value"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + real);

        if (notificationStatus.equals("bus_status")) {
            if (AppGlobals.getUserType() == 1) {

                AppGlobals.putRouteStatus(value);

                if (!AppGlobals.isFirstRun()) {
                    if (AppGlobals.getRouteStatus() < 1) {
                        showNotification("Route Stopped");
                    } else if (AppGlobals.getRouteStatus() == 1) {
                        showNotification("Route Started");
                    } else if (AppGlobals.getRouteStatus() > 1) {
                        showNotification(Helpers.parseRouteCancelledReason(AppGlobals.getRouteStatus()));
                        if (MainActivity.isHomeFragmentOpen) {
                            System.exit(0);
                        }
                    }
                }
            }
        } else if (notificationStatus.equals("student_allowed")) {
            if (AppGlobals.getUserType() == 1) {
                if (value == 0) {
                    showNotification("Your service is suspended by the admin");
                } else if (value == 1) {
                    showNotification("Your service has been restored by the admin");
                } else if (AppGlobals.getUserType() == 2 && MapsFragment.mapsFragmentOpen) {
                    studentStatusChanged = true;
                }
            }
        } else if (notificationStatus.equals("student_attending")) {
            if (AppGlobals.getUserType() == 2) {
                studentStatusChanged = true;
            }
        }

    }

    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void showNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.cast_ic_notification_on)
                .setContentTitle("BusService")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }
}
