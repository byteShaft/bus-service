package com.taibah.busservice.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.taibah.busservice.Helpers.WebServiceHelpers;
import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class RegisterStudent extends Fragment {
    public static EditText etStudentFirstName;
    public static EditText etStudentLastName;
    public static EditText etStudentContactNumber;
    public static EditText etStudentRollNumber;
    public static EditText etStudentEmail;
    public static MenuItem menuItemUndo;
    public static TextView tvMapRegisterStudentInfo;
    public static int onLongClickCounter = 0;
    public static int responseCode;

    View convertView;
    static String firstNameStudent = "";
    static String lastNameStudent;
    static String contactNumberStudent;
    static String rollNumberStudent;
    static String emailStudent;
    String studentRegistrationDetail;
    String studentStop;

    static GoogleMap mMap;

    HttpURLConnection connection;

    public static ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;

    static ArrayList<Integer> routeIdsList;
    static HashMap<Integer, ArrayList<String>> hashMapRouteData;

    static Spinner spinnerRoutesList;

    static int routeId = 0;
    static String spinnerText;

    public static LatLng latLngA;
    public static LatLng latLngB;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_register_student, null);
        setHasOptionsMenu(true);


        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());


        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) convertView.findViewById(R.id.container_student);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        routeIdsList = new ArrayList<>();
        hashMapRouteData = new HashMap<>();

        new RetrieveAllRoutesTask().execute();

        TabLayout tabLayout = (TabLayout) convertView.findViewById(R.id.tabs_student);
        tabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Helpers.closeKeyboard(getActivity());
                Log.i("OnPageSelected", "Main");
                if (position == 1) {
                    Log.i("OnPageSelected", "PositionOne");
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.detach(RegisterStudent.PlaceholderFragment.newInstance(position)).attach(RegisterStudent.PlaceholderFragment.newInstance(position)).commit();
                    RegisterStudent.PlaceholderFragment.newInstance(position);

                    if (onLongClickCounter != 0) {
                        menuItemUndo.setVisible(true);
                    }
                } else {
                    menuItemUndo.setVisible(false);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mViewPager.setOffscreenPageLimit(0);

        return convertView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_done, menu);

        menuItemUndo = menu.findItem(R.id.action_undo_button);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_undo_button:
                onLongClickCounter = 0;
                mMap.clear();
                setInitialMap();
                menuItemUndo.setVisible(false);
                PlaceholderFragment.studentStopLatLng = null;
                tvMapRegisterStudentInfo.setText("Tap and hold to set a stop");
                return true;
            case R.id.action_done_button:
                firstNameStudent = etStudentFirstName.getText().toString().trim();
                lastNameStudent = etStudentLastName.getText().toString().trim();
                contactNumberStudent = etStudentContactNumber.getText().toString().trim();
                rollNumberStudent = etStudentRollNumber.getText().toString().trim();
                emailStudent = etStudentEmail.getText().toString().trim();
                try {
                    if (!validateInfo()) {
                        Toast.makeText(getActivity(), "Incomplete info", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    studentStop = PlaceholderFragment.studentStopLatLng.toString();

                } catch (NullPointerException e) {
                    Toast.makeText(getActivity(), "Incomplete info", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    return true;
                }
                new checkInternetTask().execute();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showInternetNotWorkingDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setMessage("Internet not available");
        alertDialogBuilder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                new checkInternetTask().execute();
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void showRegInfoDialog(String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("Are you sure?");
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                register();
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

    public void register() {

        String username = "sdt" + firstNameStudent + rollNumberStudent.substring(rollNumberStudent.length() - 3);
        String password = lastNameStudent + rollNumberStudent.substring(rollNumberStudent.length() - 3);

        studentRegistrationDetail = "route_id=" + routeId
                + "&" + "first_name=" + firstNameStudent + "&" + "last_name=" + lastNameStudent
                + "&" + "password=" + password + "&" + "passconf=" + password + "&" + "type=student"
                + "&" + "username=" + username
                + "&" + "latitude=" + PlaceholderFragment.studentStopLatLng.latitude
                + "&" + "longitude=" + PlaceholderFragment.studentStopLatLng.longitude
                + "&" + "roll_number=" + rollNumberStudent;

        new RegisterStudentTask().execute();

    }

    public static boolean validateInfo() {
        boolean valid = true;

        if (firstNameStudent.isEmpty() || firstNameStudent.length() < 3) {
            etStudentFirstName.setError("at least 3 characters");
            valid = false;
        } else {
            etStudentFirstName.setError(null);
        }

        if (lastNameStudent.isEmpty() || lastNameStudent.length() < 3) {
            etStudentLastName.setError("at least 3 characters");
            valid = false;
        } else {
            etStudentLastName.setError(null);
        }

        if (rollNumberStudent.isEmpty() || rollNumberStudent.length() < 3) {
            etStudentRollNumber.setError("at least 3 characters");
            valid = false;
        } else {
            etStudentContactNumber.setError(null);
        }

        if (!contactNumberStudent.isEmpty() && !PhoneNumberUtils.isGlobalPhoneNumber(contactNumberStudent)) {
            etStudentContactNumber.setError("Number is invalid");
            valid = false;
        } else {
            etStudentContactNumber.setError(null);
        }

        if (!emailStudent.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(emailStudent).matches()) {
            etStudentEmail.setError("Email is invalid");
            valid = false;
        } else {
            etStudentEmail.setError(null);
        }

        return valid;
    }

    public void onRegistrationSuccess() {
        Toast.makeText(getActivity(), "Registration successful", Toast.LENGTH_SHORT).show();
        menuItemUndo.setVisible(false);
        Helpers.closeKeyboard(getActivity());
        getActivity().onBackPressed();
    }

    public void onRegistrationFailed() {
        Toast.makeText(getActivity(), "Registration failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        onLongClickCounter = 0;
        menuItemUndo.setVisible(false);
        PlaceholderFragment.studentStopLatLng = null;
    }

    public static class PlaceholderFragment extends Fragment implements AdapterView.OnItemSelectedListener {
        private static final String ARG_SECTION_NUMBER = "section_number";
        public static LatLng studentStopLatLng = null;
         static LatLng dummyPosition = new LatLng(24.513371, 39.576058);

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */

        private FragmentManager fm;
        private SupportMapFragment myMapFragment;
        static RoutingListener mRoutingListener;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int tabCount = getArguments().getInt(ARG_SECTION_NUMBER);
            View rootView = null;
            if (tabCount == 1) {
                rootView = inflater.inflate(R.layout.layout_register_student_info, container, false);

                etStudentFirstName = (EditText) rootView.findViewById(R.id.et_student_first_name);
                etStudentFirstName.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        firstNameStudent = etStudentFirstName.getText().toString().trim();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                etStudentLastName = (EditText) rootView.findViewById(R.id.et_student_last_name);
                etStudentContactNumber = (EditText) rootView.findViewById(R.id.et_student_contact);
                etStudentRollNumber = (EditText) rootView.findViewById(R.id.et_student_roll_number);
                etStudentEmail = (EditText) rootView.findViewById(R.id.et_student_email);


                spinnerRoutesList = (Spinner) rootView.findViewById(R.id.spinner_select_route_for_student);
                spinnerRoutesList.setOnItemSelectedListener(this);

            } else if (tabCount == 2 && latLngA != null) {
                rootView = inflater.inflate(R.layout.layout_register_student_route, container, false);
                fm = getChildFragmentManager();
                myMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.register_student_map);
                tvMapRegisterStudentInfo = (TextView) rootView.findViewById(R.id.tv_map_register_student_info);

                myMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        mMap = googleMap;
                        setInitialMap();
                        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                            @Override
                            public void onMapLongClick(LatLng latLng) {
                                if (RegisterStudent.firstNameStudent.trim().length() < 3) {
                                    Toast.makeText(getActivity(), "Put student name first", Toast.LENGTH_SHORT).show();
                                    mViewPager.setCurrentItem(0);
                                } else {
                                    onLongClickCounter++;
                                    if (onLongClickCounter == 1) {
                                        mMap.clear();
                                        menuItemUndo.setVisible(true);
                                        tvMapRegisterStudentInfo.setText("Resolving route points...");
                                        mMap.addMarker(new MarkerOptions().position(latLngB)
                                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_marker_b)));
                                        mMap.addMarker(new MarkerOptions().position(latLngA).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_marker_a)));


                                        mMap.addMarker(new MarkerOptions().position(latLng)).setIcon(BitmapDescriptorFactory.fromBitmap(Helpers.getMarkerBitmapFromView(RegisterStudent.firstNameStudent, getActivity())));


                                        LatLng[] latLngDummyList = new LatLng[]{RegisterRoute.
                                                taibahUniversityLocation, latLng, dummyPosition};
                                        buildAndDisplayRouteWithWayPoints(latLngDummyList);
                                        studentStopLatLng = latLng;
                                    }
                                }
                            }
                        });

                    }
                });

                mRoutingListener = new RoutingListener() {
                    @Override
                    public void onRoutingFailure() {

                    }

                    @Override
                    public void onRoutingStart() {

                    }

                    @Override
                    public void onRoutingSuccess(PolylineOptions polylineOptions, Route route) {
//
                        mMap.addPolyline(new PolylineOptions()
                                .addAll(polylineOptions.getPoints())
                                .width(12)
                                .geodesic(true)
                                .color(Color.parseColor("#80000000")));

                        mMap.addPolyline(new PolylineOptions()
                                .addAll(polylineOptions.getPoints())
                                .width(6)
                                .geodesic(true)
                                .color(Color.RED));
                        if (onLongClickCounter == 1) {
                            tvMapRegisterStudentInfo.setText("Stop Successfully Marked");
                        }
                    }

                    @Override
                    public void onRoutingCancelled() {

                    }
                };
            }
            return rootView;
        }

        static void buildAndDisplayRoute(LatLng startPoint, LatLng endPoint) {
            Routing routing = new Routing.Builder()
                    .travelMode(Routing.TravelMode.DRIVING)
                    .withListener(mRoutingListener)
                    .waypoints(startPoint, endPoint)
                    .routeMode(Routing.RouteMode.FASTEST)
                    .build();
            routing.execute();
        }

        private void buildAndDisplayRouteWithWayPoints(LatLng[] latLngArrayWithWayPoints) {
            Routing routing = new Routing.Builder()
                    .travelMode(Routing.TravelMode.DRIVING)
                    .withListener(mRoutingListener)
                    .waypoints(latLngArrayWithWayPoints)
                    .routeMode(Routing.RouteMode.FASTEST)
                    .build();
            routing.execute();
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            routeId = RegisterStudent.routeIdsList.get(position);
            spinnerText = hashMapRouteData.get(routeIdsList.get(position)).get(0);
            latLngA = new LatLng(Double.parseDouble(hashMapRouteData.get(routeIdsList.get(position)).get(1)), Double.parseDouble(hashMapRouteData.get(routeIdsList.get(position)).get(3)));
            latLngB = new LatLng(Double.parseDouble(hashMapRouteData.get(routeIdsList.get(position)).get(2)), Double.parseDouble(hashMapRouteData.get(routeIdsList.get(position)).get(4)));
            System.out.println(latLngA);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private class checkInternetTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            return Helpers.isInternetWorking();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Collecting information");
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            String message = "Student Name:  " + firstNameStudent + " " + lastNameStudent + "\n"
                    + "Contact Number: " + contactNumberStudent
                    + "\n" + "RollNumber: " + rollNumberStudent + "\n" + "Email ID: " + emailStudent
                    + "\n\n" + "Assigned Route: " + spinnerText
                    + "\n" + "Stop Address: " + Helpers.getAddress(getActivity(), PlaceholderFragment.studentStopLatLng);

            Helpers.dismissProgressDialog();
            if (success) {
                showRegInfoDialog(message);
            } else {
                showInternetNotWorkingDialog();
            }
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Info";
                case 1:
                    return "Stop";
            }
            return null;
        }

    }

    private class RegisterStudentTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            Helpers.showProgressDialog(getActivity(), "Registering");
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                URL url = new URL("http://46.101.75.194:8080/register");

                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());

                Log.i("Token", AppGlobals.getToken());

                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                out.writeBytes(studentRegistrationDetail);
                out.flush();
                out.close();
                responseCode = connection.getResponseCode();
                Log.i("Response", "" + responseCode);

                InputStream in = (InputStream) connection.getContent();
                int ch;
                StringBuilder sb;

                sb = new StringBuilder();
                while ((ch = in.read()) != -1)
                    sb.append((char) ch);

                Log.d("RESULT", sb.toString());

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("BEFORE", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (responseCode == 201) {
                Helpers.dismissProgressDialog();
                Toast.makeText(getActivity(), "Registration Successful", Toast.LENGTH_LONG).show();
                Helpers.closeKeyboard(getActivity());
                getActivity().onBackPressed();
            } else {
                // TODO Implement correct logic here
                Toast.makeText(getActivity(), "Invalid Response: " + responseCode, Toast.LENGTH_LONG).show();
                Helpers.dismissProgressDialog();
            }
        }
    }

    private class RetrieveAllRoutesTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Retrieving All Routes");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isNetworkAvailable() && Helpers.isInternetWorking()) {
                try {
                    connection = WebServiceHelpers.openConnectionForUrl
                            ("http://46.101.75.194:8080/routes", "GET");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connection.connect();
                    responseCode = connection.getResponseCode();
                    System.out.print(responseCode);
                    String data = WebServiceHelpers.readResponse(connection);
                    JSONArray jsonArray = new JSONArray(data);
                    System.out.println(jsonArray);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        if (!routeIdsList.contains(jsonObject.getInt("id"))) {
                            routeIdsList.add(jsonObject.getInt("id"));
                            ArrayList<String> arrayList = new ArrayList<>();
                            arrayList.add(jsonObject.getString("name"));
                            arrayList.add(jsonObject.getString("start_latitude"));
                            arrayList.add(jsonObject.getString("end_latitude"));
                            arrayList.add(jsonObject.getString("start_longitude"));
                            arrayList.add(jsonObject.getString("end_longitude"));

                            hashMapRouteData.put(jsonObject.getInt("id"), arrayList);
                            System.out.println(hashMapRouteData);
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
            if (responseCode == 200) {
                Helpers.dismissProgressDialog();

                CustomSpinnerListAdapter customSpinnerListAdapter = new CustomSpinnerListAdapter(getActivity(), R.layout.spinner_row, routeIdsList);
                spinnerRoutesList.setAdapter(customSpinnerListAdapter);
                spinnerRoutesList.setSelection(0);

            } else {
                // TODO Implement correct logic here in case of any failure
                Toast.makeText(getActivity(), "Something went wrong. Please try again", Toast.LENGTH_LONG).show();
                Helpers.dismissProgressDialog();
                getActivity().onBackPressed();
            }
        }
    }


    class CustomSpinnerListAdapter extends ArrayAdapter<String> {

        ArrayList<Integer> arrayListIntIds;

        public CustomSpinnerListAdapter(Context context, int resource, ArrayList<Integer> arrayList) {
            super(context, resource);
            arrayListIntIds = arrayList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                convertView = layoutInflater.inflate(R.layout.spinner_row, parent, false);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.tvSpinner = (TextView) convertView.findViewById(R.id.tv_spinner_row);
            viewHolder.tvSpinner.setText(hashMapRouteData.get(arrayListIntIds.get(position)).get(0));
            return convertView;
        }

        @Override
        public int getCount() {
            return arrayListIntIds.size();
        }
    }


    static class ViewHolder {
        TextView tvSpinner;
    }

    public static void setInitialMap() {
        mMap.addMarker(new MarkerOptions().position(latLngB)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_marker_b)));
        mMap.addMarker(new MarkerOptions().position(latLngA).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_marker_a)));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(RegisterRoute.
                taibahUniversityLocation, 13.0f));

        PlaceholderFragment.buildAndDisplayRoute(latLngB, latLngA);
    }
}


