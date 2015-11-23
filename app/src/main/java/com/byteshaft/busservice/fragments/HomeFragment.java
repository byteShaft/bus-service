package com.byteshaft.busservice.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.byteshaft.busservice.R;
import com.byteshaft.busservice.utils.AppGlobals;

public class HomeFragment extends Fragment implements View.OnClickListener {

    View convertView;
    Button buttonReportSituation;
    Button buttonStartStopRoute;
    RadioGroup radioGroupReportSituation;
    RelativeLayout layoutDriverButtons;
    RelativeLayout layoutRouteCancelled;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_home, null);

        layoutDriverButtons = (RelativeLayout) convertView.findViewById(R.id.layout_driver_buttons);
        layoutRouteCancelled = (RelativeLayout) convertView.findViewById(R.id.layout_driver_route_cancelled);
        layoutRouteCancelled.setOnClickListener(this);

        setRouteStatus(AppGlobals.getRouteStatus());

        buttonStartStopRoute = (Button) convertView.findViewById(R.id.btn_route_switch);
        buttonStartStopRoute.setOnClickListener(this);

        buttonReportSituation = (Button) convertView.findViewById(R.id.btn_report_situation);
        buttonReportSituation.setOnClickListener(this);

        return convertView;
    }

    private void setRouteStatus(boolean status) {
        if (status) {
            layoutDriverButtons.setVisibility(View.VISIBLE);
            layoutRouteCancelled.setVisibility(View.GONE);
            AppGlobals.putRouteStatus(true);
        } else {
            layoutDriverButtons.setVisibility(View.GONE);
            layoutRouteCancelled.setVisibility(View.VISIBLE);
            layoutRouteCancelled.invalidate();
            AppGlobals.putRouteStatus(false);
        }
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()) {

            case R.id.layout_driver_route_cancelled:
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        getActivity());
                alertDialogBuilder.setTitle("Restore Route");
                alertDialogBuilder
                        .setMessage("Are you sure?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                final ProgressDialog progressDialog = new ProgressDialog(getActivity());
                                progressDialog.setIndeterminate(true);
                                progressDialog.setMessage("Restoring Route...");
                                progressDialog.show();

                                // TODO: Implement restoration logic here.

                                new android.os.Handler().postDelayed(
                                        new Runnable() {
                                            public void run() {
                                                setRouteStatus(true);
                                                progressDialog.dismiss();
                                            }
                                        }, 2000);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog restoreRouteDialog = alertDialogBuilder.create();
                restoreRouteDialog.show();
            break;

            case R.id.btn_report_situation:
                final Dialog reportSituationDialog = new Dialog(getActivity());
                reportSituationDialog.setContentView(R.layout.layout_report_dialog);
                reportSituationDialog.setTitle("Choose a Situation");
                reportSituationDialog.setCancelable(false);

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

                        reportSituationDialog.dismiss();

                        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
                        progressDialog.setIndeterminate(true);
                        progressDialog.setMessage("Reporting Situation...");
                        progressDialog.show();

                        // TODO: Implement reporting logic here.

                        new android.os.Handler().postDelayed(
                                new Runnable() {
                                    public void run() {
                                        setRouteStatus(false);
                                        progressDialog.dismiss();
                                    }
                                }, 2000);
                    }
                });
                reportSituationDialog.show();
            break;
        }
    }
}