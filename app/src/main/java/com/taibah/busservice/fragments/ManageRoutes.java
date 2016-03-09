package com.taibah.busservice.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class ManageRoutes extends ListFragment {

    public static int responseCode;

    ArrayList<Integer> routeIdsList;
    HashMap<Integer, ArrayList<String>> hashMapRouteData;

    TextView tvRouteListName;
    TextView tvRouteListBusNumber;
    TextView tvRouteListArrivalTime;
    TextView tvRouteListDepartureTime;

    ListView routesListView;

    View convertView;
    HttpURLConnection connection;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_manage_route, null);
        setHasOptionsMenu(true);

        new RetrieveAllRegisteredRoutes().execute();


        routeIdsList = new ArrayList<>();
        hashMapRouteData = new HashMap<>();

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
                AppGlobals.replaceFragment(getFragmentManager(), new RegisterRoute());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private HttpURLConnection openConnectionForUrl(String path)
            throws IOException {

        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("GET");
        return connection;
    }

    private JSONArray readResponse(HttpURLConnection connection)
            throws IOException, JSONException {

        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder response = new StringBuilder();
        while((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        return new JSONArray(response.toString());
    }

    private class RetrieveAllRegisteredRoutes extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Retrieving All Routes...");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isNetworkAvailable() && Helpers.isInternetWorking()) {
                try {
                    connection = openConnectionForUrl("http://46.101.75.194:8080/routes");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connection.connect();
                    JSONArray jsonArray = readResponse(connection);
                    responseCode = connection.getResponseCode();
                    System.out.println(jsonArray);
                    ArrayList<String> arrayListString = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                        routeIdsList.add(jsonObject.getInt("id"));
                        arrayListString.add(jsonObject.getString("name"));
                        arrayListString.add(jsonObject.getString("bus_number"));
                        arrayListString.add(jsonObject.getString("arrival_time"));
                        arrayListString.add(jsonObject.getString("departure_time"));
                        hashMapRouteData.put(jsonObject.getInt("id"), arrayListString);
                    }
                    Log.i("Response Code: ", "" + connection.getResponseCode());
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (responseCode == 200) {
                Helpers.dismissProgressDialog();
            } else {
                Toast.makeText(getActivity(), "Invalid Response " + responseCode, Toast.LENGTH_SHORT).show();
                Helpers.dismissProgressDialog();
            }
        }
    }

    class customRoutesListAdapter extends ArrayAdapter {

        ArrayList<Integer> arrayListIntIds;

        public customRoutesListAdapter(Context context, int resource, ArrayList<Integer> arrayList) {
            super(context, resource);
            arrayListIntIds = arrayList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)

        }

        @Override
        public int getCount() {
            return super.getCount();
        }
    }

}
