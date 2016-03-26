package com.taibah.busservice.utils;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.taibah.busservice.fragments.MapsFragment;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DriverService extends Service implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static boolean driverLocationReportingServiceIsRunning;
    public static int responseCode;
    public static int onLocationChangedCounter = 0;

    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    HttpURLConnection connection;
    public static String driverCurrentSpeedInKilometers;
    public static LatLng driverCurrentLocation = null;
    public static LatLng driverLastKnownLocation = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connectGoogleApiClient();
        driverLocationReportingServiceIsRunning = true;
        new UpdateRouteStatus(getApplicationContext()).execute("status=1");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        driverLocationReportingServiceIsRunning = false;
        stopLocationService();
        DriverService.onLocationChangedCounter = 0;
        new UpdateRouteStatus(getApplicationContext()).execute("status=0");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location tempLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        driverLastKnownLocation = new LatLng(tempLocation.getLatitude(), tempLocation.getLongitude());
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("On Location Changed", "Called");
        onLocationChangedCounter++;
        if (onLocationChangedCounter == 1) {
            new DriverLocationPosterTask().execute();
        }
        driverCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        driverCurrentSpeedInKilometers = String.valueOf((int) ((location.getSpeed() * 3600) / 1000));
        if (MapsFragment.mapsFragmentOpen && driverCurrentLocation != null) {
            MapsFragment.updateDriverLocation();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "Failed to start Location Service", Toast.LENGTH_LONG).show();
    }

    private void connectGoogleApiClient() {
        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    protected void createLocationRequest() {
        long INTERVAL = 0;
        long FASTEST_INTERVAL = 0;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    public void stopLocationService() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    private class DriverLocationPosterTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                URL url = new URL("http://46.101.75.194:8080/locations/set");

                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("X-Api-Key", AppGlobals.getToken());

                Time today = new Time(Time.getCurrentTimezone());
                today.setToNow();

                String timeStamp = today.monthDay + "/" + today.month + "/" + today.year + " - " + today.format("%k:%M:%S");

                DataOutputStream out = new DataOutputStream(connection.getOutputStream());

                String driverSpeedWithTimeStamp = driverCurrentSpeedInKilometers + "   " + timeStamp;
                out.writeBytes("speed=" + driverSpeedWithTimeStamp +
                        "&" + "latitude=" + driverCurrentLocation.latitude +
                        "&" + "longitude=" + driverCurrentLocation.longitude);
                out.flush();
                out.close();
                responseCode = connection.getResponseCode();

                InputStream in = (InputStream) connection.getContent();
                int ch;
                StringBuilder sb;

                sb = new StringBuilder();
                while ((ch = in.read()) != -1)
                    sb.append((char) ch);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (responseCode == 200 && DriverService.driverLocationReportingServiceIsRunning) {
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new DriverLocationPosterTask().execute();
                    }
                }, 4000);
            }
        }
    }
}
