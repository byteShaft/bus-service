package com.taibah.busservice.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
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
import com.taibah.busservice.Helpers.WebServiceHelpers;
import com.taibah.busservice.R;
import com.taibah.busservice.gcm.MyGcmListenerService;
import com.taibah.busservice.utils.AppGlobals;
import com.taibah.busservice.utils.DriverService;
import com.taibah.busservice.utils.Helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsFragment extends Fragment {

    public static boolean mapsFragmentOpen;
    public static LinearLayout layoutRouteMapInfoStrip;
    public static Marker driverLocationMarker;
    public static LatLng latLngDriverForStudent;
    public static String locationSpeedAndTimeStampForStudent;
    static int responseCode;
    private static GoogleMap mMap = null;
    private static boolean isZooming;
    private static TextView tvDriverCurrentSpeed;
    private static TextView tvDriverCurrentLocationTimeStamp;
    private static float previousZoomLevel = -1.0f;
    private static boolean isNetworkNotAvailable = true;
    public int locationRetrievalCounterForStudent = 0;
    public GetDriverLocationTask driverLocationTask;
    public RetrieveStudentsRegisteredAgainstRoute retrieveStudentsTask;
    int routeStatus;
    HttpURLConnection connection;
    ArrayList<Integer> studentIdsList;
    HashMap<Integer, ArrayList<String>> hashMapStudentData;
    private View convertView;
    private FragmentManager fm;
    private SupportMapFragment myMapFragment;
    private RoutingListener mRoutingListener;
    private LatLng currentLatLngAuto;
    private LatLng startPoint;
    private LatLng endPoint;
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

    public static void addDriverLocationMarkerForStudent() {
        MarkerOptions a = new MarkerOptions();
        a.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bus_location));
        a.position(latLngDriverForStudent);
        driverLocationMarker = mMap.addMarker(a);
        isZooming = true;
        CameraPosition cameraPosition =
                new CameraPosition.Builder()
                        .target(latLngDriverForStudent)
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
    }

    public static void updateDriverLocationForStudent() {
        if (driverLocationMarker != null) {
            driverLocationMarker.setPosition(latLngDriverForStudent);
            System.out.println("string: " + locationSpeedAndTimeStampForStudent);
            layoutRouteMapInfoStrip.setVisibility(View.VISIBLE);
            tvDriverCurrentSpeed.setText("Speed: " + locationSpeedAndTimeStampForStudent.substring(0, 2) + " Km/h");
            tvDriverCurrentLocationTimeStamp.setText(locationSpeedAndTimeStampForStudent.substring(locationSpeedAndTimeStampForStudent.length() - 22));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        convertView = inflater.inflate(R.layout.maps, null);
        setHasOptionsMenu(true);
        fm = getChildFragmentManager();
        retrieveStudentsTask = (RetrieveStudentsRegisteredAgainstRoute) new RetrieveStudentsRegisteredAgainstRoute().execute();

        studentIdsList = new ArrayList<>();
        hashMapStudentData = new HashMap<>();

        tvDriverCurrentSpeed = (TextView) convertView.findViewById(R.id.tv_route_driver_speed);
        tvDriverCurrentLocationTimeStamp = (TextView) convertView.findViewById(R.id.tv_route_driver_location_timestamp);

        layoutRouteMapInfoStrip = (LinearLayout) convertView.findViewById(R.id.layout_route_map_info_strip);

        myMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map);

        try {
            JSONArray jsonArray = new JSONArray(AppGlobals.getStudentDriverRouteDetails());
            JSONObject jsonObject = jsonArray.getJSONObject(0);
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
        latLngDriverForStudent = null;
        mapsFragmentOpen = false;
        if (!retrieveStudentsTask.isCancelled()) {
            retrieveStudentsTask.cancel(true);
        }
        if (AppGlobals.getUserType() == 1) {
            driverLocationTask = new GetDriverLocationTask();
            if (driverLocationTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
                driverLocationTask.cancel(true);
            }
        }
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
                    if (mMap != null && latLngDriverForStudent != null) {
                        isZooming = true;
                        currentLatLngAuto = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
                        CameraPosition cameraPosition =
                                new CameraPosition.Builder()
                                        .target(latLngDriverForStudent)
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
                        Toast.makeText(getActivity(), "Driver Location unavailable", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getActivity(), "Location not available at the moment", Toast.LENGTH_SHORT).show();
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

    private void buildAndDisplayRouteWithWayPoints(List<LatLng> latLngArrayWithWayPoints) {
        Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.DRIVING)
                .withListener(mRoutingListener)
                .waypoints(latLngArrayWithWayPoints)
                .routeMode(Routing.RouteMode.FASTEST)
                .build();
        routing.execute();
    }

    private class RetrieveStudentsRegisteredAgainstRoute extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Retrieving route");
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isNetworkAvailable() && Helpers.isInternetWorking()) {
                try {
                    JSONArray userDataJsonArray = new JSONArray(AppGlobals.getStudentDriverRouteDetails());
                    Log.i("userDataJsonArray", "" + userDataJsonArray);
                    JSONObject userDataJsonObject = userDataJsonArray.getJSONObject(0);
                    JSONArray timingsArray = new JSONArray(userDataJsonObject.getString("timings"));
                    JSONObject timingsObject = timingsArray.getJSONObject(0);
                    String timingID = timingsObject.getString("id");
                    connection = WebServiceHelpers.openConnectionForUrl
                            ("http://46.101.75.194:8080/timings/" + timingID, "GET");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connection.connect();
                    responseCode = connection.getResponseCode();

                    String data = WebServiceHelpers.readResponse(connection);
                    JSONObject jsonObject = new JSONObject(data);
                    Log.i("jsonObject", "" + jsonObject);

                    JSONArray studentIdsArray = new JSONArray(jsonObject.getString("students"));

                    for (int i = 0; i < studentIdsArray.length(); i++) {
                        JSONObject jsonObjectArray = studentIdsArray.getJSONObject(i);
                        if (!studentIdsList.contains(jsonObjectArray.getInt("id")) && !jsonObjectArray.getString("attending").equals("0")
                                && !jsonObjectArray.getString("allowed").equals("0")) {
                            studentIdsList.add(jsonObjectArray.getInt("id"));
                            ArrayList<String> arrayListString = new ArrayList<>();
                            arrayListString.add(jsonObjectArray.getString("first_name"));
                            arrayListString.add(jsonObjectArray.getString("latitude"));
                            arrayListString.add(jsonObjectArray.getString("longitude"));
                            hashMapStudentData.put(jsonObjectArray.getInt("id"), arrayListString);
                        }
                    }

                    connection = WebServiceHelpers.openConnectionForUrl
                            ("http://46.101.75.194:8080/timings/" + timingID, "GET");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connection.connect();
                    responseCode = connection.getResponseCode();

                    String data1 = WebServiceHelpers.readResponse(connection);
                    JSONObject jsonObject1 = new JSONObject(data1);

                    routeStatus = Integer.parseInt(jsonObject1.getString("status"));

                    isNetworkNotAvailable = false;
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                } catch (NumberFormatException nE) {
                    routeStatus = 0;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Helpers.dismissProgressDialog();
            if (responseCode == 200) {
                mMap.clear();

                mMap.addMarker(new MarkerOptions().position(startPoint)
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_marker_a)));

                mMap.addMarker(new MarkerOptions().position(endPoint)
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_marker_b)));

                List<LatLng> studentStops = new ArrayList<>();
                studentStops.add(startPoint);

                for (int i = 0; i < studentIdsList.size(); i++) {
                    LatLng stop = new LatLng(Double.parseDouble(hashMapStudentData.get(studentIdsList.get(i)).get(1)), Double.parseDouble(hashMapStudentData.get(studentIdsList.get(i)).get(2)));
                    mMap.addMarker(new MarkerOptions().position(stop)
                            .icon(BitmapDescriptorFactory.fromBitmap(Helpers.getMarkerBitmapFromView(hashMapStudentData.get(studentIdsList.get(i)).get(0), getActivity()))));
                    studentStops.add(stop);
                }

                studentStops.add(endPoint);

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 12.0f));

                buildAndDisplayRouteWithWayPoints(studentStops);
                if (AppGlobals.getUserType() == 1) {
                    if (routeStatus == 1) {
                        if (MyGcmListenerService.studentStatusChanged) {
                            Toast.makeText(getActivity(), "Students list updated", Toast.LENGTH_LONG).show();
                            mMap.clear();
                            retrieveStudentsTask.execute();
                            MyGcmListenerService.studentStatusChanged = false;
                        } else {
                            driverLocationTask = (GetDriverLocationTask) new GetDriverLocationTask().execute();
                        }
                    }
                }
                isNetworkNotAvailable = true;
            } else {
                Toast.makeText(getActivity(), "Error: Server not responding. Check internet", Toast.LENGTH_LONG).show();
                getActivity().onBackPressed();
            }
        }
    }

    private class GetDriverLocationTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (Helpers.isNetworkAvailable() && Helpers.isInternetWorking()) {
                try {
                    JSONObject jsonObject = new JSONObject(AppGlobals.getStudentDriverRouteDetails());
                    String ID = jsonObject.getString("id");
                    connection = WebServiceHelpers.openConnectionForUrl
                            ("http://46.101.75.194:8080/locations/get?route_id=" + ID, "GET");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connection.connect();
                    responseCode = connection.getResponseCode();
                    System.out.print(responseCode);
                    String data = WebServiceHelpers.readResponse(connection);
                    JSONObject jsonObject1 = new JSONObject(data);
                    latLngDriverForStudent = new LatLng(Double.parseDouble(jsonObject1.getString("latitude"))
                            , Double.parseDouble(jsonObject1.getString("longitude")));
                    locationSpeedAndTimeStampForStudent = jsonObject1.getString("speed");
                    connection = WebServiceHelpers.openConnectionForUrl
                            ("http://46.101.75.194:8080/routes/" + ID, "GET");
                    connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());
                    connection.connect();
                    responseCode = connection.getResponseCode();

                    String data1 = WebServiceHelpers.readResponse(connection);
                    JSONObject jsonObjectRouteStatus = new JSONObject(data1);
                    routeStatus = Integer.parseInt(jsonObjectRouteStatus.getString("status"));

                    AppGlobals.putRouteStatus(routeStatus);
                    isNetworkNotAvailable = false;
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (isNetworkNotAvailable) {
                Toast.makeText(getActivity(), "Error: Server not responding. Check internet", Toast.LENGTH_LONG).show();
                layoutRouteMapInfoStrip.setVisibility(View.INVISIBLE);
                getActivity().onBackPressed();
            } else if (responseCode == 200) {
                if (latLngDriverForStudent != null) {
                    if (locationRetrievalCounterForStudent < 1) {
                        addDriverLocationMarkerForStudent();
                        updateDriverLocationForStudent();
                        locationRetrievalCounterForStudent++;
                    } else {
                        updateDriverLocationForStudent();
                    }
                }

                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!mapsFragmentOpen) {
                            cancel(true);
                        }
                        if (routeStatus == 1) {
                            driverLocationTask = (GetDriverLocationTask) new GetDriverLocationTask().execute();
                        } else if (routeStatus == 0 && locationRetrievalCounterForStudent > 0) {
                            Toast.makeText(AppGlobals.getContext(), "Route Stopped", Toast.LENGTH_LONG).show();
                            if (mapsFragmentOpen) {
                                layoutRouteMapInfoStrip.setVisibility(View.GONE);
                                driverLocationMarker.remove();
                            }
                        } else if (routeStatus > 1 && locationRetrievalCounterForStudent > 0) {
                            Toast.makeText(AppGlobals.getContext(), "Route Unavailable", Toast.LENGTH_LONG).show();
                            if (mapsFragmentOpen) {
                                layoutRouteMapInfoStrip.setVisibility(View.GONE);
                                getActivity().onBackPressed();
                            }
                        }
                        isNetworkNotAvailable = true;
                    }
                }, 4000);
            }
        }
    }

}