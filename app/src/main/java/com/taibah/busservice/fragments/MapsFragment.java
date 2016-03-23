package com.taibah.busservice.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.taibah.busservice.R;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.DriverService;
import com.taibah.busservice.utils.Helpers;

import org.json.JSONException;
import org.json.JSONObject;

public class MapsFragment extends Fragment {

    public static boolean mapsFragmentOpen;
    public static LinearLayout layoutRouteMapInfoStrip;
    public static Marker driverLocationMarker;
    private static GoogleMap mMap = null;
    private static boolean isZooming;
    private static TextView tvDriverCurrentSpeed;
    private static TextView tvDriverCurrentLocationTimeStamp;
    private static float previousZoomLevel = -1.0f;
    private View convertView;
    private FragmentManager fm;
    private SupportMapFragment myMapFragment;
    private RoutingListener mRoutingListener;
    private LocationManager mLocationManager;
    private LatLng currentLatLngAuto = null;

    private LatLng startPoint;
    private LatLng endPoint;
    private LatLng wayPoint1 = new LatLng(24.522815, 39.572929);
    private LatLng wayPoint2 = new LatLng(24.500190, 39.581002);
    private Menu actionsMenu;
    private Boolean simpleMapView = true;

    public static void addDriverLocationMarker() {
        if (DriverService.driverLocationReportingServiceIsRunning && mapsFragmentOpen) {
            layoutRouteMapInfoStrip.setVisibility(View.VISIBLE);
            MarkerOptions a = new MarkerOptions();
            a.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bus_location));
            a.position(DriverService.driverLastKnownLocation);
            driverLocationMarker = mMap.addMarker(a);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(DriverService.driverLastKnownLocation, 16.0f));
        }
    }

    public static void updateDriverLocation() {
        if (driverLocationMarker != null) {
            driverLocationMarker.setPosition(DriverService.driverCurrentLocation);
            tvDriverCurrentSpeed.setText("Speed: " + DriverService.driverCurrentSpeedInKilometers + " Km/h");
            if (!isZooming) {
                mMap.animateCamera(CameraUpdateFactory.newLatLng(DriverService.driverCurrentLocation));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.maps, null);
        setHasOptionsMenu(true);
        fm = getChildFragmentManager();

        tvDriverCurrentSpeed = (TextView) convertView.findViewById(R.id.tv_route_driver_speed);
        tvDriverCurrentLocationTimeStamp = (TextView) convertView.findViewById(R.id.tv_route_driver_location_timestamp);

        layoutRouteMapInfoStrip = (LinearLayout) convertView.findViewById(R.id.layout_route_map_info_strip);

        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        myMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map);

        try {
            JSONObject jsonObject = new JSONObject(AppGlobals.getStudentDriverRouteID());
            startPoint = new LatLng(Double.parseDouble(jsonObject.getString("start_latitude")), Double.parseDouble(jsonObject.getString("start_longitude")));
            endPoint = new LatLng(Double.parseDouble(jsonObject.getString("end_latitude")), Double.parseDouble(jsonObject.getString("end_longitude")));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        myMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;

                if (AppGlobals.getUserType() == 1 || (AppGlobals.getUserType() == 2 && !DriverService.driverLocationReportingServiceIsRunning)) {
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    mMap.getUiSettings().setCompassEnabled(true);
                }

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 12.0f));

                mMap.addMarker(new MarkerOptions().position(startPoint)
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_marker_a)));
                mMap.addMarker(new MarkerOptions().position(wayPoint1)
                        .icon(BitmapDescriptorFactory.fromBitmap(Helpers.getMarkerBitmapFromView("Name1", getActivity()))));
                mMap.addMarker(new MarkerOptions().position(wayPoint2)
                        .icon(BitmapDescriptorFactory.fromBitmap(Helpers.getMarkerBitmapFromView("Name2", getActivity()))));
                mMap.addMarker(new MarkerOptions().position(endPoint)
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_marker_b)));

                Routing routing = new Routing.Builder()
                        .travelMode(Routing.TravelMode.DRIVING)
                        .withListener(mRoutingListener)
                        .waypoints(startPoint, wayPoint1, wayPoint2, endPoint)
                        .build();
                routing.execute();

                if (AppGlobals.getUserType() == 2) {
                    tvDriverCurrentLocationTimeStamp.setVisibility(View.GONE);
                }

                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        return true;
                    }
                });

                mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        if (previousZoomLevel != cameraPosition.zoom) {
                            isZooming = true;
                        } else {
                            isZooming = false;
                        }
                        previousZoomLevel = cameraPosition.zoom;
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
                if (AppGlobals.getUserType() == 2) {
                    addDriverLocationMarker();
                }
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
        mapsFragmentOpen = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapsFragmentOpen = true;
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
                if (!Helpers.isAnyLocationServiceAvailable()) {
                    Toast.makeText(getActivity(), "Error: Location Service disabled", Toast.LENGTH_SHORT).show();
                } else if (AppGlobals.getUserType() == 1) {
                    if (mMap != null) {
                        currentLatLngAuto = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLngAuto, 16.0f));
                    } else {
                        Toast.makeText(getActivity(), "Error: Location unavailable", Toast.LENGTH_SHORT).show();
                    }
                } else if (AppGlobals.getUserType() == 2) {
                    isZooming = true;
                    if (DriverService.driverLocationReportingServiceIsRunning && DriverService.driverCurrentLocation != null) {
                        CameraPosition cameraPosition =
                            new CameraPosition.Builder()
                                    .target(DriverService.driverCurrentLocation)
                                    .zoom(16.0f)
                                    .build();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), new GoogleMap.CancelableCallback() {
                            @Override
                            public void onFinish() {
                                isZooming = false;
                            }

                            @Override
                            public void onCancel() {
                                isZooming = false;
                            }
                        });
                    } else if (!DriverService.driverLocationReportingServiceIsRunning && mMap != null) {
                        isZooming = true;
                        currentLatLngAuto = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
                        CameraPosition cameraPosition =
                                new CameraPosition.Builder()
                                        .target(currentLatLngAuto)
                                        .zoom(16.0f)
                                        .build();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), new GoogleMap.CancelableCallback() {
                            @Override
                            public void onFinish() {
                                isZooming = false;
                            }

                            @Override
                            public void onCancel() {
                                isZooming = false;
                            }
                        });
                    } else {
                        Toast.makeText(getActivity(), "Error: Location unavailable", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            case R.id.action_change_map:
                if (simpleMapView) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    setActionIcon(false);
                    simpleMapView = false;
                } else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
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