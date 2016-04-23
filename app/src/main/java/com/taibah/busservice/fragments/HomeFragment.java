package com.taibah.busservice.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.taibah.busservice.Helpers.WebServiceHelpers;
import com.taibah.busservice.MainActivity;
import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.DriverService;
import com.taibah.busservice.utils.Helpers;
import com.taibah.busservice.utils.UpdateRouteStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class HomeFragment extends Fragment implements View.OnClickListener {

    View convertView;
    Button buttonReportSituation;
    Button buttonStartStopRoute;
    RadioGroup radioGroupReportSituation;
    RadioGroup rgSituationReportingSelectTime;
    RadioButton rbSelectTimeOne;
    RadioButton rbSelectTimeTwo;
    RadioButton rbSelectTimeThree;
    RadioGroup rgStartRouteSelectTime;
    RadioButton rbStartRouteSelectTimeOne;
    RadioButton rbStartRouteSelectTimeTwo;
    RadioButton rbStartRouteSelectTimeThree;
    static RelativeLayout layoutDriverButtons;
    static RelativeLayout layoutRouteCancelled;
    static RelativeLayout layoutRouteInfo;
    LinearLayout layoutListCancelledRoutes;
    LinearLayout layoutAdminInfo;
    TextView tvUserType;
    static TextView tvRouteStatus;
    TextView tvRouteClickToRestore;
    static TextView tvRouteTimings;
    TextView tvStatusRetrievingCancelledRoutes;

    static int responseCode;
    int radioIndex;
    HttpURLConnection connection;

    String cancelledRouteName;

    ArrayList<Integer> routeIdsList;
    HashMap<Integer, ArrayList<String>> hashMapRouteData;

    ListView listViewCancelledRoutes;
    public static int checkedTimeIDForSituation;

    int timeIDforStartStopRoute;
    public static int responseCodeRoutes;
    HttpURLConnection connectionRoutes;
    public RetrieveAllCancelledRoutes mTask;
    TextView tvRouteName;
    TextView tvStudentServiceStatus;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_home, null);

        tvUserType = (TextView) convertView.findViewById(R.id.tv_user_type);
        tvRouteStatus = (TextView) convertView.findViewById(R.id.tv_route_status);
        tvRouteTimings = (TextView) convertView.findViewById(R.id.tv_home_assined_route_timing);
        tvRouteClickToRestore = (TextView) convertView.findViewById(R.id.tv_route_click_to_restore);
        tvRouteName = (TextView) convertView.findViewById(R.id.tv_route_name_home_fragment);
        tvStudentServiceStatus = (TextView) convertView.findViewById(R.id.tv_student_service_status_home_fragment);
        tvStatusRetrievingCancelledRoutes = (TextView) convertView.findViewById(R.id.tv_status_retrieving_cancelled_routes);
        layoutDriverButtons = (RelativeLayout) convertView.findViewById(R.id.layout_driver_buttons);
        layoutAdminInfo = (LinearLayout) convertView.findViewById(R.id.layout_admin_info);
        layoutListCancelledRoutes = (LinearLayout) convertView.findViewById(R.id.layout_list_cancelled_routes);
        layoutRouteCancelled = (RelativeLayout) convertView.findViewById(R.id.layout_driver_route_cancelled);
        layoutRouteCancelled.setOnClickListener(this);

        if (AppGlobals.getUserType() > 0) {
            try {
                JSONArray jsonArray = new JSONArray(AppGlobals.getStudentDriverRouteDetails());
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                System.out.println(jsonObject.getString("name"));
                Log.i("User Details ", "" + jsonObject);
                JSONArray timingsArray = new JSONArray(jsonObject.getString("timings"));
                tvRouteName.setText("Assigned Route: " + jsonObject.getString("name"));

                StringBuilder sbTimings = new StringBuilder();
                for (int i = 0; i < timingsArray.length(); i++) {
                    JSONObject timingsJsonObject = timingsArray.getJSONObject(i);
                    String arrivalTime = timingsJsonObject.get("arrival_time").toString().substring(11, 16);
                    String departureTime = timingsJsonObject.get("departure_time").toString().substring(11, 16);
                    sbTimings.append("(" + Helpers.convertTimeForUser(arrivalTime) + " - " + Helpers.convertTimeForUser(departureTime) + ")\n");
                }

                if (AppGlobals.getRouteStatus() < 2) {
                    tvRouteTimings.setText(sbTimings);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (AppGlobals.getUserType() == 1) {
            if (AppGlobals.getStudentServiceAllowed() == 0) {
                tvStudentServiceStatus.setVisibility(View.VISIBLE);
            }
        }

        routeIdsList = new ArrayList<>();
        hashMapRouteData = new HashMap<>();
        listViewCancelledRoutes = (ListView) convertView.findViewById(R.id.lv_cancelled_routes);

        layoutRouteInfo = (RelativeLayout) convertView.findViewById(R.id.layout_route_info_timing);
        registerForContextMenu(listViewCancelledRoutes);


        buttonStartStopRoute = (Button) convertView.findViewById(R.id.btn_route_switch);
        buttonStartStopRoute.setOnClickListener(this);

        buttonReportSituation = (Button) convertView.findViewById(R.id.btn_report_situation);
        buttonReportSituation.setOnClickListener(this);

        setAppView();
        setRouteStatus(AppGlobals.getRouteStatus());
        return convertView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        cancelledRouteName = hashMapRouteData.get(routeIdsList.get(info.position)).get(0);
        menu.setHeaderTitle(cancelledRouteName);
        MenuInflater inflater = this.getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_cancelled_routes_list, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.item_context_menu_cancelled_routes_list_call_driver:
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + hashMapRouteData.get(routeIdsList.get(info.position)).get(3)));
                startActivity(intent);
                return true;
        }
        return true;
    }

    public static void setRouteStatus(int status) {
        if (status == 0 || status == 1) {
            if (AppGlobals.getUserType() == 2) {
                layoutDriverButtons.setVisibility(View.VISIBLE);
            }
            layoutRouteCancelled.setVisibility(View.GONE);
            if (AppGlobals.getUserType() != 0) {
                layoutRouteInfo.setVisibility(View.VISIBLE);
            }
        } else {
            if (AppGlobals.getUserType() != 0 && AppGlobals.getUserType() != 2) {
                layoutRouteCancelled.setVisibility(View.VISIBLE);
                layoutRouteInfo.setVisibility(View.GONE);
                if (status == 2) {
                    tvRouteStatus.setText("Accident");
                } else if (status == 3) {
                    tvRouteStatus.setText("Driver unavailable");
                } else if (status == 4) {
                    tvRouteStatus.setText("Bus out of service");
                }
            }
        }
    }

    public void setAppView() {
        if (AppGlobals.getUserType() == 2) {
            layoutDriverButtons.setVisibility(View.VISIBLE);
            tvUserType.setText("UserType: Driver");
            tvRouteClickToRestore.setVisibility(View.VISIBLE);
            layoutAdminInfo.setVisibility(View.GONE);
            if (DriverService.driverLocationReportingServiceIsRunning) {
                buttonStartStopRoute.setText("End Route");
            } else {
                buttonStartStopRoute.setText("Start Route");
            }

            if (AppGlobals.getRouteStatus() == 1 && !DriverService.driverLocationReportingServiceIsRunning) {
                getActivity().startService(new Intent(getActivity(), DriverService.class));
                buttonStartStopRoute.setText("End Route");
                Toast.makeText(getActivity(), "Route was active. Location reporting started", Toast.LENGTH_LONG).show();
            }
        } else if (AppGlobals.getUserType() == 1) {
            layoutDriverButtons.setVisibility(View.GONE);
            tvUserType.setText("UserType: Student");
            layoutAdminInfo.setVisibility(View.GONE);
            layoutRouteCancelled.setClickable(false);
        } else if (AppGlobals.getUserType() == 0) {
            mTask = (RetrieveAllCancelledRoutes) new RetrieveAllCancelledRoutes().execute();
            layoutDriverButtons.setVisibility(View.GONE);
            layoutAdminInfo.setVisibility(View.VISIBLE);
            tvUserType.setText("UserType: Admin");
            layoutRouteCancelled.setClickable(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_route_switch:
                if (Helpers.isNetworkAvailable()) {
                    if (Helpers.isHighAccuracyLocationServiceAvailable()) {
                        if (!DriverService.driverLocationReportingServiceIsRunning) {

                            final Dialog startRouteSelectTimeDialog = new Dialog(getActivity());
                            startRouteSelectTimeDialog.setContentView(R.layout.layout_start_route_dialog);
                            startRouteSelectTimeDialog.setTitle("Select a Time");
                            startRouteSelectTimeDialog.setCancelable(false);

                            final Button okButtonStartRoute = (Button) startRouteSelectTimeDialog.findViewById(R.id.btn_start_route_dialog_ok);
                            rgStartRouteSelectTime = (RadioGroup) startRouteSelectTimeDialog.findViewById(R.id.rg_start_route_select_time);
                            rbStartRouteSelectTimeOne = (RadioButton) startRouteSelectTimeDialog.findViewById(R.id.rb_start_route_select_time_one);
                            rbStartRouteSelectTimeTwo = (RadioButton) startRouteSelectTimeDialog.findViewById(R.id.rb_start_route_select_time_two);
                            rbStartRouteSelectTimeThree = (RadioButton) startRouteSelectTimeDialog.findViewById(R.id.rb_start_route_select_time_three);

                            JSONArray jsonArray = null;
                            try {
                                jsonArray = new JSONArray(AppGlobals.getStudentDriverRouteDetails());
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
                                JSONArray timingsArrayForID = new JSONArray(jsonObject.getString("timings"));
                                for (int i = 0; i < timingsArrayForID.length(); i++) {
                                    JSONObject timingsJsonObject = timingsArrayForID.getJSONObject(i);
                                    String arrivalTime = timingsJsonObject.get("arrival_time").toString().substring(11, 16);
                                    String departureTime = timingsJsonObject.get("departure_time").toString().substring(11, 16);
                                    int id = timingsJsonObject.getInt("id");
                                    String routeTime = "(" + Helpers.convertTimeForUser(arrivalTime) + " - " + Helpers.convertTimeForUser(departureTime) + ")";
                                    if (i == 0) {
                                        rbStartRouteSelectTimeOne.setText(routeTime);
                                        rbStartRouteSelectTimeOne.setId(id);
                                    } else if (i == 1) {
                                        rbStartRouteSelectTimeTwo.setText(routeTime);
                                        rbStartRouteSelectTimeTwo.setId(id);
                                        rbStartRouteSelectTimeTwo.setVisibility(View.VISIBLE);
                                    } else if (i == 2) {
                                        rbStartRouteSelectTimeThree.setText(routeTime);
                                        rbStartRouteSelectTimeThree.setId(id);
                                        rbStartRouteSelectTimeThree.setVisibility(View.VISIBLE);
                                    }
                                }

                                okButtonStartRoute.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Helpers.dismissProgressDialog();
                                        DriverService.driverLocationReportingServiceIsRunning = true;
                                        new UpdateRouteStatus(getActivity()).execute("status=1");
                                        getActivity().startService(new Intent(getActivity(), DriverService.class));
                                        buttonStartStopRoute.setText("End Route");
                                        AppGlobals.replaceFragment(getFragmentManager(), new MapsFragment());
                                        MainActivity.navigationView.getMenu().getItem(1).setChecked(true);
                                        startRouteSelectTimeDialog.dismiss();
                                    }
                                });

                                rgStartRouteSelectTime.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                                        okButtonStartRoute.setEnabled(true);
                                        timeIDforStartStopRoute = rgStartRouteSelectTime.getCheckedRadioButtonId();
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            startRouteSelectTimeDialog.show();
                        } else {
                            AlertDialog.Builder alertDialogRouteSwitch = new AlertDialog.Builder(
                                    getActivity());
                            alertDialogRouteSwitch.setTitle("End Route");
                            alertDialogRouteSwitch
                                    .setMessage("Are you sure?")
                                    .setCancelable(false)
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Helpers.showProgressDialog(getActivity(), "Ending Route");
                                            new android.os.Handler().postDelayed(
                                                    new Runnable() {
                                                        public void run() {
                                                            buttonStartStopRoute.setText("Start Route");
                                                            Helpers.dismissProgressDialog();
                                                            getActivity().stopService(new Intent(getActivity(), DriverService.class));
                                                        }
                                                    }, 2000);
                                        }
                                    })
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            AlertDialog routeSwitchDialog = alertDialogRouteSwitch.create();
                            routeSwitchDialog.show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Error: Location Service not on HighAccuracy", Toast.LENGTH_SHORT).show();
                    }
                    } else {
                    Toast.makeText(getActivity(), "Error: Not connected to the network", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_report_situation:
                if (Helpers.isNetworkAvailable()) {
                    if (!DriverService.driverLocationReportingServiceIsRunning) {
                                final Dialog reportSituationDialog = new Dialog(getActivity());
                                reportSituationDialog.setContentView(R.layout.layout_report_dialog);
                        reportSituationDialog.setTitle("Choose a situation");
                                reportSituationDialog.setCancelable(false);
                        final LinearLayout layoutSelectTime = (LinearLayout)  reportSituationDialog.findViewById(R.id.layout_select_route_time);
                        final LinearLayout layoutSituationToReport = (LinearLayout)  reportSituationDialog.findViewById(R.id.layout_select_situation);
                        rbSelectTimeOne = (RadioButton) reportSituationDialog.findViewById(R.id.rb_situation_reporting_select_time_one);
                        rbSelectTimeTwo = (RadioButton) reportSituationDialog.findViewById(R.id.rb_situation_reporting_select_time_two);
                        rbSelectTimeThree = (RadioButton) reportSituationDialog.findViewById(R.id.rb_situation_reporting_select_time_three);
                        final TextView tvShowSelectedTimeForSituationReporting = (TextView) reportSituationDialog.findViewById(R.id.tv_report_dialog_timing);

                        rgSituationReportingSelectTime = (RadioGroup) reportSituationDialog.findViewById(R.id.rg_situation_reporting_select_time);
                        rgSituationReportingSelectTime.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(RadioGroup group, int checkedId) {
                                checkedTimeIDForSituation = rgSituationReportingSelectTime.getCheckedRadioButtonId();
                                String timeText = "";
                                if (rbSelectTimeOne.getId() == checkedTimeIDForSituation) {
                                    timeText = rbSelectTimeOne.getText().toString();
                                } else if (rbSelectTimeTwo.getId() == checkedTimeIDForSituation) {
                                    timeText = rbSelectTimeTwo.getText().toString();
                                } else if (rbSelectTimeThree.getId() == checkedTimeIDForSituation) {
                                    timeText = rbSelectTimeThree.getText().toString();
                                }

                                tvShowSelectedTimeForSituationReporting.setText(timeText);
                                layoutSelectTime.setVisibility(View.GONE);
                                layoutSituationToReport.setVisibility(View.VISIBLE);
                                reportSituationDialog.setTitle("Choose a situation");

                            }
                        });

                        JSONArray jsonArray = null;
                        try {
                            jsonArray = new JSONArray(AppGlobals.getStudentDriverRouteDetails());
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            JSONArray timingsArrayForID = new JSONArray(jsonObject.getString("timings"));
                            for (int i = 0; i < timingsArrayForID.length(); i++) {
                                JSONObject timingsJsonObject = timingsArrayForID.getJSONObject(i);
                                String arrivalTime = timingsJsonObject.get("arrival_time").toString().substring(11, 16);
                                String departureTime = timingsJsonObject.get("departure_time").toString().substring(11, 16);
                                int id = timingsJsonObject.getInt("id");
                                String routeTime = "(" + Helpers.convertTimeForUser(arrivalTime) + " - " + Helpers.convertTimeForUser(departureTime) + ")";
                                if (i == 0) {
                                    rbSelectTimeOne.setText(routeTime);
                                    rbSelectTimeOne.setId(id);
                                } else if (i == 1) {
                                    rbSelectTimeTwo.setText(routeTime);
                                    rbSelectTimeTwo.setId(id);
                                    rbSelectTimeTwo.setVisibility(View.VISIBLE);
                                    layoutSelectTime.setVisibility(View.VISIBLE);
                                    layoutSituationToReport.setVisibility(View.GONE);
                                    reportSituationDialog.setTitle("Select a time");
                                } else if (i == 2) {
                                    rbSelectTimeThree.setText(routeTime);
                                    rbSelectTimeThree.setId(id);
                                    rbSelectTimeThree.setVisibility(View.VISIBLE);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Button dialogCancelOne = (Button) reportSituationDialog.findViewById(R.id.btn_report_dialog_cancel_one);
                        dialogCancelOne.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                reportSituationDialog.dismiss();
                            }
                        });

                                radioGroupReportSituation = (RadioGroup) reportSituationDialog.findViewById(R.id.rg_report_situation);

                                Button dialogButtonCancel = (Button) reportSituationDialog.findViewById(R.id.btn_report_dialog_cancel);
                                dialogButtonCancel.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        reportSituationDialog.dismiss();
                                    }
                                });

                                Button dialogButtonOk = (Button) reportSituationDialog.findViewById(R.id.btn_report_dialog_ok);
                                dialogButtonOk.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (Helpers.isNetworkAvailable()) {
                                            reportSituationDialog.dismiss();
                                            int id = radioGroupReportSituation.getCheckedRadioButtonId();
                                            View radioButton = radioGroupReportSituation.findViewById(id);
                                            radioIndex = radioGroupReportSituation.indexOfChild(radioButton) + 1;
                                            int routeStatusToPut;

                                            if (radioIndex < 2) {
                                                routeStatusToPut = 0;
                                            } else {
                                                routeStatusToPut = radioIndex;
                                            }

//                                            checkedTimeIDForSituation

                                            new SituationReportTask().execute("status=" + routeStatusToPut);

                                        } else {
                                            Toast.makeText(getActivity(), "Not connected to the network", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                                reportSituationDialog.show();
                    } else {
                        Toast.makeText(getActivity(), "Error: Driver Service is running", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), "Error: Not connected to the network", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.isHomeFragmentOpen = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity.isHomeFragmentOpen = false;
        if (AppGlobals.getUserType() == 0) {
            mTask.cancel(true);
        }
    }

    public class SituationReportTask extends AsyncTask<String, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Reporting Situation");
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
//                JSONObject jsonObject = new JSONObject(AppGlobals.getStudentDriverRouteDetails());
//                String ID = jsonObject.getString("id");
                URL url = new URL("http://46.101.75.194:8080/timings/" + checkedTimeIDForSituation);

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

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (responseCode == 200) {
                AppGlobals.putRouteStatus(radioIndex);
                setRouteStatus(radioIndex);
                Helpers.dismissProgressDialog();
                Toast.makeText(getActivity(), "Situation Reported Successfully", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Situation reporting failed. Please try again", Toast.LENGTH_LONG).show();
            }
        }
    }

    public class RetrieveAllCancelledRoutes extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tvStatusRetrievingCancelledRoutes.setText("Retrieving cancelled routes list . . .");
            tvStatusRetrievingCancelledRoutes.setTextColor(Color.GRAY);
            tvStatusRetrievingCancelledRoutes.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isNetworkAvailable() && Helpers.isInternetWorking()) {
                try {
                    connectionRoutes = WebServiceHelpers.openConnectionForUrl
                            ("http://46.101.75.194:8080/routes", "GET");
                    connectionRoutes.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connectionRoutes.connect();
                    responseCodeRoutes = connectionRoutes.getResponseCode();
                    String data = WebServiceHelpers.readResponse(connectionRoutes);
                    JSONArray jsonArray = new JSONArray(data);
                    responseCodeRoutes = connectionRoutes.getResponseCode();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        if (!routeIdsList.contains(jsonObject.getInt("id"))
                                && !jsonObject.getString("status").equalsIgnoreCase("0")
                                && !jsonObject.getString("status").equalsIgnoreCase("1")
                                && !jsonObject.getString("driver").equals("null") && !jsonObject.getString("status").equals("null")) {
                            routeIdsList.add(jsonObject.getInt("id"));
                            ArrayList<String> arrayListString = new ArrayList<>();
                            arrayListString.add(jsonObject.getString("name"));
                            arrayListString.add(jsonObject.getString("bus_number"));
                            arrayListString.add(jsonObject.getString("status"));
                            String driverObject = jsonObject.getString("driver");
                            JSONObject jsonObjectDriver = new JSONObject(driverObject);
                            arrayListString.add(jsonObjectDriver.getString("phone"));
                            hashMapRouteData.put(jsonObject.getInt("id"), arrayListString);
                        }
                    }

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
            if (responseCodeRoutes == 200) {
                if (!routeIdsList.isEmpty()) {
                    tvStatusRetrievingCancelledRoutes.setVisibility(View.GONE);
                    layoutListCancelledRoutes.setVisibility(View.VISIBLE);
                    CustomRoutesListAdapter customRoutesListAdapter = new CustomRoutesListAdapter(getContext(), R.layout.routes_cancelled_list_row, routeIdsList);
                    listViewCancelledRoutes.setAdapter(customRoutesListAdapter);
                } else {
                    tvStatusRetrievingCancelledRoutes.setText("No cancelled route found.");
                    tvStatusRetrievingCancelledRoutes.setTextColor(Color.BLACK);
                }
            } else {
                tvStatusRetrievingCancelledRoutes.setText("Error retrieving cancelled routes status");
                tvStatusRetrievingCancelledRoutes.setTextColor(Color.RED);
            }
        }
    }


    class CustomRoutesListAdapter extends ArrayAdapter<String> {
        ArrayList<Integer> arrayListIntIds;
        public CustomRoutesListAdapter(Context context, int resource, ArrayList<Integer> arrayList) {
            super(context, resource);
            arrayListIntIds = arrayList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolderCancelledRoutes viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolderCancelledRoutes();
                LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                convertView = layoutInflater.inflate(R.layout.routes_cancelled_list_row, parent, false);
                viewHolder.tvRoutesCancelledName = (TextView) convertView.findViewById(R.id.tv_routes_cancelled_name);
                viewHolder.tvRoutesCancelledReason = (TextView) convertView.findViewById(R.id.tv_routes_cancelled_reason);
                viewHolder.tvRoutesCancelledBusNumber = (TextView) convertView.findViewById(R.id.tv_routes_cancelled_bus_number);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolderCancelledRoutes) convertView.getTag();
            }
            viewHolder.tvRoutesCancelledName.setText("Name: " + hashMapRouteData.get(arrayListIntIds.get(position)).get(0));
            viewHolder.tvRoutesCancelledBusNumber.setText("Bus Number: " + hashMapRouteData.get(arrayListIntIds.get(position)).get(1));
            int status = Integer.parseInt(hashMapRouteData.get(arrayListIntIds.get(position)).get(2));
            viewHolder.tvRoutesCancelledReason.setText("Reason: " + Helpers.parseRouteCancelledReason(status));
            return convertView;
        }

        @Override
        public int getCount() {
            return arrayListIntIds.size();
        }
    }

    static class ViewHolderCancelledRoutes {
        TextView tvRoutesCancelledName;
        TextView tvRoutesCancelledReason;
        TextView tvRoutesCancelledBusNumber;
    }
}