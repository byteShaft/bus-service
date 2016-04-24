package com.taibah.busservice.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.taibah.busservice.fragments.HomeFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateRouteStatus extends AsyncTask<String, Integer, Void> {

    private Context mContext;

    static int responseCode;
    HttpURLConnection connection;

    public UpdateRouteStatus(Context context) {
        super();
        mContext = context;
    }


    @Override
    protected Void doInBackground(String... params) {
        Log.i("UpdateRouteStatus", "Called");
        try {

            URL url = new URL("http://46.101.75.194:8080/timings/" + AppGlobals.timingIDForStatusUpdate);

            System.out.println("Timing ID for Status Update: " + AppGlobals.timingIDForStatusUpdate);

            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());

            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.writeBytes(params[0]);
            out.flush();
            out.close();

            responseCode = connection.getResponseCode();

            System.out.println("Update Status Response Code: " + responseCode);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (responseCode != 200) {
            Toast.makeText(mContext, "Response Code " + responseCode, Toast.LENGTH_LONG).show();
        }
    }
}
