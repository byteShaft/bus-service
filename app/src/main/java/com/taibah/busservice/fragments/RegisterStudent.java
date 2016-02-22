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
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.taibah.busservice.R;
import com.taibah.busservice.utils.Helpers;
import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class RegisterStudent extends Fragment {
    View convertView;

    public static EditText etStudentFirstName;
    public static EditText etStudentLastName;
    public static EditText etStudentContactNumber;
    public static EditText etStudentRollNumber;
    public static EditText etStudentEmail;

    public static MenuItem menuItemUndo;

    public static TextView tvMapRegisterStudentInfo;

    String firstNameStudent;
    String lastNameStudent;
    String contactNumberStudent;
    String rollNumberStudent;
    String emailStudent;

    String studentStop;

    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.layout_register_student, null);
        setHasOptionsMenu(true);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) convertView.findViewById(R.id.container_student);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) convertView.findViewById(R.id.tabs_student);
        tabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Helpers.closeKeyboard(getActivity());
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

                PlaceholderFragment.onLongClickCounter = 0;
                menuItemUndo.setVisible(false);
                PlaceholderFragment.studentStopLatLng = null;
                tvMapRegisterStudentInfo.setText("Tap and hold to set a stop");


                return true;
            case R.id.action_done_button:

                firstNameStudent =  etStudentFirstName.getText().toString().trim();
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

    private class checkInternetTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            return Helpers.isInternetWorking(getActivity());
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
                    + "\n\n" + "Stop Address: " + Helpers.getAddress(getActivity(), PlaceholderFragment.studentStopLatLng);

            Helpers.dismissProgressDialog();
            if (success) {
                showRegInfoDialog(message);
            } else {
                showInternetNotWorkingDialog();
            }
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

    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */

        private FragmentManager fm;

        private static final String ARG_SECTION_NUMBER = "section_number";
        private SupportMapFragment myMapFragment;
        private RoutingListener mRoutingListener;
        private GoogleMap mMap;
        private Polyline polyline;

        public static LatLng studentStopLatLng = null;

        public static int onLongClickCounter = 0;
        private static LatLng dummyPosition = new LatLng(24.513371, 39.576058);

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
            int tabCount =  getArguments().getInt(ARG_SECTION_NUMBER);
            View rootView = null;
            if (tabCount == 1) {
                rootView = inflater.inflate(R.layout.layout_register_student_info, container, false);

                etStudentFirstName = (EditText) rootView.findViewById(R.id.et_student_first_name);
                etStudentLastName = (EditText) rootView.findViewById(R.id.et_student_last_name);
                etStudentContactNumber = (EditText) rootView.findViewById(R.id.et_student_contact);
                etStudentRollNumber = (EditText) rootView.findViewById(R.id.et_student_roll_number);
                etStudentEmail = (EditText) rootView.findViewById(R.id.et_student_email);

            } else if (tabCount == 2) {
                rootView = inflater.inflate(R.layout.layout_register_student_route, container, false);
                fm = getChildFragmentManager();
                myMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.register_student_map);
                tvMapRegisterStudentInfo = (TextView) rootView.findViewById(R.id.tv_map_register_student_info);

                myMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        mMap = googleMap;
                        mMap.addMarker(new MarkerOptions().position(RegisterRoute.taibahUniversityLocation));
                        mMap.addMarker(new MarkerOptions().position(dummyPosition));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(RegisterRoute.
                                taibahUniversityLocation, 13.0f));

                        buildAndDisplayRoute(RegisterRoute.taibahUniversityLocation, dummyPosition);

                        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                            @Override
                            public void onMapLongClick(LatLng latLng) {
                                onLongClickCounter++;
                                if (onLongClickCounter == 1) {
                                    mMap.clear();
                                    tvMapRegisterStudentInfo.setText("Resolving route points...");
                                    mMap.addMarker(new MarkerOptions().position(RegisterRoute.taibahUniversityLocation));
                                    mMap.addMarker(new MarkerOptions().position(dummyPosition));
                                    mMap.addMarker(new MarkerOptions().position(latLng));
                                    LatLng[] latLngDummyList = new LatLng[]{RegisterRoute.
                                            taibahUniversityLocation, latLng, dummyPosition};
                                    buildAndDisplayRouteWithWayPoints(latLngDummyList);
                                    studentStopLatLng = latLng;
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
                        PolylineOptions polyoptions = new PolylineOptions();
                        polyoptions.color(Color.RED);
                        polyoptions.width(10);
                        polylineOptions.zIndex(90);
                        polyoptions.addAll(polylineOptions.getPoints());
                        mMap.addPolyline(polyoptions);
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

        private void buildAndDisplayRoute(LatLng startPoint, LatLng endPoint) {
            Routing routing = new Routing.Builder()
                    .travelMode(Routing.TravelMode.DRIVING)
                    .withListener(mRoutingListener)
                    .waypoints(startPoint, endPoint)
                    .build();
            routing.execute();
        }

        private void buildAndDisplayRouteWithWayPoints(LatLng[] latLngArrayWithWayPoints) {
            Routing routing = new Routing.Builder()
                    .travelMode(Routing.TravelMode.DRIVING)
                    .withListener(mRoutingListener)
                    .waypoints(latLngArrayWithWayPoints)
                    .build();
            routing.execute();
        }
    }



    public void register() {

        String username = "sdt" + firstNameStudent + rollNumberStudent.substring(rollNumberStudent.length() - 3);
        String password = lastNameStudent + rollNumberStudent.substring(rollNumberStudent.length() - 3 );

        Log.i("username", " " + username);
        Log.i("password", " " + password);

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

        Log.i("Status", "Valid" + valid);
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
        Log.i("OnResume", "OnResume");
    }

    @Override
    public void onPause() {
        super.onPause();

    }
}


