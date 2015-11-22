package com.byteshaft.busservice.fragments;

import android.location.Location;
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
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapOptions;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnStatusChangedListener;


public class MapsFragment extends Fragment {

    View convertView;
    FragmentManager fm;
    MapView mMapView = null;
    Location mLocation = null;
    private Menu actionsMenu;
    private boolean simpleMapView = true;
    private final MapOptions mStreetsView = new MapOptions(MapOptions.MapType.STREETS);
    private final MapOptions mSatelliteView = new MapOptions(MapOptions.MapType.SATELLITE);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.maps, null);
        setHasOptionsMenu(true);
        fm = getChildFragmentManager();

        // Retrieve the map and initial extent from XML layout
        mMapView = (MapView) convertView.findViewById(R.id.map);
        // Enable map to wrap around date line.
        mMapView.enableWrapAround(true);
        mMapView.setKeepScreenOn(true);

        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
            @Override
            public void onStatusChanged(Object o, STATUS status) {
                if (o == mMapView && status == STATUS.INITIALIZED) {
                    LocationDisplayManager ldm = mMapView.getLocationDisplayManager();
                    ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                    ldm.start();
                    mLocation = ldm.getLocation();
                }
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
        mMapView.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.unpause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main, menu);
        actionsMenu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_current_location:

                if (mLocation != null) {
                    mMapView.centerAt(mLocation.getLatitude(), mLocation.getLongitude(), true);
                }

                return true;
            case R.id.action_change_map:
                if (simpleMapView) {
                    mMapView.setMapOptions(mSatelliteView);
                    setActionIcon(false);
                    simpleMapView = false;
                } else {
                    mMapView.setMapOptions(mStreetsView);
                    setActionIcon(true);
                    simpleMapView = true;
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setActionIcon(boolean simpleMap) {
        MenuItem item = actionsMenu.findItem(R.id.action_change_map);
        if (actionsMenu != null) {
            if (simpleMap) {
                item.setIcon(R.mipmap.ic_map_satellite);
            } else {
                item.setIcon(R.mipmap.ic_map_simple);
            }
        }
    }

}