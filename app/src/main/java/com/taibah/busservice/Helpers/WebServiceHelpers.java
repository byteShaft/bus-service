package com.taibah.busservice.Helpers;


import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class WebServiceHelpers {

    private static final String mSessionURL = "http://46.101.75.194:8080/login";

    private static HttpURLConnection openConnectionForUrl(String path)
            throws IOException {

        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json");
        String authString = "superadmin" + ":" + "secret";
        String authStringEncoded = Base64.encodeToString(authString.getBytes(), Base64.DEFAULT);
        connection.setRequestProperty("Authorization", authStringEncoded);
        connection.setRequestMethod("POST");
        return connection;
    }

    public static String getToken()
            throws IOException, JSONException {
//        String data = String.format("{\"username\" : \"%s\", \"password\" : \"%s\"}", username, password);
        Multipart multipart = new Multipart(new URL(mSessionURL), "POST");
        multipart.addFormField("username", "superadmin");
        multipart.addFormField("password", "secret");
        multipart.finish();

////        sendRequestData(connection, data);
//        System.out.println(connection.getResponseCode());
//        System.out.println(connection.getResponseMessage());
//
//        JSONObject jsonObj = readResponse(connection);
//        Log.i("Response", "" + connection.getResponseCode());
//        Log.i("JSONObject", "" + jsonObj);
//        return (String) jsonObj.get("token");
        return null;
    }

    private static void sendRequestData(HttpURLConnection connection, String body)
            throws IOException {

        byte[] outputInBytes = body.getBytes("UTF-8");
        OutputStream os = connection.getOutputStream();
        os.write(outputInBytes);
        os.close();
    }

    private static JSONObject readResponse(HttpURLConnection connection)
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

    public boolean isInternetWorking() {
        boolean success = false;
        try {
            URL url = new URL("https://google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.connect();
            success = connection.getResponseCode() == 200;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return success;
    }

}
