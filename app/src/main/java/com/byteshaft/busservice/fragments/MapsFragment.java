package com.byteshaft.busservice.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.byteshaft.busservice.R;
import com.esri.android.map.MapView;

public class MapsFragment extends Fragment{

    View convertView;
    FragmentManager fm;
    MapView mMapView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.maps, null);
        fm=getChildFragmentManager();

        // Retrieve the map and initial extent from XML layout
        mMapView = (MapView) convertView.findViewById(R.id.map);
        // Enable map to wrap around date line.
        mMapView.enableWrapAround(true);

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
}
