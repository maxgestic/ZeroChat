package com.maxgestic.zerochat;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

public class GPS extends Service {

    FusedLocationProviderClient locationProviderClient;
    DocumentReference docRef;
    LocationRequest locationRequest;
    String userID;
    LocationCallback locationCallback;

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //get userID from intent
        userID = intent.getStringExtra("userID");
        //get document reference to be able to store location
        docRef = FirebaseFirestore.getInstance().collection("users").document(userID);
        //create location Provider and get initial location once
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationProviderClient.getLastLocation()
                .addOnSuccessListener(getApplicationContext().getMainExecutor(), location -> {
                    if (location != null){
                        GeoPoint geo = new GeoPoint(location.getLatitude(), location.getLongitude());
                        docRef.update("location", geo);
                    }
                });

        //create parameters for location request
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //set up the callback for location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    //push new location to firebase
                    GeoPoint geo = new GeoPoint(location.getLatitude(), location.getLongitude());
                    docRef.update("location", geo);
                }
                super.onLocationResult(locationResult);
            }
        };
        //start the location updates
        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        locationProviderClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
