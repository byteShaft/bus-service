package com.taibah.busservice.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class ManageStudent extends Fragment {

    public static int responseCode;


    ArrayList<Integer> studentIdsList;
    HashMap<Integer, ArrayList<String>> hashMapStudentData;


    ListView studentListView;

    View convertView;
    HttpURLConnection connection;
    String studentName;
    int index;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_manage_students, null);
        setHasOptionsMenu(true);
        studentListView = (ListView) convertView.findViewById(R.id.lv_list_students);

        new RetrieveAllRegisteredStudentsTask().execute();

        studentIdsList = new ArrayList<>();
        hashMapStudentData = new HashMap<>();

        return convertView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_manage, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_button:
                AppGlobals.replaceFragment(getFragmentManager(), new RegisterStudent());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(studentListView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        studentName = hashMapStudentData.get(studentIdsList.get(info.position)).get(0) +
                " " + hashMapStudentData.get(studentIdsList.get(info.position)).get(1);

        menu.setHeaderTitle(studentName);
        MenuInflater inflater = this.getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_students_list, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        index = info.position;
        System.out.println(studentIdsList.get(index));

        switch (item.getItemId()) {
            case R.id.item_context_menu_student_list_show_credentials:
                AlertDialog.Builder alertDialogDriverShowCredentials = new AlertDialog.Builder(
                        getActivity()).setTitle(studentName)
                        .setMessage("Username: " +
                                hashMapStudentData.get(studentIdsList.get(info.position)).get(2) +
                                "\nPassword: " + hashMapStudentData.get(studentIdsList.get(info.position)).get(3))
                        .setCancelable(false)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialogCredentials = alertDialogDriverShowCredentials.create();
                alertDialogCredentials.show();
                return true;
            case R.id.item_context_menu_student_list_allow_deny_service:
                if (hashMapStudentData.get(studentIdsList.get(info.position)).get(6) == "1") {
                    AlertDialog.Builder alertDialogStudentService = new AlertDialog.Builder(
                            getActivity()).setTitle(studentName)
                            .setMessage("Service allowed. Want to Deny")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // TODO Implement correct logic here
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alertDialogService = alertDialogStudentService.create();
                    alertDialogService.show();
                } else {
                    AlertDialog.Builder alertDialogStudentService = new AlertDialog.Builder(
                            getActivity()).setTitle(studentName)
                            .setMessage("Service denied. Want to Allow?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // TODO Implement correct logic here
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alertDialogService = alertDialogStudentService.create();
                    alertDialogService.show();
                }
                return true;
            case R.id.item_context_menu_student_list_delete:
                AlertDialog.Builder alertDialogStudentDelete = new AlertDialog.Builder(
                        getActivity()).setTitle(studentName)
                        .setMessage("Really want to delete?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                new DeleteStudentTask().execute();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialogDelete = alertDialogStudentDelete.create();
                alertDialogDelete.show();
                return true;
        }
        return true;
    }

    private class RetrieveAllRegisteredStudentsTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Retrieving students list");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isNetworkAvailable() && Helpers.isInternetWorking()) {
                try {
                    connection = WebServiceHelpers.openConnectionForUrl
                            ("http://46.101.75.194:8080/users/student", "GET");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connection.connect();
                    String data = WebServiceHelpers.readResponse(connection);
                    JSONObject jsonObj = new JSONObject(data);
                    responseCode = connection.getResponseCode();
                    Log.i("Driver", ": " + jsonObj);
                    Log.i("Response Code: ", "" + connection.getResponseCode());
                    JSONArray jsonArray = jsonObj.getJSONArray("users");


                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        if (!studentIdsList.contains(jsonObject.getInt("id"))) {
                            studentIdsList.add(jsonObject.getInt("id"));
                            ArrayList<String> arrayListString = new ArrayList<>();
                            arrayListString.add(jsonObject.getString("first_name"));
                            arrayListString.add(jsonObject.getString("last_name"));
                            arrayListString.add(jsonObject.getString("username"));
                            arrayListString.add(jsonObject.getString("password"));
                            arrayListString.add(jsonObject.getString("roll_number"));
                            arrayListString.add(jsonObject.getString("attending"));
                            arrayListString.add(jsonObject.getString("allowed"));
                            hashMapStudentData.put(jsonObject.getInt("id"), arrayListString);
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
            if (responseCode == 200) {
                Helpers.dismissProgressDialog();
                CustomStudentsListAdapter customRoutesListAdapter = new CustomStudentsListAdapter(getContext(), R.layout.route_list_row, studentIdsList);
                studentListView.setAdapter(customRoutesListAdapter);
            } else {

                // TODO Implement correct logic here in case of any failure
                Toast.makeText(getActivity(), "Invalid Response " + responseCode, Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
            }
        }
    }


    class CustomStudentsListAdapter extends ArrayAdapter<String> {

        ArrayList<Integer> arrayListIntIds;

        public CustomStudentsListAdapter(Context context, int resource, ArrayList<Integer> arrayList) {
            super(context, resource);
            arrayListIntIds = arrayList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                convertView = layoutInflater.inflate(R.layout.student_list_row, parent, false);
                viewHolder.tvStudentListName = (TextView) convertView.findViewById(R.id.tv_student_list_name);
                viewHolder.tvStudentUsername = (TextView) convertView.findViewById(R.id.tv_student_list_username);
                viewHolder.tvStudentRollNumber = (TextView) convertView.findViewById(R.id.tv_student_list_roll_number);
                viewHolder.tvStudentAttending = (TextView) convertView.findViewById(R.id.tv_student_list_attending);
                viewHolder.tvStudentAllowed = (TextView) convertView.findViewById(R.id.tv_student_list_allowed);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.tvStudentListName.setText("Name: " + hashMapStudentData.get(arrayListIntIds.get(position)).get(0) + " " + hashMapStudentData.get(arrayListIntIds.get(position)).get(1));
            viewHolder.tvStudentUsername.setText("Username: " + hashMapStudentData.get(arrayListIntIds.get(position)).get(2));
            viewHolder.tvStudentRollNumber.setText("RollNumber: " + hashMapStudentData.get(arrayListIntIds.get(position)).get(4));

            System.out.println("yoooyoyo" + hashMapStudentData.get(arrayListIntIds.get(position)).get(5));
            if (hashMapStudentData.get(arrayListIntIds.get(position)).get(5).equalsIgnoreCase("1")) {
                viewHolder.tvStudentAttending.setText("Attending Route: Yes");
            } else {
                viewHolder.tvStudentAttending.setText("Attending Route: No");
            }

            if (hashMapStudentData.get(arrayListIntIds.get(position)).get(6).equalsIgnoreCase("1")) {
                viewHolder.tvStudentAllowed.setText("Allowed by Admin: Yes" );
            } else {
                viewHolder.tvStudentAllowed.setText("Allowed by Admin: No" );
            }

            return convertView;
        }

        @Override
        public int getCount() {
            return arrayListIntIds.size();
        }
    }

    static class ViewHolder {
        TextView tvStudentListName;
        TextView tvStudentUsername;
        TextView tvStudentRollNumber;
        TextView tvStudentAttending;
        TextView tvStudentAllowed;
    }

    public class DeleteStudentTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Deleting...");
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                URL url = new URL("http://46.101.75.194:8080/users/" + studentIdsList.get(index));

                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());

                responseCode = connection.getResponseCode();

                System.out.println("Delete Student Response Code: " + responseCode);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (responseCode == 204) {
                Toast.makeText(getActivity(), "Student Deleted", Toast.LENGTH_SHORT).show();
                Helpers.dismissProgressDialog();
                new RetrieveAllRegisteredStudentsTask().execute();
            }
        }
    }
}
