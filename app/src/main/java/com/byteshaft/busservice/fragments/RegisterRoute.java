package com.byteshaft.busservice.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
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

import com.byteshaft.busservice.R;
import com.byteshaft.busservice.utils.Helpers;
import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;


public class RegisterRoute extends Fragment {

    private ViewPager mViewPager;
    private View convertView;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    public static MenuItem menuItemUndo;

    public static EditText etRouteName;
    public static EditText etBusNumber;

    public static TextView tvMapRegisterInfo;

    String routeName;
    String busNumber;
    String arrivalTime;
    String departureTime;
    String locationPointA;
    String locationPointB;

    public static TimePicker timePickerArrivalTime;
    public static TimePicker timePickerDepartureTime;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_register_route, null);
        setHasOptionsMenu(true);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

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
                Log.i("getItem", " position " + position);
                if (position == 2) {
                    if (PlaceholderFragment.pointA != null) {
                        Log.i("test", " test " + position);
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
                PlaceholderFragment.mMap.clear();
                PlaceholderFragment.onLongClickCounter = 0;
                menuItemUndo.setVisible(false);
                PlaceholderFragment.pointA = null;
                tvMapRegisterInfo.setText("Tap and hold to set Point 'A'");
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

                Helpers.showProgressDialog(getActivity(), "Collecting Information");

                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        getActivity());
                alertDialogBuilder.setTitle("Are you sure?");
                try {
                    alertDialogBuilder
                            .setMessage("Route Name: " + routeName + "\n" + "Bus Number: " + busNumber
                                    + "\n\n" + "Arrival Time: " + arrivalTime + "\n" + "Departure Time: " + departureTime
                                    + "\n\n" + "Point A: " + Helpers.getAddress(getActivity(),
                                    PlaceholderFragment.pointA) + "\n" + "Point B: "
                                    + Helpers.getAddress(getActivity(), PlaceholderFragment.pointB))
                            .setCancelable(false)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    register();
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    Helpers.dismissProgressDialog();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class PlaceholderFragment extends Fragment {

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private FragmentManager fm;
        public static GoogleMap mMap;
        private SupportMapFragment myMapFragment;
        private RoutingListener mRoutingListener;
        private LatLng taibahUniversityLocation = new LatLng(24.481778, 39.545373);
        public static LatLng pointA = null;
        public static LatLng pointB = null;
        public static int onLongClickCounter = 0;

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

                tvMapRegisterInfo = (TextView) rootView.findViewById(R.id.tv_map_register_info);

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
                                    mMap.addMarker(new MarkerOptions().position(latLng));
                                    tvMapRegisterInfo.setText("Tap and hold to set Point 'B'");
                                    pointA = latLng;
                                } else if (onLongClickCounter == 2) {
                                    mMap.addMarker(new MarkerOptions().position(latLng));
                                    tvMapRegisterInfo.setText("Resolving route points...");
                                    pointB = latLng;
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
                    polyOptions.width(12);
                    polylineOptions.zIndex(102);
                    polyOptions.addAll(polylineOptions.getPoints());
                    mMap.addPolyline(polyOptions);
                    tvMapRegisterInfo.setText("Route Successfully Established");
                }

                @Override
                public void onRoutingCancelled() {

                }
            };
            return rootView;
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

    public void register() {

        Helpers.showProgressDialog(getActivity(), "Registering");

        // TODO: Implement registration here.

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        onRegistrationSuccess();
                        Helpers.dismissProgressDialog();
                    }
                }, 2000);
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
        Helpers.closeKeyboard(getActivity(), etBusNumber.getWindowToken());
        getActivity().onBackPressed();
    }

    public void onRegistrationFailed() {
        Toast.makeText(getActivity(), "Registration failed", Toast.LENGTH_SHORT).show();
    }
}
