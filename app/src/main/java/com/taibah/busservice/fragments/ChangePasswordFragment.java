package com.taibah.busservice.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.taibah.busservice.LoginActivity;
import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ChangePasswordFragment extends Fragment {

    private View mBaseView;
    EditText editTextPasswordOld;
    EditText editTextPasswordNew;
    EditText editTextPasswordRepeat;
    Button buttonDone;

    String passwordOld;
    String passwordNew;
    String passwordRepeat;

    TextView tvChangePasswordName;
    TextView tvChangePasswordUserName;

    static int responseCode;
    HttpURLConnection connection;

    boolean internetNotWorking = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBaseView = inflater.inflate(R.layout.layout_change_password, container, false);
        tvChangePasswordName = (TextView) mBaseView.findViewById(R.id.tv_changepassword_firstname_lastname);
        tvChangePasswordUserName = (TextView) mBaseView.findViewById(R.id.tv_changepassword_username);
        editTextPasswordOld = (EditText) mBaseView.findViewById(R.id.input_password_old);
        editTextPasswordNew = (EditText) mBaseView.findViewById(R.id.input_password_new);
        editTextPasswordRepeat = (EditText) mBaseView.findViewById(R.id.input_password_repeat);
        buttonDone = (Button) mBaseView.findViewById(R.id.btn_change_password);

        tvChangePasswordName.setText(AppGlobals.getName());
        tvChangePasswordUserName.setText(AppGlobals.getUsername());

        buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                change();
            }
        });

        return mBaseView;
    }

    public void change() {
        if (!validate()) {
            onChangeFailed();
            return;
        }
        buttonDone.setEnabled(false);
        new ChangePasswordTask().execute();
    }

    public void onChangeSuccess() {
        buttonDone.setEnabled(true);
        getActivity().finish();
    }

    public void onChangeFailed() {
        Toast.makeText(getActivity(), "Password change failed", Toast.LENGTH_LONG).show();
        buttonDone.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        passwordOld = editTextPasswordOld.getText().toString();
        passwordNew = editTextPasswordNew.getText().toString();
        passwordRepeat = editTextPasswordRepeat.getText().toString();

        if (passwordOld.trim().isEmpty() || passwordOld.length() < 6 ) {
            editTextPasswordOld.setError("at least 6 characters");
            valid = false;
        } else if (!passwordOld.equals(AppGlobals.getUserPassword())) {
            editTextPasswordOld.setError("current password is invalid");
            valid = false;
        } else {
                editTextPasswordOld.setError(null);
        }

        if (passwordNew.trim().isEmpty() || passwordNew.length() < 6 ) {
            editTextPasswordNew.setError("at least 6 characters");
            valid = false;
        } else {
            editTextPasswordNew.setError(null);
        }

        if (passwordRepeat.trim().isEmpty() || passwordRepeat.length() < 6) {
            editTextPasswordRepeat.setError("at least 6 characters");
            valid = false;
        } else {
            editTextPasswordRepeat.setError(null);
        }

        if (!passwordNew.equals(passwordRepeat)) {
            editTextPasswordRepeat.setError("password doesn't match");
            valid = false;
        } else {
            editTextPasswordRepeat.setError(null);
        }

        return valid;
    }

    public class ChangePasswordTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Changing password");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isInternetWorking() && Helpers.isNetworkAvailable()) {
                try {
                    JSONObject jsonObject = new JSONObject(AppGlobals.getStudentDriverRouteDetails());
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

                    System.out.println("Password Update Response Code: " + responseCode);

                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.writeBytes("password=" + passwordNew + "&" + "passconf=" + passwordNew);
                    out.flush();
                    out.close();

                    responseCode = connection.getResponseCode();

                } catch (IOException | JSONException e) {
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
            Helpers.dismissProgressDialog();
            if (internetNotWorking) {
                Toast.makeText(getActivity(), "Internet is not working. Make sure you are " +
                        "properly connected to the Internet", Toast.LENGTH_SHORT).show();
                internetNotWorking = false;
            }
            if (responseCode == 200) {
                Toast.makeText(getActivity(), "Password successfully changed", Toast.LENGTH_LONG).show();
                AppGlobals.setFirstRun(true);
                AppGlobals.putToken(null);
                AppGlobals.putGcmToken(null);
                launchLoginActivity();
                onChangeSuccess();
            } else {
                Toast.makeText(getActivity(), "Password change failed. Please try again", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void launchLoginActivity() {
        Intent startIntent = new Intent(getActivity(), LoginActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getActivity().startActivity(startIntent);
    }

}
