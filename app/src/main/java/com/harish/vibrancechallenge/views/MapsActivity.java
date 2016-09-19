package com.harish.vibrancechallenge.views;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.harish.vibrancechallenge.R;
import com.harish.vibrancechallenge.services.GeofenceTransitionsIntentService;

import java.util.ArrayList;
import java.util.List;

//fused location provider api

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private static String TAG = "MapsActivity-->";
    protected ArrayList<Geofence> mGeofenceList = new ArrayList<Geofence>();
    private Marker myMarker;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private List<LatLng> latLngList = new ArrayList<>();
    private LatLng lastLocation;
    private int DISTANCE = 50;
    public static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        init();


        if (ActivityCompat.checkSelfPermission(getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);

            return;
        }

    }

    private void init() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buildGoogleApiClient();
        initPlacesFragment();
    }

    private void initPlacesFragment() {
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                createGeofences(String.valueOf(place.getName()), place.getLatLng(), 50);
                getGeofencingRequest();

                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, mGeofenceList,
                        getGeofencePendingIntent());
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName());
                Log.i(TAG, "location: " + place.getLatLng());
                LatLng selectePlaceLatLng = place.getLatLng();
                MarkerOptions marker = new MarkerOptions().position(selectePlaceLatLng).title((String) place.getName())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.hut_icon));
                mMap.addMarker(marker);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(selectePlaceLatLng));
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                double roundOff = Math.round(distance(marker.getPosition(), lastLocation) * 1000.0) / 1000.0;
                marker.setSnippet("distance in km : " + roundOff);
                return false;
            }
        });
    }

    public void createGeofences(String placeName, LatLng latLng, float distance) {
        double latitude = latLng.latitude;
        double longitude = latLng.longitude;

        String id = placeName;
        Geofence fence = new Geofence.Builder()
                .setRequestId(id)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setCircularRegion(latitude, longitude, distance) // Try changing your radius
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
        mGeofenceList.add(fence);

        drawCircle(latitude, longitude, distance);

    }

    private void drawCircle(double latitude, double longitude, float distance) {
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(latitude, longitude))
                .radius(distance)
                .fillColor(0x40ff0000)
                .strokeColor(Color.BLUE)
                .strokeWidth(2);
        mMap.addCircle(circleOptions);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

       locationInit();
    }

    private void initOrUpdateMarker(LatLng latLng) {
        if (myMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Current Location").snippet("" + latLng.latitude + "," + latLng.longitude).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location_blue_500_24dp));
            myMarker = mMap.addMarker(markerOptions);
            CameraUpdate location = CameraUpdateFactory.newLatLngZoom(latLng, 17);
            mMap.animateCamera(location);
            drawPath(latLng);
        } else {
            myMarker.setPosition(latLng);
        }
        drawPath(latLng);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("location----->", "" + location.getLatitude() + "##" + location.getLongitude());
        lastLocation = new LatLng(location.getLatitude(), location.getLongitude());

        initOrUpdateMarker(lastLocation);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        buildGoogleApiClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    private void drawPath(LatLng latLng) {
        latLngList.add(latLng);
        mMap.addPolyline((new PolylineOptions())
                .addAll(latLngList)
                .width(5).color(Color.BLUE)
                .geodesic(true));
    }

    private double distance(LatLng from, LatLng to) {
        double lat1 = from.latitude;
        double lon1 = from.longitude;
        double lat2 = to.latitude;
        double lon2 = to.longitude;
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        return;
                    }
                    locationInit();

                } else {

                }
                return;
            }

        }
    }
    private void locationInit()
    {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(100); // Update location every second
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            Log.d("location----->", "" + mLastLocation.getLatitude() + "##" + mLastLocation.getLongitude());
            lastLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            initOrUpdateMarker(lastLocation);
        }
    }
}
