package com.maxgestic.zerochat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.maxgestic.zerochat.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, EventListener<DocumentSnapshot> {
    private GoogleMap locationMap;
    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private String otherPersonName;
    private DocumentReference otherPersonDoc;
    private GeoPoint otherPersonLoc;
    private LatLng otherPersonCoords;
    private Marker marker;
    private static final String KEY_LOCATION = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        }
        com.maxgestic.zerochat.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent fromIntent = getIntent();
        String otherPersonID = fromIntent.getStringExtra("otherPerson");
        otherPersonName = fromIntent.getStringExtra("otherPersonName");
        otherPersonDoc = FirebaseFirestore.getInstance().collection("users").document(otherPersonID);
        otherPersonDoc.addSnapshotListener(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (locationMap != null) {
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        locationMap = googleMap;
        otherPersonDoc.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()){
                            DocumentSnapshot doc = task.getResult();
                            otherPersonLoc = doc.getGeoPoint("location");
                            if (otherPersonLoc != null) {
                                otherPersonCoords = new LatLng(otherPersonLoc.getLatitude(), otherPersonLoc.getLongitude());
                                marker = locationMap.addMarker(new MarkerOptions().position(otherPersonCoords).title(otherPersonName));
                                locationMap.moveCamera(CameraUpdateFactory.newLatLng(otherPersonCoords));
                            }
                    }
                });
        getLocationPermission();
        updateLocationUI();
        getDeviceLocation();
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                @SuppressLint("MissingPermission") Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        lastKnownLocation = task.getResult();
                        if (lastKnownLocation != null) {
                            locationMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(lastKnownLocation.getLatitude(),
                                            lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        }
                    } else {
                        Log.d("TEST", "Current location is null. Using defaults.");
                        Log.e("TEST", "Exception: %s", task.getException());
                        locationMap.moveCamera(CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                        locationMap.getUiSettings().setMyLocationButtonEnabled(false);
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        if (requestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        updateLocationUI();
    }

    @SuppressLint("MissingPermission")
    private void updateLocationUI() {
        if (locationMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                locationMap.setMyLocationEnabled(true);
                locationMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                locationMap.setMyLocationEnabled(false);
                locationMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
        //updating the other persons location maker when their location is updated in firebase
        otherPersonDoc.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()){
                        DocumentSnapshot doc = task.getResult();
                        otherPersonLoc = doc.getGeoPoint("location");
                        if (otherPersonLoc != null) {
                            //gets the lat and lang of new position and then clears map and adds new marker in new location
                            otherPersonCoords = new LatLng(otherPersonLoc.getLatitude(), otherPersonLoc.getLongitude());
                            locationMap.clear();
                            marker = locationMap.addMarker(new MarkerOptions().position(otherPersonCoords).title(otherPersonName));
                            locationMap.moveCamera(CameraUpdateFactory.newLatLng(otherPersonCoords));
                        }
                        else{
                            //if there is no location remove the marker
                            if (marker != null){
                                marker.remove();
                            }
                        }
                    }
                });
    }
}