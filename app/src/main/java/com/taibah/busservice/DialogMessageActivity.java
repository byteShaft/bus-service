package com.taibah.busservice;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.taibah.busservice.utils.NetworkChangeReceiver;

public class DialogMessageActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showNoNetworkDialog();
    }

    private void showNoNetworkDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("BusService");
        alertDialogBuilder.setMessage("Internet disconnected. Reconnect to resume reporting location");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("Recheck", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (NetworkChangeReceiver.status != "Not connected to Internet") {
                    dialog.dismiss();
                }
            }
        });
        alertDialogBuilder.setNegativeButton("Stop", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialogBuilder.create();
        alertDialogBuilder.show();
    }
}
