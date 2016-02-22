package com.taibah.busservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;

public class LoginActivity extends Activity {

    EditText editTextUsername;
    EditText editTextPassword;
    Button buttonLogin;
    String username;
    String password;

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
                username = editTextUsername.getText().toString();
                password = editTextPassword.getText().toString();
                login();
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void login() {

        if (!validate()) {
            onLoginFailed();
            return;
        }

        buttonLogin.setEnabled(false);

        Helpers.showProgressDialog(LoginActivity.this, "Authenticating...");

        // TODO: Implement authentication here.

        // TODO: fetch Route Status here.

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        onLoginSuccess();
                        Helpers.dismissProgressDialog();
                    }
                }, 2000);
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
        AppGlobals.setVirgin(false);
        AppGlobals.putUsername(username);
        buttonLogin.setEnabled(true);
        setUserType();
        launchHomeFragment();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();
        buttonLogin.setEnabled(true);
    }

    public void launchHomeFragment() {
        Intent startIntent = new Intent(LoginActivity.this, MainActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        LoginActivity.this.startActivity(startIntent);
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
        if (!AppGlobals.isVirgin()) {
            finish();
        }
    }
}
