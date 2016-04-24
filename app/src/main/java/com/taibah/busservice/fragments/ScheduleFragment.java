package com.taibah.busservice.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import com.taibah.busservice.Helpers.WebServiceHelpers;
import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;
import com.taibah.busservice.utils.UpdateStudentStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
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
    static RadioGroup rgScheduleTimings;
    static RadioButton rbScheduleTimingsOne;
    static RadioButton rbScheduleTimingsTwo;
    static RadioButton rbScheduleTimingsThree;
    static LinearLayout layoutScheduleTiming;
    static int routeID;
    static int timingID;
    static JSONObject jsonObject;
    static int radioCheckCounter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_schedule, null);

        radioCheckCounter = 0;

        new RetrieveStudentDetails().execute();

        switchStudentSchedule = (Switch) convertView.findViewById(R.id.switch_student_schedule);
        rgScheduleTimings = (RadioGroup) convertView.findViewById(R.id.rg_student_schedule_timings);
        rbScheduleTimingsOne = (RadioButton) convertView.findViewById(R.id.rb_student_schedule_timings_one);
        rbScheduleTimingsTwo = (RadioButton) convertView.findViewById(R.id.rb_student_schedule_timings_two);
        rbScheduleTimingsThree = (RadioButton) convertView.findViewById(R.id.rb_student_schedule_timings_three);
        layoutScheduleTiming = (LinearLayout) convertView.findViewById(R.id.layout_student_schedule_timings);

        rgScheduleTimings.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.i("CheckedCID", ": " + rgScheduleTimings.getCheckedRadioButtonId());
                if (radioCheckCounter > 1) {
                    new UpdateStudentTimingStatus().execute("timing_ids[]="
                            + rgScheduleTimings.getCheckedRadioButtonId());
                }
                radioCheckCounter++;
            }
        });

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
                    Log.i("jSOn", "" + jsonObject);
                    String routeData = jsonObject.getString("routes");
                    JSONArray jsonArray = new JSONArray(routeData);
                    JSONObject jsonObject1 = jsonArray.getJSONObject(0);
                    Log.i("Route Data", "" + jsonObject1);
                    routeID = jsonObject1.getInt("id");
                    JSONArray routeTimingJsonArray = new JSONArray(jsonObject1.getString("timings"));
                    Log.i("Timings Array", "" + routeTimingJsonArray);
                    JSONObject routeTimingObject = routeTimingJsonArray.getJSONObject(0);
                    timingID = routeTimingObject.getInt("id");
                    Log.i("Route ID", "" + routeID);
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
                new RetrieveRouteDetails().execute();
            }
        }
    }

    public class RetrieveRouteDetails extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Retrieving route detail");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isNetworkAvailable() && Helpers.isInternetWorking()) {
                try {
                    connection = WebServiceHelpers.openConnectionForUrl
                            ("http://46.101.75.194:8080/routes/" + routeID, "GET");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connection.connect();
                    responseCode = connection.getResponseCode();
                    String data = WebServiceHelpers.readResponse(connection);
                    jsonObject = new JSONObject(data);
                    System.out.println(jsonObject);
                    responseCode = connection.getResponseCode();

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Helpers.dismissProgressDialog();
            if (responseCode == 200) {
                showTimings();
            } else {
                // TODO Implement correct logic here in case of any failure
                Toast.makeText(getActivity(), "Invalid Response " + responseCode, Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
            }
        }
    }

    public static void showTimings() {
        try {
            Log.i("User Details ", "" + jsonObject);
            JSONArray timingsArray = new JSONArray(jsonObject.getString("timings"));

//                JSONArray jsonArray = new JSONArray(AppGlobals.getStudentDriverRouteDetails());
//                JSONObject jsonObject = jsonArray.getJSONObject(0);
//                System.out.println(jsonObject.getString("name"));
//                Log.i("User Details ", "" + jsonObject);
//                JSONArray timingsArrayForID = new JSONArray(jsonObject.getString("timings"));
//                JSONObject jsonObject1 = timingsArrayForID.getJSONObject(0);
//            int studentAssignedTimingID = jsonObject1.getInt("id");
//            Log.i("Timing ID ", "" + studentAssignedTimingID);

            for (int i = 0; i < timingsArray.length(); i++) {
                JSONObject timingsJsonObject = timingsArray.getJSONObject(i);
                String arrivalTime = timingsJsonObject.get("arrival_time").toString().substring(11, 16);
                String departureTime = timingsJsonObject.get("departure_time").toString().substring(11, 16);
                int id = timingsJsonObject.getInt("id");
                String routeTime = "(" + Helpers.convertTimeForUser(arrivalTime) + " - " + Helpers.convertTimeForUser(departureTime) + ")";
                if (i == 0) {
                    rbScheduleTimingsOne.setText(routeTime);
                    rbScheduleTimingsOne.setId(id);
                } else if (i == 1) {
                    rbScheduleTimingsTwo.setText(routeTime);
                    rbScheduleTimingsTwo.setId(id);
                    layoutScheduleTiming.setVisibility(View.VISIBLE);
                } else if (i == 2) {
                    rbScheduleTimingsThree.setText(routeTime);
                    rbScheduleTimingsThree.setId(id);
                    rbScheduleTimingsThree.setVisibility(View.VISIBLE);
                }
            }
            rgScheduleTimings.check(timingID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public class UpdateStudentTimingStatus extends AsyncTask<String, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Updating Route Time");
        }

        @Override
        protected Void doInBackground(String... params) {
            Log.i("UpdateRouteStatus", "Called");
            try {
                JSONArray jsonArray = new JSONArray(AppGlobals.getStudentDriverRouteDetails());
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                String ID = jsonObject.getString("id");
                URL url = new URL("http://46.101.75.194:8080/users/" + ID);

                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());

                System.out.println("Update Status Response Code: " + responseCode);

                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                out.writeBytes(params[0]);
                out.flush();
                out.close();

                responseCode = connection.getResponseCode();

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (responseCode != 200) {
                Helpers.dismissProgressDialog();
                Toast.makeText(getActivity(), "Failed to update", Toast.LENGTH_LONG).show();
                getActivity().onBackPressed();
            } else {
                Toast.makeText(getActivity(), "Timing updated successfully", Toast.LENGTH_LONG).show();
                new FetchStudentTimingDetails().execute();
            }
        }
    }

    public class FetchStudentTimingDetails extends AsyncTask<String, Integer, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                JSONArray jsonArray = new JSONArray(AppGlobals.getStudentDriverRouteDetails());
                JSONObject jsonObject = jsonArray.getJSONObject(0);

                // timing ID for student
                JSONArray timingsArray = new JSONArray(jsonObject.getString("timings"));
                JSONObject timingsJsonObject = timingsArray.getJSONObject(0);
                String timingID = timingsJsonObject.getString("id");

                connection = WebServiceHelpers.openConnectionForUrl
                        ("http://46.101.75.194:8080/timings/" + timingID, "GET");
                connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                connection.connect();
                responseCode = connection.getResponseCode();

                String timingsData = WebServiceHelpers.readResponse(connection);
                JSONObject jsonObjectRouteStatus = new JSONObject(timingsData);
                Log.i("Timings Data", ""  + jsonObjectRouteStatus);

                AppGlobals.putArrivalTime(jsonObjectRouteStatus.getString("arrival_time"));
                AppGlobals.putDepartureTime(jsonObjectRouteStatus.getString("departure_time"));
                System.out.println("Arrival Time" + jsonObjectRouteStatus.getString("arrival_time"));
                System.out.println("Departure Time" + jsonObjectRouteStatus.getString("departure_time"));

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Helpers.dismissProgressDialog();
        }
    }

}
