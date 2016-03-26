package com.taibah.busservice.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.taibah.busservice.Helpers.WebServiceHelpers;
import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class RegisterDriver extends Fragment implements AdapterView.OnItemSelectedListener {

    public static int responseCode;

    View convertView;
    EditText etDriverFirstName;
    EditText etDriverLastName;
    EditText etDriverContactNumber;
    String firstNameDriver;
    String lastNameDriver;
    String contactNumberDriver;
    HttpURLConnection connection;
    String registrationDetail;
    Spinner spinnerUnAssignedRoutesList;

    ArrayList<Integer> unAssignedRouteIdsList;
    HashMap<Integer, String> hashMapUnAssignedRouteData;
    int routeId = 0;
    String spinnerText;
    Menu mMenu;
    MenuInflater mMenuInflater;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_register_driver, null);
        setHasOptionsMenu(true);

        etDriverFirstName = (EditText) convertView.findViewById(R.id.et_driver_first_name);
        etDriverLastName = (EditText) convertView.findViewById(R.id.et_driver_last_name);
        etDriverContactNumber = (EditText) convertView.findViewById(R.id.et_driver_contact);

        spinnerUnAssignedRoutesList = (Spinner) convertView.findViewById(R.id.spinner_register_driver_select_route);

        unAssignedRouteIdsList = new ArrayList<>();
        hashMapUnAssignedRouteData = new HashMap<>();
        spinnerUnAssignedRoutesList.setOnItemSelectedListener(this);

        new RetrieveUnassignedRoutesTask().execute();


        return convertView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenu = menu;
        mMenuInflater = inflater;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done_button:

                firstNameDriver = etDriverFirstName.getText().toString().trim();
                lastNameDriver = etDriverLastName.getText().toString().trim();
                contactNumberDriver = etDriverContactNumber.getText().toString().trim();

                if (!validateInfo()) {
                    Toast.makeText(getActivity(), "Incomplete info", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (spinnerText == null) {
                    Toast.makeText(getActivity(), "Incomplete info. Route not found", Toast.LENGTH_SHORT).show();
                    return true;
                }
                new checkInternetTask().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void register() {

        String username = "dvr" + firstNameDriver + contactNumberDriver.substring(contactNumberDriver.length() - 3);
        String password = lastNameDriver + contactNumberDriver.substring(contactNumberDriver.length() - 3);

        registrationDetail = "route_id=" + routeId + "&"
                + "first_name=" + firstNameDriver + "&" + "last_name=" + lastNameDriver
                + "&" + "password=" + password + "&" + "passconf=" + password + "&" + "type=driver"
                + "&" + "username=" + username
                + "&" + "phone=" + contactNumberDriver;

        new RegisterDriverTask().execute();
    }

    public boolean validateInfo() {
        boolean valid = true;

        if (firstNameDriver.isEmpty() || firstNameDriver.length() < 3) {
            etDriverFirstName.setError("at least 3 characters");
            valid = false;
        } else {
            etDriverFirstName.setError(null);
        }

        if (lastNameDriver.isEmpty() || lastNameDriver.length() < 3) {
            etDriverLastName.setError("at least 3 characters");
            valid = false;
        } else {
            etDriverLastName.setError(null);
        }

        if (contactNumberDriver.isEmpty()) {
            etDriverContactNumber.setError("Contact is required");
            valid = false;
        } else {
            etDriverContactNumber.setError(null);
        }

        if (valid && !PhoneNumberUtils.isGlobalPhoneNumber(contactNumberDriver) || contactNumberDriver.length() < 3) {
            etDriverContactNumber.setError("Contact is invalid");
            valid = false;
        }

        return valid;
    }

    public void onRegistrationSuccess() {
        Toast.makeText(getActivity(), "Registration successful", Toast.LENGTH_SHORT).show();
        Helpers.closeKeyboard(getActivity());
        getActivity().onBackPressed();
    }

    public void onRegistrationFailed() {
        Toast.makeText(getActivity(), "Registration failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        routeId = unAssignedRouteIdsList.get(position);
        spinnerText = hashMapUnAssignedRouteData.get(unAssignedRouteIdsList.get(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    class checkInternetTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            return Helpers.isInternetWorking();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Checking internet availability");
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            Helpers.dismissProgressDialog();
            if (success) {
                String message = "Driver Name: " + firstNameDriver + " " + lastNameDriver
                        + "\n" + "Driver Contact: " + contactNumberDriver + "\n\n" + "Assigned Route: "
                        + spinnerText;
                showRegInfoDialog(message);
            } else {
                showInternetNotWorkingDialog();
            }
        }
    }

    public void showInternetNotWorkingDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setMessage("Internet not available");
        alertDialogBuilder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                new checkInternetTask().execute();
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void showRegInfoDialog(String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Are you sure?");
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                register();
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialogBuilder.create();
        alertDialogBuilder.show();
    }

    private class RegisterDriverTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            Helpers.showProgressDialog(getActivity(), "Collecting information");
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                URL url = new URL("http://46.101.75.194:8080/register");

                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                out.writeBytes(registrationDetail);
                out.flush();
                out.close();
                responseCode = connection.getResponseCode();
                InputStream in = (InputStream) connection.getContent();
                int ch;
                StringBuilder sb;

                sb = new StringBuilder();
                while ((ch = in.read()) != -1)
                    sb.append((char) ch);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (responseCode == 201) {
                Helpers.dismissProgressDialog();
                onRegistrationSuccess();
            } else {
                // TODO Implement correct logic here
                onRegistrationFailed();
                Helpers.dismissProgressDialog();
            }
        }
    }

    private class RetrieveUnassignedRoutesTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Retrieving unassigned routes list");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isNetworkAvailable() && Helpers.isInternetWorking()) {
                try {
                    connection = WebServiceHelpers.openConnectionForUrl
                            ("http://46.101.75.194:8080/routes?unassigned=true", "GET");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connection.connect();
                    responseCode = connection.getResponseCode();
                    System.out.print(responseCode);
                    String data = WebServiceHelpers.readResponse(connection);
                    JSONArray jsonArray = new JSONArray(data);
                    System.out.println(jsonArray);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        if (!unAssignedRouteIdsList.contains(jsonObject.getInt("id"))) {
                            unAssignedRouteIdsList.add(jsonObject.getInt("id"));
                            hashMapUnAssignedRouteData.put(jsonObject.getInt("id"), jsonObject.getString("name"));
                            System.out.println(hashMapUnAssignedRouteData);
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
            if (responseCode == 200) {
                Helpers.dismissProgressDialog();
                mMenuInflater.inflate(R.menu.menu_done, mMenu);
                CustomSpinnerListAdapter customSpinnerListAdapter = new CustomSpinnerListAdapter(getActivity(), R.layout.spinner_row, unAssignedRouteIdsList);
                spinnerUnAssignedRoutesList.setAdapter(customSpinnerListAdapter);
                spinnerUnAssignedRoutesList.setSelection(0);
            } else {
                Toast.makeText(getActivity(), "Something went wrong. Please try again", Toast.LENGTH_LONG).show();
                Helpers.dismissProgressDialog();
                getActivity().onBackPressed();
            }
        }
    }

    class CustomSpinnerListAdapter extends ArrayAdapter<String> {

        ArrayList<Integer> arrayListIntIds;

        public CustomSpinnerListAdapter(Context context, int resource, ArrayList<Integer> arrayList) {
            super(context, resource);
            arrayListIntIds = arrayList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                convertView = layoutInflater.inflate(R.layout.spinner_row, parent, false);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.tvSpinner = (TextView) convertView.findViewById(R.id.tv_spinner_row);
            viewHolder.tvSpinner.setText(hashMapUnAssignedRouteData.get(arrayListIntIds.get(position)));
            return convertView;
        }

        @Override
        public int getCount() {
            return arrayListIntIds.size();
        }
    }


    static class ViewHolder {
        TextView tvSpinner;
    }
}
