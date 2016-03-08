package com.taibah.busservice.fragments;

import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegisterDriver extends Fragment {

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_register_driver, null);
        setHasOptionsMenu(true);

        etDriverFirstName = (EditText) convertView.findViewById(R.id.et_driver_first_name);
        etDriverLastName = (EditText) convertView.findViewById(R.id.et_driver_last_name);
        etDriverContactNumber = (EditText) convertView.findViewById(R.id.et_driver_contact);

        return convertView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_done, menu);
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
                new checkInternetTask().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void register() {

        String username = "dvr" + firstNameDriver + contactNumberDriver.substring(contactNumberDriver.length() - 3);
        String password = lastNameDriver + contactNumberDriver.substring(contactNumberDriver.length() - 3 );

        registrationDetail = "first_name=" + firstNameDriver + "&" + "last_name=" + lastNameDriver
                + "&" + "password=" + password + "&" + "passconf=" + password + "&" + "type=driver"
                + "&" + "username=" + username;
        Log.i("Registration Detail: ",  registrationDetail);

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

        if (valid && !PhoneNumberUtils.isGlobalPhoneNumber(contactNumberDriver)) {
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
                        + "\n" + "Driver Contact: " + contactNumberDriver;
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
            Helpers.showProgressDialog(getActivity(), "Collecting Information...");
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

                Log.i("Token", AppGlobals.getToken());

                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                out.writeBytes(registrationDetail);
                out.flush();
                out.close();
                responseCode = connection.getResponseCode();
                Log.i("Response", "" + responseCode);

                InputStream in = (InputStream) connection.getContent();
                int ch;
                StringBuilder sb;

                sb = new StringBuilder();
                while ((ch = in.read()) != -1)
                    sb.append((char) ch);

                Log.d("RESULT", sb.toString());

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("BEFORE", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (responseCode == 201) {
                Helpers.dismissProgressDialog();
                Toast.makeText(getActivity(), "Registration Successful", Toast.LENGTH_LONG).show();
                Helpers.closeKeyboard(getActivity());
                getActivity().onBackPressed();
            } else {
                // TODO Implement correct logic here
                Toast.makeText(getActivity(), "Invalid Response: " + responseCode, Toast.LENGTH_LONG).show();
            }
        }
    }
}
