package com.taibah.busservice.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;

public class ManageDrivers extends Fragment {

    ArrayList<Integer> driversIdsList;
    HashMap<Integer, ArrayList<String>> hashMapDriverData;

    ListView driversListView;

    public static int responseCode;

    View convertView;
    HttpURLConnection connection;

    String driversName;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_manage_drivers, null);
        setHasOptionsMenu(true);

        driversListView = (ListView) convertView.findViewById(R.id.lv_list_drivers);

        driversIdsList = new ArrayList<>();
        hashMapDriverData = new HashMap<>();

        new RetrieveAllRegisteredDrivers().execute();

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
                AppGlobals.replaceFragment(getFragmentManager(), new RegisterDriver());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(driversListView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        driversName = hashMapDriverData.get(driversIdsList.get(info.position)).get(0) +
                " " + hashMapDriverData.get(driversIdsList.get(info.position)).get(1);

        menu.setHeaderTitle(driversName);
        MenuInflater inflater = this.getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_drivers_list, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int index = info.position;
        System.out.println(driversIdsList.get(index));

        switch (item.getItemId()) {
            case R.id.item_context_menu_drivers_list_show_credentials:
                AlertDialog.Builder alertDialogDriverShowCredentials = new AlertDialog.Builder(
                        getActivity()).setTitle(driversName)
                        .setMessage("Username: " +
                                hashMapDriverData.get(driversIdsList.get(info.position)).get(2) +
                        "\nPassword: " + hashMapDriverData.get(driversIdsList.get(info.position)).get(3))
                        .setCancelable(false)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialogCredentials = alertDialogDriverShowCredentials.create();
                alertDialogCredentials.show();
                return true;
            case R.id.item_context_menu_drivers_list_call:
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + hashMapDriverData.get(driversIdsList.get(info.position)).get(4)));
                startActivity(intent);
                return true;
        }
        return true;
    }

    private class RetrieveAllRegisteredDrivers extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Retrieving drivers list");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isNetworkAvailable() && Helpers.isInternetWorking()) {
                try {
                    connection = WebServiceHelpers.openConnectionForUrl
                            ("http://46.101.75.194:8080/users/driver", "GET");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connection.connect();
                    String data = WebServiceHelpers.readResponse(connection);
                    JSONObject jsonObj = new JSONObject(data);
                    responseCode = connection.getResponseCode();
                    JSONArray jsonArray = jsonObj.getJSONArray("users");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        if (!driversIdsList.contains(jsonObject.getInt("id"))) {
                            driversIdsList.add(jsonObject.getInt("id"));
                            ArrayList<String> arrayListString = new ArrayList<>();
                            arrayListString.add(jsonObject.getString("first_name"));
                            arrayListString.add(jsonObject.getString("last_name"));
                            arrayListString.add(jsonObject.getString("username"));
                            arrayListString.add(jsonObject.getString("password"));
                            arrayListString.add(jsonObject.getString("phone"));
                            hashMapDriverData.put(jsonObject.getInt("id"), arrayListString);
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
                CustomDriverListAdapter customRoutesListAdapter = new CustomDriverListAdapter(getContext(), R.layout.route_list_row, driversIdsList);
                driversListView.setAdapter(customRoutesListAdapter);
            } else {
                Toast.makeText(getActivity(), "Invalid Response " + responseCode, Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
            }
        }
    }

    class CustomDriverListAdapter extends ArrayAdapter<String> {

        ArrayList<Integer> arrayListIntIds;

        public CustomDriverListAdapter(Context context, int resource, ArrayList<Integer> arrayList) {
            super(context, resource);
            arrayListIntIds = arrayList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                convertView = layoutInflater.inflate(R.layout.driver_list_row, parent, false);
                viewHolder.tvDriverListName = (TextView) convertView.findViewById(R.id.tv_driver_list_name);
                viewHolder.tvDriverUsername = (TextView) convertView.findViewById(R.id.tv_driver_list_username);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.tvDriverListName.setText("Driver Name: " + hashMapDriverData.get(arrayListIntIds.get(position)).get(0) + " " + hashMapDriverData.get(arrayListIntIds.get(position)).get(1));
            viewHolder.tvDriverUsername.setText("Username: " + hashMapDriverData.get(arrayListIntIds.get(position)).get(2));
            return convertView;
        }

        @Override
        public int getCount() {
            return arrayListIntIds.size();
        }
    }

    static class ViewHolder {
        TextView tvDriverListName;
        TextView tvDriverUsername;
    }
}
