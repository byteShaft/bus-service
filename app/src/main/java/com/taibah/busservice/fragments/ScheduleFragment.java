package com.taibah.busservice.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.taibah.busservice.R;

public class ScheduleFragment extends Fragment {

    View convertView;
    Switch switchStudentSchedule;
    boolean service;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_schedule, null);

        switchStudentSchedule = (Switch) convertView.findViewById(R.id.switch_student_schedule);

        switchStudentSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message;
                if (service) {
                    message = "Turn the service off";
                } else {
                    message = "Turn the service on";
                }
                showSwitchingDialog(message);
            }
        });
        return convertView;
    }

    public void showSwitchingDialog(String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Are you sure?");
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // TODO: Implement registration here.

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
}
