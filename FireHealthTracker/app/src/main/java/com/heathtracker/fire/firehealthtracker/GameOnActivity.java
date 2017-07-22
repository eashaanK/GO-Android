package com.heathtracker.fire.firehealthtracker;

import android.app.Activity;
import android.app.Dialog;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by eashaan on 3/29/17.
 */

public class GameOnActivity extends Activity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    //
    GoogleMap mGoogleMap;
    GoogleApiClient mGoogleApiClient;
    Location myCurrentLocation = null;
    Circle circle;
    Polyline myPolyLine;
    Marker startMarker;
    ArrayList<Location> lastLocations = new ArrayList<Location>();
    ArrayList<LatLng> points = new ArrayList<LatLng>();
    GameEventsGenerator eventsGenerator = new GameEventsGenerator();
    TextView totalDistanceText, nextDistanceGoal, distanceProgressMade;
    double currentTargetDistance = 0, lastSavedDistance = 0;
    long starttime = 0;

    private final DecimalFormat df = new DecimalFormat("0.0");

    private final static int REQUEST_ID = 2132;
    private final static int UPDATE_INT_MILLI = 2000;
    private final static int UPDATE_INT_FASTEST_MILLI = 1000;

    private final static int DIST_UPDATE_LINE_PATH = 5;
    private final static float ZOOM_LEVEL = 18f;

    private final static int DOT_COLOR = Color.argb(200, 255, 0, 0);
    private final static int PATH_COLOR = Color.argb(100, 255, 0, 0);

    private final static int START_FLAG_SIZE = 75;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_on);

        totalDistanceText = (TextView) findViewById(R.id.totalDistanceText);
        nextDistanceGoal = (TextView) findViewById(R.id.nextDistanceGoal);
        distanceProgressMade = (TextView) findViewById(R.id.distaceProgressMade);

        starttime = System.currentTimeMillis();

        if (!googleServicesAvailable()) {
            Toast.makeText(this, "No Google Maps Layout", Toast.LENGTH_LONG).show();
            return;
        }
        /*Toast.makeText(this, "Google Maps available", Toast.LENGTH_LONG).show();*/
        initMap();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

    }

    /**
     * Activities Start Method
     */
    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    /**
     * Activities Stop Method
     */
    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Activities Pause Method
     */
    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    /**
     * Activities Resume Method
     */
    @Override
    protected void onResume() {
        super.onResume();
        if(mGoogleApiClient.isConnected()){
            startLocationUpdates();
        }
    }

    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
    }

    protected void stopLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    public boolean googleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Cant connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
    }

    /**
     * Goes to provided location
     * @param lat latitude
     * @param lng longitude
     * @param zoom zoom amount
     */
    protected void goToLocation(double lat, double lng, float zoom) {
        LatLng ll = new LatLng(lat, lng);
       /* CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
        mGoogleMap.moveCamera(update);*/
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(ll).zoom(zoom).build();
        mGoogleMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));
    }

    /**
     * Google Api For fetching current location
     * @param bundle Info
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onConnected(@Nullable Bundle bundle) {
       /* Toast.makeText(this, "Google API Connected", Toast.LENGTH_LONG).show(); */
        startLocationUpdates();

    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
    }

    /**
     * Creates Location request
     * @return LocationRequest object
     */
    protected LocationRequest createLocationRequest(){
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INT_MILLI);
        mLocationRequest.setFastestInterval(UPDATE_INT_FASTEST_MILLI);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult (LocationSettingsResult result){
                final Status status = result.getStatus();
                final LocationSettingsStates states = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    GameOnActivity.this,
                                    REQUEST_ID);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.

                        break;
                }
            }
        });
        return mLocationRequest;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID: {
                for( int i = 0; i < permissions.length; i++ ) {
                    if( grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("Permissions", "Permission Granted: " + permissions[i]);
                    } else if( grantResults[i] == PackageManager.PERMISSION_DENIED ) {
                        Log.d( "Permissions", "Permission Denied: " + permissions[i] );
                    }
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Google API Suspended", Toast.LENGTH_LONG).show();
    }

    /**
     * Failed listener for Google Api
     * @param connectionResult Connection result...I guess
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Google API Failed", Toast.LENGTH_LONG).show();
    }

    /**
     * Location listener update received
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        //Toast.makeText(this, "Distance traveled: " + getTotalDistance(), Toast.LENGTH_LONG).show();
        myCurrentLocation = location;
        if(myCurrentLocation == null) {
            return;
        }

        lastLocations.add(myCurrentLocation);
        goToLocation(myCurrentLocation.getLatitude(), myCurrentLocation.getLongitude(), ZOOM_LEVEL);
        LatLng latLng = new LatLng(myCurrentLocation.getLatitude(), myCurrentLocation.getLongitude());
        // Only add more lines if moved far enough and has > 2 points
        if(lastLocations.get(0).distanceTo(lastLocations.get(lastLocations.size() - 1)) > DIST_UPDATE_LINE_PATH ||
                points.size() < 2) {
            points.add(latLng);
        }
        else {
            points.set(points.size() - 1, latLng);
        }
        // Start marker
        if(lastLocations.size() == 1){
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current Position");
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("start_flag", START_FLAG_SIZE, START_FLAG_SIZE)));
            startMarker = mGoogleMap.addMarker(markerOptions);
        }
        // Vertex markers
        if(myPolyLine == null){
            PolylineOptions lineOptions = new PolylineOptions();
            lineOptions.addAll(points);
            lineOptions.width(5);
            lineOptions.color(PATH_COLOR);
            lineOptions.zIndex(0);
            myPolyLine = mGoogleMap.addPolyline(lineOptions);
        }
        else{
            myPolyLine.setPoints(points);
        }
        // Current marker
        if(circle == null) {
            CircleOptions options = new CircleOptions().center(latLng).radius(2).fillColor(DOT_COLOR).zIndex(1);
            options.strokeWidth(0);

            circle = mGoogleMap.addCircle(options);
        }
        else{
            circle.setCenter(new LatLng(myCurrentLocation.getLatitude(), myCurrentLocation.getLongitude()));
        }
        // Target distance
        if(currentTargetDistance <= 0){
            currentTargetDistance = eventsGenerator.generateNextTargetDistance();
        }
        else if(currentTargetDistance <= getProgressMadeTowardGoal()){
          reachedCheckpoint(latLng);
        }

        //UI
        totalDistanceText.setText("Distance Traveled: " + (int)getTotalDistance() + " m");
        nextDistanceGoal.setText("Goal: " + (int)currentTargetDistance + " m");
        distanceProgressMade.setText("Progress: " + (int)(getProgressMadeTowardGoal()) + " m");
    }

    public void reachedCheckpoint(LatLng latLng){
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Time: " + getCurrentTime());
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("finish_flag", START_FLAG_SIZE, START_FLAG_SIZE)));
        Marker checkpoint = mGoogleMap.addMarker(markerOptions);
        checkpoint.showInfoWindow();
        currentTargetDistance = eventsGenerator.generateNextTargetDistance();
        lastSavedDistance = getTotalDistance();
    }

    public String getCurrentTime() {
        long millis = System.currentTimeMillis() - starttime;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds     = seconds % 60;

        return (String.format("%d:%02d", minutes, seconds));
    }

    public double getProgressMadeTowardGoal(){
        return getTotalDistance() - lastSavedDistance;
    }

    public Bitmap resizeMapIcons(String iconName, int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName, "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }

    public double getTotalDistance(){
        if(lastLocations.size() <= 1){
            return 0;
        }
        double ans = lastLocations.get(0).distanceTo(lastLocations.get(1));
        for(int i = 2; i < lastLocations.size() - 1; i++){
            ans += lastLocations.get(i).distanceTo(lastLocations.get(i+1));
        }
        ans = Double.parseDouble(df.format(ans));
        return ans;
    }

    /**
     * Generates random location within a given radius
     * @param point original longitude
     * @param radius
     * @return
     */
    public LatLng generateRandomLocationNearby(LatLng point, int radius) {

       /* List<LatLng> randomPoints = new ArrayList<>();
        List<Float> randomDistances = new ArrayList<>();*/
        Location myLocation = new Location("");
        myLocation.setLatitude(point.latitude);
        myLocation.setLongitude(point.longitude);

            double x0 = point.latitude;
            double y0 = point.longitude;

            Random random = new Random((long)(Math.random() * Integer.MAX_VALUE));

            // Convert radius from meters to degrees
            double radiusInDegrees = radius / 111000f;

            double u = random.nextDouble();
            double v = random.nextDouble();
            double w = radiusInDegrees * Math.sqrt(u);
            double t = 2 * Math.PI * v;
            double x = w * Math.cos(t);
            double y = w * Math.sin(t);

            // Adjust the x-coordinate for the shrinking of the east-west distances
            double new_x = x / Math.cos(y0);

            double foundLatitude = new_x + x0;
            double foundLongitude = y + y0;
            LatLng randomLatLng = new LatLng(foundLatitude, foundLongitude);
            //randomPoints.add(randomLatLng);
            /*Location l1 = new Location("");
            l1.setLatitude(randomLatLng.latitude);
            l1.setLongitude(randomLatLng.longitude);*/
            //randomDistances.add(l1.distanceTo(myLocation));

        return randomLatLng;
    }

}