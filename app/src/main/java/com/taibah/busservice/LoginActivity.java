package com.taibah.busservice;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends Activity {

    EditText editTextUsername;
    EditText editTextPassword;
    Button buttonLogin;
    String username;
    String password;

    JSONObject response;
    public String token = "";
    static int responseCode;
    HttpURLConnection connection;

    boolean internetNotWorking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextUsername = (EditText) findViewById(R.id.et_username_login);
        editTextPassword = (EditText) findViewById(R.id.et_password_login);
        buttonLogin = (Button) findViewById(R.id.btn_login);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = editTextUsername.getText().toString().trim();
                password = editTextPassword.getText().toString().trim();
                if (username.equals("adntaibah") && password.equals("12345")) {
                    login();
                } else {
                    new LoginDriverAndStudentTask().execute();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void login() {

        if (!validate()) {
            onLoginFailed();
            return;
        }

        // TODO: Implement authentication here.
        if (Helpers.isNetworkAvailable()) {
            new LoginAdminTask().execute();
        } else {
            Toast.makeText(LoginActivity.this, "Please connect your device to the Internet",
                    Toast.LENGTH_SHORT).show();
        }

        // TODO: fetch Route Status here.

    }

    public boolean validate() {
        boolean valid = true;

        if (username.trim().isEmpty() || username.length() < 6) {
            editTextUsername.setError("at least 6 characters");
            valid = false;
        } else if (!username.startsWith("dvr") && !username.startsWith("sdt") && !username.startsWith("adn")) {
            editTextUsername.setError("invalid username");
            valid = false;
        } else {
            editTextUsername.setError(null);
        }

        if (password.trim().isEmpty() || password.length() < 4) {
            editTextPassword.setError("at least 4 characters");
            valid = false;
        } else {
            editTextPassword.setError(null);
        }
        return valid;
    }

    public void onLoginSuccess() {
        AppGlobals.setFirstRun(false);
        AppGlobals.putUsername(username);
        buttonLogin.setEnabled(true);
        setUserType();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();
        buttonLogin.setEnabled(true);
    }

    public void setUserType() {
        if (TextUtils.equals(username.substring(0,3), "dvr")) {
            AppGlobals.putUserType(2);
        } else if (TextUtils.equals(username.substring(0,3), "sdt")){
            AppGlobals.putUserType(1);
        } else if (TextUtils.equals(username.substring(0,3), "adn")){
            AppGlobals.putUserType(0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!AppGlobals.isFirstRun()) {
            finish();
        }
    }

    public class LoginAdminTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(LoginActivity.this, "Authenticating...");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isInternetWorking()) {
                try {
                    URL url = new URL("http://46.101.75.194:8080/login");

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("charset", "utf-8");

                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.writeBytes("username=superadmin&password=secret");
                    out.flush();
                    out.close();

                    InputStream in = (InputStream) connection.getContent();

                    int ch;
                    StringBuilder sb = new StringBuilder();
                    while((ch = in.read()) != -1)
                        sb.append((char)ch);

                    Log.d("RESULT", sb.toString());

                    response = new JSONObject(sb.toString());
                    token = response.getString("token");
                    AppGlobals.putToken(token);
                    Log.d("TOKEN", token);

                    connection.disconnect();

                    url = new URL("http://46.101.75.194:8080/user");

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(false);
                    connection.setDoInput(true);
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("X-Api-Key", token);

                    in = (InputStream) connection.getContent();

                    sb = new StringBuilder();
                    while((ch = in.read()) != -1)
                        sb.append((char)ch);

                    Log.d("RESULT", sb.toString());

                } catch (JSONException | IOException e) {
                    Log.e("BEFORE", e.getMessage());
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
                Toast.makeText(LoginActivity.this, "Internet is not working. Make sure you are " +
                        "properly connected to the Internet", Toast.LENGTH_SHORT).show();
                Helpers.dismissProgressDialog();
                internetNotWorking = false;
            }
            if (!token.isEmpty()) {
                Helpers.dismissProgressDialog();
                onLoginSuccess();
            }
        }
    }

    public class LoginDriverAndStudentTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(LoginActivity.this, "Authenticating...");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isInternetWorking()) {
                try {
                    URL url = new URL("http://46.101.75.194:8080/login");

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("charset", "utf-8");

                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    String loginDetails = "username=" + username + "&" + "password=" + password
                            + "&" + "token=" + AppGlobals.getGcmToken();
                    out.writeBytes(loginDetails);
                    Log.i("Login Details ", loginDetails);
                    out.flush();
                    out.close();

                    responseCode = connection.getResponseCode();

                    InputStream in = (InputStream) connection.getContent();

                    int ch;
                    StringBuilder sb = new StringBuilder();
                    while((ch = in.read()) != -1)
                        sb.append((char)ch);

                    Log.d("LoginRESULT", sb.toString());

                    response = new JSONObject(sb.toString());
                    token = response.getString("token");
                    AppGlobals.putToken(token);
                    Log.d("TOKEN", token);

                    AppGlobals.putStudentDriverRouteID(response.getString("route"));
                    connection.disconnect();

                    url = new URL("http://46.101.75.194:8080/user");

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(false);
                    connection.setDoInput(true);
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("X-Api-Key", token);

                    in = (InputStream) connection.getContent();

                    sb = new StringBuilder();
                    while((ch = in.read()) != -1)
                        sb.append((char)ch);

                    Log.d("UserdetailRESULT", sb.toString());

                    JSONObject jsonObjectUser = new JSONObject(sb.toString());

                    AppGlobals.putName(jsonObjectUser.getString("first_name") + " " + jsonObjectUser.getString("last_name"));

                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                    Log.e("BEFORE", e.getMessage());
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
                Toast.makeText(LoginActivity.this, "Internet is not working. Make sure you are " +
                        "properly connected to the Internet", Toast.LENGTH_SHORT).show();
                Helpers.dismissProgressDialog();
                internetNotWorking = false;
            } else if (!token.isEmpty()) {
                Helpers.dismissProgressDialog();
                onLoginSuccess();
            } else if (responseCode == 401) {
                Toast.makeText(LoginActivity.this, "Authentication Failed", Toast.LENGTH_LONG).show();
                Helpers.dismissProgressDialog();
            }
        }
    }
}
