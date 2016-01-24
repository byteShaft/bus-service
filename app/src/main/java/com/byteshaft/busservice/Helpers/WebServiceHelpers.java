package com.byteshaft.busservice.Helpers;


import android.os.AsyncTask;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebServiceHelpers extends AsyncTask<Void, Void, JSONObject> {

    private static final String mSessionURL = "Needed";


    private HttpURLConnection openConnectionForUrl(String path)
            throws IOException {

        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json");
        String authString = "Needed";
        System.out.println("auth string: " + authString);
        String authEncBytes = Base64.encodeToString(authString.getBytes(), Base64.DEFAULT);
        System.out.println("Base64 encoded auth string: " + authEncBytes);
        connection.setRequestProperty("Authorization", "Basic " + authEncBytes);
        connection.setRequestMethod("GET");
        return connection;
    }

    private JSONObject postLocationUpdate()
            throws IOException, JSONException {
        HttpURLConnection connection = openConnectionForUrl(mSessionURL);
        return readResponse(connection);
    }

    private JSONObject readResponse(HttpURLConnection connection)
            throws IOException, JSONException {

        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder response = new StringBuilder();
        while((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        return new JSONObject(response.toString());
    }

    @Override
    protected JSONObject doInBackground(Void... params) {
        JSONObject out = null;
        try {
            out = postLocationUpdate();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return out;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        try {
            System.out.println(jsonObject.get("latitude"));
            System.out.println(jsonObject.get("longitude"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        super.onPostExecute(jsonObject);
    }
}
