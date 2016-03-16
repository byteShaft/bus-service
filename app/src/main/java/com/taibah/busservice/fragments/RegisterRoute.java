package com.taibah.busservice.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
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
import com.taibah.busservice.LoginActivity;
import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.Helpers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class RegisterRoute extends Fragment {

    public static MenuItem menuItemUndo;
    public static GoogleMap mMap;
    public static EditText etRouteName;
    public static EditText etBusNumber;
    public static TextView tvMapRegisterRouteInfo;
    public static int onLongClickCounter = 0;
    public static TimePicker timePickerArrivalTime;
    public static TimePicker timePickerDepartureTime;
    public static LatLng taibahUniversityLocation = new LatLng(24.481778, 39.545373);
    public static LatLng[] latLngList;
    public static int responseCode;
    HttpURLConnection connection;
    String routeName;
    String busNumber;
    String arrivalTime;
    String departureTime;
    String locationPointA;
    String locationPointB;
    String routeInfoDialogMessage;
    String routeInfo = "";
    private ViewPager mViewPager;
    private View convertView;
    private SectionsPagerAdapter mSectionsPagerAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_register_route, null);
        setHasOptionsMenu(true);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) convertView.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) convertView.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Helpers.closeKeyboard(getActivity());
                if (position == 2) {
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

        mViewPager.setOffscreenPageLimit(3);
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
                mMap.clear();
                onLongClickCounter = 0;
                menuItemUndo.setVisible(false);
                PlaceholderFragment.pointA = null;
                tvMapRegisterRouteInfo.setText("Tap and hold to set Point 'A'");
                return true;
            case R.id.action_done_button:

                try {
                    routeName = etRouteName.getText().toString();
                    busNumber = etBusNumber.getText().toString();

                    if (!validateInfo()) {
                        Toast.makeText(getActivity(), "Incomplete info", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    arrivalTime = timePickerArrivalTime.getCurrentHour() + ":" + timePickerArrivalTime.getCurrentMinute();
                    departureTime = timePickerDepartureTime.getCurrentHour() + ":" + timePickerDepartureTime.getCurrentMinute();

                    locationPointA = PlaceholderFragment.pointA.toString();
                    locationPointB = PlaceholderFragment.pointB.toString();

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
                routeInfo = "name=" + routeName + "&" + "bus_number=" + busNumber + "&"
                        + "arrival_time=2016-02-19 "
                        + arrivalTime + ":00" + "&" + "departure_time=2016-02-19 "
                        + departureTime + ":00"
                        + "&" + "start_latitude=" + PlaceholderFragment.pointA.latitude
                        + "&" + "end_latitude=" + PlaceholderFragment.pointB.latitude
                        + "&" + "start_longitude=" + PlaceholderFragment.pointA.longitude
                        + "&" + "end_longitude=" + PlaceholderFragment.pointB.longitude
                        + "&" + "total_stops=10";
                Log.i("routeInfo", routeInfo);
                new RegisterRouteTask().execute();
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

    public boolean validateInfo() {
        boolean valid = true;

        if (routeName.trim().isEmpty() || routeName.trim().length() < 6) {
            etRouteName.setError("at least 6 characters");
            valid = false;
        } else {
            etRouteName.setError(null);
        }

        if (busNumber.trim().isEmpty() || busNumber.trim().length() < 4) {
            etBusNumber.setError("at least 4 characters");
            valid = false;
        } else {
            etBusNumber.setError(null);
        }
        return valid;
    }

    public void onRegistrationSuccess() {
        Toast.makeText(getActivity(), "Registration successful", Toast.LENGTH_SHORT).show();
        menuItemUndo.setVisible(true);
        Helpers.closeKeyboard(getActivity());
        getActivity().onBackPressed();
    }

    public void onRegistrationFailed() {
        Toast.makeText(getActivity(), "Registration failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        onLongClickCounter = 0;
        PlaceholderFragment.pointA = null;
        PlaceholderFragment.pointB = null;
    }

    public static class PlaceholderFragment extends Fragment {

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        public static LatLng pointA = null;
        public static LatLng pointB = null;
        private FragmentManager fm;
        private SupportMapFragment myMapFragment;
        private RoutingListener mRoutingListener;
        private LatLng taibahUniversityLocation = new LatLng(24.481778, 39.545373);

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
                rootView = inflater.inflate(R.layout.layout_register_route_info, container, false);
                etRouteName = (EditText) rootView.findViewById(R.id.et_route_name);
                etBusNumber = (EditText) rootView.findViewById(R.id.et_bus_number);

            } else if (tabCount == 2) {
                rootView = inflater.inflate(R.layout.layout_route_register_timepicker, container, false);
                timePickerArrivalTime = (TimePicker) rootView.findViewById(R.id.tp_register_route_arrival_time);
                timePickerDepartureTime = (TimePicker) rootView.findViewById(R.id.tp_register_route_departure_time);

            } else if (tabCount == 3) {
                rootView = inflater.inflate(R.layout.layout_route_register_map, container, false);
                fm = getChildFragmentManager();
                myMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map1);

                tvMapRegisterRouteInfo = (TextView) rootView.findViewById(R.id.tv_map_register_route_info);

                myMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        mMap = googleMap;
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(taibahUniversityLocation, 13.0f));
                        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                            @Override
                            public void onMapLongClick(LatLng latLng) {
                                menuItemUndo.setVisible(true);
                                onLongClickCounter++;
                                if (onLongClickCounter == 1) {
                                    mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_marker_a)));
                                    tvMapRegisterRouteInfo.setText("Tap and hold to set Point 'B'");
                                    pointA = latLng;
                                } else if (onLongClickCounter == 2) {
                                    mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_marker_b)));
                                    tvMapRegisterRouteInfo.setText("Resolving route points...");
                                    pointB = latLng;
                                    latLngList = new LatLng[]{pointA, pointB};
                                    Routing routing = new Routing.Builder()
                                            .travelMode(Routing.TravelMode.DRIVING)
                                            .withListener(mRoutingListener)
                                            .waypoints(pointA, pointB)
                                            .build();
                                    routing.execute();
                                }
                            }
                        });
                    }
                });
            }

            mRoutingListener = new RoutingListener() {
                @Override
                public void onRoutingFailure() {

                }

                @Override
                public void onRoutingStart() {

                }

                @Override
                public void onRoutingSuccess(PolylineOptions polylineOptions, Route route) {
                    PolylineOptions polyOptions = new PolylineOptions();
                    polyOptions.color(Color.RED);
                    polyOptions.width(10);
                    polylineOptions.zIndex(102);
                    polyOptions.addAll(polylineOptions.getPoints());
                    mMap.addPolyline(polyOptions);
                    tvMapRegisterRouteInfo.setText("Route Successfully Established");
                }

                @Override
                public void onRoutingCancelled() {

                }
            };
            return rootView;
        }
    }

    private class checkInternetTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            routeInfoDialogMessage = "Route Name: "
                    + routeName + "\n" + "Bus Number: " + busNumber
                    + "\n\n" + "Arrival Time: " + arrivalTime + "\n" + "Departure Time: " + departureTime
                    + "\n\n" + "Point A: " + Helpers.getAddress(getActivity(),
                    PlaceholderFragment.pointA) + "\n" + "Point B: "
                    + Helpers.getAddress(getActivity(), PlaceholderFragment.pointB);

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

            Helpers.dismissProgressDialog();
            if (success) {
                showRegInfoDialog(routeInfoDialogMessage);
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
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Info";
                case 1:
                    return "Time";
                case 2:
                    return "Route";
            }
            return null;
        }
    }

    private class RegisterRouteTask extends AsyncTask<Void, Integer, Void> {

        LoginActivity loginActivity = new LoginActivity();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Registering route");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (loginActivity.token.isEmpty() && Helpers.isInternetWorking()) {
                try {
                    URL url = new URL("http://46.101.75.194:8080/routes");

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
                    out.writeBytes(routeInfo);
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
                    onRegistrationFailed();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (responseCode == 201) {
                Helpers.dismissProgressDialog();
                onRegistrationSuccess();
            } else {
                Toast.makeText(getActivity(), "Invalid Response " + responseCode, Toast.LENGTH_SHORT).show();
                Helpers.dismissProgressDialog();
            }
        }
    }
}
