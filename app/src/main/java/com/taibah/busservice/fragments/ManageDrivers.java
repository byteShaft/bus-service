package com.taibah.busservice.fragments;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ManageDrivers extends Fragment {

    public static int responseCode;

    View convertView;
    HttpURLConnection connection;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_manage_drivers, null);
        setHasOptionsMenu(true);

        return convertView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_manage, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_button:
                new RetrieveAllRegisteredRoutes().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class RetrieveAllRegisteredRoutes extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Retrieving All Routes...");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isNetworkAvailable() && Helpers.isInternetWorking()) {
                try {
                    connection = openConnectionForUrl("http://46.101.75.194:8080/routes");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connection.connect();
                    JSONArray jsonObj = readResponse(connection);
                    responseCode = connection.getResponseCode();
                    System.out.println(jsonObj);
                    Log.i("Response Code: ", "" + connection.getResponseCode());
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (responseCode == 200) {
                Helpers.dismissProgressDialog();
                AppGlobals.replaceFragment(getFragmentManager(), new RegisterDriver());
            } else {
                Toast.makeText(getActivity(), "Invalid Response " + responseCode, Toast.LENGTH_SHORT).show();
                Helpers.dismissProgressDialog();
            }
        }
    }

    private HttpURLConnection openConnectionForUrl(String path)
            throws IOException {

        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("GET");
        return connection;
    }

    private JSONArray readResponse(HttpURLConnection connection)
            throws IOException, JSONException {

        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder response = new StringBuilder();
        while((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        return new JSONArray(response.toString());
    }
}
