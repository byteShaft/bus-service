package com.byteshaft.busservice.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.byteshaft.busservice.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class MapsFragment extends Fragment {

    private View convertView;
    private FragmentManager fm;
    private SupportMapFragment myMapFragment;
    private GoogleMap mMap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.maps, null);
        setHasOptionsMenu(true);
        fm = getChildFragmentManager();
        myMapFragment=(SupportMapFragment) fm.findFragmentById(R.id.map);

        myMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;

//                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f));
            }
        });

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