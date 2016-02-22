package com.taibah.busservice.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.taibah.busservice.R;
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

public class MapsFragment extends Fragment {

    private View convertView;
    private FragmentManager fm;
    private SupportMapFragment myMapFragment;
    private GoogleMap mMap;
    private RoutingListener mRoutingListener;

    private LatLng startPoint = new LatLng(24.546198, 39.590284);
    private LatLng endPoint = new LatLng(24.481133,39.5432913);
    private LatLng wayPoint1 = new LatLng(24.522815, 39.572929);
    private LatLng wayPoint2 = new LatLng(24.500190, 39.581002);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.maps, null);
        setHasOptionsMenu(true);
        fm = getChildFragmentManager();
        myMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map);

        myMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
//                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                LatLng currentDummyLocation = new LatLng(24.546198, 39.590284);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentDummyLocation, 13.0f));

                mMap.addMarker(new MarkerOptions().position(startPoint)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bus)));
                mMap.addMarker(new MarkerOptions().position(wayPoint1)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_stop)));
                mMap.addMarker(new MarkerOptions().position(wayPoint2)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_stop)));
                mMap.addMarker(new MarkerOptions().position(endPoint)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_finish)));

                Routing routing = new Routing.Builder()
                        .travelMode(Routing.TravelMode.DRIVING)
                        .withListener(mRoutingListener)
                        .waypoints(startPoint, wayPoint1, wayPoint2, endPoint)
                        .build();
                routing.execute();
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
                polyoptions.width(15);
                polylineOptions.zIndex(102);
                polyoptions.addAll(polylineOptions.getPoints());
                mMap.addPolyline(polyoptions);
            }

            @Override
            public void onRoutingCancelled() {

            }
        };

        return convertView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main, menu);
//        actionsMenu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_current_location:
//
//                if (mLocation != null) {
//                    mMapView.centerAt(mLocation.getLatitude(), mLocation.getLongitude(), true);
//                }
//
//                return true;
//            case R.id.action_change_map:
//                if (simpleMapView) {
//                    mMapView.setMapOptions(mSatelliteView);
//                    setActionIcon(false);
//                    simpleMapView = false;
//                } else {
//                    mMapView.setMapOptions(mStreetsView);
//                    setActionIcon(true);
//                    simpleMapView = true;
//                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setActionIcon(boolean simpleMap) {
//        MenuItem item = actionsMenu.findItem(R.id.action_change_map);
//        if (actionsMenu != null) {
//            if (simpleMap) {
//                item.setIcon(R.mipmap.ic_map_satellite);
//            } else {
//                item.setIcon(R.mipmap.ic_map_simple);
//            }
//        }
    }

}