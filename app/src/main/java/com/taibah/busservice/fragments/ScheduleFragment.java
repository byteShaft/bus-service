package com.taibah.busservice.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;

import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;
import com.taibah.busservice.utils.UpdateStudentStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ScheduleFragment extends Fragment {

    View convertView;
    Switch switchStudentSchedule;

    static int responseCode;
    HttpURLConnection connection;
    boolean internetNotWorking = false;
    boolean isAttending;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_schedule, null);

        new RetrieveStudentDetails().execute();

        switchStudentSchedule = (Switch) convertView.findViewById(R.id.switch_student_schedule);

        switchStudentSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAttending) {
                    new UpdateStudentStatus(getActivity()).execute("attending=0");
                } else {
                    new UpdateStudentStatus(getActivity()).execute("attending=1");
                }
                getActivity().onBackPressed();
            }
        });

        return convertView;
    }

    public class RetrieveStudentDetails extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Retrieving status");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isInternetWorking() && Helpers.isNetworkAvailable()) {
                try {
                    URL url = new URL("http://46.101.75.194:8080/user");

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(false);
                    connection.setDoInput(true);
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());

                    responseCode = connection.getResponseCode();

                    InputStream in = (InputStream) connection.getContent();

                    int ch;

                    StringBuilder sb = new StringBuilder();
                    while((ch = in.read()) != -1)
                        sb.append((char)ch);
                    JSONObject jsonObject = new JSONObject(sb.toString());
                    if (jsonObject.getString("attending").equalsIgnoreCase("1")) {
                        isAttending = true;
                    } else {
                        isAttending = false;
                    }

                } catch (IOException e) {
                    Log.e("BEFORE", e.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                internetNotWorking = true;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (internetNotWorking) {
                Toast.makeText(getActivity(), "Internet is not working. Make sure you are " +
                        "properly connected to the Internet", Toast.LENGTH_SHORT).show();
                Helpers.dismissProgressDialog();
                getActivity().onBackPressed();
                internetNotWorking = false;
            } else if (responseCode == 200) {
                switchStudentSchedule.setVisibility(View.VISIBLE);
                if (isAttending) {
                    switchStudentSchedule.setChecked(true);
                } else {
                    switchStudentSchedule.setChecked(false);
                }
                Helpers.dismissProgressDialog();
            }
        }
    }
}
