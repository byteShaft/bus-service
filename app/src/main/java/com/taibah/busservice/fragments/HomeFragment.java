package com.taibah.busservice.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.taibah.busservice.MainActivity;
import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.DriverService;
import com.taibah.busservice.utils.Helpers;

public class HomeFragment extends Fragment implements View.OnClickListener {

    View convertView;
    Button buttonReportSituation;
    Button buttonStartStopRoute;
    RadioGroup radioGroupReportSituation;
    RelativeLayout layoutDriverButtons;
    RelativeLayout layoutRouteCancelled;
    RelativeLayout layoutRouteInfo;
    LinearLayout layoutAdminInfo;
    TextView tvUserType;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_home, null);

        tvUserType = (TextView) convertView.findViewById(R.id.tv_user_type);
        layoutDriverButtons = (RelativeLayout) convertView.findViewById(R.id.layout_driver_buttons);
        layoutAdminInfo = (LinearLayout) convertView.findViewById(R.id.layout_admin_info);
        layoutRouteCancelled = (RelativeLayout) convertView.findViewById(R.id.layout_driver_route_cancelled);
        layoutRouteCancelled.setOnClickListener(this);

        layoutRouteInfo = (RelativeLayout) convertView.findViewById(R.id.layout_route_info);


        buttonStartStopRoute = (Button) convertView.findViewById(R.id.btn_route_switch);
        buttonStartStopRoute.setOnClickListener(this);

        buttonReportSituation = (Button) convertView.findViewById(R.id.btn_report_situation);
        buttonReportSituation.setOnClickListener(this);

        setAppView();
        setRouteStatus(AppGlobals.getRouteStatus());

        return convertView;
    }

    private void setRouteStatus(boolean status) {
        if (status) {
            if (AppGlobals.getUserType() == 2) {
                layoutDriverButtons.setVisibility(View.VISIBLE);
            }
            layoutRouteCancelled.setVisibility(View.GONE);
            if (AppGlobals.getUserType() != 0) {
                layoutRouteInfo.setVisibility(View.VISIBLE);
            }
            AppGlobals.putRouteStatus(true);
        } else {
            if (AppGlobals.getUserType() == 2) {
                layoutDriverButtons.setVisibility(View.GONE);
            }
            layoutRouteCancelled.setVisibility(View.VISIBLE);
            if (AppGlobals.getUserType() != 0) {
                layoutRouteInfo.setVisibility(View.GONE);
            }
            AppGlobals.putRouteStatus(false);
        }
    }

    public void setAppView() {
        if (AppGlobals.getUserType() == 2) {
            layoutDriverButtons.setVisibility(View.VISIBLE);
            tvUserType.setText("UserType: Driver");
            layoutAdminInfo.setVisibility(View.GONE);
            if (DriverService.driverLocationReportingServiceIsRunning) {
                buttonStartStopRoute.setText("End Route");
            } else {
                buttonStartStopRoute.setText("Start Route");
            }
        } else if (AppGlobals.getUserType() == 1) {
            layoutDriverButtons.setVisibility(View.GONE);
            tvUserType.setText("UserType: Student");
            layoutAdminInfo.setVisibility(View.GONE);
            layoutRouteCancelled.setClickable(false);
        } else if (AppGlobals.getUserType() == 0) {
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
                if (!DriverService.driverLocationReportingServiceIsRunning) {
                    AlertDialog.Builder alertDialogRouteSwitch = new AlertDialog.Builder(
                            getActivity());
                    alertDialogRouteSwitch.setTitle("Start Route");
                    alertDialogRouteSwitch
                            .setMessage("Are you sure?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    Helpers.showProgressDialog(getActivity(), "Starting Route");

                                    // TODO: Implement route starting logic here.

                                    new android.os.Handler().postDelayed(
                                            new Runnable() {
                                                public void run() {
                                                    buttonStartStopRoute.setText("End Route");
                                                    Helpers.dismissProgressDialog();
                                                    getActivity().startService(new Intent(getActivity(), DriverService.class));
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

                                    // TODO: Implement route starting logic here.

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
                    Toast.makeText(getActivity(), "Not connected to the network", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.layout_driver_route_cancelled:
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        getActivity());
                alertDialogBuilder.setTitle("Restore Route");
                alertDialogBuilder
                        .setMessage("Are you sure?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                Helpers.showProgressDialog(getActivity(), "Restoring Route...");

                                // TODO: Implement restoration logic here.

                                new android.os.Handler().postDelayed(
                                        new Runnable() {
                                            public void run() {
                                                setRouteStatus(true);
                                                Helpers.dismissProgressDialog();
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

                        Helpers.showProgressDialog(getActivity(), "Reporting Situation...");

                        int id= radioGroupReportSituation.getCheckedRadioButtonId();
                        View radioButton = radioGroupReportSituation.findViewById(id);
                        int radioIndex = radioGroupReportSituation.indexOfChild(radioButton);
                        Log.i("BusService", "SituationReportingIndex: " + radioIndex);

                        // TODO: Implement reporting logic here.

                        new android.os.Handler().postDelayed(
                                new Runnable() {
                                    public void run() {
                                        setRouteStatus(false);
                                        Helpers.dismissProgressDialog();
                                    }
                                }, 2000);
                    }
                });
                reportSituationDialog.show();
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
    }
}