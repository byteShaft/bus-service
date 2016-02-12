package com.byteshaft.busservice.fragments;

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

import com.byteshaft.busservice.R;
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

public class RegisterRoute extends Fragment {

    private ViewPager mViewPager;
    private View convertView;
    private SectionsPagerAdapter mSectionsPagerAdapter;

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

        return convertView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_done, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done_button:
                return true;
            default:
                return super.onOptionsItemSelected(item);
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

    public static class PlaceholderFragment extends Fragment {

        private FragmentManager fm;

        private GoogleMap mMap;
        private SupportMapFragment myMapFragment;
        private RoutingListener mRoutingListener;

        private LatLng taibahUniversityLocation = new LatLng(24.481778, 39.545373);
        private LatLng pointA, pointB;

        private int onLongClickCounter = 0;

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

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
                rootView = inflater.inflate(R.layout.layout_register_route_info, container, false);
            } else if (tabCount == 2) {
                rootView = inflater.inflate(R.layout.layout_route_register_timepicker, container, false);
            }  else if (tabCount == 3) {
                rootView = inflater.inflate(R.layout.layout_route_register_map, container, false);
                fm = getChildFragmentManager();
                myMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map1);

                myMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        mMap = googleMap;
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(taibahUniversityLocation, 13.0f));

                        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                            @Override
                            public void onMapLongClick(LatLng latLng) {
                                onLongClickCounter++;
                                Log.i("Long Click Counter", "" + onLongClickCounter);
                                if (onLongClickCounter == 1) {
                                    mMap.addMarker(new MarkerOptions().position(latLng));
                                    pointA = latLng;
                                } else if (onLongClickCounter == 2) {
                                    mMap.addMarker(new MarkerOptions().position(latLng));
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
                    PolylineOptions polyoptions = new PolylineOptions();
                    polyoptions.color(Color.RED);
                    polyoptions.width(15);
                    polylineOptions.zIndex(102);
                    polyoptions.addAll(polylineOptions.getPoints());
                    mMap.addPolyline(polyoptions);
                }

                @Override
                public void onRoutingCancelled() {

                }
            };
            return rootView;
        }
    }
}
